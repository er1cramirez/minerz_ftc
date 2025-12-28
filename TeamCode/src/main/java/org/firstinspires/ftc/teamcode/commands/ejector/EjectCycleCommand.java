package org.firstinspires.ftc.teamcode.commands.ejector;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;

import org.firstinspires.ftc.teamcode.constants.EjectorConstants;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;

public class EjectCycleCommand extends SequentialCommandGroup {
    private Boolean finished;
    
    public EjectCycleCommand(EjectorSubsystem ejector) {
        addCommands(
            new InstantCommand(ejector::eject, ejector),
            new WaitCommand(EjectorConstants.Timing.EJECT_TIME_MS),
            new InstantCommand(ejector::stow, ejector),
            new WaitCommand(EjectorConstants.Timing.STOW_TIME_MS)
        );
        addRequirements(ejector);
    }

    @Override
    public void initialize() {
        finished = false;
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
    
    @Override
    public void end(boolean interrupted) {
        finished = true;
    }
}