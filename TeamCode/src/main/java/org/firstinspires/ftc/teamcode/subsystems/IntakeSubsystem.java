package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.constants.IntakeConstants;
import org.firstinspires.ftc.teamcode.constants.IntakeConstants.IntakeState; 

public class IntakeSubsystem extends SubsystemBase {
    
    // Hardware
    private final MotorEx intakeMotor;

    private IntakeState currentState;

    public IntakeSubsystem(HardwareMap hardwareMap) {
        intakeMotor = hardwareMap.get(MotorEx.class, IntakeConstants.MOTOR_NAME);
        currentState = IntakeState.IDLE;
        
        intakeMotor.setInverted(IntakeConstants.MOTOR_INVERTED);
        intakeMotor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
    }
    
    public void intake() {
        currentState = IntakeState.INTAKING;
        intakeMotor.set(IntakeConstants.INTAKING_POWER);
    }
    
    public void stop() {
        currentState = IntakeState.IDLE;
        intakeMotor.set(IntakeConstants.STOPPED_POWER);
    }
    public void outtake() {
        currentState = IntakeState.OUTTAKING;
        intakeMotor.set(IntakeConstants.OUTTAKING_POWER);
    }
    
 
    public IntakeState getState() {
        return currentState;
    }

    public boolean isActive() {
        return currentState == IntakeState.INTAKING;
    }
    
    public boolean isIdle() {
        return currentState == IntakeState.IDLE;
    }
    
    public double getCurrentValue() {
        return intakeMotor.getCurrent(CurrentUnit.AMPS);
    }
    
    @Override
    public void periodic() {
    }
}