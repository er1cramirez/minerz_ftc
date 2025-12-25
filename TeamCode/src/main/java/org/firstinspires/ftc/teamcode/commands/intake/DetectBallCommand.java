package org.firstinspires.ftc.teamcode.commands.intake;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * Comando que detecta el color de una pelota y etiqueta el slot actual.
 * 
 * RESPONSABILIDAD:
 * - Leer sensor hasta confirmar color
 * - Etiquetar el slot actual con el color detectado
 * 
 * NO CONTROLA: intake (eso es responsabilidad del comando padre)
 * 
 * TERMINA cuando:
 * - Se confirma un color (N lecturas consecutivas)
 * - Timeout sin confirmar color pero con pelota presente → UNKNOWN
 */
public class DetectBallCommand extends CommandBase {

    // ==================== CONFIGURACIÓN ====================

    private static final double DISTANCE_BALL_PRESENT = 3.0;

    // GREEN
    private static final float GREEN_HUE_MIN = 71f;
    private static final float GREEN_HUE_MAX = 190f;
    private static final float GREEN_SAT_MIN = 0.43f;

    // PURPLE
    private static final float PURPLE_HUE_MIN = 123f;
    private static final float PURPLE_HUE_MAX = 263f;
    private static final float PURPLE_SAT_MIN = 0.31f;

    private static final int READINGS_TO_CONFIRM = 5;
    private static final long COLOR_TIMEOUT_MS = 2000;

    // ==================== DEPENDENCIAS ====================

    private final SpindexerSubsystem spindexer;

    // ==================== ESTADO ====================

    private final ElapsedTime timer = new ElapsedTime();
    private int consecutiveGreen = 0;
    private int consecutivePurple = 0;
    private int consecutivePresent = 0;
    private boolean ballDetected = false;
    private SlotState detectedColor = SlotState.EMPTY;
    private int slotToLabel;

    // ==================== CONSTRUCTOR ====================

    public DetectBallCommand(SpindexerSubsystem spindexer) {
        this.spindexer = spindexer;
    }

    // ==================== LIFECYCLE ====================

    @Override
    public void initialize() {
        consecutiveGreen = 0;
        consecutivePurple = 0;
        consecutivePresent = 0;
        ballDetected = false;
        detectedColor = SlotState.EMPTY;
        timer.reset();
        slotToLabel = spindexer.getCurrentSlotIndex();
    }

    @Override
    public void execute() {
        if (ballDetected) return;

        double distance = spindexer.getDistance();
        float hue = spindexer.getHue();
        float sat = spindexer.getSaturation();

        boolean ballPresent = distance < DISTANCE_BALL_PRESENT;

        if (ballPresent) {
            consecutivePresent++;
            SlotState colorRead = classifyColor(hue, sat);

            if (colorRead == SlotState.GREEN) {
                consecutiveGreen++;
                consecutivePurple = 0;
            } else if (colorRead == SlotState.PURPLE) {
                consecutivePurple++;
                consecutiveGreen = 0;
            } else {
                consecutiveGreen = 0;
                consecutivePurple = 0;
            }

            if (consecutiveGreen >= READINGS_TO_CONFIRM) {
                ballDetected = true;
                detectedColor = SlotState.GREEN;
            } else if (consecutivePurple >= READINGS_TO_CONFIRM) {
                ballDetected = true;
                detectedColor = SlotState.PURPLE;
            } else if (timer.milliseconds() > COLOR_TIMEOUT_MS && consecutivePresent >= READINGS_TO_CONFIRM) {
                ballDetected = true;
                detectedColor = SlotState.UNKNOWN;
            }
        } else {
            consecutivePresent = 0;
            consecutiveGreen = 0;
            consecutivePurple = 0;
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (!interrupted && ballDetected) {
            spindexer.setSlotState(slotToLabel, detectedColor);
        }
    }

    @Override
    public boolean isFinished() {
        return ballDetected;
    }

    // ==================== HELPERS ====================

    private SlotState classifyColor(float hue, float sat) {
        if (hue >= GREEN_HUE_MIN && hue <= GREEN_HUE_MAX && sat >= GREEN_SAT_MIN) {
            return SlotState.GREEN;
        }
        if (hue >= PURPLE_HUE_MIN && hue <= PURPLE_HUE_MAX && sat >= PURPLE_SAT_MIN) {
            return SlotState.PURPLE;
        }
        return SlotState.UNKNOWN;
    }
}