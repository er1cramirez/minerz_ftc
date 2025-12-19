package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

import org.firstinspires.ftc.teamcode.constants.FlywheelConstants;

/**
 * Subsystem para controlar el flywheel del shooter.
 * 
 * Características:
 * - Control de velocidad con Feed-Forward + PID
 * - Compensación automática de voltaje de batería
 * - Estados claros para integración con comandos
 * - Soporte para velocidad variable según distancia
 * 
 * El control se divide en:
 * - Feed-Forward (~80% del trabajo): kS + kV × targetRPM + kA × aceleración
 * - PID (~20% corrección fina): kP × error
 * - Compensación de voltaje: output × (12V / voltajeActual)
 */
public class FlywheelSubsystem extends SubsystemBase {
    
    // ==================== ESTADOS ====================
    public enum FlywheelState {
        /** Motor completamente apagado */
        IDLE,
        /** Acelerando hacia la velocidad objetivo */
        SPINNING_UP,
        /** Velocidad objetivo alcanzada y estable */
        AT_SPEED,
        /** Girando a velocidad baja de espera */
        IDLE_SPIN
    }
    
    // ==================== HARDWARE ====================
    private final MotorEx motor;
    private final VoltageSensor voltageSensor;
    
    // ==================== ESTADO ====================
    private FlywheelState currentState;
    private double targetRPM;
    private double currentRPM;
    private double lastRPM;
    
    // ==================== CONTROL ====================
    private double integralSum;
    private double lastError;
    private double lastFeedForward;
    private double lastPIDOutput;
    private double lastTotalOutput;
    
    // ==================== TIMING ====================
    private final ElapsedTime stabilityTimer;
    private final ElapsedTime spinupTimer;
    private long lastUpdateTimeMs;
    private boolean isStable;
    
