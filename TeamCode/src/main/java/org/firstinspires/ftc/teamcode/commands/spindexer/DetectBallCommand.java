package org.firstinspires.ftc.teamcode.commands.spindexer;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * Command that detects the color of a ball and labels the current slot.
 * 
 * Responsibility:
 * - Read sensor until color is confirmed
 * - Label the current slot with the detected color
 * 
 * Ends when:
 * - A color is confirmed (N consecutive readings)
 * - Timeout without confirming color but with ball present → UNKNOWN
 */
public class DetectBallCommand extends CommandBase {

    private static final int READINGS_TO_CONFIRM = 5;
    private static final long COLOR_TIMEOUT_MS = 2000;

    private final SpindexerSubsystem spindexer;

    private final ElapsedTime timer = new ElapsedTime();
    private int consecutiveGreen = 0;
    private int consecutivePurple = 0;
    private int consecutivePresent = 0;
    private boolean ballDetected = false;
    private SlotState detectedColor = SlotState.EMPTY;
    private int slotToLabel;

    public DetectBallCommand(SpindexerSubsystem spindexer) {
        this.spindexer = spindexer;
        // addRequirements(spindexer);
    }

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
        if (ballDetected)
            return;

        double distance = spindexer.getDistance();
        float hue = spindexer.getHue();
        float sat = spindexer.getSaturation();

        boolean ballPresent = distance < SpindexerConstants.DISTANCE_BALL_PRESENT;

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
                consecutivePurple = 0;
            }

            if (consecutiveGreen >= READINGS_TO_CONFIRM) {
                ballDetected = true;
                detectedColor = SlotState.GREEN;
            } else if (consecutivePurple >= READINGS_TO_CONFIRM) {
                ballDetected = true;
                detectedColor = SlotState.PURPLE;
            } else if (timer.milliseconds() > COLOR_TIMEOUT_MS) {
                // TIMEOUT LOGIC
                // We reduce the threshold to 2 to catch "shaky" balls that reset the counter
                // often,
                // but we keep it > 0 to avoid "phantom" balls from single-frame noise.
                if (consecutivePresent >= 2) {
                    ballDetected = true;
                    // "Majority Vote" / Best Guess
                    if (consecutiveGreen > consecutivePurple && consecutiveGreen > 0) {
                        detectedColor = SlotState.GREEN;
                    } else if (consecutivePurple > consecutiveGreen && consecutivePurple > 0) {
                        detectedColor = SlotState.PURPLE;
                    } else {
                        detectedColor = SlotState.UNKNOWN;
                    }
                }
            }
        } else {
            consecutivePresent = 0;
            consecutiveGreen = 0;
            consecutivePurple = 0;

            // Should accurate detection require seeing nothing for a bit?
            // Maybe not needed for this fix, but if ball was removed, we reset.
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

    private SlotState classifyColor(float hue, float sat) {
        if (hue >= SpindexerConstants.GREEN_HUE_MIN && hue <= SpindexerConstants.GREEN_HUE_MAX
                && sat >= SpindexerConstants.GREEN_SAT_MIN) {
            return SlotState.GREEN;
        }
        if (hue >= SpindexerConstants.PURPLE_HUE_MIN && hue <= SpindexerConstants.PURPLE_HUE_MAX
                && sat >= SpindexerConstants.PURPLE_SAT_MIN) {
            return SlotState.PURPLE;
        }
        return SlotState.UNKNOWN;
    }
}