package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

/**
 * Comando inteligente que coordina Intake + Spindexer para capturar una pelota.
 * 
 * FLUJO AUTOMÁTICO:
 * 1. Verificar que spindexer tenga al menos un slot vacío
 * 2. Mover spindexer al primer slot vacío (posición de intake)
 * 3. Activar intake motor
 * 4. Esperar hasta que sensor detecte pelota (distancia)
 * 5. Detener intake
 * 6. (Opcional) Detectar color y actualizar slot
 * 7. Terminar
 * 
 * MODOS DE OPERACIÓN:
 * - Con detección de color: Identifica y etiqueta la pelota
 * - Sin detección: Solo verifica presencia (más rápido)
 * 
 * ESTADOS:
 * - CHECKING: Verificando disponibilidad
 * - POSITIONING: Moviendo spindexer a slot vacío
 * - INTAKING: Motor activo, esperando pelota
 * - DETECTING: (Opcional) Detectando color
 * - DONE: Completado
 * 
 * SEGURIDAD:
 * - Timeout si pelota no llega
 * - Verifica spindexer en posición antes de activar intake
 * - Detiene intake si es interrumpido
 */
public class SmartIntakeCommand extends CommandBase {
    
    // Estados internos del comando
    private enum IntakeState {
        CHECKING,      // Verificando disponibilidad de slots
        POSITIONING,   // Moviendo spindexer
        INTAKING,      // Esperando pelota
        DETECTING,     // Detectando color (opcional)
        DONE           // Completado
    }
    
    private final IntakeSubsystem intake;
    private final SpindexerSubsystem spindexer;
    private final boolean detectColor;
    private final long intakeTimeoutMs;
    
    private IntakeState state;
    private int targetSlot;
    private long intakeStartTime;
    
    /**
     * Crea comando de intake sin detección de color.
     * Más rápido, solo verifica presencia de pelota.
     * 
     * @param intake Subsystem de intake
     * @param spindexer Subsystem de spindexer
     */
    public SmartIntakeCommand(IntakeSubsystem intake, SpindexerSubsystem spindexer) {
        this(intake, spindexer, false, 3000);
    }
    
    /**
     * Crea comando de intake con opción de detección de color.
     * 
     * @param intake Subsystem de intake
     * @param spindexer Subsystem de spindexer
     * @param detectColor true para detectar y etiquetar color
     * @param intakeTimeoutMs Timeout en ms para detección de pelota
     */
    public SmartIntakeCommand(
        IntakeSubsystem intake, 
        SpindexerSubsystem spindexer,
        boolean detectColor,
        long intakeTimeoutMs
    ) {
        this.intake = intake;
        this.spindexer = spindexer;
        this.detectColor = detectColor;
        this.intakeTimeoutMs = intakeTimeoutMs;
        
        addRequirements(intake, spindexer);
    }
    
    @Override
    public void initialize() {
        state = IntakeState.CHECKING;
        targetSlot = -1;
        intakeStartTime = 0;
    }
    
    @Override
    public void execute() {
        switch (state) {
            case CHECKING:
                handleChecking();
                break;
                
            case POSITIONING:
                handlePositioning();
                break;
                
            case INTAKING:
                handleIntaking();
                break;
                
            case DETECTING:
                // La detección se maneja en el subcomando
                // Este estado es placeholder para cuando usemos
                // un SequentialCommandGroup en versión avanzada
                break;
                
            case DONE:
                // Nada que hacer
                break;
        }
    }
    
    @Override
    public boolean isFinished() {
        return state == IntakeState.DONE;
    }
    
    @Override
    public void end(boolean interrupted) {
        // Siempre detener intake al terminar
        intake.stop();
        
        if (interrupted && targetSlot >= 0) {
            // Si fue interrumpido, marcar slot como UNKNOWN
            spindexer.setSlotState(targetSlot, SpindexerSubsystem.SlotState.UNKNOWN);
        }
    }
    
    // ==================== MANEJADORES DE ESTADO ====================
    
    private void handleChecking() {
        // Verificar si hay slot vacío
        if (!spindexer.hasEmptySlot()) {
            // No hay espacio, terminar inmediatamente
            state = IntakeState.DONE;
            return;
        }
        
        // Obtener primer slot vacío
        targetSlot = spindexer.getFirstEmptySlotIndex();
        
        if (targetSlot < 0) {
            // Esto no debería pasar, pero por seguridad
            state = IntakeState.DONE;
            return;
        }
        
        // Mover spindexer a posición de intake del slot vacío
        spindexer.moveToIntakePosition(targetSlot);
        state = IntakeState.POSITIONING;
    }
    
    private void handlePositioning() {
        // Esperar a que spindexer esté en posición
        // En un robot real, podrías verificar tiempo transcurrido
        // o usar encoders del servo si están disponibles
        
        // Por simplicidad, asumimos que después de un ciclo
        // el servo ya se está moviendo y podemos empezar intake
        if (spindexer.isAtIntake()) {
            // Activar intake
            intake.intake();
            intakeStartTime = System.currentTimeMillis();
            state = IntakeState.INTAKING;
        }
    }
    
    private void handleIntaking() {
        // Verificar si sensor detecta pelota
        if (spindexer.isBallDetected()) {
            // ¡Pelota detectada!
            intake.stop();
            
            if (detectColor) {
                // Si queremos detección de color, la versión completa
                // usaría un SequentialCommandGroup con DetectBallColorCommand
                // Por ahora, simplemente marcamos como UNKNOWN
                spindexer.setSlotState(targetSlot, SpindexerSubsystem.SlotState.UNKNOWN);
            } else {
                // Sin detección, marcamos como UNKNOWN (hay algo pero no sabemos qué)
                spindexer.setSlotState(targetSlot, SpindexerSubsystem.SlotState.UNKNOWN);
            }
            
            state = IntakeState.DONE;
            return;
        }
        
        // Verificar timeout
        long elapsed = System.currentTimeMillis() - intakeStartTime;
        if (elapsed >= intakeTimeoutMs) {
            // Timeout - pelota no llegó
            intake.stop();
            state = IntakeState.DONE;
        }
    }
    
    // ==================== MÉTODOS DE CONSULTA ====================
    
    /**
     * Verifica si el intake fue exitoso (pelota detectada).
     * Solo válido después de que el comando termine.
     */
    public boolean wasSuccessful() {
        return state == IntakeState.DONE && 
               targetSlot >= 0 && 
               !spindexer.isSlotEmpty(targetSlot);
    }
    
    /**
     * Obtiene el slot donde se guardó la pelota.
     * Retorna -1 si no se guardó nada.
     */
    public int getTargetSlot() {
        return targetSlot;
    }
}
