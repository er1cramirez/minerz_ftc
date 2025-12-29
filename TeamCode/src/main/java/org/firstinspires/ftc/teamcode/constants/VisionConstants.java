package org.firstinspires.ftc.teamcode.constants;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Constants file for the VisionSubsystem.
 */
public class VisionConstants {
    /** Name of the camera in the robot configuration */
    public static final String CAMERA_NAME = "Camera_1";

    /** Offset of the camera with respect to the turret (inches) */
    // Relative distance between the turret center and the robot center(turret from robot center),
    // Its suposed that the offset is only in the x direction
    public static final double D_R_T = -6.0;  // Inches
    // Relative distance between the camera center and the turret center(camera from turret center),
    // Its suposed that the offset is only in the x direction
    public static final double D_T_C = -3.0;  // Inches

    public static final double CAMERA_FORWARD_OFFSET_INCHES = 0.0;  // TODO: Calibrate
    public static final double CAMERA_LEFT_OFFSET_INCHES = 0.0;     // TODO: Calibrate
    public static final double CAMERA_UP_OFFSET_INCHES = 6.0;       // TODO: Calibrate
    public static final double CAMERA_HEADING_OFFSET_DEG = 0.0;     // TODO: Calibrate

    public static final double TURRET_FORWARD_OFFSET_INCHES = 0.0;  // TODO: Calibrar
    public static final double TURRET_LEFT_OFFSET_INCHES = 0.0;     // TODO: Calibrar

    public static final double FX = 545.55;  // Focal length X
    public static final double FY = 544.19;  // Focal length Y
    public static final double CX = 325.87;  // Principal point X
    public static final double CY = 259.50;  // Principal point Y

    public static final boolean USE_MANUAL_EXPOSURE = true;
    public static final int MANUAL_EXPOSURE_MS = 6;  // TODO: Calibrar con tuning OpMode
    public static final int MANUAL_GAIN = 250;       // TODO: Calibrar con tuning OpMode

    // TAG IDs - DECODE 2025-2026
    public static final int TAG_SEQUENCE_GPP = 21;  // Green-Purple-Purple
    public static final int TAG_SEQUENCE_PGP = 22;  // Purple-Green-Purple
    public static final int TAG_SEQUENCE_PPG = 23;  // Purple-Purple-Green

    // Goals
    public static final int TAG_GOAL_RED = 24;
    public static final int TAG_GOAL_BLUE = 20;

    /**
     * Returns the sequence string from the tag ID.
     * @return "GPP", "PGP", "PPG" or null if it is not a sequence tag
     */
    public static String getSequenceFromTagId(int tagId) {
        switch (tagId) {
            case TAG_SEQUENCE_GPP: return "GPP";
            case TAG_SEQUENCE_PGP: return "PGP";
            case TAG_SEQUENCE_PPG: return "PPG";
            default: return null;
        }
    }

    /**
     * Maximum ambiguity allowed (0 = perfect, >0.2 = poor).
     * The SDK uses decisionMargin = 1/ambiguity.
     * With MAX_AMBIGUITY = 0.15, we require decisionMargin >= 6.67
     */
    public static final double MAX_AMBIGUITY = 0.15;
    
    /** Minimum detection range in inches */
    public static final double MIN_DETECTION_RANGE_INCHES = 6.0;
    
    /** Maximum detection range in inches (~200" = diagonal of field) */
    public static final double MAX_DETECTION_RANGE_INCHES = 200.0;

    /** Distance unit */
    public static final DistanceUnit DISTANCE_UNIT = DistanceUnit.INCH;
    /** Angle unit */
    public static final AngleUnit ANGLE_UNIT = AngleUnit.DEGREES;

    /** Streaming width */
    public static final int STREAM_WIDTH = 640;
    /** Streaming height */
    public static final int STREAM_HEIGHT = 480;

    /** Detection timeout in milliseconds */
    public static final int DETECTION_TIMEOUT_MS = 3000;

    /** Enable live view (disable in competition to save CPU) */
    public static final boolean ENABLE_LIVE_VIEW = false;

}