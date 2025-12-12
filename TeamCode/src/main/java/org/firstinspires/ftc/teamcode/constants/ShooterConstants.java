package org.firstinspires.ftc.teamcode.constants;

public class ShooterConstants {

    // ===== HARDWARE =====
    public static final String FLYWHEEL_NAME = "flyWheel";
    public static final double CPR = 28.0;  // Rev Hex motor
    public static final double MAX_RPM = 6000.0;

    // ===== VELOCIDADES (RPM) =====
    public static final double IDLE_VELOCITY = 2000.0;
    public static final double CLOSE_VELOCITY = 2700.0;
    public static final double FAR_VELOCITY = 3300.0;

    // Warm-up (para transición suave OFF → IDLE)
    public static final double WARMUP_VELOCITY = 1000.0;
    public static final int WARMUP_DELAY_MS = 200;

    // ===== TOLERANCIAS =====
    public static final double VELOCITY_TOLERANCE = 80.0;  // ±80 RPM

    // ===== CONTROL PID + FF =====
    // Valores del tuning
    public static final double kP = 2.65;
    public static final double kI = 0.0;
    public static final double kD = 0.0;
    public static final double kF = 0.0;
    public static final double kV = 1.8;

    // ===== CONVERSIÓN =====
    public static final double COUNTS_TO_RPM = 60.0 / CPR;
    public static final double RPM_TO_COUNTS = CPR / 60.0;
}