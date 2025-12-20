package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;
import com.seattlesolvers.solverslib.controller.PIDController;

import org.firstinspires.ftc.teamcode.constants.TurretConstants;

/**
 * Subsystem para controlar la torreta rotativa.
 * 
 * Características:
 * - Control de posición en grados con PID
 * - Soft limits para proteger el cableado
 * - Tracking de posición relativa al robot
 * - Modos: IDLE, HOMING, MANUAL, POSITION, TRACKING
 * 
 * La posición 0° representa la torreta apuntando al frente del robot.
 * Ángulos positivos = rotación antihoraria (visto desde arriba)
 * Ángulos negativos = rotación horaria
 */
public class TurretSubsystem extends SubsystemBase {
    
    // ==================== ENUM DE ESTADOS ====================
    public enum TurretState {
        /** Motor sin energía, sin control activo */
        IDLE,
        /** Buscando posición de referencia (home) */
        HOMING,
        /** Control directo por joystick, sin PID */
        MANUAL,
        /** Yendo a una posición específica con PID */
        POSITION,
        /** Siguiendo un target dinámico (visión) */
        TRACKING
    }
    
    // ==================== HARDWARE ====================
    private final MotorEx motor;
    
    // ==================== CONTROLADOR ====================
    private final PIDController pidController;
    
    // ==================== VARIABLES DE ESTADO ====================
    private TurretState currentState;
    private double targetAngleDeg;
    private double homeOffsetTicks;  // Offset para calibración de home
    private boolean isHomed;
    
    // ==================== CACHE DE SENSORES ====================
    private double currentPositionDeg;
    private double currentVelocityDegPerSec;
    private double lastPositionTicks;
    private long lastUpdateTimeMs;
    
