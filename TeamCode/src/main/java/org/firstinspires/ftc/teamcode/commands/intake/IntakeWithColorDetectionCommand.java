package org.firstinspires.ftc.teamcode.commands.intake;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * Comando que ejecuta UNA secuencia de intake con detección de color.
 * 
 * FLUJO:
 * 1. Activar intake y leer sensor continuamente
 * 2. Cuando se detecta y confirma pelota → etiquetar slot
 * 3. Detener intake y rotar al siguiente slot vacío
 * 4. Comando TERMINA
 * 
 * Al mantener el trigger presionado, el comando se re-ejecuta automáticamente.
 * 
 * DETECCIÓN ROBUSTA:
 * - Confirmación por distancia Y color (N lecturas consecutivas)
 * - Si timeout sin confirmar color pero sí distancia → UNKNOWN
 * - Si no se detecta nada → el comando sigue esperando (hasta soltar trigger)
 */
public class IntakeWithColorDetectionCommand extends CommandBase {

    // ==================== CONFIGURACIÓN ====================

    // Umbrales de distancia (cm)
    private static final double DISTANCE_BALL_PRESENT = 3.0;

    // Umbrales GREEN (antes amarillo)
    private static final float GREEN_HUE_MIN = 71f;
    private static final float GREEN_HUE_MAX = 190f;
    private static final float GREEN_SAT_MIN = 0.43f;

    // Umbrales PURPLE
    private static final float PURPLE_HUE_MIN = 123f;
    private static final float PURPLE_HUE_MAX = 263f;
    private static final float PURPLE_SAT_MIN = 0.31f;

    // Confirmación
    private static final int READINGS_TO_CONFIRM = 5;
    private static final long COLOR_TIMEOUT_MS = 2000;

    // ==================== DEPENDENCIAS ====================

    private final IntakeSubsystem intake;
    private final SpindexerSubsystem spindexer;

    // ==================== ESTADO INTERNO ====================

    private final ElapsedTime timer = new ElapsedTime();
    private int consecutiveGreen = 0;
    private int consecutivePurple = 0;
    private int consecutivePresent = 0;  // Pelota presente (por distancia)
    private boolean ballDetected = false;
    private SlotState detectedColor = SlotState.EMPTY;

    private int currentSlot;

    // ==================== CONSTRUCTOR ====================

    public IntakeWithColorDetectionCommand(IntakeSubsystem intake, SpindexerSubsystem spindexer) {
        this.intake = intake;
        this.spindexer = spindexer;
        addRequirements(intake, spindexer);
    }

    // ==================== COMMAND LIFECYCLE ====================

    @Override
    public void initialize() {
        // Reset estado
        consecutiveGreen = 0;
        consecutivePurple = 0;
        consecutivePresent = 0;
        ballDetected = false;
        detectedColor = SlotState.EMPTY;
        timer.reset();

        // Activar intake solo si estamos en posición
        if (spindexer.isAtIntake()) {
            intake.intake();
            currentSlot = spindexer.getCurrentSlotIndex();
        }
    }

    @Override
    public void execute() {
        // Seguridad: solo operar si estamos en posición de intake
        if (!spindexer.isAtIntake()) {
            intake.stop();
            return;
        }

        // Mantener intake activo
        intake.intake();

        // Si ya detectamos, no seguir leyendo
        if (ballDetected) {
            return;
        }

        // Leer sensor
        double distance = spindexer.getDistance();
        float hue = spindexer.getHue();
        float sat = spindexer.getSaturation();

        // Verificar presencia por distancia
        boolean ballPresent = distance < DISTANCE_BALL_PRESENT;

        if (ballPresent) {
            consecutivePresent++;

            // Clasificar color
            SlotState colorRead = classifyColor(hue, sat);

            if (colorRead == SlotState.GREEN) {
                consecutiveGreen++;
                consecutivePurple = 0;
            } else if (colorRead == SlotState.PURPLE) {
                consecutivePurple++;
                consecutiveGreen = 0;
            } else {
                // No es ninguno de los dos, resetear
                consecutiveGreen = 0;
                consecutivePurple = 0;
            }

            // Verificar confirmación de color
            if (consecutiveGreen >= READINGS_TO_CONFIRM) {
                ballDetected = true;
                detectedColor = SlotState.GREEN;
            } else if (consecutivePurple >= READINGS_TO_CONFIRM) {
                ballDetected = true;
                detectedColor = SlotState.PURPLE;
            }
            // Timeout: hay pelota pero no se confirma color
            else if (timer.milliseconds() > COLOR_TIMEOUT_MS && consecutivePresent >= READINGS_TO_CONFIRM) {
                ballDetected = true;
                detectedColor = SlotState.UNKNOWN;
            }

        } else {
            // No hay pelota, resetear contadores
            consecutivePresent = 0;
            consecutiveGreen = 0;
            consecutivePurple = 0;
        }
    }

    @Override
    public void end(boolean interrupted) {
        intake.stop();

        if (!interrupted && ballDetected) {
            // Etiquetar slot actual
            spindexer.setSlotState(currentSlot, detectedColor);

            // Rotar al siguiente slot vacío
            int nextEmpty = spindexer.getNextEmptySlot();
            if (nextEmpty != -1) {
                spindexer.moveToIntakePosition(nextEmpty);
            }
        }
    }

    @Override
    public boolean isFinished() {
        // Termina cuando se detectó una pelota
        return ballDetected;
    }

    // ==================== HELPERS ====================

    private SlotState classifyColor(float hue, float sat) {
        // GREEN
        if (hue >= GREEN_HUE_MIN && hue <= GREEN_HUE_MAX && sat >= GREEN_SAT_MIN) {
            return SlotState.GREEN;
        }
        // PURPLE
        if (hue >= PURPLE_HUE_MIN && hue <= PURPLE_HUE_MAX && sat >= PURPLE_SAT_MIN) {
            return SlotState.PURPLE;
        }
        return SlotState.UNKNOWN;
    }

    // ==================== GETTERS PARA TELEMETRÍA ====================

    public SlotState getDetectedColor() {
        return detectedColor;
    }

    public boolean isBallDetected() {
        return ballDetected;
    }

    public int getConsecutiveGreen() {
        return consecutiveGreen;
    }

    public int getConsecutivePurple() {
        return consecutivePurple;
    }
}
