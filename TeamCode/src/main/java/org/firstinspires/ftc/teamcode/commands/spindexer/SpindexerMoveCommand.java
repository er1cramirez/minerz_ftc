package org.firstinspires.ftc.teamcode.commands.spindexer;

import com.seattlesolvers.solverslib.command.InstantCommand;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

/**
 * Basic command to move the Spindexer to a specific slot and position (Intake/Outtake).
 * Uses InstantCommand because servo movement is effectively instantaneous from the code's perspective
 * (we don't wait for it to arrive, though we could add a wait if precise timing is needed).
 */
public class SpindexerMoveCommand extends InstantCommand {

    public enum Position {
        INTAKE,
        OUTTAKE
    }

    public SpindexerMoveCommand(SpindexerSubsystem spindexer, int slotIndex, Position position) {
        super(() -> {
            if (position == Position.INTAKE) {
                spindexer.moveToIntakePosition(slotIndex);
            } else {
                spindexer.moveToOuttakePosition(slotIndex);
            }
        });
        addRequirements(spindexer);
    }
}
