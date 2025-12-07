package org.firstinspires.ftc.teamcode.subsystems;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.servos.ServoEx;

import org.firstinspires.ftc.teamcode.constants.EjectorConstants;
import org.firstinspires.ftc.teamcode.constants.EjectorConstants.EjectorState;

public class EjectorSubsystem extends SubsystemBase {
    private EjectorState currentState;

    // Hardware
    private ServoEx ejectorServo;

    // Constructor
    public EjectorSubsystem() {
        currentState = EjectorState.STOWED;
        ejectorServo = new ServoEx(hardwareMap, EjectorConstants.SERVO_NAME);
        ejectorServo.set(EjectorConstants.STOW_POSITION);
    }

    public void eject() {
        currentState = EjectorState.EJECTING;
        ejectorServo.set(EjectorConstants.EJECT_POSITION);
    }
    public void stow() {
        currentState = EjectorState.RETURNING;
        ejectorServo.set(EjectorConstants.STOW_POSITION);
        currentState = EjectorState.STOWED;
    }

    public EjectorState getState() { return currentState; }
    public boolean isStowed() { return currentState == EjectorState.STOWED; }
}