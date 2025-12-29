package org.firstinspires.ftc.teamcode.commands.ejector;

import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.constants.EjectorConstants;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;

public class EjectCycleCommand extends SequentialCommandGroup {

    
    public EjectCycleCommand(EjectorSubsystem ejector) {
        addCommands(
            new InstantCommand(ejector::eject, ejector),
            new WaitCommand(EjectorConstants.Timing.EJECT_TIME_MS),
            new InstantCommand(ejector::stow, ejector),
            new WaitCommand(EjectorConstants.Timing.HOLD_TIME_MS)
        );
        addRequirements(ejector);
    }

}