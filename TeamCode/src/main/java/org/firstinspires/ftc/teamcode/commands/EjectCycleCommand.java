package org.firstinspires.ftc.teamcode.commands;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.EjectorConstants;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;

/**
 * Command that performs a complete eject cycle: eject -> hold -> stow.
 * This command waits for the full cycle to complete before finishing.
 * 
 * Use this in autonomous or when you need to ensure the ball is ejected
 * before proceeding to the next action.
 */
public class EjectCycleCommand extends CommandBase {

    private final EjectorSubsystem ejector;
    private final ElapsedTime timer;
    private final long ejectTimeMs;
    private final long holdTimeMs;
    
    private enum Phase {
        EJECTING,
        HOLDING,
        RETURNING,
        DONE
    }
    
    private Phase currentPhase;

    /**
     * Creates an eject cycle command with default timing.
     * @param ejector The ejector subsystem
     */
    public EjectCycleCommand(EjectorSubsystem ejector) {
        this(ejector, 
             EjectorConstants.Timing.EJECT_TIME_MS, 
             EjectorConstants.Timing.HOLD_TIME_MS);
    }

    /**
     * Creates an eject cycle command with custom timing.
     * @param ejector The ejector subsystem
     * @param ejectTimeMs Time for servo to reach eject position
     * @param holdTimeMs Time to hold in eject position
     */
    public EjectCycleCommand(EjectorSubsystem ejector, long ejectTimeMs, long holdTimeMs) {
        this.ejector = ejector;
        this.ejectTimeMs = ejectTimeMs;
        this.holdTimeMs = holdTimeMs;
        this.timer = new ElapsedTime();
        
        addRequirements(ejector);
    }

    @Override
    public void initialize() {
        ejector.eject();
        timer.reset();
        currentPhase = Phase.EJECTING;
    }

    @Override
    public void execute() {
        double elapsed = timer.milliseconds();
        
        switch (currentPhase) {
            case EJECTING:
                // Wait for servo to reach eject position
                if (elapsed >= ejectTimeMs) {
                    currentPhase = Phase.HOLDING;
                    timer.reset();
                }
                break;
                
            case HOLDING:
                // Hold in eject position
                if (elapsed >= holdTimeMs) {
                    ejector.stow();
                    currentPhase = Phase.RETURNING;
                    timer.reset();
                }
                break;
                
            case RETURNING:
                // Wait a bit for stow, then we're done
                if (elapsed >= ejectTimeMs) {
                    currentPhase = Phase.DONE;
                }
                break;
                
            case DONE:
                // Nothing to do
                break;
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            ejector.stow();
        }
    }

    @Override
    public boolean isFinished() {
        return currentPhase == Phase.DONE;
    }
}
