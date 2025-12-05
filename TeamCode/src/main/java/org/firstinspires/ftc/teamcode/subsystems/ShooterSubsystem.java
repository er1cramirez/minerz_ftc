package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.Motor.RunMode;
import com.seattlesolvers.solverslib.hardware.motors.Motor.ZeroPowerBehavior;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

import org.firstinspires.ftc.teamcode.constants.ShooterConstants;

/**
 * Subsystem for the flywheel shooter mechanism.
 * 
 * Features:
 * - Velocity-controlled flywheel with PID + Feedforward
 * - Idle/cruise mode for faster spin-up during matches
 * - Shooting velocity with ready-state detection
 * - Angle adjustment servo for distance compensation
 * - Support for both constant and variable velocity modes
 * 
 * States:
 * - STOPPED: Flywheel not spinning
 * - IDLE: Flywheel at cruise velocity, ready for quick spin-up
 * - SPINNING_UP: Transitioning to shooting velocity
 * - READY: At target shooting velocity, ready to fire
 * - SHOOTING: Actively firing (at velocity, ball being fed)
 */
public class ShooterSubsystem extends SubsystemBase {

    /**
     * Shooter states for coordination with other subsystems.
     */
    public enum ShooterState {
        STOPPED,      // Flywheel not spinning
        IDLE,         // At cruise velocity
        SPINNING_UP,  // Transitioning to shoot velocity
        READY,        // At target velocity, ready to fire
        SHOOTING      // Actively shooting
    }

    /**
     * Shooting mode selection.
     */
    public enum ShootingMode {
        CONSTANT_VELOCITY,  // Fixed velocity, angle servo adjusts
        VARIABLE_VELOCITY   // Velocity changes based on distance
    }

    private final MotorEx flywheelMotor;
    private final Servo angleServo;
    
    private ShooterState currentState;
    private ShootingMode shootingMode;
    private double targetVelocity;
    private double currentAngle;
    
    // For distance-based shooting (future AprilTag integration)
    private double targetDistance;

    /**
     * Creates a new ShooterSubsystem.
     * @param hardwareMap The hardware map from the OpMode
     */
    public ShooterSubsystem(HardwareMap hardwareMap) {
        // Initialize flywheel motor
        flywheelMotor = new MotorEx(hardwareMap,
                ShooterConstants.Motor.FLYWHEEL_MOTOR,
                28,  // Encoder ticks per revolution for Rev HD Hex motor
                6000  // Max RPM for Rev HD Hex motor
        );
        
        // Configure for velocity control
        flywheelMotor.setZeroPowerBehavior(ZeroPowerBehavior.FLOAT);
        
        // Invert motor if needed (not encoder - they should match)
        flywheelMotor.setInverted(true);  // Change to false if wrong direction
        
        // Set velocity control mode and PID coefficients
        flywheelMotor.setRunMode(RunMode.VelocityControl);
        flywheelMotor.setVeloCoefficients(
                ShooterConstants.VelocityPID.kP,
                ShooterConstants.VelocityPID.kI,
                ShooterConstants.VelocityPID.kD
        );
        flywheelMotor.setFeedforwardCoefficients(
                ShooterConstants.VelocityPID.kS,
                ShooterConstants.VelocityPID.kV
        );
        
        // Initialize angle servo
        angleServo = hardwareMap.get(Servo.class, ShooterConstants.Motor.ANGLE_SERVO);
        
        // Initialize state
        currentState = ShooterState.STOPPED;
        shootingMode = ShootingMode.CONSTANT_VELOCITY;
        targetVelocity = 0;
        currentAngle = ShooterConstants.AnglePositions.STOW;
        targetDistance = 0;
        
        // Set initial angle
        setAngle(currentAngle);
    }

    // ==================== Flywheel Control ====================

    /**
     * Starts the flywheel at idle/cruise velocity.
     * Use this at the start of auto or teleop for faster spin-up.
     */
    public void startIdle() {
        setVelocity(ShooterConstants.Velocities.IDLE_VELOCITY);
        currentState = ShooterState.IDLE;
    }

