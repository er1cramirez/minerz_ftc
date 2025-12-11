package org.firstinspires.ftc.teamcode.commands.shooter;

import com.seattlesolvers.solverslib.command.CommandBase;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Mantiene el shooter en velocidad CLOSE (2600 RPM).
 */
public class SpinUpCloseCommand extends CommandBase {

    private final ShooterSubsystem shooter;

    public SpinUpCloseCommand(ShooterSubsystem shooter) {
        this.shooter = shooter;
        addRequirements(shooter);
    }

    @Override
    public void initialize() {
        shooter.spinUpClose();
    }

    @Override
    public boolean isFinished() {
        return false;  // Nunca termina
    }
}