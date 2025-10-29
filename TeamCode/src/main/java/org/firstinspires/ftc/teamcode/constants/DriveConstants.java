package org.firstinspires.ftc.teamcode.constants;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Constants for the drive subsystem.
 * All distances in INCHES, all angles in RADIANS.
 */
public final class DriveConstants {
    
    // Prevent instantiation
    private DriveConstants() {}
    
    /**
     * Speed multipliers for different drive modes
     */
    public static final class SpeedMultipliers {
        // Normal/base driving speed
        public static final double NORMAL_SPEED = 0.70;
        
        // Precision mode (hold left bumper)
        public static final double PRECISION_SPEED = 0.35;
        
        // Turbo mode (hold right bumper)
        public static final double TURBO_SPEED = 1.0;
    }
    
    /**
     * Odometry configuration
     */
    public static final class Odometry {
        // Pinpoint offset from robot center in INCHES
        // Computer/IMU is centered in X but aligned with back wheels in Y
        // Negative Y because it's behind the center of the robot
        public static final double X_OFFSET_INCHES = 0.0;
        public static final double Y_OFFSET_INCHES = -6.615; // Half of wheelbase, moved back
        
        // Encoder directions (adjust if odometry reading is inverted)
        public static final GoBildaPinpointDriver.EncoderDirection X_DIRECTION = 
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        public static final GoBildaPinpointDriver.EncoderDirection Y_DIRECTION = 
                GoBildaPinpointDriver.EncoderDirection.FORWARD;
        
        // Pod type
        public static final GoBildaPinpointDriver.GoBildaOdometryPods POD_TYPE = 
                GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD;
        
        // Distance unit for Pinpoint
        public static final DistanceUnit DISTANCE_UNIT = DistanceUnit.INCH;
    }
    
    /**
     * Drive mode settings
     */
    public static final class DriveMode {
        // Default to robot-centric control
        public static final boolean DEFAULT_FIELD_RELATIVE = false;
    }
}
