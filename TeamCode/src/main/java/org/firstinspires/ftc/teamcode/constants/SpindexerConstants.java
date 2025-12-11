package org.firstinspires.ftc.teamcode.constants;

/**
 * Constantes para el SpindexerSubsystem.
 *
 * IMPORTANTE: Los umbrales de color deben calibrarse usando
 * el OpMode SpindexerSensorCalibration antes de competencia.
 */
public class SpindexerConstants {

    // ==================== HARDWARE NAMES ====================

    public static final String SERVO_NAME = "spindexer";
    public static final String COLOR_SENSOR_NAME = "colorSensor";

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
    // Desfasadas 60° de las posiciones de intake
    public static final double SLOT_0_OUTTAKE_POSITION = degreesToPosition(60.0);
    public static final double SLOT_1_OUTTAKE_POSITION = degreesToPosition(180.0);
    public static final double SLOT_2_OUTTAKE_POSITION = degreesToPosition(300.0);

    // ==================== DETECCIÓN DE PELOTA ====================

    /**
     * Distancia máxima para detectar presencia de pelota (en cm).
     * Si el sensor lee distancia > este valor, asume que no hay pelota.
     *
     * CALIBRAR: Usa SpindexerSensorCalibration para determinar valor óptimo.
     * Valor inicial basado en sensor a 3-4cm de la pelota.
     */
    public static final double BALL_DETECTION_DISTANCE = 3.89;

    // ==================== UMBRALES DE COLOR ====================

    /*
     * IMPORTANTE: Estos son valores iniciales aproximados.
     * DEBES calibrar usando SpindexerSensorCalibration antes de usar.
     *
     * Los valores son PORCENTAJES (0-100) de color normalizado.
     * Ejemplo: Si R=100, G=100, B=50, entonces:
     *   Total = 250
     *   Red% = 40%
     *   Green% = 40%
     *   Blue% = 20%
     */

    // Umbrales para AMARILLO
    // Amarillo típicamente tiene: alto rojo, alto verde, bajo azul
    public static final double YELLOW_RED_MIN = 21.53;      // Rojo mínimo %
    public static final double YELLOW_GREEN_MIN = 43.86;    // Verde mínimo %
    public static final double YELLOW_BLUE_MAX = 29.05;     // Azul máximo %

    // Umbrales para PÚRPURA
    // Púrpura típicamente tiene: alto azul, algo de rojo, bajo verde
    public static final double PURPLE_RED_MIN = 14.80;      // Rojo mínimo %
    public static final double PURPLE_BLUE_MIN = 33.80;     // Azul mínimo %
    public static final double PURPLE_GREEN_MAX = 42.78 ;    // Verde máximo %

    // ==================== CONFIGURACIÓN DE DETECCIÓN ====================

    /**
     * Número de samples para votación durante detección.
     * Más samples = más preciso pero más lento.
     *
     * Recomendado: 15-25
     */
    public static final int DEFAULT_VOTING_SAMPLES = 20;

    /**
     * Delay entre samples durante votación (en milisegundos).
     *
     * Recomendado: 30-100ms
     */
    public static final int DEFAULT_VOTING_DELAY_MS = 50;

    // ==================== TIMEOUTS ====================

    /**
     * Tiempo máximo esperado para que el servo llegue a posición (ms).
     * Usado por comandos para timeout safety.
     */
    public static final int SERVO_MOVEMENT_TIMEOUT_MS = 1000;

    /**
     * Tiempo máximo para detección de color (ms).
     * Si la detección tarda más, se cancela.
     */
    public static final int DETECTION_TIMEOUT_MS = 2000;
}