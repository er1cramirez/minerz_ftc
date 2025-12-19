package org.firstinspires.ftc.teamcode.constants;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Constantes para el VisionSubsystem.
 * Incluye configuración de cámara, IDs de AprilTags, y umbrales de validación.
 */
public class VisionConstants {

    // ===== HARDWARE =====
    public static final String CAMERA_NAME = "arducam";

    // ===== CAMERA OFFSET RESPECTO A TORRETA =====
    // Estos valores deben calibrarse midiendo físicamente
    // Positivo X = hacia adelante de la torreta
    // Positivo Y = hacia la izquierda de la torreta
    public static final double CAMERA_FORWARD_OFFSET_INCHES = 0.0;  // TODO: Calibrar
    public static final double CAMERA_LEFT_OFFSET_INCHES = 0.0;     // TODO: Calibrar
    public static final double CAMERA_UP_OFFSET_INCHES = 6.0;       // TODO: Calibrar
    public static final double CAMERA_HEADING_OFFSET_DEG = 0.0;     // TODO: Calibrar

    // ===== APRILTAG IDs - DECODE 2025-2026 =====
    // Secuencias del obelisco
    public static final int TAG_SEQUENCE_YYP = 21;  // Secuencia: Yellow-Yellow-Purple
    public static final int TAG_SEQUENCE_YPY = 22;  // Secuencia: Yellow-Purple-Yellow
    public static final int TAG_SEQUENCE_PYY = 23;  // Secuencia: Purple-Yellow-Yellow

    // Goals
    public static final int TAG_GOAL_RED = 20;
    public static final int TAG_GOAL_BLUE = 24;

    // ===== SECUENCIAS (mapeo de tag ID a string) =====
    public static String getSequenceFromTagId(int tagId) {
        switch (tagId) {
            case TAG_SEQUENCE_YYP: return "YYP";
            case TAG_SEQUENCE_YPY: return "YPY";
            case TAG_SEQUENCE_PYY: return "PYY";
            default: return null;
        }
    }

    // ===== VALIDACIÓN DE DETECCIONES =====
    // Ambiguity: 0 = perfecto, >0.2 = poco confiable
    public static final double MAX_AMBIGUITY = 0.15;

    // Rango válido de detección (pulgadas)
    public static final double MIN_DETECTION_RANGE_INCHES = 6.0;
    public static final double MAX_DETECTION_RANGE_INCHES = 72.0;  // 6 feet

    // ===== UNIDADES DEFAULT =====
    public static final DistanceUnit DISTANCE_UNIT = DistanceUnit.INCH;
    public static final AngleUnit ANGLE_UNIT = AngleUnit.DEGREES;

    // ===== STREAMING/DEBUG =====
    public static final boolean ENABLE_LIVE_VIEW = true;  // Para debug en DS
    public static final int STREAM_WIDTH = 640;
    public static final int STREAM_HEIGHT = 480;

    // ===== TIMEOUTS =====
    public static final int DETECTION_TIMEOUT_MS = 3000;  // Para comandos que esperan detección
}
