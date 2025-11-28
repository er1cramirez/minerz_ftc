package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.subsystems.ArtifactFeeder;

/**
 * Feeds an artifact by moving servo and backing up.
 */
public class FeedArtifact extends SequentialCommandGroup {

    /**
     * Creates a new FeedArtifact command.
     */
    public FeedArtifact(ArtifactFeeder feeder) {
        addCommands(
                new RealseArtifact(feeder),
                new WaitCommand(1000),
                new StowFeeder(feeder)
        );
    }

}