package org.firstinspires.ftc.teamcode.constants;

/**
 * Constantes para la detección de color de pelotas de Decode.
 * 
 * INSTRUCCIONES DE CALIBRACIÓN:
 * 
 * 1. Ejecutar ColorCalibrationOpMode
 * 2. Para cada color de pelota:
 *    a. Seleccionar el color objetivo con X
 *    b. Colocar pelota en el slot
 *    c. Presionar A para iniciar grabación
 *    d. Hacer girar la pelota con intake
 *    e. Presionar A para detener
 *    f. Anotar los valores "UMBRALES SUGERIDOS"
 * 3. Actualizar los valores aquí
 * 4. Probar con ColorDetectionTestOpMode
 * 
 * @author Seattle Solvers
 */
public class ColorDetectionConstants {
    
    // ==================== HARDWARE ====================
    
    public static final String COLOR_SENSOR_NAME = "colorSensor";
    
    // ==================== UMBRALES DE DISTANCIA ====================
    
    /**
     * Distancia máxima (cm) para considerar que hay pelota presente.
     * Si la distancia es menor a este valor, hay una pelota (o parte de ella).
     */
    public static final double DISTANCE_BALL_PRESENT = 3.0;
    
    /**
     * Distancia máxima (cm) para considerar que estamos viendo un agujero.
     * Entre DISTANCE_BALL_PRESENT y este valor, podría ser un agujero.
     * Mayor a este valor = no hay pelota.
     */
    public static final double DISTANCE_HOLE_MAX = 5.0;
    
    // ==================== YELLOW (Amarillo - Testing) ====================
    
    /**
     * Hue mínimo para amarillo.
     * Amarillo típico: 40-60°
     * Con margen de seguridad: 30-70°
     */
    public static final float YELLOW_HUE_MIN = 71f;
    public static final float YELLOW_HUE_MAX = 190f;
    
    /**
     * Saturación mínima para considerar amarillo válido.
     * Evita lecturas de agujeros/sombras.
     */
    public static final float YELLOW_SATURATION_MIN = 0.43f;
    
    /**
     * Value (brillo) mínimo para considerar amarillo válido.
     */
    public static final float YELLOW_VALUE_MIN = 4.98f;
    
    // ==================== GREEN (Verde - Oficial) ====================
    
    /**
     * Hue mínimo/máximo para verde.
     * Verde típico: 90-150°
     * Con margen: 80-150°
     */
    public static final float GREEN_HUE_MIN = 80f;
    public static final float GREEN_HUE_MAX = 150f;
    
    public static final float GREEN_SATURATION_MIN = 0.30f;
    public static final float GREEN_VALUE_MIN = 0.20f;
    
    // ==================== PURPLE (Púrpura - Oficial) ====================
    
    /**
     * Hue mínimo/máximo para púrpura.
     * Púrpura típico: 270-310°
     * Con margen: 260-320°
     * 
     * NOTA: Púrpura puede cruzar 360°/0° en algunos casos.
     */
    public static final float PURPLE_HUE_MIN = 113f;
    public static final float PURPLE_HUE_MAX = 196f;
    
    public static final float PURPLE_SATURATION_MIN = 0.27f;
    public static final float PURPLE_VALUE_MIN = 0.3f;
    
    // ==================== PARÁMETROS DE CONFIRMACIÓN ====================
    
    /**
     * Número de lecturas CONSECUTIVAS del mismo color necesarias
     * para confirmar la detección.
     * 
     * Más alto = más confiable pero más lento.
     * Recomendado: 3-7
     */
    public static final int CONSECUTIVE_READINGS_REQUIRED = 5;
    
    /**
     * Número máximo de lecturas antes de declarar UNKNOWN.
     * Si después de este número no hay confirmación, algo está mal.
     * 
     * Recomendado: 30-50
     */
    public static final int MAX_READINGS_BEFORE_TIMEOUT = 50;
    
    /**
     * Tiempo máximo (ms) para confirmar un color.
     * Timeout de seguridad.
     * 
     * Recomendado: 1500-2500ms
     */
    public static final long CONFIRMATION_TIMEOUT_MS = 2000;
    
    // ==================== FILTROS ADICIONALES ====================
    
    /**
     * Saturación mínima global para cualquier lectura válida.
     * Lecturas con saturación menor se ignoran (probablemente agujero/sombra).
     */
    public static final float GLOBAL_SATURATION_MIN = 0.15f;
    
    /**
     * Value (brillo) mínimo global para cualquier lectura válida.
     */
    public static final float GLOBAL_VALUE_MIN = 0.15f;
    
    // ==================== HELPERS ====================
    
    /**
     * Verifica si un valor HSV cae dentro del rango de un color específico.
     */
    public static boolean isInYellowRange(float hue, float sat, float val) {
        return hue >= YELLOW_HUE_MIN && hue <= YELLOW_HUE_MAX
            && sat >= YELLOW_SATURATION_MIN
            && val >= YELLOW_VALUE_MIN;
    }
    
    public static boolean isInGreenRange(float hue, float sat, float val) {
        return hue >= GREEN_HUE_MIN && hue <= GREEN_HUE_MAX
            && sat >= GREEN_SATURATION_MIN
            && val >= GREEN_VALUE_MIN;
    }
    
    public static boolean isInPurpleRange(float hue, float sat, float val) {
        // Purple puede cruzar 360°
        boolean hueMatch = hue >= PURPLE_HUE_MIN || hue <= (PURPLE_HUE_MAX - 360f);
        return hueMatch
            && sat >= PURPLE_SATURATION_MIN
            && val >= PURPLE_VALUE_MIN;
    }
    
    /**
     * Imprime una representación de los umbrales actuales para debugging.
     */
    public static String getThresholdsSummary() {
        return String.format(
            "=== COLOR THRESHOLDS ===\n" +
            "Distance: present<%.1f, hole<%.1f\n" +
            "YELLOW: H[%.0f-%.0f] S>%.2f V>%.2f\n" +
            "GREEN:  H[%.0f-%.0f] S>%.2f V>%.2f\n" +
            "PURPLE: H[%.0f-%.0f] S>%.2f V>%.2f\n" +
            "Confirm: %d consec, %d max, %dms timeout",
            DISTANCE_BALL_PRESENT, DISTANCE_HOLE_MAX,
            YELLOW_HUE_MIN, YELLOW_HUE_MAX, YELLOW_SATURATION_MIN, YELLOW_VALUE_MIN,
            GREEN_HUE_MIN, GREEN_HUE_MAX, GREEN_SATURATION_MIN, GREEN_VALUE_MIN,
            PURPLE_HUE_MIN, PURPLE_HUE_MAX, PURPLE_SATURATION_MIN, PURPLE_VALUE_MIN,
            CONSECUTIVE_READINGS_REQUIRED, MAX_READINGS_BEFORE_TIMEOUT, CONFIRMATION_TIMEOUT_MS
        );
    }
}
