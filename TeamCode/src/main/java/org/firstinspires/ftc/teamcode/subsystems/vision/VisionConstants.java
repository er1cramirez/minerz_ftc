package org.firstinspires.ftc.teamcode.subsystems.vision;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Constantes para el VisionSubsystem.
 * Incluye configuración de cámara, IDs de AprilTags, y umbrales de validación.
 */
public class VisionConstants {

    // ===== HARDWARE =====
    public static final String CAMERA_NAME = "Camera_1" +
            "";

    // ===== CAMERA OFFSET RESPECTO A TORRETA =====
    // Estos valores deben calibrarse midiendo físicamente
    // Positivo X = hacia adelante de la torreta
    // Positivo Y = hacia la izquierda de la torreta
    public static final double CAMERA_FORWARD_OFFSET_INCHES = 0.0;  // TODO: Calibrar
    public static final double CAMERA_LEFT_OFFSET_INCHES = 0.0;     // TODO: Calibrar
    public static final double CAMERA_UP_OFFSET_INCHES = 6.0;       // TODO: Calibrar
    public static final double CAMERA_HEADING_OFFSET_DEG = 0.0;     // TODO: Calibrar


    public static final double FX = 545.55;
    public static final double FY = 544.19;
    public static final double CX = 325.87;
    public static final double CY = 259.50;

    // ===== APRILTAG IDs - DECODE 2025-2026 =====
    // Secuencias del obelisco
    public static final int TAG_SEQUENCE_GPP = 21;  // Secuencia: Green-Purple-Purple
    public static final int TAG_SEQUENCE_PGP = 22;  // Secuencia: Purple-Green-Purple
    public static final int TAG_SEQUENCE_PPG = 23;  // Secuencia: Purple-Purple-Green

    // Goals
    public static final int TAG_GOAL_RED = 24;
    public static final int TAG_GOAL_BLUE = 20;

    // ===== SECUENCIAS (mapeo de tag ID a string) =====
    public static String getSequenceFromTagId(int tagId) {
        switch (tagId) {
            case TAG_SEQUENCE_GPP: return "GPP";
            case TAG_SEQUENCE_PGP: return "PGP";
            case TAG_SEQUENCE_PPG: return "PPG";
            default: return null;
        }
    }

    // ===== VALIDACIÓN DE DETECCIONES =====
    // Ambiguity: 0 = perfecto, >0.2 = poco confiable
    public static final double MAX_AMBIGUITY = 0.15;

    // Rango válido de detección (pulgadas)
    public static final double MIN_DETECTION_RANGE_INCHES = 6.0;
    public static final double MAX_DETECTION_RANGE_INCHES = 122.0;  // 6 feet

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
