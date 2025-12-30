package org.firstinspires.ftc.teamcode.commands.spindexer;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.command.CommandScheduler;

import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;

/**
 * Command that quickly checks if there is a ball in the current slot.
 * 
 * Flow:
 * - Reads distance ONCE
 * - If there is a ball → runs DetectBallCommand to label
 * - Terminates immediately
 * 
 * The parent command can then consult spindexer.getCurrentSlotState()
 * to decide if it needs to activate intake or not.
 */
public class CheckSlotCommand extends CommandBase {

    // private static final double DISTANCE_BALL_PRESENT = 3.0; // Moved to
    // SpindexerConstants

    private final SpindexerSubsystem spindexer;
    private boolean checked = false;

    public CheckSlotCommand(SpindexerSubsystem spindexer) {
        this.spindexer = spindexer;
    }

    @Override
    public void initialize() {
        checked = false;
    }

    @Override
    public void execute() {
        if (checked)
            return;

        double distance = spindexer.getDistance();

        if (distance < SpindexerConstants.DISTANCE_BALL_PRESENT) {
            // There is a ball. Only mark as UNKNOWN if it was previously considered EMPTY.
            // If it is already labelled (GREEN/PURPLE) or UNKNOWN, we preserve that state.
            if (spindexer.getCurrentSlotState() == SpindexerSubsystem.SlotState.EMPTY) {
                spindexer.setSlotState(spindexer.getCurrentSlotIndex(), SpindexerSubsystem.SlotState.UNKNOWN);
            }
        }

        checked = true;
    }

    @Override
    public boolean isFinished() {
        return checked;
    }
}
