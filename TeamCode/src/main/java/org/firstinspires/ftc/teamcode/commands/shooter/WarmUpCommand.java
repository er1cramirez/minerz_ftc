package org.firstinspires.ftc.teamcode.commands.shooter;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.ShooterConstants;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Calienta el shooter: STOPPED → WARMING_UP → IDLE
 */
public class WarmUpCommand extends CommandBase {

    private final ShooterSubsystem shooter;
    private final ElapsedTime timer;

    private boolean transitioned;
    private static final double TIMEOUT_SECONDS = 1.5;

    public WarmUpCommand(ShooterSubsystem shooter) {
        this.shooter = shooter;
        this.timer = new ElapsedTime();
        addRequirements(shooter);
    }

    @Override
    public void initialize() {
        shooter.startWarmUp();
        transitioned = false;
        timer.reset();
    }

    @Override
    public void execute() {
        if (!transitioned && timer.milliseconds() >= ShooterConstants.WARMUP_DELAY_MS) {
            shooter.idle();
            transitioned = true;
        }
    }

    @Override
    public boolean isFinished() {
        if (!transitioned) {
            return false;
        }

        double error = Math.abs(
                shooter.getCurrentRpm() - ShooterConstants.IDLE_VELOCITY
        );

        return error <= ShooterConstants.VELOCITY_TOLERANCE
                || timer.seconds() > TIMEOUT_SECONDS;
    }

    @Override
    public void end(boolean interrupted) {
        if (!shooter.isIdle()) {
            shooter.idle();
        }
    }
}