package org.firstinspires.ftc.teamcode.util;

import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Clase auxiliar para detectar el color de las pelotas de Decode.
 * Diseñada para ser integrada posteriormente en SpindexerSubsystem.
 * 
 * Estrategia de detección:
 * 1. Detectar presencia de pelota por distancia (< umbral)
 * 2. Confirmar color con múltiples lecturas consistentes
 * 3. Manejar agujeros de la pelota mediante lecturas promediadas
 * 
 * @author Seattle Solvers
 */
public class BallColorDetector {
    
    // ==================== ENUMS ====================
    
    public enum DetectedColor {
        NONE,       // No hay pelota o no se puede determinar
        YELLOW,     // Pelota amarilla (para testing)
        GREEN,      // Pelota verde (oficial)
        PURPLE,     // Pelota púrpura
        UNKNOWN     // Hay pelota pero color no reconocido
    }
    
    public enum DetectionState {
        IDLE,               // Sin pelota detectada
        DETECTING,          // Pelota presente, acumulando lecturas
        CONFIRMED           // Color confirmado
    }
    
    // ==================== CONFIGURACIÓN ====================
    
    // Umbrales de distancia (en CM)
    private double distanceThresholdPresent = 3.0;  // Pelota presente si < este valor
    private double distanceThresholdHole = 5.0;     // Posible agujero si > este valor pero < infinito
    
    // Umbrales de color HSV para cada tipo de pelota
    // Estos valores se calibrarán con el OpMode de calibración
    
    // YELLOW (Hue ~40-60)
    private float yellowHueMin = 71f;
    private float yellowHueMax = 190f;
    private float yellowSatMin = 0.43f;
    private float yellowValMin = 4.98f;
    
    // GREEN (Hue ~80-150)
    private float greenHueMin = 80f;
    private float greenHueMax = 150f;
    private float greenSatMin = 0.3f;
    private float greenValMin = 0.2f;
    
    // PURPLE (Hue ~260-320)
    private float purpleHueMin = 113f;
    private float purpleHueMax = 196f;
    private float purpleSatMin = 0.27f;
    private float purpleValMin = 0.3f;
    
    // Parámetros de confirmación
    private int requiredConsecutiveReadings = 5;    // Lecturas consecutivas del mismo color
    private int maxReadingsToConfirm = 50;          // Máximo de lecturas antes de declarar UNKNOWN
    private long maxTimeToConfirmMs = 2000;         // Timeout de confirmación
    
    // ==================== ESTADO ====================
    
    private final SensorRevColorV3 colorSensor;
    private DetectionState state = DetectionState.IDLE;
    private DetectedColor currentColor = DetectedColor.NONE;
    private DetectedColor lastReadColor = DetectedColor.NONE;
    
    private int consecutiveCount = 0;
    private int totalReadings = 0;
    private long detectionStartTime = 0;
    
    // Estadísticas de la última detección
    private float lastHue, lastSat, lastVal;
    private double lastDistance;
    private int lastR, lastG, lastB;
    
    // Buffer para promedios (útil para debugging)
    private float avgHue, avgSat, avgVal;
    private int avgCount = 0;
    
    // ==================== CONSTRUCTOR ====================
    
    public BallColorDetector(SensorRevColorV3 colorSensor) {
        this.colorSensor = colorSensor;
    }
    
    // ==================== MÉTODOS PRINCIPALES ====================
    
    /**
     * Actualiza el estado de detección. Llamar en cada ciclo del loop.
     * @return true si el estado cambió a CONFIRMED
     */
    public boolean update() {
        // Leer sensores
        readSensorValues();
        
        boolean wasConfirmed = (state == DetectionState.CONFIRMED);
        
        // Máquina de estados
        switch (state) {
            case IDLE:
                handleIdleState();
                break;
            case DETECTING:
                handleDetectingState();
                break;
            case CONFIRMED:
                // Mantener estado hasta reset manual
                // Opcionalmente verificar si la pelota sigue presente
                if (lastDistance > distanceThresholdHole * 2) {
                    // Pelota removida
                    reset();
                }
                break;
        }
        
        return !wasConfirmed && state == DetectionState.CONFIRMED;
    }
    
    /**
     * Resetea el detector para una nueva detección.
     */
    public void reset() {
        state = DetectionState.IDLE;
        currentColor = DetectedColor.NONE;
        lastReadColor = DetectedColor.NONE;
        consecutiveCount = 0;
        totalReadings = 0;
        detectionStartTime = 0;
        avgHue = avgSat = avgVal = 0;
        avgCount = 0;
    }
    
    // ==================== MÁQUINA DE ESTADOS ====================
    
    private void handleIdleState() {
        if (isBallPresent()) {
            state = DetectionState.DETECTING;
            detectionStartTime = System.currentTimeMillis();
            totalReadings = 0;
            consecutiveCount = 0;
        }
    }
    
