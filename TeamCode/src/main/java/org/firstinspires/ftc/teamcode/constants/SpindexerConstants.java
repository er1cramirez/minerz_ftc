package org.firstinspires.ftc.teamcode.constants;

/**
 * Constantes para el SpindexerSubsystem.
 *
 * IMPORTANTE: Los umbrales de color deben calibrarse usando
 * el OpMode SpindexerSensorCalibration antes de competencia.
 */
public class SpindexerConstants {

    // ==================== HARDWARE NAMES ====================
    public static final String MOTOR_NAME = "spindexMotor";
    public static final String LIMIT_SWITCH_NAME = "spindexLimit";
    public static final String COLOR_SENSOR_NAME = "colorSensor";
    
    // ==================== MOTOR/ENCODER CONFIG ====================
    // goBILDA 312 RPM (5203 series) - 537.7 PPR
    public static final double MOTOR_TICKS_PER_REV = 537.7;
    public static final double GEAR_RATIO = 1.0;  // 1:1 direct drive
    public static final double TICKS_PER_SPINDEXER_REV = MOTOR_TICKS_PER_REV * GEAR_RATIO;
    public static final double TICKS_PER_DEGREE = TICKS_PER_SPINDEXER_REV / 360.0;
    public static final double DEGREES_PER_TICK = 360.0 / TICKS_PER_SPINDEXER_REV;

    // ==================== PD CONTROL ====================
    // Tuned values from SpindexerMotorTuning OpMode
    public static double kP = 0.0075;
    public static double kD = 0.00022;
    public static final double MAX_POWER = 0.65;

    // ==================== TOLERANCES ====================
    public static final double POSITION_TOLERANCE_DEG = 3.0;
    public static final double VELOCITY_TOLERANCE_DEG_PER_SEC = 10.0;

    // ==================== HOMING CONFIG ====================
    public static final double HOMING_POWER = 0.25;
    public static final long HOMING_TIMEOUT_MS = 5000;
    public static final double SMART_HOMING_THRESHOLD_DEG = 15.0;
    
    // Directional offsets - calibrate with SpindexerMotorTuning
    public static double HOME_OFFSET_CCW_DEG = 0.0;
    public static double HOME_OFFSET_CW_DEG = 0.0;
    
    // ==================== SLOT ANGLES (degrees) ====================
    // Intake positions (where ball enters)
    public static final double SLOT_0_INTAKE_ANGLE = 0.0;
    public static final double SLOT_1_INTAKE_ANGLE = 120.0;
    public static final double SLOT_2_INTAKE_ANGLE = 240.0;

    // Outtake positions (where ball is launched) - 180° offset from intake
    public static final double SLOT_0_OUTTAKE_ANGLE = 180.0;
    public static final double SLOT_1_OUTTAKE_ANGLE = 300.0;
    public static final double SLOT_2_OUTTAKE_ANGLE = 60.0;
    
    // ==================== SENSOR THRESHOLDS ====================
    public static final double DISTANCE_BALL_PRESENT = 3.0; // cm

    // GREEN
    public static final float GREEN_HUE_MIN = 119f;
    public static final float GREEN_HUE_MAX = 212f;
    public static final float GREEN_SAT_MIN = 0.49f;

    // PURPLE
    public static final float PURPLE_HUE_MIN = 194f;
    public static final float PURPLE_HUE_MAX = 295f;
    public static final float PURPLE_SAT_MIN = 0.28f;
}