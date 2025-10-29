package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.Motor.ZeroPowerBehavior;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

import org.firstinspires.ftc.teamcode.constants.RobotConstants;

/**
 * Subsystem for the shooter flywheel mechanism.
 * 
 * FUTURE IMPLEMENTATION:
 * - Velocity control for consistent shooting
 * - Distance-based speed adjustment
 * - AprilTag-based auto-aiming integration
 * - Optional turret for orientation
 * 
 * This is a basic template to be expanded as the mechanical design is finalized.
 */
public class ShooterSubsystem extends SubsystemBase {

    private final MotorEx shooterMotor;
    
    // Target velocity in RPM (to be tuned)
    private double targetVelocity = 0;

    /**
     * Creates a new ShooterSubsystem.
     * 
     * @param hardwareMap The hardware map from the OpMode
     */
    public ShooterSubsystem(HardwareMap hardwareMap) {
        shooterMotor = new MotorEx(hardwareMap, 
                RobotConstants.HardwareNames.SHOOTER_MOTOR, 
                Motor.GoBILDA.RPM_312);
        shooterMotor.setZeroPowerBehavior(ZeroPowerBehavior.FLOAT); // Flywheel should coast
        
        // Enable velocity control if using encoder feedback
        // shooterMotor.setRunMode(Motor.RunMode.VelocityControl);
    }

    /**
     * Sets the shooter motor power directly.
     * 
     * @param power The power to set (0.0 to 1.0)
     */
    public void setPower(double power) {
        shooterMotor.set(power);
    }

    /**
     * Sets the target velocity for the flywheel.
     * 
     * @param rpm Target velocity in RPM
     */
    public void setVelocity(double rpm) {
        targetVelocity = rpm;
        // Implementation will depend on control strategy
        // shooterMotor.setVelocity(rpm);
    }

    /**
     * Starts the shooter at a default speed.
     */
    public void spinUp() {
        setPower(0.8); // Placeholder value
    }

    /**
     * Stops the shooter.
     */
    public void stop() {
        setPower(0);
        targetVelocity = 0;
    }

    /**
     * Gets the current velocity of the flywheel.
     * 
     * @return Current velocity in RPM
     */
    public double getVelocity() {
        return shooterMotor.getVelocity();
    }

    /**
     * Checks if the shooter is at target velocity.
     * 
     * @return true if within tolerance of target velocity
     */
    public boolean atTargetVelocity() {
        if (targetVelocity == 0) return false;
        double tolerance = 50; // RPM tolerance (to be tuned)
        return Math.abs(getVelocity() - targetVelocity) < tolerance;
    }

    /**
     * Checks if the shooter is currently running.
     * 
     * @return true if the motor power is not zero
     */
    public boolean isRunning() {
        return Math.abs(shooterMotor.get()) > 0.01;
    }

    @Override
    public void periodic() {
        // Future: Monitor velocity and adjust control if needed
        // Future: Update telemetry for tuning
    }
}
