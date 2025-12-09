// constants/VisionConstants.java
package org.firstinspires.ftc.teamcode.constants;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Constantes para el VisionSubsystem.
 * Incluye calibración de cámara, IDs de AprilTags y parámetros de detección.
 */
public class VisionConstants {
    
    // ===== HARDWARE =====
    public static final String CAMERA_NAME = "Camera_1";
    
    // ===== RESOLUCIÓN Y PERFORMANCE =====
    // OV9281 puede hacer 640x480 @ 60fps o 1280x720 @ 30fps
    public static final int CAMERA_WIDTH = 640;
    public static final int CAMERA_HEIGHT = 480;
    public static final int TARGET_FPS = 60;  // Empezar alto, ajustar si CPU es problema
    
    /**
     * Decimation reduce la resolución de procesamiento.
     * - 1: Máxima calidad (lento)
     * - 2: Balance (RECOMENDADO)
     * - 3: Rápido pero menos preciso
     */
    public static final float DECIMATION = 2.0f;
    
    // ===== APRILTAG IDS =====
    /**
     * IDs de secuencia (solo para detección inicial en auto).
     * 21: Green, Purple, Purple
     * 22: Purple, Green, Purple
     * 23: Purple, Purple, Green
     */
    public static final int[] SEQUENCE_IDS = {21, 22, 23};
    
    /**
     * IDs de targets para aiming.
     * 20: Azul
     * 24: Rojo
     */
    public static final int TARGET_ID_BLUE = 20;
    public static final int TARGET_ID_RED = 24;
    
    // ===== TAMAÑO FÍSICO DEL APRILTAG =====
    /**
     * Tamaño real del cuadrado negro del AprilTag en inches.
     * FTC usa tags de 6 inches.
     */
    public static final double TAG_SIZE = 6.0;  // inches
    
    // ===== POSICIÓN DE CÁMARA EN TURRET =====
    /**
     * La cámara está montada EN EL TURRET, por lo que gira con él.
     * Offsets relativos al centro de rotación del turret.
     * 
     * Sistema de coordenadas:
     * - X: Positivo hacia adelante (dirección del shooter)
     * - Y: Positivo hacia la izquierda
     * - Z: Positivo hacia arriba
     */
    // TODO: MEDIR ESTOS VALORES EN EL ROBOT REAL
    public static final double CAMERA_OFFSET_X = 0.0;  // inches (adelante/atrás)
    public static final double CAMERA_OFFSET_Y = 0.0;  // inches (izquierda/derecha)
    public static final double CAMERA_OFFSET_Z = 0.0;  // inches (arriba/abajo del eje turret)
    
    /**
     * Ángulo de inclinación de la cámara (pitch).
     * - 0°: Horizontal (perpendicular al suelo)
     * - +15°: Inclinada hacia arriba
     * - -15°: Inclinada hacia abajo
     */
    // TODO: MEDIR ESTE VALOR EN EL ROBOT REAL
    public static final double CAMERA_PITCH_DEGREES = 0.0;
    
    // ===== CALIBRACIÓN INTRÍNSECOS (OPCIONAL) =====
    /**
     * Usar calibración personalizada o dejar que VisionPortal use sus defaults.
     * Empezar en FALSE. Si la detección de distancia no es precisa, cambiar a TRUE
     * y calibrar con AprilTag Calibration Tool.
     */
    public static final boolean USE_CUSTOM_CALIBRATION = false;
    
    // Valores típicos para OV9281 (solo si USE_CUSTOM_CALIBRATION = true)
    public static final double FX = 578.272;
    public static final double FY = 578.272;
    public static final double CX = 320.0;
    public static final double CY = 240.0;
    
    // ===== TOLERANCIAS =====
    /**
     * Tolerancia de alineación en grados.
     * Si |bearing| < ALIGNMENT_TOLERANCE, se considera alineado.
     */
    public static final double ALIGNMENT_TOLERANCE_DEGREES = 2.0;
    
    /**
     * Tiempo máximo sin detección antes de considerar target "perdido".
     */
    public static final long TARGET_LOST_TIMEOUT_MS = 500;  // 0.5 segundos
    
    /**
     * Distancia máxima de detección válida (filtrar falsos positivos lejanos).
     */
    public static final double MAX_DETECTION_RANGE_INCHES = 120.0;  // 10 pies
    
    // ===== UNIDADES =====
    public static final DistanceUnit DISTANCE_UNIT = DistanceUnit.INCH;
}