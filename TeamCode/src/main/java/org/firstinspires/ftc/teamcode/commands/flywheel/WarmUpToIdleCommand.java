package org.firstinspires.ftc.teamcode.commands.flywheel;

import com.seattlesolvers.solverslib.command.ConditionalCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.constants.FlywheelConstants;
import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;

/**
 * Command to sequentially ramp up the flywheel to idle speed.
 * This helps prevent sudden current spikes and potential mechanical stress.
 * 
 * Steps:
 * 1. 30% of Idle Speed -> Wait 500ms
 * 2. 60% of Idle Speed -> Wait 500ms
 * 3. 100% of Idle Speed -> Wait until stable
 */
public class WarmUpToIdleCommand extends SequentialCommandGroup {
    FlywheelSubsystem flywheel;
    public WarmUpToIdleCommand(FlywheelSubsystem flywheel) {
        this.flywheel = flywheel;
        addCommands(
            new ConditionalCommand(
                new SequentialCommandGroup(
                    // Step 1: 30% speed
                    new SpinUpCommand(flywheel, FlywheelConstants.IDLE_SPIN_RPM * 0.3, false),
                    new WaitCommand(300),
                    
                    // Step 2: 60% speed
                    new SpinUpCommand(flywheel, FlywheelConstants.IDLE_SPIN_RPM * 0.6, false),
                    new WaitCommand(200),
                    
                    // Step 3: Full Idle Speed (and wait for stable)
                    new SpinUpCommand(flywheel, FlywheelConstants.IDLE_SPIN_RPM, true)
                ),
                    new SpinUpCommand(flywheel, FlywheelConstants.IDLE_SPIN_RPM, true),
                () -> (flywheel.isIdleSpin() || (flywheel.getCurrentRPM() > FlywheelConstants.IDLE_SPIN_RPM))
            ),
                new InstantCommand(flywheel::setIdleSpin)
        );
        
        addRequirements(flywheel);
    }

//    @Override
//    public void end(boolean interrupted) {
//        if (!interrupted) {
//            flywheel.setIdleSpin();
//        }
//    }

// Not necessary since SpinUpCommand handles it
//    @Override
//    public boolean isFinished() {
//        return flywheel.isAtSpeed();
//    }
}