    /**
     * Spins up to full shooting velocity.
     */
    public void spinUp() {
        setVelocity(ShooterConstants.Velocities.SHOOT_VELOCITY);
        currentState = ShooterState.SPINNING_UP;
    }

    /**
     * Spins up to a specific velocity.
     * @param velocity Target velocity in ticks/sec
     */
    public void spinUpToVelocity(double velocity) {
        setVelocity(velocity);
        currentState = ShooterState.SPINNING_UP;
    }

    /**
     * Returns to idle velocity after shooting.
     */
    public void returnToIdle() {
        setVelocity(ShooterConstants.Velocities.IDLE_VELOCITY);
        currentState = ShooterState.IDLE;
    }

    /**
     * Stops the flywheel completely.
     */
    public void stop() {
        flywheelMotor.stopMotor();
        targetVelocity = 0;
        currentState = ShooterState.STOPPED;
    }

    /**
     * Sets the target velocity directly.
     * @param velocity Target velocity in ticks/sec
     */
    private void setVelocity(double velocity) {
        targetVelocity = velocity;
        flywheelMotor.setVelocity(velocity);
    }

    // ==================== Angle Servo Control ====================

    /**
     * Sets the angle servo position.
     * @param position Servo position (0.0 to 1.0)
     */
    public void setAngle(double position) {
        position = Math.max(ShooterConstants.AnglePositions.MIN_ANGLE,
                   Math.min(ShooterConstants.AnglePositions.MAX_ANGLE, position));
        angleServo.setPosition(position);
        currentAngle = position;
    }

    /**
     * Sets angle for close range shots.
     */
    public void setCloseRange() {
        setAngle(ShooterConstants.AnglePositions.CLOSE_RANGE);
    }

    /**
     * Sets angle for medium range shots.
     */
    public void setMediumRange() {
        setAngle(ShooterConstants.AnglePositions.MEDIUM_RANGE);
    }

    /**
     * Sets angle for long range shots.
     */
    public void setLongRange() {
        setAngle(ShooterConstants.AnglePositions.LONG_RANGE);
    }

    /**
     * Stows the angle servo.
     */
    public void stowAngle() {
        setAngle(ShooterConstants.AnglePositions.STOW);
    }

    // ==================== Distance-Based Shooting ====================

    /**
     * Configures shooter for a specific distance.
     * In CONSTANT_VELOCITY mode: adjusts angle only
     * In VARIABLE_VELOCITY mode: adjusts velocity and angle
     * 
     * @param distanceInches Distance to target in inches
     */
    public void setForDistance(double distanceInches) {
        targetDistance = distanceInches;
        
        if (shootingMode == ShootingMode.CONSTANT_VELOCITY) {
            // Constant velocity mode: adjust angle based on distance
            setAngleForDistance(distanceInches);
        } else {
            // Variable velocity mode: adjust both velocity and angle
            setVelocityForDistance(distanceInches);
            setAngleForDistance(distanceInches);
        }
    }

    /**
     * Sets angle based on distance (for constant velocity mode).
     */
    private void setAngleForDistance(double distanceInches) {
        if (distanceInches <= ShooterConstants.ShootingProfiles.CLOSE_DISTANCE_MAX) {
            setCloseRange();
        } else if (distanceInches <= ShooterConstants.ShootingProfiles.MEDIUM_DISTANCE_MAX) {
            setMediumRange();
        } else {
            setLongRange();
        }
    }

    /**
     * Sets velocity based on distance (for variable velocity mode).
     * Override this with your own interpolation/lookup table.
     */
    private void setVelocityForDistance(double distanceInches) {
        // Simple linear interpolation example
        // TODO: Replace with actual calibrated values or lookup table
        double minDistance = 36.0;  // 3 feet
        double maxDistance = 120.0; // 10 feet
        double minVelocity = ShooterConstants.Velocities.SHOOT_VELOCITY * 0.7;
        double maxVelocity = ShooterConstants.Velocities.SHOOT_VELOCITY;
        
        double normalized = (distanceInches - minDistance) / (maxDistance - minDistance);
        normalized = Math.max(0, Math.min(1, normalized));
        
        double velocity = minVelocity + (maxVelocity - minVelocity) * normalized;
        spinUpToVelocity(velocity);
    }

