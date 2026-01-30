package org.firstinspires.ftc.teamcode.constants;

/**
 * Constantes para el FlywheelSubsystem.
 * 
 * Motor: REV HD Hex 6000 RPM
 * Encoder: 28 PPR (integrado)
 * Relación: 1:1 (directo al flywheel)
 */
public class FlywheelConstants {
    
    // ==================== HARDWARE ====================
    public static final String  MOTOR_NAME = "flywheelMotor";
    
    // ==================== ESPECIFICACIONES DEL MOTOR ====================
    /** RPM libre del motor REV HD Hex */
    public static final double MOTOR_FREE_RPM = 6000.0;
    
    /** Pulsos por revolución del encoder REV */
    public static final double ENCODER_PPR = 28.0;
    
    /** Relación de transmisión (1:1 = directo) */
    public static final double GEAR_RATIO = 1.0;
    
    /** Ticks por revolución del flywheel */
    public static final double TICKS_PER_REV = ENCODER_PPR * GEAR_RATIO;
    
    // ==================== VELOCIDADES OBJETIVO ====================
    /** Velocidad para zona lejana (fija) */
    public static double FAR_ZONE_RPM = 3600.0;
    
    /** Velocidad para zona cercana (mínima del rango) */
    public static double NEAR_ZONE_MIN_RPM = 2500.0;
    
    /** Velocidad para zona cercana (máxima del rango) */
    public static double NEAR_ZONE_MAX_RPM = 3200.0;
    
    /** Velocidad de idle spin (mantener girando sin gastar mucha batería) */
    public static double IDLE_SPIN_RPM = 2000.0;
    
    /** Tolerancia de velocidad para considerar "at speed" */
    public static double RPM_TOLERANCE = 70.0;
    
    // ==================== CONTROL FEED-FORWARD ====================
    /**
     * kS: Potencia estática para vencer fricción.
     * Es la potencia mínima para que el motor empiece a girar.
     * TUNEAR: Incrementar hasta que el motor apenas empiece a moverse.
     */
    public static double kS = 0.06;
    
    /**
     * kV: Potencia por unidad de velocidad.
     * Relación entre potencia y RPM en estado estable.
     * Fórmula teórica inicial: 1.0 / MOTOR_FREE_RPM ≈ 0.000167
     * TUNEAR: Ajustar hasta que FF solo llegue cerca del target.
     */
    public static double kV = 0.00018;
    
    /**
     * kA: Potencia extra durante aceleración.
     * Ayuda a acelerar más rápido (especialmente con flywheel pesado).
     * TUNEAR: Incrementar si el spin-up es muy lento.
     */
    public static double kA = 0.0001;
    
    // ==================== CONTROL PID ====================
    /**
     * kP: Ganancia proporcional.
     * Corrección fina basada en el error de velocidad.
     * TUNEAR: Empezar pequeño, incrementar hasta eliminar error estacionario.
     */
    public static double kP = 0.00016;
    
    /**
     * kI: Ganancia integral.
     * Generalmente 0 o muy pequeño para control de velocidad.
     * TUNEAR: Solo si hay error estacionario persistente que kP no corrige.
     */
    public static double kI = 0.0;
    
    /**
     * kD: Ganancia derivativa.
     * Generalmente 0 para control de velocidad (ya es derivada de posición).
     */
    public static double kD = 0.0;
    
    // ==================== COMPENSACIÓN DE VOLTAJE ====================
    /** Voltaje nominal de referencia (batería llena) */
    public static final double NOMINAL_VOLTAGE = 12.0;
    
    /** Voltaje mínimo aceptable (protección) */
    public static final double MIN_VOLTAGE = 9.0;
    
    /** Habilitar/deshabilitar compensación de voltaje */
    public static boolean VOLTAGE_COMPENSATION_ENABLED = true;
    
    // ==================== LÍMITES DE SEGURIDAD ====================
    /** Potencia máxima permitida */
    public static double MAX_POWER = 1.0;
    
    /** Potencia mínima (para evitar valores muy pequeños que no muevan el motor) */
    public static double MIN_POWER = 0.0;
    
    /** RPM máximo permitido (protección) */
    public static double MAX_RPM = 5500.0;
    
    // ==================== TIEMPOS ====================
    /** Tiempo mínimo en AT_SPEED antes de considerar "ready to shoot" (ms) */
    public static long STABILITY_TIME_MS = 100;
    
    /** Timeout para spin-up (si no llega en este tiempo, algo está mal) (ms) */
    public static long SPINUP_TIMEOUT_MS = 3000;
    
    // ==================== UTILIDADES ====================
    
    /**
     * Convierte ticks/segundo a RPM.
     * @param ticksPerSec Velocidad en ticks por segundo
     * @return Velocidad en RPM
     */
    public static double ticksPerSecToRPM(double ticksPerSec) {
        return (ticksPerSec * 60.0) / TICKS_PER_REV;
    }
    
    /**
     * Convierte RPM a ticks/segundo.
     * @param rpm Velocidad en RPM
     * @return Velocidad en ticks por segundo
     */
    public static double rpmToTicksPerSec(double rpm) {
        return (rpm * TICKS_PER_REV) / 60.0;
    }
    
    /**
     * Limita un valor de RPM al rango seguro.
     * @param rpm RPM a limitar
     * @return RPM limitado entre 0 y MAX_RPM
     */
    public static double clampRPM(double rpm) {
        return Math.max(0, Math.min(MAX_RPM, rpm));
    }
    
    /**
     * Verifica si un RPM está dentro del rango operativo.
     * @param rpm RPM a verificar
     * @return true si está en rango operativo
     */
    public static boolean isInOperatingRange(double rpm) {
        return rpm >= NEAR_ZONE_MIN_RPM && rpm <= FAR_ZONE_RPM;
    }
    
    /**
     * Calcula el RPM interpolado para una distancia dada.
     * Útil para el comando de visión que calcula velocidad según distancia.
     * 
     * @param distance Distancia al target (unidades arbitrarias)
     * @return RPM interpolado entre NEAR_ZONE_MIN_RPM y NEAR_ZONE_MAX_RPM
     */
    public static double interpolateRPMForDistance(double distance) {
        //temporal holder
        return 2600.0;
    }
}
