package org.firstinspires.ftc.teamcode.constants;

/**
 * Constantes para el SpindexerSubsystem.
 *
 * IMPORTANTE: Los umbrales de color deben calibrarse usando
 * el OpMode SpindexerSensorCalibration antes de competencia.
 */
public class SpindexerConstants {

    // ==================== HARDWARE NAMES ====================

    public static final String SERVO_NAME = "spindexServo";
    public static final String COLOR_SENSOR_NAME = "colorSensor";
    
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

    // ==================== POSICIONES DEL SERVO ====================

    // Rango del servo goBILDA
    private static final double MAX_DEGREE = 300.0;

    /**
     * Convierte grados a posición de servo normalizada (0.0 - 1.0).
     *
     * @param degrees Ángulo en grados (0-300)
     * @return Posición normalizada
     */
    private static double degreesToPosition(double degrees) {
        return degrees / MAX_DEGREE;
    }

    // Posiciones de intake (donde entra la pelota)
    public static final double SLOT_0_INTAKE_POSITION = degreesToPosition(0.0);
    public static final double SLOT_1_INTAKE_POSITION = degreesToPosition(120.0);
    public static final double SLOT_2_INTAKE_POSITION = degreesToPosition(240.0);

    // Posiciones de outtake (donde se lanza la pelota)
    // Desfasadas 180° de las posiciones de intake
    public static final double SLOT_0_OUTTAKE_POSITION = degreesToPosition(180.0);
    public static final double SLOT_1_OUTTAKE_POSITION = degreesToPosition(300.0);
    public static final double SLOT_2_OUTTAKE_POSITION = degreesToPosition(60.0);

}