    private void handleDetectingState() {
        // Verificar timeout
        if (System.currentTimeMillis() - detectionStartTime > maxTimeToConfirmMs) {
            currentColor = DetectedColor.UNKNOWN;
            state = DetectionState.CONFIRMED;
            return;
        }
        
        // Verificar máximo de lecturas
        if (totalReadings >= maxReadingsToConfirm) {
            currentColor = DetectedColor.UNKNOWN;
            state = DetectionState.CONFIRMED;
            return;
        }
        
        // Si la pelota ya no está (agujero grande o removida), seguir esperando
        if (!isBallPresent()) {
            // No resetear contadores, solo esperar
            return;
        }
        
        // Clasificar color actual
        DetectedColor readColor = classifyColor();
        totalReadings++;
        
        // Acumular para promedio
        if (readColor != DetectedColor.NONE && readColor != DetectedColor.UNKNOWN) {
            avgHue = (avgHue * avgCount + lastHue) / (avgCount + 1);
            avgSat = (avgSat * avgCount + lastSat) / (avgCount + 1);
            avgVal = (avgVal * avgCount + lastVal) / (avgCount + 1);
            avgCount++;
        }
        
        // Verificar consistencia
        if (readColor == lastReadColor && readColor != DetectedColor.NONE && readColor != DetectedColor.UNKNOWN) {
            consecutiveCount++;
            if (consecutiveCount >= requiredConsecutiveReadings) {
                currentColor = readColor;
                state = DetectionState.CONFIRMED;
            }
        } else {
            consecutiveCount = 1;
            lastReadColor = readColor;
        }
    }
    
    // ==================== LECTURA Y CLASIFICACIÓN ====================
    
    private void readSensorValues() {
        // RGB
        lastR = colorSensor.red();
        lastG = colorSensor.green();
        lastB = colorSensor.blue();
        
        // Distancia
        lastDistance = colorSensor.distance(DistanceUnit.CM);
        
        // Convertir a HSV
        float[] hsv = new float[3];
        colorSensor.RGBtoHSV(lastR, lastG, lastB, hsv);
        lastHue = hsv[0];
        lastSat = hsv[1];
        lastVal = hsv[2];
    }
    
    private boolean isBallPresent() {
        return lastDistance < distanceThresholdPresent;
    }
    
    private DetectedColor classifyColor() {
        // Si la distancia indica agujero, no clasificar
        if (lastDistance > distanceThresholdPresent) {
            return DetectedColor.NONE;
        }
        
        // Verificar saturación y valor mínimos (evitar lecturas de agujeros/sombras)
        if (lastSat < 0.15f || lastVal < 0.15f) {
            return DetectedColor.NONE;
        }
        
        // Clasificar por Hue
        // YELLOW
        if (lastHue >= yellowHueMin && lastHue <= yellowHueMax 
            && lastSat >= yellowSatMin && lastVal >= yellowValMin) {
            return DetectedColor.YELLOW;
        }
        
        // GREEN
        if (lastHue >= greenHueMin && lastHue <= greenHueMax 
            && lastSat >= greenSatMin && lastVal >= greenValMin) {
            return DetectedColor.GREEN;
        }
        
        // PURPLE (puede cruzar 360°)
        if ((lastHue >= purpleHueMin || lastHue <= (purpleHueMax - 360f)) 
            && lastSat >= purpleSatMin && lastVal >= purpleValMin) {
            return DetectedColor.PURPLE;
        }
        
        return DetectedColor.UNKNOWN;
    }
    
    // ==================== GETTERS ====================
    
    public DetectionState getState() { return state; }
    public DetectedColor getCurrentColor() { return currentColor; }
    public DetectedColor getLastReadColor() { return lastReadColor; }
    
    public boolean isConfirmed() { return state == DetectionState.CONFIRMED; }
    public boolean isDetecting() { return state == DetectionState.DETECTING; }
    public boolean isIdle() { return state == DetectionState.IDLE; }
    
    public int getConsecutiveCount() { return consecutiveCount; }
    public int getTotalReadings() { return totalReadings; }
    
    // Valores crudos del sensor
    public int getLastR() { return lastR; }
    public int getLastG() { return lastG; }
    public int getLastB() { return lastB; }
    public float getLastHue() { return lastHue; }
    public float getLastSat() { return lastSat; }
    public float getLastVal() { return lastVal; }
    public double getLastDistance() { return lastDistance; }
    
    // Promedios de la sesión de detección
    public float getAvgHue() { return avgHue; }
    public float getAvgSat() { return avgSat; }
    public float getAvgVal() { return avgVal; }
    
    // ==================== SETTERS PARA CALIBRACIÓN ====================
    
    public void setDistanceThresholds(double present, double hole) {
        this.distanceThresholdPresent = present;
        this.distanceThresholdHole = hole;
    }
    
    public void setYellowThresholds(float hueMin, float hueMax, float satMin, float valMin) {
        this.yellowHueMin = hueMin;
        this.yellowHueMax = hueMax;
        this.yellowSatMin = satMin;
        this.yellowValMin = valMin;
    }
    
    public void setGreenThresholds(float hueMin, float hueMax, float satMin, float valMin) {
        this.greenHueMin = hueMin;
        this.greenHueMax = hueMax;
        this.greenSatMin = satMin;
        this.greenValMin = valMin;
    }
    
    public void setPurpleThresholds(float hueMin, float hueMax, float satMin, float valMin) {
        this.purpleHueMin = hueMin;
        this.purpleHueMax = hueMax;
        this.purpleSatMin = satMin;
        this.purpleValMin = valMin;
    }
    
    public void setConfirmationParams(int consecutiveRequired, int maxReadings, long timeoutMs) {
        this.requiredConsecutiveReadings = consecutiveRequired;
        this.maxReadingsToConfirm = maxReadings;
        this.maxTimeToConfirmMs = timeoutMs;
    }
    
    // ==================== DEBUG STRING ====================
    
    public String getDebugString() {
        return String.format(
            "State: %s | Color: %s\n" +
            "RGB: %d,%d,%d | HSV: %.1f,%.2f,%.2f\n" +
            "Dist: %.2fcm | Consec: %d/%d | Total: %d",
            state, currentColor,
            lastR, lastG, lastB,
            lastHue, lastSat, lastVal,
            lastDistance,
            consecutiveCount, requiredConsecutiveReadings,
            totalReadings
        );
    }
}
