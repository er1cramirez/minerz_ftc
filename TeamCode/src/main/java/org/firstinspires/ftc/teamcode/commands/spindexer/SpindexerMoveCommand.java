package org.firstinspires.ftc.teamcode.commands.spindexer;

import com.seattlesolvers.solverslib.command.CommandBase;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

/**
 * Command to move the Spindexer to a specific slot and position.
 * Now waits for motor to reach position before finishing.
 */
public class SpindexerMoveCommand extends CommandBase {

    public enum Position {
        INTAKE,
        OUTTAKE
    }

    private final SpindexerSubsystem spindexer;
    private final int slotIndex;
    private final Position position;

    public SpindexerMoveCommand(SpindexerSubsystem spindexer, int slotIndex, Position position) {
        this.spindexer = spindexer;
        this.slotIndex = slotIndex;
        this.position = position;
        addRequirements(spindexer);
    }

    @Override
    public void initialize() {
        if (position == Position.INTAKE) {
            spindexer.moveToIntakePosition(slotIndex);
        } else {
            spindexer.moveToOuttakePosition(slotIndex);
        }
    }

    @Override
    public boolean isFinished() {
        return spindexer.isAtPosition();
    }
}
