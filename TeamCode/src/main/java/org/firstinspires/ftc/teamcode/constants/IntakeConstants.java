package org.firstinspires.ftc.teamcode.constants;

public class IntakeConstants {

    public enum IntakeState {
        IDLE,
        INTAKING,
        OUTTAKING
    }
    
    // Hardware names
    public static final String MOTOR_NAME = "intakeMotor";
    public static final Boolean MOTOR_INVERTED = false;
    
    // Valores de operación
    public static final double INTAKING_POWER = 0.8;
    public static final double OUTTAKING_POWER = -0.8;
    public static final double STOPPED_POWER = 0.0;
    
}