package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;
import com.seattlesolvers.solverslib.controller.PIDController;

import org.firstinspires.ftc.teamcode.constants.TurretConstants;

/**
 * Turret subsystem with vision tracking support.
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Position control in degrees with PID</li>
 *   <li>Soft limits to protect wiring</li>
 *   <li>Relative position tracking</li>
 *   <li>Vision-based auto-aiming with target acquisition states</li>
 * </ul>
 *
 * <h2>States:</h2>
 * <ul>
 *   <li><b>IDLE:</b> Motor off</li>
 *   <li><b>HOMING:</b> Calibration routine</li>
 *   <li><b>MANUAL:</b> Direct power control</li>
 *   <li><b>POSITION:</b> PID to fixed position</li>
 *   <li><b>TRACKING:</b> PID with continuous target updates</li>
 * </ul>
 *
 * <h2>Vision Tracking States:</h2>
 * <ul>
 *   <li><b>DISABLED:</b> Vision tracking off</li>
 *   <li><b>SEARCHING:</b> Waiting for target acquisition</li>
 *   <li><b>LOCKED:</b> Actively tracking, within tolerance</li>
 *   <li><b>ACQUIRING:</b> Target visible but not yet stable</li>
 *   <li><b>HOLDING:</b> Target lost, maintaining last position</li>
 * </ul>
 *
 * <p>Position 0° represents the turret pointing forward.
 * Positive angles = counter-clockwise rotation (seen from above).
 * Negative angles = clockwise rotation.</p>
 */
public class TurretSubsystem extends SubsystemBase {

    // ===== TURRET STATES (hardware/control mode) =====
    public enum TurretState {
        IDLE,
        HOMING,
        MANUAL,
        POSITION,
        TRACKING
    }

    // ===== VISION TRACKING STATES (acquisition status) =====
    public enum VisionTrackingState {
        /** Vision tracking is disabled */
        DISABLED,
        /** Waiting for target - no valid detection yet */
        SEARCHING,
        /** Target visible, actively tracking, within tolerance */
        LOCKED,
        /** Target visible but not yet within tolerance */
        ACQUIRING,
        /** Target lost, holding last known position */
        HOLDING
    }

    // ===== HARDWARE =====
    private final MotorEx motor;
    private final PIDController pidController;

    // ===== TURRET STATE =====
    private TurretState currentState;
    private double targetAngleDeg;
    private double homeOffsetTicks;
    private boolean isHomed;

    // ===== SENSOR CACHE =====
    private double currentPositionDeg;
    private double currentVelocityDegPerSec;
    private double lastPositionTicks;
    private long lastUpdateTimeMs;

    // ===== VISION TRACKING STATE =====
    private VisionTrackingState visionState;
    private double lastValidBearing;
    private double lastKnownTargetAngle;
    private boolean hasEverSeenTarget;
    private final ElapsedTime holdingTimer;
    private final ElapsedTime stateDebounceTimer;
    private int consecutiveDetections;
    private int consecutiveLosses;

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
        motor.setInverted(false);
        motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        motor.setRunMode(Motor.RunMode.RawPower);

        // Configure unit conversion (ticks -> degrees)
        motor.setDistancePerPulse(TurretConstants.DEGREES_PER_TICK);

        // Initialize PID controller
        pidController = new PIDController(
                TurretConstants.kP,
                TurretConstants.kI,
                TurretConstants.kD
        );
        pidController.setTolerance(TurretConstants.POSITION_TOLERANCE_DEG);

        // Initialize turret state
        currentState = TurretState.IDLE;
        targetAngleDeg = 0.0;
        homeOffsetTicks = 0.0;
        isHomed = false;

        // Initialize sensor cache
        currentPositionDeg = 0.0;
        currentVelocityDegPerSec = 0.0;
        lastPositionTicks = 0.0;
        lastUpdateTimeMs = System.currentTimeMillis();

