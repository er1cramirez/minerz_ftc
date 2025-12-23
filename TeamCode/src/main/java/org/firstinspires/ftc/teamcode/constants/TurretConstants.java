package org.firstinspires.ftc.teamcode.constants;

// NOTA: Si instalas FTC Dashboard después, descomenta la siguiente línea:
// import com.acmerobotics.dashboard.config.Config;

/**
 * Constantes para el TurretSubsystem.
 *
 * NOTA: Si instalas FTC Dashboard, agrega @Config arriba de la clase
 * para poder modificar valores en tiempo real.
 */
// @Config  // Descomentar si tienes FTC Dashboard
public class TurretConstants {

    // ==================== HARDWARE ====================
    public static final String MOTOR_NAME = "turretMotor";

    // ==================== GEAR RATIO ====================
    /** Dientes del piñón (motor) */
    public static final int PINION_TEETH = 18;
    /** Dientes del gear (torreta) */
    public static final int TURRET_GEAR_TEETH = 74;
    /** Ticks del encoder por revolución del eje del motor (gobuilda 312 RPM) */
    public static final double MOTOR_TICKS_PER_REV = 537.7;

    /** Relación de transmisión total: gear/pinion */
    public static final double GEAR_RATIO = (double) TURRET_GEAR_TEETH / PINION_TEETH; // 4.111

    /** Ticks del encoder por revolución completa de la torreta */
    public static final double TICKS_PER_TURRET_REV = MOTOR_TICKS_PER_REV * GEAR_RATIO; // ~2210.3

    /** Ticks por grado de rotación de la torreta */
    public static final double TICKS_PER_DEGREE = TICKS_PER_TURRET_REV / 360.0; // ~6.14

    /** Grados por tick (para setDistancePerPulse) */
    public static final double DEGREES_PER_TICK = 360.0 / TICKS_PER_TURRET_REV; // ~0.1629

    // ==================== LÍMITES DE ROTACIÓN ====================
    /**
     * Límite de rotación en sentido antihorario (visto desde arriba)
     * AJUSTAR según restricciones de cableado
     */
    public static double MIN_ANGLE_DEG = -135.0;

    /**
     * Límite de rotación en sentido horario (visto desde arriba)
     * AJUSTAR según restricciones de cableado
     */
    public static double MAX_ANGLE_DEG = 105.0;

    /** Margen de seguridad antes de los límites físicos (grados) */
    public static double SOFT_LIMIT_MARGIN_DEG = 5.0;

    // ==================== CONTROL PID ====================
    // NOTA: Estos valores son 'static' (no 'final') para permitir
    // modificación en tiempo real desde el OpMode de tuning

    /** Constante proporcional - TUNEAR */
    public static double kP = 0.08;
    /** Constante integral - TUNEAR */
    public static double kI = 0.0;
    /** Constante derivativa - TUNEAR */
    public static double kD = 0.004;
    /** Feed-forward estático para compensar fricción - TUNEAR */
    public static double kF = 0.0;

    /** Tolerancia de posición en grados para considerar "en target" */
    public static double POSITION_TOLERANCE_DEG = 2.0;

    /** Tolerancia de velocidad (grados/seg) para considerar "estable" */
    public static double VELOCITY_TOLERANCE_DEG_PER_SEC = 5.0;

    // ==================== LÍMITES DE VELOCIDAD ====================
    /** Potencia máxima del motor (0.0 - 1.0) */
    public static double MAX_POWER = 0.7;

    /** Potencia mínima para vencer fricción estática */
    public static double MIN_POWER = 0.1;

    /** Potencia para control manual */
    public static double MANUAL_POWER_SCALE = 0.5;

    // ==================== HOMING ====================
    /** Potencia durante la secuencia de homing */
    public static double HOMING_POWER = 0.3;

    /** Rango de búsqueda para homing automático (±grados) */
    public static double HOMING_SEARCH_RANGE_DEG = 15.0;

    /** Timeout para la secuencia de homing (ms) */
    public static long HOMING_TIMEOUT_MS = 3000;

    // ==================== POSICIONES PREDEFINIDAS ====================
    /** Posición frontal (0° = apuntando al frente del robot) */
    public static final double POSITION_FRONT = 0.0;

    /** Posición izquierda */
    public static final double POSITION_LEFT = 90.0;

    /** Posición derecha */
    public static final double POSITION_RIGHT = -90.0;


    // ----- Tracking Behavior -----
    
    /**
     * Bearing tolerance to consider "locked on" (degrees).
     * When |bearing| < this value AND turret is stable, we're locked.
     */
    public static final double VISION_BEARING_TOLERANCE_DEG = 2.0;
    
    /**
     * Gain applied to bearing for tracking (0.0 - 1.0).
     * - 1.0 = Full correction each cycle (aggressive, may overshoot)
     * - 0.5 = Half correction each cycle (smoother, slower response)
     */
    public static final double VISION_TRACKING_GAIN = 0.8;
    
    /**
     * Minimum bearing change to apply correction (degrees).
     * Prevents micro-adjustments that cause jitter.
     */
    public static final double VISION_MIN_BEARING_CHANGE_DEG = 0.5;
    
    // ----- State Timing -----
    
    /**
     * Time to wait in HOLDING state before transitioning to SEARCHING (seconds).
     */
    public static final double VISION_HOLDING_TIMEOUT_SEC = 2.0;
    
    /**
     * Minimum time between state transitions (seconds).
     * Prevents rapid flickering between states.
     */
    public static final double VISION_STATE_DEBOUNCE_SEC = 0.1;
    
    /**
     * Consecutive frames required to confirm target acquisition.
     */
    public static final int VISION_ACQUISITION_FRAMES = 2;
    
    /**
     * Consecutive frames without detection before declaring target lost.
     */
    public static final int VISION_LOSS_FRAMES = 3;
    // ==================== UTILIDADES ====================
    /**
     * Convierte grados a ticks del encoder.
     * @param degrees Ángulo en grados
     * @return Ticks del encoder
     */
    public static double degreesToTicks(double degrees) {
        return degrees * TICKS_PER_DEGREE;
    }

    /**
     * Convierte ticks del encoder a grados.
     * @param ticks Ticks del encoder
     * @return Ángulo en grados
     */
    public static double ticksToDegrees(double ticks) {
        return ticks * DEGREES_PER_TICK;
    }

    /**
     * Limita un ángulo al rango permitido.
     * @param angle Ángulo deseado en grados
     * @return Ángulo limitado al rango [MIN_ANGLE, MAX_ANGLE]
     */
    public static double clampAngle(double angle) {
        return Math.max(MIN_ANGLE_DEG + SOFT_LIMIT_MARGIN_DEG,
                Math.min(MAX_ANGLE_DEG - SOFT_LIMIT_MARGIN_DEG, angle));
    }

    /**
     * Verifica si un ángulo está dentro del rango permitido.
     * @param angle Ángulo a verificar en grados
     * @return true si está dentro del rango
     */
    public static boolean isAngleInRange(double angle) {
        return angle >= (MIN_ANGLE_DEG + SOFT_LIMIT_MARGIN_DEG) &&
                angle <= (MAX_ANGLE_DEG - SOFT_LIMIT_MARGIN_DEG);
    }
}