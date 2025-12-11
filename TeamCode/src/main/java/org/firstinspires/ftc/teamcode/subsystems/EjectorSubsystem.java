package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.servos.ServoEx;

import org.firstinspires.ftc.teamcode.constants.EjectorConstants;

/**
 * Subsystem for the ball ejector mechanism.
 * A simple servo with two positions:
 * - Stow: retracted, not interfering with the ball path
 * - Eject: extended, pushing the ball into the shooter
 * 
 * Provides both manual control and automatic eject->stow sequences with timeout.
 */
public class EjectorSubsystem extends SubsystemBase {

    /**
     * Enum representing ejector states.
     */
    public enum EjectorState {
        STOWED,
        EJECTING,
        RETURNING  // In the process of returning to stow after eject
    }

    private final ServoEx ejectorServo;
    private EjectorState currentState;
    private final ElapsedTime cycleTimer;
    private boolean autoReturnEnabled;

    /**
     * Creates a new EjectorSubsystem.
     * @param hardwareMap The hardware map from the OpMode
     */
    public EjectorSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, EjectorConstants.SERVO_NAME);
    }

    /**
     * Creates a new EjectorSubsystem with a custom servo name.
     * @param hardwareMap The hardware map from the OpMode
     * @param servoName The name of the servo in the hardware configuration
     */
    public EjectorSubsystem(HardwareMap hardwareMap, String servoName) {
        ejectorServo = new ServoEx(hardwareMap, servoName);
        cycleTimer = new ElapsedTime();
        currentState = EjectorState.STOWED;
        autoReturnEnabled = false;
        
        // Start in stowed position
        stow();
    }

    /**
     * Periodic method called by the command scheduler.
     * Handles automatic return to stow after eject.
     */
    @Override
    public void periodic() {
        if (autoReturnEnabled && currentState == EjectorState.EJECTING) {
            // Check if enough time has passed to return to stow
            if (cycleTimer.milliseconds() >= EjectorConstants.Timing.FULL_CYCLE_TIME_MS) {
                stow();
                autoReturnEnabled = false;
            }
        }
    }

    // ==================== Manual Control Methods ====================

    /**
     * Moves the ejector to the stow position immediately.
     */
    public void stow() {
        ejectorServo.set(EjectorConstants.Positions.STOW_POSITION);
        currentState = EjectorState.STOWED;
        autoReturnEnabled = false;
    }

    /**
     * Moves the ejector to the eject position immediately.
     * Does NOT automatically return to stow.
     */
    public void eject() {
        ejectorServo.set(EjectorConstants.Positions.EJECT_POSITION);
        currentState = EjectorState.EJECTING;
    }

    /**
     * Sets the servo to a specific position (for tuning).
     * @param position Servo position (0.0 to 1.0)
     */
    public void setPosition(double position) {
        ejectorServo.set(position);
    }

    // ==================== Automatic Sequence Methods ====================

    /**
     * Starts an eject cycle that automatically returns to stow after the timeout.
     * The sequence is: eject -> hold -> stow
     * 
     * This method returns immediately; the periodic() method handles the timing.
     */
    public void ejectAndStow() {
        eject();
        cycleTimer.reset();
        autoReturnEnabled = true;
    }

    /**
     * Cancels any ongoing automatic return sequence and stows immediately.
     */
    public void cancelAndStow() {
        autoReturnEnabled = false;
        stow();
    }

    // ==================== State Query Methods ====================

    /**
     * Gets the current state of the ejector.
     * @return The current EjectorState
     */
    public EjectorState getState() {
        return currentState;
    }

    /**
     * Checks if the ejector is currently stowed.
     * @return true if stowed
     */
    public boolean isStowed() {
        return currentState == EjectorState.STOWED;
    }

    /**
     * Checks if the ejector is currently ejecting.
     * @return true if ejecting
     */
    public boolean isEjecting() {
        return currentState == EjectorState.EJECTING;
    }

    /**
     * Checks if an automatic eject->stow cycle is in progress.
     * @return true if a cycle is running
     */
    public boolean isCycleInProgress() {
        return autoReturnEnabled;
    }

    /**
     * Gets the elapsed time since the current cycle started.
     * @return Time in milliseconds, or 0 if no cycle is active
     */
    public double getCycleElapsedMs() {
        return autoReturnEnabled ? cycleTimer.milliseconds() : 0;
    }

    /**
     * Gets the current servo position.
     * @return Servo position (0.0 to 1.0)
     */
    public double getServoPosition() {
        return ejectorServo.getRawPosition();
    }
}