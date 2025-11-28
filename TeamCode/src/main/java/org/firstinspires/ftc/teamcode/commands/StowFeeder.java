package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.ArtifactFeeder;

/**
 * A simple command that stow the artifact feeder in order to feed other artifact with the {@link ArtifactFeeder}
 */
public class StowFeeder extends CommandBase {

    // The subsystem the command runs on
    private final ArtifactFeeder feeder;

    public StowFeeder(ArtifactFeeder feeder) {
        this.feeder = feeder;
        addRequirements(feeder);
    }

    @Override
    public void initialize() {
        feeder.stow();
    }

    @Override
    public boolean isFinished() {
        return true;
    }

}