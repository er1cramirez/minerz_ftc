package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer;
import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer.BallColor;

/**
 * Command to load a ball and track it in a slot.
 * Moves to the next empty slot's intake position and records the ball color.
 * 
 * This command can be extended in the future to:
 * - Wait for a sensor to detect the ball
 * - Automatically advance to the next slot when ball is detected
 */
public class LoadBallCommand extends CommandBase {

    private final ArtifactIndexer indexer;
    private final BallColor ballColor;
    private boolean slotFound;

    /**
     * Creates a command to load a ball into the next available slot.
     * @param indexer The indexer subsystem
     * @param color The color of the ball being loaded
     */
    public LoadBallCommand(ArtifactIndexer indexer, BallColor color) {
        this.indexer = indexer;
        this.ballColor = color;
        addRequirements(indexer);
    }

    /**
     * Creates a command to load a ball without specifying color.
     * Useful when color is not yet known or not important.
     * @param indexer The indexer subsystem
     */
    public LoadBallCommand(ArtifactIndexer indexer) {
        this(indexer, BallColor.NONE);
    }

    @Override
    public void initialize() {
        // Try to find an empty slot
        slotFound = indexer.goToNextEmptyIntake();
    }

    @Override
    public void execute() {
        // Future: Check sensor here to detect when ball enters slot
        // if (indexer.isBallDetected()) {
        //     indexer.loadBall(ballColor);
        // }
    }

    @Override
    public void end(boolean interrupted) {
        // Record the ball in the current slot (for now, immediately on command end)
        if (slotFound && !interrupted) {
            indexer.loadBall(ballColor);
        }
    }

    @Override
    public boolean isFinished() {
        // For now, finish immediately after positioning
        // Future: Return true when sensor detects ball
        return true;
    }

    /**
     * Returns true if an empty slot was found.
     */
    public boolean wasSlotFound() {
        return slotFound;
    }
}
