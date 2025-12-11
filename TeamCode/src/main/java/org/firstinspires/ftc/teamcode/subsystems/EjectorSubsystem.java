package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
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
        STOWED,    // Retraído
        EJECTING   // Extendido
    }

    private final ServoEx ejectorServo;
    private EjectorState currentState;

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
        currentState = EjectorState.STOWED;
        
        // Start in stowed position
        stow();
    }
    public void stow() {
        ejectorServo.set(EjectorConstants.Positions.STOW_POSITION);
        currentState = EjectorState.STOWED;
    }

    public void eject() {
        ejectorServo.set(EjectorConstants.Positions.EJECT_POSITION);
        currentState = EjectorState.EJECTING;
    }

    public void setPosition(double position) {
        ejectorServo.set(position);
    }

    // Métodos de consulta
    public EjectorState getState() { return currentState; }
    public boolean isStowed() { return currentState == EjectorState.STOWED; }
    public boolean isEjecting() { return currentState == EjectorState.EJECTING; }
    public double getServoPosition() { return ejectorServo.getRawPosition(); }
}