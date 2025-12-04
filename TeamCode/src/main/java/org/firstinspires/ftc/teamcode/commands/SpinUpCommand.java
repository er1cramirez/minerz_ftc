package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Command to spin up the shooter to shooting velocity and wait until ready.
 * Finishes when the shooter reaches target velocity.
 * 
 * Use this before shooting to ensure the flywheel is at proper speed.
 */
public class SpinUpCommand extends CommandBase {

    private final ShooterSubsystem shooter;
    private final double targetVelocity;
    private final boolean useDefaultVelocity;

    /**
     * Creates a spin-up command with default shooting velocity.
     * @param shooter The shooter subsystem
     */
    public SpinUpCommand(ShooterSubsystem shooter) {
        this.shooter = shooter;
        this.targetVelocity = 0;
        this.useDefaultVelocity = true;
        addRequirements(shooter);
    }

    /**
     * Creates a spin-up command with a specific velocity.
     * @param shooter The shooter subsystem
     * @param velocity Target velocity in ticks/sec
     */
    public SpinUpCommand(ShooterSubsystem shooter, double velocity) {
        this.shooter = shooter;
        this.targetVelocity = velocity;
        this.useDefaultVelocity = false;
        addRequirements(shooter);
    }

    @Override
    public void initialize() {
        if (useDefaultVelocity) {
            shooter.spinUp();
        } else {
            shooter.spinUpToVelocity(targetVelocity);
        }
    }

    @Override
    public boolean isFinished() {
        return shooter.isReady();
    }

    @Override
    public void end(boolean interrupted) {
        // Don't stop the shooter - leave it running for shooting
        // The caller can chain with other commands or return to idle
    }
}