    // ==================== CONSTRUCTOR ====================
    /**
     * Crea el TurretSubsystem.
     * @param hardwareMap El HardwareMap del OpMode
     */
    public TurretSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, TurretConstants.MOTOR_NAME);
    }
    
    /**
     * Crea el TurretSubsystem con nombre de motor personalizado.
     * @param hardwareMap El HardwareMap del OpMode
     * @param motorName Nombre del motor en la configuración
     */
    public TurretSubsystem(HardwareMap hardwareMap, String motorName) {
        // Inicializar motor
        motor = new MotorEx(hardwareMap, motorName, Motor.GoBILDA.RPM_312);
        
        // Configurar motor
        motor.setInverted(false);  // AJUSTAR según montaje
        motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        motor.setRunMode(Motor.RunMode.RawPower);  // Usaremos nuestro propio PID
        
        // Configurar conversión de unidades (ticks -> grados)
        motor.setDistancePerPulse(TurretConstants.DEGREES_PER_TICK);
        
        // Inicializar controlador PID
        pidController = new PIDController(
            TurretConstants.kP,
            TurretConstants.kI,
            TurretConstants.kD
        );
        pidController.setTolerance(TurretConstants.POSITION_TOLERANCE_DEG);
        
        // Inicializar estado
        currentState = TurretState.IDLE;
        targetAngleDeg = 0.0;
        homeOffsetTicks = 0.0;
        isHomed = false;
        
        // Inicializar cache
        currentPositionDeg = 0.0;
        currentVelocityDegPerSec = 0.0;
        lastPositionTicks = 0.0;
        lastUpdateTimeMs = System.currentTimeMillis();
        
        // Resetear encoder
        motor.resetEncoder();
    }
    
    // ==================== PERIODIC ====================
    @Override
    public void periodic() {
        // Actualizar lecturas de sensores
        updateSensorCache();
        
        // Ejecutar lógica según estado
        switch (currentState) {
            case POSITION:
            case TRACKING:
                executePositionControl();
                break;
            case MANUAL:
                // El control manual se maneja en setManualPower()
                break;
            case HOMING:
                // El homing se maneja en el comando de homing
                break;
            case IDLE:
            default:
                // No hacer nada
                break;
        }
    }
    
    /**
     * Actualiza el cache de lecturas de sensores.
     */
    private void updateSensorCache() {
        long currentTimeMs = System.currentTimeMillis();
        double deltaTimeSec = (currentTimeMs - lastUpdateTimeMs) / 1000.0;
        
        // Obtener posición actual en ticks y convertir a grados
        double currentTicks = motor.getCurrentPosition();
        currentPositionDeg = TurretConstants.ticksToDegrees(currentTicks - homeOffsetTicks);
        
        // Calcular velocidad
        if (deltaTimeSec > 0) {
            double deltaTicks = currentTicks - lastPositionTicks;
            currentVelocityDegPerSec = TurretConstants.ticksToDegrees(deltaTicks) / deltaTimeSec;
        }
        
        // Actualizar valores anteriores
        lastPositionTicks = currentTicks;
        lastUpdateTimeMs = currentTimeMs;
    }
    
    /**
     * Ejecuta el control PID de posición.
     */
    private void executePositionControl() {
        // Actualizar coeficientes PID (permite tuning en tiempo real)
        pidController.setPID(
            TurretConstants.kP,
            TurretConstants.kI,
            TurretConstants.kD
        );
        
        // Calcular salida PID
        double output = pidController.calculate(currentPositionDeg, targetAngleDeg);
        
        // Agregar feed-forward si es necesario
        output += Math.signum(targetAngleDeg - currentPositionDeg) * TurretConstants.kF;
        
        // Limitar potencia
        output = clampPower(output);
        
        // Verificar soft limits antes de aplicar potencia
        if (!isSafeToMove(output)) {
            output = 0;
        }
        
        motor.set(output);
    }
    
    // ==================== MÉTODOS DE ACCIÓN ====================
    
    /**
     * Establece la posición objetivo de la torreta.
     * Cambia al estado POSITION y usa control PID.
     * @param angleDeg Ángulo objetivo en grados (0 = frente del robot)
     */
    public void setTargetPosition(double angleDeg) {
        // Limitar al rango permitido
        targetAngleDeg = TurretConstants.clampAngle(angleDeg);
        currentState = TurretState.POSITION;
        pidController.reset();  // Resetear acumulador integral
    }
    
    /**
     * Establece la posición objetivo para tracking (sin resetear PID).
     * Usado para seguimiento continuo de targets dinámicos.
     * @param angleDeg Ángulo objetivo en grados
     */
    public void setTrackingTarget(double angleDeg) {
        targetAngleDeg = TurretConstants.clampAngle(angleDeg);
        if (currentState != TurretState.TRACKING) {
            currentState = TurretState.TRACKING;
            pidController.reset();
        }
    }

    /**
     * Ajusta el ángulo objetivo relativo a la posición actual.
     * Útil para lazos de visión donde se calcula un error relativo (bearing).
     * @param deltaDeg Cambio en grados (positivo = izquierda/antihorario)
     */
    public void adjustAngle(double deltaDeg) {
        // Usamos setTrackingTarget para no resetear el PID si ya estamos trackeando
        // El nuevo target es la posición actual + el error medido
        setTrackingTarget(currentPositionDeg + deltaDeg);
    }
    
    /**
     * Control manual de la torreta con potencia directa.
     * @param power Potencia del motor (-1.0 a 1.0)
     */
    public void setManualPower(double power) {
        currentState = TurretState.MANUAL;
        
        // Escalar potencia
        double scaledPower = power * TurretConstants.MANUAL_POWER_SCALE;
        
        // Verificar soft limits
        if (!isSafeToMove(scaledPower)) {
            scaledPower = 0;
        }
        
        motor.set(scaledPower);
    }
    
    /**
     * Detiene la torreta y cambia a estado IDLE.
     */
    public void stop() {
        currentState = TurretState.IDLE;
        motor.set(0);
        pidController.reset();
    }
    
    /**
     * Establece la posición actual como "home" (0 grados).
     * Usar cuando la torreta está físicamente en la posición de referencia.
     */
    public void setCurrentPositionAsHome() {
        homeOffsetTicks = motor.getCurrentPosition();
        currentPositionDeg = 0.0;
        targetAngleDeg = 0.0;
        isHomed = true;
    }
    
    /**
     * Resetea el home offset (para re-calibración).
     */
    public void resetHome() {
        homeOffsetTicks = 0.0;
        isHomed = false;
        motor.resetEncoder();
    }
    
    /**
     * Cambia al estado de homing.
     */
    public void startHoming() {
        currentState = TurretState.HOMING;
    }
    
    /**
     * Mueve la torreta con potencia directa (para homing).
     * NO cambia el estado.
     * @param power Potencia del motor
     */
    public void setRawPower(double power) {
        motor.set(clampPower(power));
    }
    
    // ==================== MÉTODOS DE CONSULTA ====================
    
    /**
     * @return Estado actual de la torreta
     */
    public TurretState getState() {
        return currentState;
    }
    
    /**
     * @return Nombre del estado actual (para telemetría)
     */
    public String getStateName() {
        return currentState.name();
    }
    
    /**
     * @return Posición actual en grados relativa al home
     */
    public double getCurrentAngleDeg() {
        return currentPositionDeg;
    }
    
    /**
     * @return Posición objetivo actual en grados
     */
    public double getTargetAngleDeg() {
        return targetAngleDeg;
    }
    
    /**
     * @return Error de posición actual (target - current) en grados
     */
    public double getPositionErrorDeg() {
        return targetAngleDeg - currentPositionDeg;
    }
    
    /**
     * @return Velocidad actual en grados por segundo
     */
    public double getVelocityDegPerSec() {
        return currentVelocityDegPerSec;
    }
    
    /**
     * @return true si la torreta está en la posición objetivo (dentro de tolerancia)
     */
    public boolean isAtTarget() {
        return Math.abs(getPositionErrorDeg()) <= TurretConstants.POSITION_TOLERANCE_DEG;
    }
    
    /**
     * @return true si la torreta está en la posición objetivo Y estable (baja velocidad)
     */
    public boolean isAtTargetAndStable() {
        return isAtTarget() && 
               Math.abs(currentVelocityDegPerSec) <= TurretConstants.VELOCITY_TOLERANCE_DEG_PER_SEC;
    }
    
    /**
     * @return true si la torreta ha sido calibrada (home establecido)
     */
    public boolean isHomed() {
        return isHomed;
    }
    
    /**
     * @return true si el estado actual es IDLE
     */
    public boolean isIdle() {
        return currentState == TurretState.IDLE;
    }
    
    /**
     * @return true si está en control de posición (POSITION o TRACKING)
     */
    public boolean isInPositionControl() {
        return currentState == TurretState.POSITION || currentState == TurretState.TRACKING;
    }
    
    /**
     * @return Posición actual del encoder en ticks (raw)
     */
    public double getRawEncoderTicks() {
        return motor.getCurrentPosition();
    }
    
    /**
     * @return Potencia actual del motor
     */
    public double getMotorPower() {
        return motor.get();
    }
    
    /**
     * Verifica si un ángulo específico está dentro del rango permitido.
     * @param angleDeg Ángulo a verificar
     * @return true si está en rango
     */
    public boolean isAngleReachable(double angleDeg) {
        return TurretConstants.isAngleInRange(angleDeg);
    }
    
    // ==================== MÉTODOS PRIVADOS DE UTILIDAD ====================
    
    /**
     * Verifica si es seguro moverse en la dirección indicada.
     * @param power Potencia que se quiere aplicar (signo indica dirección)
     * @return true si es seguro moverse
     */
    private boolean isSafeToMove(double power) {
        // Si no está homeado, permitir movimiento (para homing manual)
        if (!isHomed) {
            return true;
        }
        
        double minAngle = TurretConstants.MIN_ANGLE_DEG + TurretConstants.SOFT_LIMIT_MARGIN_DEG;
        double maxAngle = TurretConstants.MAX_ANGLE_DEG - TurretConstants.SOFT_LIMIT_MARGIN_DEG;
        
        // Si está en el límite inferior y quiere ir más negativo
        if (currentPositionDeg <= minAngle && power < 0) {
            return false;
        }
        
        // Si está en el límite superior y quiere ir más positivo
        if (currentPositionDeg >= maxAngle && power > 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Limita la potencia al rango permitido.
     * @param power Potencia sin limitar
     * @return Potencia limitada a [-MAX_POWER, MAX_POWER]
     */
    private double clampPower(double power) {
        return Math.max(-TurretConstants.MAX_POWER, 
                       Math.min(TurretConstants.MAX_POWER, power));
    }
}
