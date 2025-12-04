package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer;
import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer.IndexerPosition;

/**
 * Command to move the indexer to a specific position.
 * This command finishes immediately after setting the position.
 * For commands that wait for the servo to reach position, consider
 * adding a timeout or sensor feedback.
 */
public class IndexerToPositionCommand extends CommandBase {

    private final ArtifactIndexer indexer;
    private final IndexerPosition targetPosition;

    /**
     * Creates a command to move the indexer to a specific position.
     * @param indexer The indexer subsystem
     * @param position The target position
     */
    public IndexerToPositionCommand(ArtifactIndexer indexer, IndexerPosition position) {
        this.indexer = indexer;
        this.targetPosition = position;
        addRequirements(indexer);
    }

    @Override
    public void initialize() {
        indexer.setPosition(targetPosition);
    }

    @Override
    public boolean isFinished() {
        // Servo commands finish immediately since we don't have position feedback
        // For more precise control, you could add a timer or sensor
        return true;
    }
}
