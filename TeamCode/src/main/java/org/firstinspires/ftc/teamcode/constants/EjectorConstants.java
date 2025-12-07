// XConstants.java
package org.firstinspires.ftc.teamcode.constants;

public class EjectorConstants {

    public enum EjectorState {
        STOWED,
        EJECTING,
        RETURNING 
    }
    
    // Hardware names
    public static final String SERVO_NAME = "ejectorServo";

    // Posiciones del servo del eyector en valores normalizados (0.0 a 1.0)
    public static final double STOW_POSITION = 0.0;
    public static final double EJECT_POSITION = 0.5;
    
}