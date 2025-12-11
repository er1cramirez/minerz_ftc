package org.firstinspires.ftc.teamcode.commands.shooter;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Espera hasta que shooter.isReady() retorne true.
 *
 * Uso en secuencias:
 * new SequentialCommandGroup(
 *     new SpinUpCloseCommand(shooter),
 *     new WaitForShooterReadyCommand(shooter),
 *     new EjectCommand(ejector)
 * )
 */
public class WaitForShooterReadyCommand extends CommandBase {

    private final ShooterSubsystem shooter;
    private final ElapsedTime timer;
    private static final double TIMEOUT_SECONDS = 2.0;

    public WaitForShooterReadyCommand(ShooterSubsystem shooter) {
        this.shooter = shooter;
        this.timer = new ElapsedTime();
        // NO addRequirements - solo observa
    }

    @Override
    public void initialize() {
        timer.reset();
    }

    @Override
    public boolean isFinished() {
        return shooter.isReady() || timer.seconds() > TIMEOUT_SECONDS;
    }
}