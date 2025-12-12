package org.firstinspires.ftc.teamcode.commands.ejector;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.EjectorConstants;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;

public class EjectCycleCommand extends CommandBase {
    private final EjectorSubsystem ejector;
    private final ElapsedTime timer;
    
    public EjectCycleCommand(EjectorSubsystem ejector) {
        this.ejector = ejector;
        this.timer = new ElapsedTime();
        addRequirements(ejector);
    }
    
    @Override
    public void initialize() {
        ejector.eject();
        timer.reset();
    }
    
    @Override
    public boolean isFinished() {
        return timer.milliseconds() >= EjectorConstants.Timing.FULL_CYCLE_TIME_MS;
    }
    
    @Override
    public void end(boolean interrupted) {
        ejector.stow();
    }
}