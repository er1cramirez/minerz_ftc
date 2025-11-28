package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.ArtifactFeeder;

/**
 * A simple command that releases a artifact with the {@link ArtifactFeeder}.
 */
public class RealseArtifact extends CommandBase {

    // The subsystem the command runs on
    private final ArtifactFeeder feeder;

    public RealseArtifact(ArtifactFeeder feeder) {
        this.feeder = feeder;
        addRequirements(feeder);
    }

    @Override
    public void initialize() {
        feeder.feed();
    }

    @Override
    public boolean isFinished() {
        return true;
    }

}