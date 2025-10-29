package org.firstinspires.ftc.teamcode.constants;

/**
 * Constants for the intake subsystem.
 */
public final class IntakeConstants {
    
    // Prevent instantiation
    private IntakeConstants() {}
    
    /**
     * Motor power levels
     */
    public static final class Power {
        // Power when intaking game elements
        public static final double INTAKE_POWER = 1.0;
        
        // Power when outtaking/ejecting game elements
        public static final double OUTTAKE_POWER = -1.0;
        
        // Power when stopped
        public static final double STOP_POWER = 0.0;
    }
}
