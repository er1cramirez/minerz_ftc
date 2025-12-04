package org.firstinspires.ftc.teamcode.constants;

/**
 * Constants for the Shooter subsystem.
 * Flywheel-based shooter with velocity control and angle adjustment servo.
 */
public final class ShooterConstants {
    
    // Prevent instantiation
    private ShooterConstants() {}
    
    /**
     * Motor configuration
     */
    public static final class Motor {
        // Hardware names
        public static final String FLYWHEEL_MOTOR = "shooter";
        public static final String ANGLE_SERVO = "shooterAngle";
        
        // Motor type - using bare 6000 RPM motor for flywheel
        // Adjust based on your actual motor
        public static final com.seattlesolvers.solverslib.hardware.motors.Motor.GoBILDA 
            MOTOR_TYPE = com.seattlesolvers.solverslib.hardware.motors.Motor.GoBILDA.BARE;
    }
    
    /**
     * Velocity control PID/FF coefficients.
     * These need to be tuned for your specific flywheel.
     */
    public static final class VelocityPID {
        // Proportional gain
        public static double kP = 15.0;
        
        // Integral gain (usually 0 for flywheels)
        public static double kI = 0.0;
        
        // Derivative gain (usually 0 for flywheels)
        public static double kD = 0.0;
        
        // Feedforward: velocity gain (most important for flywheels)
        // kV = 1 / max_velocity_in_ticks_per_second * max_motor_power
        public static double kV = 0.00017;
        
        // Feedforward: static friction compensation
        public static double kS = 0.0;
    }
    
    /**
     * Velocity setpoints in ticks per second.
     * Convert RPM to ticks/sec: RPM * encoder_ticks_per_rev / 60
     * For GoBILDA 6000 RPM motor: 28 ticks per rev at motor
     */
    public static final class Velocities {
        // Idle/cruise velocity - keeps flywheel spinning for faster spin-up
        public static final double IDLE_VELOCITY = 1000.0;  // ticks/sec
        
        // Full shooting velocity
        public static final double SHOOT_VELOCITY = 2500.0;  // ticks/sec
        
        // Maximum velocity (for reference)
        public static final double MAX_VELOCITY = 2800.0;  // ticks/sec
        
        // Velocity tolerance for "at target" check
        public static final double VELOCITY_TOLERANCE = 100.0;  // ticks/sec
        
        // Minimum velocity to consider shooter "running"
        public static final double MIN_RUNNING_VELOCITY = 200.0;  // ticks/sec
    }
    
    /**
     * Timing constants
     */
    public static final class Timing {
        // Time to wait for flywheel to reach shooting velocity from idle (ms)
        public static final long SPIN_UP_TIME_MS = 500;
        
        // Time to maintain shooting velocity after shot (ms)
        public static final long POST_SHOT_HOLD_TIME_MS = 200;
        
        // Time between shots in burst mode (ms)
        public static final long BURST_INTERVAL_MS = 300;
    }
    
    /**
     * Angle servo positions for different shot distances.
     * These need to be calibrated for your specific geometry.
     */
    public static final class AnglePositions {
        // Close range shot (e.g., 1-2 meters)
        public static final double CLOSE_RANGE = 0.3;
        
        // Medium range shot (e.g., 2-3 meters)
        public static final double MEDIUM_RANGE = 0.5;
        
        // Long range shot (e.g., 3+ meters)
        public static final double LONG_RANGE = 0.7;
        
        // Stow position (for transport/start)
        public static final double STOW = 0.0;
        
        // Minimum and maximum servo positions
        public static final double MIN_ANGLE = 0.0;
        public static final double MAX_ANGLE = 1.0;
    }
    
    /**
     * Distance-based shooting profiles.
     * Maps distance (in inches or meters) to velocity and angle.
     * These are example values - tune for your robot.
     */
    public static final class ShootingProfiles {
        // Distance thresholds in inches
        public static final double CLOSE_DISTANCE_MAX = 48.0;   // Up to 4 feet
        public static final double MEDIUM_DISTANCE_MAX = 84.0;  // Up to 7 feet
        // Beyond MEDIUM_DISTANCE_MAX = long range
    }
}
