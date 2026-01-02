package org.firstinspires.ftc.teamcode.commands.flywheel;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;

/**
 * Command to spin up the flywheel to a specific RPM.
 * 
 * Usage:
 * <pre>
 * // Go to specific RPM
 * new SpinUpCommand(flywheel, 3200);
 * 
 * // With timeout
 * new SpinUpCommand(flywheel, 3600).withTimeout(2000);
 * 
 * // Wait only until it reaches speed (don't verify stability)
 * new SpinUpCommand(flywheel, 3000, false);
 * </pre>
 */
public class SpinUpCommand extends CommandBase {
    
    private final FlywheelSubsystem flywheel;
    private final double targetRPM;
    private final boolean waitForStable;
    
    /**
     * Creates a command to spin up the flywheel to a specific RPM.
     * 
     * @param flywheel The flywheel subsystem
     * @param targetRPM The target RPM
     */
    public SpinUpCommand(FlywheelSubsystem flywheel, double targetRPM) {
        this(flywheel, targetRPM, false);
    }
    
    /**
     * Creates a command to spin up the flywheel to a specific RPM.
     * 
     * @param flywheel The flywheel subsystem
     * @param targetRPM The target RPM
     * @param waitForStable If true, waits for stability; if false, ends when it reaches speed
     */
    public SpinUpCommand(FlywheelSubsystem flywheel, double targetRPM, boolean waitForStable) {
        this.flywheel = flywheel;
        this.targetRPM = targetRPM;
        this.waitForStable = waitForStable;
        
        addRequirements(flywheel);
    }
    
    @Override
    public void initialize() {
        flywheel.spinUp(targetRPM);
    }
    
    @Override
    public void execute() {
        // The control is executed in periodic() of the subsystem
    }
    
    @Override
    public void end(boolean interrupted) {
        // We do not stop the flywheel when finished - it maintains the speed
        // This allows chaining with shooting commands
        
        if (interrupted) {
            // If interrupted, we might want to stop it
            // But generally we want to keep it spinning
            // flywheel.stop();  // Uncomment if you want to stop on interrupt
        }
    }
    
    @Override
    public boolean isFinished() {
        if (waitForStable) {
            return flywheel.isReadyToShoot();
        } else {
            return flywheel.isAtSpeed();
        }
    }
}
