package org.firstinspires.ftc.teamcode.commands.shooter;

import com.seattlesolvers.solverslib.command.InstantCommand;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Regresa el shooter a IDLE (2000 RPM).
 */
public class ReturnToIdleCommand extends InstantCommand {
    public ReturnToIdleCommand(ShooterSubsystem shooter) {
        super(shooter::idle, shooter);
    }
}