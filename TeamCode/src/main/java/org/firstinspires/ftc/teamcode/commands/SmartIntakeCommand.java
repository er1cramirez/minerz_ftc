package org.firstinspires.ftc.teamcode.commands;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

import java.util.function.BooleanSupplier;

/**
 * Comando inteligente de intake con ciclo automático y reinicio.
 *
 * FLUJO POR CICLO:
 * 1. Verificar que haya slot vacío disponible
 * 2. Mover spindexer a posición de intake del slot vacío
 * 3. Encender intake motor
 * 4. Esperar hasta detectar pelota (sensor de distancia)
 * 5. Apagar intake inmediatamente al detectar pelota
 * 6. Marcar slot como ocupado (UNKNOWN - sin detección de color)
 * 7. Mover al siguiente slot vacío
 * 8. Si trigger sigue activo → Reiniciar ciclo
 *    Si trigger no activo → Finalizar comando
 *
 * CARACTERÍSTICAS ESPECIALES:
 * - Reinicio automático: Mientras el trigger esté presionado, sigue ciclando
 * - Detección rápida: Apaga intake inmediatamente al detectar pelota
 * - Sin detección de color: Prioriza velocidad sobre identificación
 * - Timeout de seguridad: Si pelota no llega en X tiempo, continúa
 * - Stop automático: Si spindexer está lleno, termina aunque trigger esté activo
 *
 * USO TÍPICO:
 * ```java
 * // Bind a botón - cicla mientras esté presionado
 * operatorGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
 *     .whenPressed(new SmartIntakeCommand(
 *         intake,
 *         spindexer,
 *         operatorGamepad::getRightBumper  // Trigger
 *     ));
 * ```
 */
public class SmartIntakeCommand extends CommandBase {

    // ==================== ESTADOS INTERNOS ====================

    private enum IntakeState {
        CHECKING,       // Verificando disponibilidad de slot vacío
        POSITIONING,    // Moviendo spindexer a posición de intake
        INTAKING,       // Motor activo, esperando detección de pelota
        DETECTED,       // Pelota detectada, apagando intake
        ROTATING,       // Moviendo al siguiente slot
        WAITING,        // Esperando antes de verificar reinicio
        DONE            // Finalizado (trigger no activo o lleno)
    }

    // ==================== DEPENDENCIAS ====================

    private final IntakeSubsystem intake;
    private final SpindexerSubsystem spindexer;
    private final BooleanSupplier triggerSupplier;

    // ==================== CONFIGURACIÓN ====================

    private final long intakeTimeoutMs;        // Timeout para detectar pelota
    private final long positioningDelayMs;     // Delay para que servo llegue a posición
    private final long rotationDelayMs;        // Delay para rotación entre slots
    private final long waitBeforeRestartMs;    // Delay antes de verificar reinicio

    // ==================== ESTADO ====================

    private IntakeState currentState;
    private int targetSlot;
    private ElapsedTime stateTimer;
    private int cyclesCompleted;

    // ==================== CONSTRUCTORES ====================

    /**
     * Constructor con parámetros por defecto.
     *
     * @param intake Subsystem de intake
     * @param spindexer Subsystem de spindexer
     * @param triggerSupplier Supplier que indica si el trigger está activo (ej: botón presionado)
     */
    public SmartIntakeCommand(
            IntakeSubsystem intake,
            SpindexerSubsystem spindexer,
            BooleanSupplier triggerSupplier
    ) {
        this(intake, spindexer, triggerSupplier, 3000, 400, 300, 150);
    }