    /**
     * Sets the shooting mode.
     * @param mode CONSTANT_VELOCITY or VARIABLE_VELOCITY
     */
    public void setShootingMode(ShootingMode mode) {
        this.shootingMode = mode;
    }

    /**
     * Gets the current shooting mode.
     */
    public ShootingMode getShootingMode() {
        return shootingMode;
    }

    // ==================== State Queries ====================

    /**
     * Gets the current shooter state.
     */
    public ShooterState getState() {
        return currentState;
    }

    /**
     * Marks the shooter as actively shooting.
     * Call this when a ball is being fed.
     */
    public void setShootingState() {
        if (currentState == ShooterState.READY) {
            currentState = ShooterState.SHOOTING;
        }
    }

    /**
     * Checks if the shooter is ready to fire.
     * @return true if at target velocity and ready
     */
    public boolean isReady() {
        return currentState == ShooterState.READY;
    }

    /**
     * Checks if the flywheel is at target velocity.
     * @return true if within tolerance of target
     */
    public boolean atTargetVelocity() {
        if (targetVelocity == 0) return false;
        double currentVelocity = getVelocity();
        return Math.abs(currentVelocity - targetVelocity) < 
               ShooterConstants.Velocities.VELOCITY_TOLERANCE;
    }

    /**
     * Checks if the shooter is running (not stopped).
     * @return true if flywheel is spinning
     */
    public boolean isRunning() {
        return currentState != ShooterState.STOPPED && 
               getVelocity() > ShooterConstants.Velocities.MIN_RUNNING_VELOCITY;
    }

    /**
     * Checks if the shooter is at idle velocity.
     * @return true if at idle/cruise velocity
     */
    public boolean isIdle() {
        return currentState == ShooterState.IDLE;
    }

    /**
     * Gets the current flywheel velocity.
     * @return Current velocity in ticks/sec
     */
    public double getVelocity() {
        return flywheelMotor.getVelocity();
    }

    /**
     * Gets the target velocity.
     * @return Target velocity in ticks/sec
     */
    public double getTargetVelocity() {
        return targetVelocity;
    }

    /**
     * Gets the current angle servo position.
     * @return Servo position (0.0 to 1.0)
     */
    public double getCurrentAngle() {
        return currentAngle;
    }

    /**
     * Gets the velocity error (target - current).
     * @return Velocity error in ticks/sec
     */
    public double getVelocityError() {
        return targetVelocity - getVelocity();
    }

    // ==================== Periodic Update ====================

    @Override
    public void periodic() {
        // Update state based on velocity
        if (currentState == ShooterState.SPINNING_UP && atTargetVelocity()) {
            currentState = ShooterState.READY;
        }
        
        // If we were READY/SHOOTING but dropped below target, go back to SPINNING_UP
        if ((currentState == ShooterState.READY || currentState == ShooterState.SHOOTING) 
                && !atTargetVelocity()) {
            currentState = ShooterState.SPINNING_UP;
        }
        
        // If in SHOOTING state and at velocity, return to READY
        if (currentState == ShooterState.SHOOTING && atTargetVelocity()) {
            currentState = ShooterState.READY;
        }
    }

    // ==================== Tuning Helpers ====================

    /**
     * Updates PID coefficients at runtime (for tuning).
     * @param kP Proportional gain
     * @param kI Integral gain
     * @param kD Derivative gain
     */
    public void setPIDCoefficients(double kP, double kI, double kD) {
        flywheelMotor.setVeloCoefficients(kP, kI, kD);
    }

    /**
     * Updates feedforward coefficients at runtime (for tuning).
     * @param kS Static friction compensation
     * @param kV Velocity feedforward
     */
    public void setFeedforwardCoefficients(double kS, double kV) {
        flywheelMotor.setFeedforwardCoefficients(kS, kV);
    }

    /**
     * Gets telemetry data for debugging.
     * @return Formatted string with shooter status
     */
    public String getTelemetryString() {
        return String.format("State: %s | Vel: %.0f/%.0f | Angle: %.2f",
                currentState.name(),
                getVelocity(),
                targetVelocity,
                currentAngle);
    }
}
