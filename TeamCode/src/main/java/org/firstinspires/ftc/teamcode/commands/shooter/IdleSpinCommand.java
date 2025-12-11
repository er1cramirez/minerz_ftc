package org.firstinspires.ftc.teamcode.commands.shooter;

import com.seattlesolvers.solverslib.command.CommandBase;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Mantiene el shooter en IDLE (2000 RPM).
 */
public class IdleSpinCommand extends CommandBase {

    private final ShooterSubsystem shooter;

    public IdleSpinCommand(ShooterSubsystem shooter) {
        this.shooter = shooter;
        addRequirements(shooter);
    }

    @Override
    public void initialize() {
        shooter.idle();
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}