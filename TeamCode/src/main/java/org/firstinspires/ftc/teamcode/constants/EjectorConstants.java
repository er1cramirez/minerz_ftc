package org.firstinspires.ftc.teamcode.constants;

public class EjectorConstants {
    
    // Hardware names
    public static final String SERVO_NAME = "ejector";

    // Servo positions
    public static class Positions {
        public static final double STOW_POSITION = 0.5;
        public static final double EJECT_POSITION = 0.2;
    }
    
    // Timing constants (in milliseconds)
    public static class Timing {
        // Time for servo to reach eject position
        public static final long EJECT_TIME_MS = 200;
        
        // Time to hold in eject position (push the ball)
        public static final long HOLD_TIME_MS = 300;
        
        // Total cycle time (eject + hold)
        public static final long FULL_CYCLE_TIME_MS = EJECT_TIME_MS + HOLD_TIME_MS;
    }
}