package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;
import com.seattlesolvers.solverslib.controller.PIDController;

import org.firstinspires.ftc.teamcode.constants.TurretConstants;

/**
 * Turret subsystem
 * 
 * Features:
 * - Position control in degrees with PID
 * - Soft limits to protect wiring
 * - Relative position tracking
 * - States: IDLE, HOMING, MANUAL, POSITION, TRACKING
 * 
 * Position 0° represents the turret pointing forward.
 * Positive angles = counter-clockwise rotation (seen from above)
 * Negative angles = clockwise rotation
 */
public class TurretSubsystem extends SubsystemBase {
    public enum TurretState {
        IDLE,// Motor off
        HOMING,// Homing
        MANUAL,// Manual control
        POSITION,// PID control
        TRACKING,// Tracking
    }
    
    private final MotorEx motor;
    private final PIDController pidController;
    
    private TurretState currentState;
    private double targetAngleDeg;
    private double homeOffsetTicks;
    private boolean isHomed;
    
    private double currentPositionDeg;
    private double currentVelocityDegPerSec;
    private double lastPositionTicks;
    private long lastUpdateTimeMs;
    
    /**
     * Creates the TurretSubsystem.
     * @param hardwareMap The HardwareMap of the OpMode
     */
    public TurretSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, TurretConstants.MOTOR_NAME);
    }
    
    /**
     * Creates the TurretSubsystem with a custom motor name.
     * @param hardwareMap The HardwareMap of the OpMode
     * @param motorName The motor name in the configuration
     */
    public TurretSubsystem(HardwareMap hardwareMap, String motorName) {
        // Initialize motor
        motor = new MotorEx(hardwareMap, motorName, Motor.GoBILDA.RPM_312);
        
        // Configure motor
        motor.setInverted(false);  // Adjust according to mounting
        motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        motor.setRunMode(Motor.RunMode.RawPower);  // Use our own PID
        
        // Configure unit conversion (ticks -> degrees)
        motor.setDistancePerPulse(TurretConstants.DEGREES_PER_TICK);
        
        // Initialize PID controller
        pidController = new PIDController(
            TurretConstants.kP,
            TurretConstants.kI,
            TurretConstants.kD
        );
        pidController.setTolerance(TurretConstants.POSITION_TOLERANCE_DEG);
        
        // Initialize state
        currentState = TurretState.IDLE;
        targetAngleDeg = 0.0;
        homeOffsetTicks = 0.0;
        isHomed = false;
        
        // Initialize cache
        currentPositionDeg = 0.0;
        currentVelocityDegPerSec = 0.0;
        lastPositionTicks = 0.0;
        lastUpdateTimeMs = System.currentTimeMillis();
        
        // Reset encoder
        motor.resetEncoder();
    }
    
    @Override
    public void periodic() {
        // Update sensor readings
        updateSensorCache();
        
        // Execute logic based on state
        switch (currentState) {
            case POSITION:
            case TRACKING:
                executePositionControl();
                break;
            case MANUAL:
                break;
            case HOMING:
                break;
            case IDLE:
            default:
                break;
        }
    }
    
    /**
     * Updates the sensor readings cache.
     */
    private void updateSensorCache() {
        long currentTimeMs = System.currentTimeMillis();
        double deltaTimeSec = (currentTimeMs - lastUpdateTimeMs) / 1000.0;
        
        // Get current position in ticks and convert to degrees
        double currentTicks = motor.getCurrentPosition();
        currentPositionDeg = TurretConstants.ticksToDegrees(currentTicks - homeOffsetTicks);
        
        // Calculate velocity
        if (deltaTimeSec > 0) {
            double deltaTicks = currentTicks - lastPositionTicks;
            currentVelocityDegPerSec = TurretConstants.ticksToDegrees(deltaTicks) / deltaTimeSec;
        }
        
        // Update previous values
        lastPositionTicks = currentTicks;
        lastUpdateTimeMs = currentTimeMs;
    }
    
    /**
     * Executes the position PID control.
     */
    private void executePositionControl() {
        // Update PID coefficients (allows tuning in real-time)
        pidController.setPID(
            TurretConstants.kP,
            TurretConstants.kI,
            TurretConstants.kD
        );
        
        // Calculate PID output
        double output = pidController.calculate(currentPositionDeg, targetAngleDeg);
        
        // Add feed-forward if necessary
        output += Math.signum(targetAngleDeg - currentPositionDeg) * TurretConstants.kF;
        
        // Limit power
        output = clampPower(output);
        
        // Verify soft limits before applying power
        if (!isSafeToMove(output)) {
            output = 0;
        }
        
        motor.set(output);
    }
    
    /**
     * Sets the target position of the turret.
     * Changes to the POSITION state and uses PID control.
     * @param angleDeg Target angle in degrees (0 = front of the robot)
     */
    public void setTargetPosition(double angleDeg) {
        // Limit to allowed range
        targetAngleDeg = TurretConstants.clampAngle(angleDeg);
        currentState = TurretState.POSITION;
        pidController.reset();  // Reset integral accumulator
    }
    
    /**
     * Sets the target position for tracking (without resetting PID).
     * Used for continuous tracking of dynamic targets.
     * @param angleDeg Target angle in degrees
     */
    public void setTrackingTarget(double angleDeg) {
        targetAngleDeg = TurretConstants.clampAngle(angleDeg);
        if (currentState != TurretState.TRACKING) {
            currentState = TurretState.TRACKING;
            pidController.reset();
        }
    }

    /**
     * Adjusts the target angle relative to the current position.
     * Useful for vision loops where a relative error (bearing) is calculated.
     * @param deltaDeg Change in degrees (positive = left/counter-clockwise)
     */
    public void adjustAngle(double deltaDeg) {
        // Use setTrackingTarget to not reset PID if already tracking
        setTrackingTarget(currentPositionDeg + deltaDeg);
    }
    
    /**
     * Manual control of the turret with direct power.
     * @param power Motor power (-1.0 to 1.0)
     */
    public void setManualPower(double power) {
        currentState = TurretState.MANUAL;
        
        // Scale power
        double scaledPower = power * TurretConstants.MANUAL_POWER_SCALE;
        
        // Verify soft limits
        if (!isSafeToMove(scaledPower)) {
            scaledPower = 0;
        }
        
        motor.set(scaledPower);
    }
    
    /**
     * Stops the turret and changes to IDLE state.
     */
    public void stop() {
        currentState = TurretState.IDLE;
        motor.set(0);
        pidController.reset();
    }
    
    /**
     * Sets the current position as "home" (0 degrees).
     * Use when the turret is physically at the reference position.
     */
    public void setCurrentPositionAsHome() {
        homeOffsetTicks = motor.getCurrentPosition();
        currentPositionDeg = 0.0;
        targetAngleDeg = 0.0;
        isHomed = true;
    }
    
    /**
     * Resets the home offset (for re-calibration).
     */
    public void resetHome() {
        homeOffsetTicks = 0.0;
        isHomed = false;
        motor.resetEncoder();
    }
    
    /**
     * Changes to homing state.
     */
    public void startHoming() {
        currentState = TurretState.HOMING;
    }
    
    /**
     * Moves the turret with direct power (for homing).
     * Does not change the state.
     * @param power Motor power (-1.0 to 1.0)
     */
    public void setRawPower(double power) {
        motor.set(clampPower(power));
    }
    
    /**
     * @return Current state of the turret
     */
    public TurretState getState() {
        return currentState;
    }
    
    /**
     * @return Current state name (for telemetry)
     */
    public String getStateName() {
        return currentState.name();
    }
    
    /**
     * @return Current position in degrees relative to home
     */
    public double getCurrentAngleDeg() {
        return currentPositionDeg;
    }
    
    /**
     * @return Current target angle in degrees
     */
    public double getTargetAngleDeg() {
        return targetAngleDeg;
    }
    
    /**
     * @return Position error (target - current) in degrees
     */
    public double getPositionErrorDeg() {
        return targetAngleDeg - currentPositionDeg;
    }
    
    /**
     * @return Current velocity in degrees per second
     */
    public double getVelocityDegPerSec() {
        return currentVelocityDegPerSec;
    }
    
    /**
     * @return true if the turret is at the target position (within tolerance)
     */
    public boolean isAtTarget() {
        return Math.abs(getPositionErrorDeg()) <= TurretConstants.POSITION_TOLERANCE_DEG;
    }
    
    /**
     * @return true if the turret is at the target position and stable (low velocity)
     */
    public boolean isAtTargetAndStable() {
        return isAtTarget() && 
               Math.abs(currentVelocityDegPerSec) <= TurretConstants.VELOCITY_TOLERANCE_DEG_PER_SEC;
    }
    
    /**
     * @return true if the turret has been calibrated (home set)
     */
    public boolean isHomed() {
        return isHomed;
    }
    
    /**
     * @return true if the current state is IDLE
     */
    public boolean isIdle() {
        return currentState == TurretState.IDLE;
    }
    
    /**
     * @return true if the turret is in position control (POSITION or TRACKING)
     */
    public boolean isInPositionControl() {
        return currentState == TurretState.POSITION || currentState == TurretState.TRACKING;
    }
    
    /**
     * @return Current position of the encoder in ticks (raw)
     */
    public double getRawEncoderTicks() {
        return motor.getCurrentPosition();
    }
    
    /**
     * @return Current motor power
     */
    public double getMotorPower() {
        return motor.get();
    }
    
    /**
     * @return true if a specific angle is within the allowed range
     */
    public boolean isAngleReachable(double angleDeg) {
        return TurretConstants.isAngleInRange(angleDeg);
    }
    
    /**
     * @return true if it is safe to move in the specified direction
     */
    private boolean isSafeToMove(double power) {
        // If not homed, allow movement (for manual homing)
        if (!isHomed) {
            return true;
        }
        
        double minAngle = TurretConstants.MIN_ANGLE_DEG + TurretConstants.SOFT_LIMIT_MARGIN_DEG;
        double maxAngle = TurretConstants.MAX_ANGLE_DEG - TurretConstants.SOFT_LIMIT_MARGIN_DEG;
        
        // Si está en el límite inferior y quiere ir más negativo
        if (currentPositionDeg <= minAngle && power < 0) {
            return false;
        }
        
        // Si está en el límite superior y quiere ir más positivo
        if (currentPositionDeg >= maxAngle && power > 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * @return Power clamped to [-MAX_POWER, MAX_POWER]
     */
    private double clampPower(double power) {
        return Math.max(-TurretConstants.MAX_POWER, 
                       Math.min(TurretConstants.MAX_POWER, power));
    }
}