    // ==================== TELEMETRÍA/DEBUG ====================
    private double currentVoltage;
    private double compensationFactor;
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Crea el FlywheelSubsystem.
     * @param hardwareMap El HardwareMap del OpMode
     */
    public FlywheelSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, FlywheelConstants.MOTOR_NAME);
    }
    
    /**
     * Crea el FlywheelSubsystem con nombre de motor personalizado.
     * @param hardwareMap El HardwareMap del OpMode
     * @param motorName Nombre del motor en la configuración
     */
    public FlywheelSubsystem(HardwareMap hardwareMap, String motorName) {
        // Inicializar motor
        motor = new MotorEx(
                hardwareMap,
                motorName,
                (int) FlywheelConstants.ENCODER_PPR,
                FlywheelConstants.MOTOR_FREE_RPM
        );
        
        // Configurar motor
        motor.setInverted(false);  // AJUSTAR según montaje
        motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.FLOAT);  // Float para flywheel
        motor.setRunMode(Motor.RunMode.RawPower);
        
        // Obtener sensor de voltaje
        voltageSensor = hardwareMap.voltageSensor.iterator().next();
        
        // Inicializar estado
        currentState = FlywheelState.IDLE;
        targetRPM = 0;
        currentRPM = 0;
        lastRPM = 0;
        
        // Inicializar control
        integralSum = 0;
        lastError = 0;
        lastFeedForward = 0;
        lastPIDOutput = 0;
        lastTotalOutput = 0;
        
        // Inicializar timing
        stabilityTimer = new ElapsedTime();
        spinupTimer = new ElapsedTime();
        lastUpdateTimeMs = System.currentTimeMillis();
        isStable = false;
        
        // Inicializar telemetría
        currentVoltage = FlywheelConstants.NOMINAL_VOLTAGE;
        compensationFactor = 1.0;
    }
    
    // ==================== PERIODIC ====================
    
    @Override
    public void periodic() {
        // 1. Actualizar lectura de velocidad
        updateVelocityReading();
        
        // 2. Actualizar lectura de voltaje
        updateVoltageReading();
        
        // 3. Ejecutar control según estado
        switch (currentState) {
            case SPINNING_UP:
            case AT_SPEED:
            case IDLE_SPIN:
                executeVelocityControl();
                break;
                
            case IDLE:
            default:
                // Asegurar que el motor esté apagado
                motor.set(0);
                resetControlState();
                break;
        }
        
        // 4. Actualizar estado
        updateState();
    }
    
    /**
     * Lee la velocidad actual del encoder.
     */
    private void updateVelocityReading() {
        // Guardar velocidad anterior para cálculo de aceleración
        lastRPM = currentRPM;
        
        // Obtener velocidad del encoder (ticks/segundo)
        double velocityTicksPerSec = motor.getCorrectedVelocity();
        
        // Convertir a RPM
        currentRPM = FlywheelConstants.ticksPerSecToRPM(velocityTicksPerSec);
        
        // Actualizar tiempo
        lastUpdateTimeMs = System.currentTimeMillis();
    }
    
    /**
     * Lee el voltaje actual de la batería.
     */
    private void updateVoltageReading() {
        currentVoltage = voltageSensor.getVoltage();
        
        // Calcular factor de compensación
        if (FlywheelConstants.VOLTAGE_COMPENSATION_ENABLED) {
            // Proteger contra voltajes muy bajos
            double safeVoltage = Math.max(FlywheelConstants.MIN_VOLTAGE, currentVoltage);
            compensationFactor = FlywheelConstants.NOMINAL_VOLTAGE / safeVoltage;
        } else {
            compensationFactor = 1.0;
        }
    }
    
    /**
     * Ejecuta el control de velocidad: Feed-Forward + PID + Compensación.
     */
    private void executeVelocityControl() {
        // 1. Calcular error
        double error = targetRPM - currentRPM;
        
        // 2. Calcular Feed-Forward
        lastFeedForward = calculateFeedForward(error);
        
        // 3. Calcular PID
        lastPIDOutput = calculatePID(error);
        
        // 4. Combinar FF + PID
        double rawOutput = lastFeedForward + lastPIDOutput;
        
        // 5. Aplicar compensación de voltaje
        double compensatedOutput = rawOutput * compensationFactor;
        
        // 6. Limitar salida
        lastTotalOutput = clampOutput(compensatedOutput);
        
        // 7. Aplicar al motor
        motor.set(lastTotalOutput);
    }
    
    /**
     * Calcula el componente Feed-Forward.
     * FF = kS + kV × targetRPM + kA × aceleración_deseada
     */
    private double calculateFeedForward(double error) {
        if (targetRPM <= 0) {
            return 0;
        }
        
        // Componente estático (vencer fricción)
        double staticFF = FlywheelConstants.kS;
        
        // Componente de velocidad (proporcional al target)
        double velocityFF = FlywheelConstants.kV * targetRPM;
        
        // Componente de aceleración (boost durante spin-up)
        double accelFF = 0;
        if (currentState == FlywheelState.SPINNING_UP && error > 0) {
            // Más boost cuando está más lejos del target
            accelFF = FlywheelConstants.kA * error;
        }
        
        return staticFF + velocityFF + accelFF;
    }
    
    /**
     * Calcula el componente PID (principalmente P para velocidad).
     */
    private double calculatePID(double error) {
        // Proporcional
        double pTerm = FlywheelConstants.kP * error;
        
        // Integral (con anti-windup básico)
        if (Math.abs(error) < FlywheelConstants.RPM_TOLERANCE * 3) {
            // Solo acumular integral cuando estamos cerca del target
            integralSum += error;
            // Limitar integral para evitar wind-up
            integralSum = Math.max(-1000, Math.min(1000, integralSum));
        } else {
            // Reset si estamos muy lejos
            integralSum = 0;
        }
        double iTerm = FlywheelConstants.kI * integralSum;
        
        // Derivativo (generalmente 0 para velocidad)
        double dTerm = FlywheelConstants.kD * (error - lastError);
        lastError = error;
        
        return pTerm + iTerm + dTerm;
    }
    
    /**
     * Limita la salida al rango permitido.
     */
    private double clampOutput(double output) {
        // No permitir valores negativos (el flywheel solo gira en una dirección)
        if (output < FlywheelConstants.MIN_POWER) {
            return FlywheelConstants.MIN_POWER;
        }
        if (output > FlywheelConstants.MAX_POWER) {
            return FlywheelConstants.MAX_POWER;
        }
        return output;
    }
    
    /**
     * Resetea el estado interno del controlador.
     */
    private void resetControlState() {
        integralSum = 0;
        lastError = 0;
        lastFeedForward = 0;
        lastPIDOutput = 0;
        lastTotalOutput = 0;
    }
    
    /**
     * Actualiza el estado del flywheel basado en velocidad actual.
     */
    private void updateState() {
        if (currentState == FlywheelState.IDLE) {
            isStable = false;
            return;
        }
        
        double error = Math.abs(targetRPM - currentRPM);
        boolean withinTolerance = error <= FlywheelConstants.RPM_TOLERANCE;
        
        // Determinar si está en velocidad de idle o de disparo
        boolean isIdleTarget = Math.abs(targetRPM - FlywheelConstants.IDLE_SPIN_RPM) < 100;
        
        if (withinTolerance) {
            // Está dentro de tolerancia
            if (currentState == FlywheelState.SPINNING_UP) {
                // Acaba de llegar al target
                stabilityTimer.reset();
                if (isIdleTarget) {
                    currentState = FlywheelState.IDLE_SPIN;
                } else {
                    currentState = FlywheelState.AT_SPEED;
                }
            }
            
            // Verificar estabilidad temporal
            isStable = stabilityTimer.milliseconds() >= FlywheelConstants.STABILITY_TIME_MS;
            
        } else {
            // Fuera de tolerancia
            isStable = false;
            
            if (currentState == FlywheelState.AT_SPEED || currentState == FlywheelState.IDLE_SPIN) {
                // Perdió la velocidad, volver a spinning up
                currentState = FlywheelState.SPINNING_UP;
            }
        }
        
        // Verificar timeout de spin-up
        if (currentState == FlywheelState.SPINNING_UP) {
            if (spinupTimer.milliseconds() > FlywheelConstants.SPINUP_TIMEOUT_MS) {
                // Timeout - algo puede estar mal, pero seguimos intentando
                // Podrías agregar un estado de ERROR aquí si lo deseas
            }
        }
    }
    
    // ==================== MÉTODOS DE ACCIÓN ====================
    
    /**
     * Activa el flywheel a la velocidad de la zona lejana.
     */
    public void spinUpFarZone() {
        setTargetRPM(FlywheelConstants.FAR_ZONE_RPM);
    }
    
    /**
     * Activa el flywheel a velocidad de idle (espera).
     */
    public void idleSpin() {
        setTargetRPM(FlywheelConstants.IDLE_SPIN_RPM);
    }
    
    /**
     * Establece una velocidad objetivo específica.
     * Usar para velocidades calculadas por visión/distancia.
     * 
     * @param rpm Velocidad objetivo en RPM
     */
    public void setTargetRPM(double rpm) {
        double clampedRPM = FlywheelConstants.clampRPM(rpm);
        
        // Solo resetear si el target cambió significativamente
        if (Math.abs(clampedRPM - targetRPM) > 50) {
            resetControlState();
            spinupTimer.reset();
        }
        
        targetRPM = clampedRPM;
        
        if (targetRPM > 0) {
            if (currentState == FlywheelState.IDLE) {
                currentState = FlywheelState.SPINNING_UP;
                spinupTimer.reset();
            }
        } else {
            stop();
        }
    }
    
    /**
     * Detiene completamente el flywheel.
     */
    public void stop() {
        targetRPM = 0;
        currentState = FlywheelState.IDLE;
        motor.set(0);
        resetControlState();
        isStable = false;
    }
    
    // ==================== MÉTODOS DE CONSULTA ====================
    
    /**
     * @return Estado actual del flywheel
     */
    public FlywheelState getState() {
        return currentState;
    }
    
    /**
     * @return Nombre del estado actual (para telemetría)
     */
    public String getStateName() {
        return currentState.name();
    }
    
    /**
     * @return Velocidad actual en RPM
     */
    public double getCurrentRPM() {
        return currentRPM;
    }
    
    /**
     * @return Velocidad objetivo en RPM
     */
    public double getTargetRPM() {
        return targetRPM;
    }
    
    /**
     * @return Error de velocidad (target - current) en RPM
     */
    public double getVelocityError() {
        return targetRPM - currentRPM;
    }
    
    /**
     * @return true si está en la velocidad objetivo (dentro de tolerancia)
     */
    public boolean isAtSpeed() {
        return currentState == FlywheelState.AT_SPEED || 
               currentState == FlywheelState.IDLE_SPIN;
    }
    
    /**
     * @return true si está listo para disparar (at speed Y estable temporalmente)
     */
    public boolean isReadyToShoot() {
        return currentState == FlywheelState.AT_SPEED && isStable;
    }
    
    /**
     * @return true si está en idle (motor apagado)
     */
    public boolean isIdle() {
        return currentState == FlywheelState.IDLE;
    }
    
    /**
     * @return true si está en spin de espera
     */
    public boolean isIdleSpinning() {
        return currentState == FlywheelState.IDLE_SPIN;
    }
    
    /**
     * @return true si está acelerando
     */
    public boolean isSpinningUp() {
        return currentState == FlywheelState.SPINNING_UP;
    }
    
    /**
     * @return Voltaje actual de la batería
     */
    public double getBatteryVoltage() {
        return currentVoltage;
    }
    
    /**
     * @return Factor de compensación de voltaje actual
     */
    public double getVoltageCompensation() {
        return compensationFactor;
    }
    
    /**
     * @return Potencia actual aplicada al motor
     */
    public double getMotorPower() {
        return lastTotalOutput;
    }
    
    /**
     * @return Componente Feed-Forward del último cálculo
     */
    public double getLastFeedForward() {
        return lastFeedForward;
    }
    
    /**
     * @return Componente PID del último cálculo
     */
    public double getLastPIDOutput() {
        return lastPIDOutput;
    }
    
    /**
     * @return Aceleración actual (RPM/ciclo)
     */
    public double getAcceleration() {
        return currentRPM - lastRPM;
    }
    
    /**
     * @return Tiempo en estado estable (ms)
     */
    public double getStabilityTimeMs() {
        return isStable ? stabilityTimer.milliseconds() : 0;
    }
}
