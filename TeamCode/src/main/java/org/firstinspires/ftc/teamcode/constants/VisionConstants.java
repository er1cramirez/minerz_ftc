package org.firstinspires.ftc.teamcode.constants;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Constantes para el VisionSubsystem.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * CALIBRACIÓN REQUERIDA
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Antes de usar en competencia, calibra:
 * 
 * 1. OFFSET DE CÁMARA (medir físicamente):
 *    - CAMERA_FORWARD_OFFSET_INCHES
 *    - CAMERA_LEFT_OFFSET_INCHES
 *    - CAMERA_UP_OFFSET_INCHES
 *    - CAMERA_HEADING_OFFSET_DEG
 * 
 * 2. LENS INTRINSICS (usar calibración de cámara):
 *    - FX, FY, CX, CY
 *    - O dejar valores default y el SDK intentará cargar calibración predefinida
 * 
 * 3. EXPOSICIÓN Y GANANCIA (usar VisionExposureTuningOpMode):
 *    - MANUAL_EXPOSURE_MS
 *    - MANUAL_GAIN
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class VisionConstants {

    // ═══════════════════════════════════════════════════════════════════════
    // HARDWARE
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Nombre de la cámara en la configuración del robot */
    public static final String CAMERA_NAME = "Camera_1";

    // ═══════════════════════════════════════════════════════════════════════
    // OFFSET DE CÁMARA RESPECTO A LA TORRETA
    // ═══════════════════════════════════════════════════════════════════════
    // 
    // La cámara está montada en la torreta. Estos valores definen el offset
    // desde el centro de rotación de la torreta hasta el lente de la cámara.
    // 
    // Sistema de coordenadas (visto desde arriba, torreta apuntando "adelante"):
    //   +X = adelante (dirección que apunta la torreta)
    //   +Y = izquierda
    //   +Z = arriba
    //
    // IMPORTANTE: Medir físicamente y actualizar estos valores.
    
    /** Offset hacia adelante de la cámara respecto a la torreta (pulgadas) */
    public static final double CAMERA_FORWARD_OFFSET_INCHES = 0.0;  // TODO: Calibrar
    
    /** Offset hacia la izquierda de la cámara respecto a la torreta (pulgadas) */
    public static final double CAMERA_LEFT_OFFSET_INCHES = 0.0;     // TODO: Calibrar
    
    /** Offset hacia arriba de la cámara respecto a la torreta (pulgadas) */
    public static final double CAMERA_UP_OFFSET_INCHES = 6.0;       // TODO: Calibrar
    
    /** Offset de heading de la cámara respecto a la torreta (grados) */
    // Si la cámara no mira exactamente hacia donde apunta la torreta
    public static final double CAMERA_HEADING_OFFSET_DEG = 0.0;     // TODO: Calibrar

    // ═══════════════════════════════════════════════════════════════════════
    // OFFSET DE TORRETA RESPECTO AL CENTRO DEL ROBOT
    // ═══════════════════════════════════════════════════════════════════════
    // 
    // La torreta gira sobre un punto que puede no ser el centro del robot.
    // Estos valores definen el vector fijo desde el centro del robot al eje de la torreta.
    //
    // Sistema de coordenadas (Robot Frame):
    //   +X = adelante del robot
    //   +Y = izquierda del robot
    
    /** Distancia desde el centro del robot al eje de rotación de la torreta en X (pulgadas) */
    public static final double TURRET_FORWARD_OFFSET_INCHES = 0.0;  // TODO: Calibrar
    
    /** Distancia desde el centro del robot al eje de rotación de la torreta en Y (pulgadas) */
    public static final double TURRET_LEFT_OFFSET_INCHES = 0.0;     // TODO: Calibrar

    // ═══════════════════════════════════════════════════════════════════════
    // LENS INTRINSICS (Calibración de cámara)
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Estos valores mejoran la precisión de las mediciones de pose.
    // Si no están calibrados, el SDK intentará usar valores predefinidos
    // para cámaras conocidas.
    //
    // Para calibrar: usar herramienta de calibración de OpenCV o similar.
    // Estos valores son ejemplo para Arducam OV9281 a 640x480.
    
    public static final double FX = 545.55;  // Focal length X
    public static final double FY = 544.19;  // Focal length Y
    public static final double CX = 325.87;  // Principal point X
    public static final double CY = 259.50;  // Principal point Y

    // ═══════════════════════════════════════════════════════════════════════
    // EXPOSICIÓN Y GANANCIA
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Usar VisionExposureTuningOpMode para encontrar valores óptimos.
    // Objetivo: exposición más BAJA que detecte de forma confiable.
    // Menor exposición = menos motion blur.
    
    /** Si true, usa exposición manual. Si false, usa auto-exposición. */
    public static final boolean USE_MANUAL_EXPOSURE = true;
    
    /** Exposición manual en milisegundos (típico: 5-15 ms) */
    public static final int MANUAL_EXPOSURE_MS = 6;  // TODO: Calibrar con tuning OpMode
    
    /** Ganancia manual (típico: máximo disponible) */
    public static final int MANUAL_GAIN = 250;       // TODO: Calibrar con tuning OpMode

    // ═══════════════════════════════════════════════════════════════════════
    // TAG IDs - DECODE 2025-2026
    // ═══════════════════════════════════════════════════════════════════════
    
    // Secuencias del obelisco
    public static final int TAG_SEQUENCE_GPP = 21;  // Green-Purple-Purple
    public static final int TAG_SEQUENCE_PGP = 22;  // Purple-Green-Purple
    public static final int TAG_SEQUENCE_PPG = 23;  // Purple-Purple-Green

    // Goals
    public static final int TAG_GOAL_RED = 24;
    public static final int TAG_GOAL_BLUE = 20;

    /**
     * Convierte un tag ID a string de secuencia.
     * @return "GPP", "PGP", "PPG" o null si no es tag de secuencia
     */
    public static String getSequenceFromTagId(int tagId) {
        switch (tagId) {
            case TAG_SEQUENCE_GPP: return "GPP";
            case TAG_SEQUENCE_PGP: return "PGP";
            case TAG_SEQUENCE_PPG: return "PPG";
            default: return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // VALIDACIÓN DE DETECCIONES
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Estos umbrales determinan si una detección es "confiable".
    // Ajustar según condiciones de campo.
    
    /**
     * Ambigüedad máxima permitida (0 = perfecta, >0.2 = pobre).
     * El SDK usa decisionMargin = 1/ambiguity.
     * Con MAX_AMBIGUITY = 0.15, requerimos decisionMargin >= 6.67
     */
    public static final double MAX_AMBIGUITY = 0.15;
    
    /** Distancia mínima de detección en pulgadas */
    public static final double MIN_DETECTION_RANGE_INCHES = 6.0;
    
    /** Distancia máxima de detección en pulgadas (~200" = diagonal de cancha) */
    public static final double MAX_DETECTION_RANGE_INCHES = 200.0;

    // ═══════════════════════════════════════════════════════════════════════
    // UNIDADES
    // ═══════════════════════════════════════════════════════════════════════
    
    public static final DistanceUnit DISTANCE_UNIT = DistanceUnit.INCH;
    public static final AngleUnit ANGLE_UNIT = AngleUnit.DEGREES;

    // ═══════════════════════════════════════════════════════════════════════
    // STREAMING / DEBUG
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Habilitar vista en vivo (desactivar en competencia para ahorrar CPU) */
    public static final boolean ENABLE_LIVE_VIEW = false;
    
    public static final int STREAM_WIDTH = 640;
    public static final int STREAM_HEIGHT = 480;

    // ═══════════════════════════════════════════════════════════════════════
    // TIMEOUTS
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Timeout para comandos que esperan detección (ms) */
    public static final int DETECTION_TIMEOUT_MS = 3000;
}