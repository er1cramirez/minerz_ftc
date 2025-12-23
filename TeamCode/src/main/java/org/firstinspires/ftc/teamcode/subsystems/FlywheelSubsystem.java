package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

import org.firstinspires.ftc.teamcode.constants.FlywheelConstants;

/**
 * Flywheel subsystem
 * 
 * Features:
 * - Velocity control with Feed-Forward + P
 * - ks + kv * targetRPM + ka * acceleration
 * - kp * error
 * - Automatic voltage compensation
 */
public class FlywheelSubsystem extends SubsystemBase {
    public enum FlywheelState {
        IDLE, // Motor is not running
        SPINNING_UP, // Motor is accelerating to target speed
        AT_SPEED, // Motor is at target speed
        IDLE_SPIN // Motor is spinning at a low speed for stability
    }
    
    private final MotorEx motor;
    private final VoltageSensor voltageSensor;
    
    private FlywheelState currentState;
    private double targetRPM;
    private double currentRPM;

    private double lastUpdateTimeMs;
    
    private final ElapsedTime stabilityTimer;
    private final ElapsedTime spinupTimer;
    private boolean isStable;
    
    private double currentVoltage;
    private double compensationFactor;
    
    /**
     * FlywheelSubsystem constructor
     * @param hardwareMap
     */
    public FlywheelSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, FlywheelConstants.MOTOR_NAME);
    }
    
    /**
     * FlywheelSubsystem constructor with custom motor name
     * @param hardwareMap
     * @param motorName
     */
    public FlywheelSubsystem(HardwareMap hardwareMap, String motorName) {
        // Inicializar motor
        motor = new MotorEx(
                hardwareMap,
                motorName,
                (int) FlywheelConstants.ENCODER_PPR,
                FlywheelConstants.MOTOR_FREE_RPM
        );
        
        // Configure motor
        motor.setInverted(false);  // Adjust according to mounting
        motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.FLOAT);  // Float for flywheel
        motor.setRunMode(Motor.RunMode.RawPower);
        
        // Get voltage sensor
        voltageSensor = hardwareMap.voltageSensor.iterator().next();
        
        // Initialize state
        currentState = FlywheelState.IDLE;
        targetRPM = 0;
        currentRPM = 0;

        
        // Initialize timing
        stabilityTimer = new ElapsedTime();
        spinupTimer = new ElapsedTime();
        lastUpdateTimeMs = System.currentTimeMillis();
        isStable = false;
        
        // Initialize telemetry
        currentVoltage = FlywheelConstants.NOMINAL_VOLTAGE;
        compensationFactor = 1.0;
    }
    
    @Override
    public void periodic() {
        // 1. Update velocity reading
        updateVelocityReading();
        
        // 2. Update voltage reading
        updateVoltageReading();
        
        // 3. Execute control based on state
        switch (currentState) {
            case SPINNING_UP:
            case AT_SPEED:
            case IDLE_SPIN:
                executeVelocityControl();
                break;
                
            case IDLE:
            default:
                // Asegurar que el motor esté apagado
                motor.set(0);
                break;
        }
        
        // 4. Update state
        updateState();
    }
    
    /**
     * Read the current encoder velocity.
     */
    private void updateVelocityReading() {
        // Get encoder velocity (ticks/second)
        double velocityTicksPerSec = motor.getCorrectedVelocity();
        // Convert to RPM
        currentRPM = FlywheelConstants.ticksPerSecToRPM(velocityTicksPerSec);
        // Update time
        lastUpdateTimeMs = System.currentTimeMillis();
    }
    
    /**
     * Read the current battery voltage.
     */
    private void updateVoltageReading() {
        currentVoltage = voltageSensor.getVoltage();
        // Calculate compensation factor
        if (FlywheelConstants.VOLTAGE_COMPENSATION_ENABLED) {
            // Protect against very low voltages
            double safeVoltage = Math.max(FlywheelConstants.MIN_VOLTAGE, currentVoltage);
            compensationFactor = FlywheelConstants.NOMINAL_VOLTAGE / safeVoltage;
        } else {
            compensationFactor = 1.0;
        }
    }
    
    /**
     * Execute velocity control: Feed-Forward + PID + Compensation.
     */
    private void executeVelocityControl() {
        // 1. Calculate error
        double error = targetRPM - currentRPM;
        // 2. Calculate Feed-Forward
        // 3. Calculate PID
        // 4. Combine FF + PID
        double rawOutput = calculateFeedForward(error) + calculateProportional(error);
        // 5. Apply voltage compensation
        double compensatedOutput = rawOutput * compensationFactor;
        // 6. Limit output
        double lastTotalOutput = clampOutput(compensatedOutput);
        // 7. Apply to  motor
        motor.set(lastTotalOutput);
    }
    
    /**
     * Calculate the Feed-Forward component.
     * FF = kS + kV × targetRPM + kA × aceleración_deseada
     */
    private double calculateFeedForward(double error) {
        if (targetRPM <= 0) {
            return 0;
        }
        // Static component (overcome friction)
        double staticFF = FlywheelConstants.kS;
        // Velocity component (proportional to target)
        double velocityFF = FlywheelConstants.kV * targetRPM;
        // Acceleration component (boost during spin-up)
        double accelFF = 0;
        if (currentState == FlywheelState.SPINNING_UP && error > 0) {
            // More boost when further from target
            accelFF = FlywheelConstants.kA * error;
        }
        
        return staticFF + velocityFF + accelFF;
    }
    
    /**
     * Calculate the PID component (primarily P for velocity).
     */
    private double calculateProportional(double error) {
        // Proportional
        return FlywheelConstants.kP * error;
    }
    
    /**
     * Limit output to the allowed range.
     */
    private double clampOutput(double output) {
        if (output < FlywheelConstants.MIN_POWER) {
            return FlywheelConstants.MIN_POWER;
        }
        if (output > FlywheelConstants.MAX_POWER) {
            return FlywheelConstants.MAX_POWER;
        }
        return output;
    }
    
   
    
    /**
     * Updates the flywheel state based on current velocity.
     */
    private void updateState() {
        if (currentState == FlywheelState.IDLE) {
            isStable = false;
            return;
        }
        
        double error = Math.abs(targetRPM - currentRPM);
        boolean withinTolerance = error <= FlywheelConstants.RPM_TOLERANCE;
        
        // Determine if it's in idle or firing speed
        boolean isIdleTarget = Math.abs(targetRPM - FlywheelConstants.IDLE_SPIN_RPM) < 100;
        
        if (withinTolerance) {
            // Within tolerance
            if (currentState == FlywheelState.SPINNING_UP) {
                // Just reached the target
                stabilityTimer.reset();
                if (isIdleTarget) {
                    currentState = FlywheelState.IDLE_SPIN;
                } else {
                    currentState = FlywheelState.AT_SPEED;
                }
            }
            
            // Check stability
            isStable = stabilityTimer.milliseconds() >= FlywheelConstants.STABILITY_TIME_MS;
            
        } else {
            // Out of tolerance
            isStable = false;
            
            if (currentState == FlywheelState.AT_SPEED || currentState == FlywheelState.IDLE_SPIN) {
                // After losing speed, go back to spinning up
                currentState = FlywheelState.SPINNING_UP;
            }
        }
        
        // Check spin-up timeout
        if (currentState == FlywheelState.SPINNING_UP) {
            if (spinupTimer.milliseconds() > FlywheelConstants.SPINUP_TIMEOUT_MS) {
                // Timeout - something might be wrong, but we keep trying
                // You could add an ERROR state here if you want
            }
        }
    }
    
    /**
     * Sets the flywheel to spin up for the far zone.
     */
    public void spinUpFarZone() {
        setTargetRPM(FlywheelConstants.FAR_ZONE_RPM);
    }
    
    /**
     * Sets the flywheel to idle spin.
     */
    public void idleSpin() {
        setTargetRPM(FlywheelConstants.IDLE_SPIN_RPM);
    }
    
    /**
     * Sets the target RPM for the flywheel.
     * 
     * @param rpm The target RPM
     */
    public void setTargetRPM(double rpm) {
        double clampedRPM = FlywheelConstants.clampRPM(rpm);
        if (Math.abs(clampedRPM - targetRPM) > 50) {
            spinupTimer.reset();
        }
        targetRPM = clampedRPM;
        if (targetRPM > 0) {
            if (currentState == FlywheelState.IDLE) {
                currentState = FlywheelState.SPINNING_UP;
                spinupTimer.reset();
            }
        } else {
            stop();
        }
    }
    
    /**
     * Detiene completamente el flywheel.
     */
    public void stop() {
        targetRPM = 0;
        currentState = FlywheelState.IDLE;
        motor.set(0);
        isStable = false;
    }
    
    // ==================== MÉTODOS DE CONSULTA ====================
    
    /**
     * @return Current state of the flywheel
     */
    public FlywheelState getState() {
        return currentState;
    }
    
    /**
     * @return Name of the current state (for telemetry)
     */
    public String getStateName() {
        return currentState.name();
    }
    
    /**
     * @return Current RPM
     */
    public double getCurrentRPM() {
        return currentRPM;
    }
    
    /**
     * @return Target RPM
     */
    public double getTargetRPM() {
        return targetRPM;
    }
    
    /**
     * @return Velocity error (target - current) in RPM
     */
    public double getVelocityError() {
        return targetRPM - currentRPM;
    }
    
    /**
     * @return true if it is at speed
     */
    public boolean isAtSpeed() {
        return currentState == FlywheelState.AT_SPEED || 
               currentState == FlywheelState.IDLE_SPIN;
    }
    
    /**
     * @return true if it is at speed and stable
     */
    public boolean isReadyToShoot() {
        return currentState == FlywheelState.AT_SPEED && isStable;
    }
    
    /**
     * @return true if it is idle (motor apagado)
     */
    public boolean isIdle() {
        return currentState == FlywheelState.IDLE;
    }
    
    /**
     * @return true if it is idle spinning
     */
    public boolean isIdleSpinning() {
        return currentState == FlywheelState.IDLE_SPIN;
    }
    
    /**
     * @return true if it is spinning up
     */
    public boolean isSpinningUp() {
        return currentState == FlywheelState.SPINNING_UP;
    }
    
    /**
     * @return Battery voltage
     */
    public double getBatteryVoltage() {
        return currentVoltage;
    }
    
    /**
     * @return Voltage compensation factor
     */
    public double getVoltageCompensation() {
        return compensationFactor;
    }
    
    /**
     * @return Stability time in milliseconds
     */
    public double getStabilityTimeMs() {
        return isStable ? stabilityTimer.milliseconds() : 0;
    }
}
