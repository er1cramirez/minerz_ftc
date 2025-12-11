package org.firstinspires.ftc.teamcode.commands.shooter;

import com.seattlesolvers.solverslib.command.CommandBase;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Mantiene el shooter en velocidad FAR (3300 RPM).
 */
public class SpinUpFarCommand extends CommandBase {

    private final ShooterSubsystem shooter;

    public SpinUpFarCommand(ShooterSubsystem shooter) {
        this.shooter = shooter;
        addRequirements(shooter);
    }

    @Override
    public void initialize() {
        shooter.spinUpFar();
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}