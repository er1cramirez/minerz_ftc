package org.firstinspires.ftc.teamcode.constants;

/**
 * Physical constants for the robot.
 * All distances in INCHES, all angles in RADIANS.
 */
public final class RobotConstants {
    
    // Prevent instantiation
    private RobotConstants() {}
    
    /**
     * Drivetrain dimensions
     */
    public static final class DrivetrainDimensions {
        // Wheel diameter in inches
        public static final double WHEEL_DIAMETER_INCHES = 4.094; // 104mm converted
        
        // Distance between left and right wheels (center to center) in inches
        public static final double TRACK_WIDTH_INCHES = 13.69; // 347.7mm converted
        
        // Distance between front and back wheels (center to center) in inches
        public static final double WHEEL_BASE_INCHES = 13.23; // 336mm converted
        
        // Robot dimensions including wheels
        public static final double ROBOT_LENGTH_INCHES = 17.32; // 440mm converted
        public static final double ROBOT_WIDTH_INCHES = 17.78; // 451.7mm converted
    }
    
    /**
     * Hardware configuration names
     */
    public static final class HardwareNames {
        // Drive motors
        public static final String FRONT_LEFT_MOTOR = "frontLeft";
        public static final String FRONT_RIGHT_MOTOR = "frontRight";
        public static final String BACK_LEFT_MOTOR = "backLeft";
        public static final String BACK_RIGHT_MOTOR = "backRight";
        
        // Odometry
        public static final String PINPOINT_ODOMETRY = "od";
        
        // Intake
        public static final String INTAKE_MOTOR = "intake";
        
        // Shooter (future)
        public static final String SHOOTER_MOTOR = "shooter";
    }
}
