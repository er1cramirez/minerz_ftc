package org.firstinspires.ftc.teamcode.constants;

/**
 * Constants for the Ball Ejector subsystem.
 * Simple servo mechanism with two positions: stow and eject.
 */
public final class EjectorConstants {
    
    // Prevent instantiation
    private EjectorConstants() {}
    
    /**
     * Servo positions (0.0 to 1.0)
     */
    public static final class Positions {
        // Stow position - servo retracted, not interfering with ball
        public static final double STOW_POSITION = 0.0;
        
        // Eject position - servo extended, pushing ball into shooter
        public static final double EJECT_POSITION = 0.5;
    }
    
    /**
     * Timing constants
     */
    public static final class Timing {
        // Time to wait for servo to reach eject position (milliseconds)
        public static final long EJECT_TIME_MS = 300;
        
        // Time to hold in eject position before returning to stow (milliseconds)
        public static final long HOLD_TIME_MS = 200;
        
        // Total cycle time for eject->stow sequence
        public static final long FULL_CYCLE_TIME_MS = EJECT_TIME_MS + HOLD_TIME_MS;
    }
    
    /**
     * Hardware name
     */
    public static final String SERVO_NAME = "ejector_servo";
}