        // Initialize vision tracking state
        visionState = VisionTrackingState.DISABLED;
        lastValidBearing = 0.0;
        lastKnownTargetAngle = 0.0;
        hasEverSeenTarget = false;
        holdingTimer = new ElapsedTime();
        stateDebounceTimer = new ElapsedTime();
        consecutiveDetections = 0;
        consecutiveLosses = 0;

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
            case HOMING:
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

        double currentTicks = motor.getCurrentPosition();
        currentPositionDeg = TurretConstants.ticksToDegrees(currentTicks - homeOffsetTicks);

        if (deltaTimeSec > 0) {
            double deltaTicks = currentTicks - lastPositionTicks;
            currentVelocityDegPerSec = TurretConstants.ticksToDegrees(deltaTicks) / deltaTimeSec;
        }

        lastPositionTicks = currentTicks;
        lastUpdateTimeMs = currentTimeMs;
    }

    /**
     * Executes the position PID control.
     */
    private void executePositionControl() {
        double output = pidController.calculate(currentPositionDeg, targetAngleDeg);
        output = clampPower(output);

        if (!isSafeToMove(output)) {
            output = 0;
        }

        motor.set(output);
    }

    // =========================================================================
    // VISION TRACKING METHODS
    // =========================================================================

    /**
     * Enables vision tracking mode.
     * Call this to start the auto-aim system.
     * The turret will enter SEARCHING state until a target is detected.
     */
    public void enableVisionTracking() {
        if (visionState == VisionTrackingState.DISABLED) {
            visionState = VisionTrackingState.SEARCHING;
            currentState = TurretState.TRACKING;
            stateDebounceTimer.reset();
            consecutiveDetections = 0;
            consecutiveLosses = 0;

            // Start at current position or last known target
            if (hasEverSeenTarget) {
                targetAngleDeg = lastKnownTargetAngle;
            }
            // Otherwise keep current targetAngleDeg
        }
    }

    /**
     * Disables vision tracking mode.
     * The turret will stop and enter IDLE state.
     */
    public void disableVisionTracking() {
        visionState = VisionTrackingState.DISABLED;
        stop();
    }

    /**
     * Updates the vision tracking with a new bearing measurement.
     * Call this method every loop iteration when vision tracking is enabled.
     *
     * @param bearing The bearing to target in degrees (from vision system).
     *                Null if target is not visible.
     */
    public void updateVisionTracking(Double bearing) {
        if (visionState == VisionTrackingState.DISABLED) {
            return;
        }

        boolean hasValidTarget = (bearing != null);

        // Update detection counters
        if (hasValidTarget) {
            consecutiveDetections++;
            consecutiveLosses = 0;
        } else {
            consecutiveLosses++;
            consecutiveDetections = 0;
        }

        // State machine for vision tracking
        switch (visionState) {
            case SEARCHING:
                handleSearchingState(hasValidTarget, bearing);
                break;
            case ACQUIRING:
                handleAcquiringState(hasValidTarget, bearing);
                break;
            case LOCKED:
                handleLockedState(hasValidTarget, bearing);
                break;
            case HOLDING:
                handleHoldingState(hasValidTarget, bearing);
                break;
            default:
                break;
        }
    }

    private void handleSearchingState(boolean hasValidTarget, Double bearing) {
        if (hasValidTarget && consecutiveDetections >= TurretConstants.VISION_ACQUISITION_FRAMES) {
            // Target acquired, transition to ACQUIRING
            transitionVisionState(VisionTrackingState.ACQUIRING);
            applyBearingCorrection(bearing);
        }
        // While searching, turret maintains current target position
    }

    private void handleAcquiringState(boolean hasValidTarget, Double bearing) {
        if (!hasValidTarget && consecutiveLosses >= TurretConstants.VISION_LOSS_FRAMES) {
            // Lost target during acquisition
            transitionVisionState(VisionTrackingState.SEARCHING);
        } else if (hasValidTarget) {
            applyBearingCorrection(bearing);

            // Check if we're locked (within tolerance and stable)
            if (isAtTargetAndStable() && Math.abs(bearing) < TurretConstants.VISION_BEARING_TOLERANCE_DEG) {
                transitionVisionState(VisionTrackingState.LOCKED);
            }
        }
    }

    private void handleLockedState(boolean hasValidTarget, Double bearing) {
        if (!hasValidTarget && consecutiveLosses >= TurretConstants.VISION_LOSS_FRAMES) {
            // Lost target, transition to HOLDING
            transitionVisionState(VisionTrackingState.HOLDING);
            holdingTimer.reset();
        } else if (hasValidTarget) {
            applyBearingCorrection(bearing);

            // Check if we lost lock (outside tolerance)
            if (Math.abs(bearing) > TurretConstants.VISION_BEARING_TOLERANCE_DEG * 2) {
                transitionVisionState(VisionTrackingState.ACQUIRING);
            }
        }
    }

    private void handleHoldingState(boolean hasValidTarget, Double bearing) {
        if (hasValidTarget && consecutiveDetections >= TurretConstants.VISION_ACQUISITION_FRAMES) {
            // Target reacquired
            transitionVisionState(VisionTrackingState.ACQUIRING);
            applyBearingCorrection(bearing);
        } else if (holdingTimer.seconds() > TurretConstants.VISION_HOLDING_TIMEOUT_SEC) {
            // Timeout, go back to searching
            transitionVisionState(VisionTrackingState.SEARCHING);
        }
        // While holding, turret maintains lastKnownTargetAngle
    }

    /**
     * Applies bearing correction to the turret target.
     */
    private void applyBearingCorrection(Double bearing) {
        if (bearing == null) return;

        lastValidBearing = bearing;
        hasEverSeenTarget = true;

        // Only apply correction if bearing is significant
        if (Math.abs(bearing) > TurretConstants.VISION_MIN_BEARING_CHANGE_DEG) {
            double correction = bearing * TurretConstants.VISION_TRACKING_GAIN;
            double newTarget = currentPositionDeg + correction;

            // Update target (clamping happens in setTrackingTarget)
            lastKnownTargetAngle = TurretConstants.clampAngle(newTarget);
            targetAngleDeg = lastKnownTargetAngle;
        }
    }

    /**
     * Transitions to a new vision state with debounce.
     */
    private void transitionVisionState(VisionTrackingState newState) {
        if (stateDebounceTimer.seconds() > TurretConstants.VISION_STATE_DEBOUNCE_SEC) {
            visionState = newState;
            stateDebounceTimer.reset();
        }
    }

    /**
     * @return true if vision tracking is enabled
     */
    public boolean isVisionTrackingEnabled() {
        return visionState != VisionTrackingState.DISABLED;
    }

    /**
     * @return true if the turret is locked on target and ready to shoot
     */
    public boolean isLockedOnTarget() {
        return visionState == VisionTrackingState.LOCKED && isAtTargetAndStable();
    }

    /**
     * @return Current vision tracking state
     */
    public VisionTrackingState getVisionTrackingState() {
        return visionState;
    }

    /**
     * @return Vision tracking state name for telemetry
     */
    public String getVisionTrackingStateName() {
        return visionState.name();
    }

    /**
     * @return Last valid bearing from vision (degrees)
     */
    public double getLastValidBearing() {
        return lastValidBearing;
    }

    /**
     * @return true if a target has ever been seen since enabling
     */
    public boolean hasEverSeenTarget() {
        return hasEverSeenTarget;
    }

    // =========================================================================
    // EXISTING TURRET METHODS (unchanged)
    // =========================================================================

    /**
     * Sets the target position of the turret.
     * Changes to the POSITION state and uses PID control.
     * @param angleDeg Target angle in degrees (0 = front of the robot)
     */
    public void setTargetPosition(double angleDeg) {
        targetAngleDeg = TurretConstants.clampAngle(angleDeg);
        currentState = TurretState.POSITION;
        visionState = VisionTrackingState.DISABLED; // Disable vision tracking
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
     * @param deltaDeg Change in degrees (positive = left/counter-clockwise)
     */
    public void adjustAngle(double deltaDeg) {
        setTrackingTarget(currentPositionDeg + deltaDeg);
    }

    /**
     * Manual control of the turret with direct power.
     * @param power Motor power (-1.0 to 1.0)
     */
    public void setManualPower(double power) {
        currentState = TurretState.MANUAL;
        visionState = VisionTrackingState.DISABLED; // Disable vision tracking

        double scaledPower = power * TurretConstants.MANUAL_POWER_SCALE;

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
     * @param power Motor power (-1.0 to 1.0)
     */
    public void setRawPower(double power) {
        motor.set(clampPower(power));
    }

    // =========================================================================
    // GETTERS
    // =========================================================================

    public TurretState getState() {
        return currentState;
    }

    public String getStateName() {
        return currentState.name();
    }

    public double getCurrentAngleDeg() {
        return currentPositionDeg;
    }

    public double getTargetAngleDeg() {
        return targetAngleDeg;
    }

    public double getPositionErrorDeg() {
        return targetAngleDeg - currentPositionDeg;
    }

    public double getVelocityDegPerSec() {
        return currentVelocityDegPerSec;
    }

    public boolean isAtTarget() {
        return Math.abs(getPositionErrorDeg()) <= TurretConstants.POSITION_TOLERANCE_DEG;
    }

    public boolean isAtTargetAndStable() {
        return isAtTarget() &&
                Math.abs(currentVelocityDegPerSec) <= TurretConstants.VELOCITY_TOLERANCE_DEG_PER_SEC;
    }

    public boolean isHomed() {
        return isHomed;
    }

    public boolean isIdle() {
        return currentState == TurretState.IDLE;
    }

    public boolean isInPositionControl() {
        return currentState == TurretState.POSITION || currentState == TurretState.TRACKING;
    }

    public double getRawEncoderTicks() {
        return motor.getCurrentPosition();
    }

    public double getMotorPower() {
        return motor.get();
    }

    public boolean isAngleReachable(double angleDeg) {
        return TurretConstants.isAngleInRange(angleDeg);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private boolean isSafeToMove(double power) {
        if (!isHomed) {
            return true;
        }

        double minAngle = TurretConstants.MIN_ANGLE_DEG + TurretConstants.SOFT_LIMIT_MARGIN_DEG;
        double maxAngle = TurretConstants.MAX_ANGLE_DEG - TurretConstants.SOFT_LIMIT_MARGIN_DEG;

        if (currentPositionDeg <= minAngle && power < 0) {
            return false;
        }

        if (currentPositionDeg >= maxAngle && power > 0) {
            return false;
        }

        return true;
    }

    private double clampPower(double power) {
        return Math.max(-TurretConstants.MAX_POWER,
                Math.min(TurretConstants.MAX_POWER, power));
    }
    
    /**
     * Returns a compact status string for telemetry.
     * Format: 🔄 ↻45° 🎯LOCKED
     */
    public String getCompactStatus() {
        String stateIcon;
        String stateName;
        switch (visionState) {
            case LOCKED:
                stateIcon = "🎯";
                stateName = "LOCKED";
                break;
            case ACQUIRING:
                stateIcon = "🔵";
                stateName = "ACQ";
                break;
            case SEARCHING:
                stateIcon = "🔍";
                stateName = "SEARCH";
                break;
            case HOLDING:
                stateIcon = "⏸";
                stateName = "HOLD";
                break;
            default:
                stateIcon = "⚫";
                stateName = currentState.name();
        }
        return String.format("🔄 ↻%.0f° %s%s", currentPositionDeg, stateIcon, stateName);
    }
}