    /**
     * Constructor con parámetros personalizables.
     *
     * @param intake Subsystem de intake
     * @param spindexer Subsystem de spindexer
     * @param triggerSupplier Supplier que indica si el trigger está activo
     * @param intakeTimeoutMs Timeout para detectar pelota (ms)
     * @param positioningDelayMs Delay para que servo llegue a posición (ms)
     * @param rotationDelayMs Delay para rotación entre slots (ms)
     * @param waitBeforeRestartMs Delay antes de verificar reinicio (ms)
     */
    public SmartIntakeCommand(
            IntakeSubsystem intake,
            SpindexerSubsystem spindexer,
            BooleanSupplier triggerSupplier,
            long intakeTimeoutMs,
            long positioningDelayMs,
            long rotationDelayMs,
            long waitBeforeRestartMs
    ) {
        this.intake = intake;
        this.spindexer = spindexer;
        this.triggerSupplier = triggerSupplier;
        this.intakeTimeoutMs = intakeTimeoutMs;
        this.positioningDelayMs = positioningDelayMs;
        this.rotationDelayMs = rotationDelayMs;
        this.waitBeforeRestartMs = waitBeforeRestartMs;

        this.stateTimer = new ElapsedTime();

        addRequirements(intake, spindexer);
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void initialize() {
        currentState = IntakeState.CHECKING;
        targetSlot = -1;
        cyclesCompleted = 0;
        stateTimer.reset();
    }

    @Override
    public void execute() {
        switch (currentState) {
            case CHECKING:
                handleChecking();
                break;

            case POSITIONING:
                handlePositioning();
                break;

            case INTAKING:
                handleIntaking();
                break;

            case DETECTED:
                handleDetected();
                break;

            case ROTATING:
                handleRotating();
                break;

            case WAITING:
                handleWaiting();
                break;

            case DONE:
                // Nada que hacer, esperando isFinished()
                break;
        }
    }

    @Override
    public boolean isFinished() {
        return currentState == IntakeState.DONE;
    }

    @Override
    public void end(boolean interrupted) {
        // SIEMPRE apagar intake al terminar
        intake.stop();

        if (interrupted && targetSlot >= 0) {
            // Si fue interrumpido en medio de intake, marcar slot como UNKNOWN
            // por si acaso había algo entrando
            if (currentState == IntakeState.INTAKING) {
                spindexer.setSlotState(targetSlot, SlotState.UNKNOWN);
            }
        }
    }

    // ==================== MANEJADORES DE ESTADO ====================

    /**
     * Estado CHECKING: Verificar si hay slot vacío disponible.
     */
    private void handleChecking() {
        // Verificar si spindexer está lleno
        if (!spindexer.hasEmptySlot()) {
            // No hay espacio, terminar
            currentState = IntakeState.DONE;
            return;
        }

        // Obtener primer slot vacío
        targetSlot = spindexer.getFirstEmptySlotIndex();

        if (targetSlot < 0) {
            // Esto no debería pasar, pero por seguridad
            currentState = IntakeState.DONE;
            return;
        }

        // Mover spindexer a posición de intake del slot vacío
        spindexer.moveToIntakePosition(targetSlot);
        transitionTo(IntakeState.POSITIONING);
    }

    /**
     * Estado POSITIONING: Esperar a que servo llegue a posición.
     */
    private void handlePositioning() {
        // Esperar tiempo configurado para que servo llegue a posición
        if (stateTimer.milliseconds() >= positioningDelayMs) {
            // Verificar que realmente esté en posición de intake
            if (spindexer.isAtIntake()) {
                // Activar intake motor
                intake.intake();
                transitionTo(IntakeState.INTAKING);
            } else {
                // Si no está en posición después del delay, algo salió mal
                // Intentar de nuevo
                spindexer.moveToIntakePosition(targetSlot);
                stateTimer.reset();
            }
        }
    }

    /**
     * Estado INTAKING: Esperando detección de pelota.
     */
    private void handleIntaking() {
        // Verificar si sensor detecta pelota
        if (spindexer.isBallDetected()) {
            // ¡Pelota detectada! Apagar intake INMEDIATAMENTE
            intake.stop();

            // Marcar slot como ocupado (UNKNOWN porque no detectamos color)
            spindexer.setSlotState(targetSlot, SlotState.UNKNOWN);

            transitionTo(IntakeState.DETECTED);
            return;
        }

        // Verificar timeout
        if (stateTimer.milliseconds() >= intakeTimeoutMs) {
            // Timeout - pelota no llegó
            // Apagar intake y continuar (no bloquear el flujo)
            intake.stop();
            transitionTo(IntakeState.DETECTED);
        }
    }

    /**
     * Estado DETECTED: Pelota detectada, preparar para siguiente slot.
     */
    private void handleDetected() {
        // Incrementar contador de ciclos
        cyclesCompleted++;

        // Mover al siguiente slot vacío
        // Si no hay más slots vacíos, esto no hará nada
        int nextSlot = spindexer.getFirstEmptySlotIndex();

        if (nextSlot >= 0) {
            // Hay siguiente slot vacío, mover ahí
            spindexer.moveToIntakePosition(nextSlot);
            transitionTo(IntakeState.ROTATING);
        } else {
            // No hay más slots vacíos, spindexer está lleno
            transitionTo(IntakeState.DONE);
        }
    }

    /**
     * Estado ROTATING: Esperando a que servo complete rotación.
     */
    private void handleRotating() {
        // Esperar a que servo complete la rotación
        if (stateTimer.milliseconds() >= rotationDelayMs) {
            transitionTo(IntakeState.WAITING);
        }
    }

    /**
     * Estado WAITING: Verificar si debe reiniciar o terminar.
     */
    private void handleWaiting() {
        // Pequeño delay antes de verificar reinicio
        // (Evita reinicio inmediato si el botón se suelta muy rápido)
        if (stateTimer.milliseconds() >= waitBeforeRestartMs) {

            // Verificar si trigger sigue activo
            if (triggerSupplier.getAsBoolean()) {
                // Trigger activo → Reiniciar ciclo
                transitionTo(IntakeState.CHECKING);
            } else {
                // Trigger no activo → Finalizar
                transitionTo(IntakeState.DONE);
            }
        }
    }

    // ==================== HELPERS ====================

    /**
     * Transición a un nuevo estado con reset del timer.
     */
    private void transitionTo(IntakeState newState) {
        currentState = newState;
        stateTimer.reset();
    }

    // ==================== MÉTODOS DE CONSULTA ====================

    /**
     * Obtiene el número de ciclos completados.
     * Un ciclo = intake de una pelota.
     *
     * @return Número de ciclos completados
     */
    public int getCyclesCompleted() {
        return cyclesCompleted;
    }

    /**
     * Obtiene el slot actual donde está trabajando.
     * Retorna -1 si no hay slot target.
     *
     * @return Índice del slot actual
     */
    public int getCurrentTargetSlot() {
        return targetSlot;
    }

    /**
     * Verifica si el comando está activamente intaking.
     *
     * @return true si el motor está encendido esperando pelota
     */
    public boolean isActivelyIntaking() {
        return currentState == IntakeState.INTAKING;
    }

    /**
     * Obtiene el estado interno actual (para debugging/telemetría).
     *
     * @return Nombre del estado actual
     */
    public String getCurrentStateName() {
        return currentState.name();
    }
}