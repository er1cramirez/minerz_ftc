package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * OpMode para calibrar los umbrales de detección de color de las pelotas de Decode.
 * 
 * ===== CONTROLES =====
 * 
 * GAMEPAD 1:
 * - A: Iniciar/Detener grabación de sesión
 * - B: Resetear estadísticas de la sesión actual
 * - X: Cambiar color objetivo (YELLOW/GREEN/PURPLE)
 * - Y: Guardar snapshot del valor actual
 * 
 * - DPAD UP/DOWN: Ajustar umbral de distancia (presente)
 * - DPAD LEFT/RIGHT: Ajustar umbral de distancia (agujero)
 * 
 * - LEFT BUMPER: Disminuir lecturas requeridas
 * - RIGHT BUMPER: Aumentar lecturas requeridas
 *
 * - RIGHT TRIGGER: Intake (girar para rotar pelota)
 * - LEFT TRIGGER: Outtake (expulsar)
 * 
 * ===== INSTRUCCIONES =====
 * 
 * 1. Coloca una pelota del color a calibrar en el slot de intake
 * 2. Presiona X para seleccionar el color objetivo
 * 3. Presiona A para iniciar grabación
 * 4. Si tienes intake, hazlo girar para rotar la pelota
 * 5. Observa los valores min/max de HSV durante la grabación
 * 6. Presiona A para detener y ver resumen
 * 7. Usa estos valores para configurar BallColorDetector
 * 
 * @author Seattle Solvers
 */
//@Disabled
@TeleOp(name = "Color Calibration", group = "Tuning")
public class ColorCalibrationOpMode extends OpMode {
    
    // ==================== CONFIGURACIÓN ====================
    
    private static final String COLOR_SENSOR_NAME = "colorSensor"; // Cambiar según config
    
    // ==================== HARDWARE ====================
    
    private SensorRevColorV3 colorSensor;
    private IntakeSubsystem intake;
    
    // ==================== ESTADOS ====================
    
    private enum CalibrationColor {
        YELLOW, GREEN, PURPLE
    }
    
    private CalibrationColor targetColor = CalibrationColor.YELLOW;
    private boolean isRecording = false;
    
    // ==================== ESTADÍSTICAS DE SESIÓN ====================
    
    private int sampleCount = 0;
    
    // RGB
    private int minR, maxR, sumR;
    private int minG, maxG, sumG;
    private int minB, maxB, sumB;
    
    // HSV
    private float minHue, maxHue, sumHue;
    private float minSat, maxSat, sumSat;
    private float minVal, maxVal, sumVal;
    
    // Distancia
    private double minDist, maxDist, sumDist;
    
    // Para detección de cruce de 0° en Hue (para púrpura)
    private boolean hueWrapsAround = false;
    
    // ==================== VALORES ACTUALES ====================
    
    private int currentR, currentG, currentB;
    private float currentHue, currentSat, currentVal;
    private double currentDistance;
    
    // ==================== SNAPSHOTS ====================
    
    private float[] snapshotHue = new float[10];
    private float[] snapshotSat = new float[10];
    private float[] snapshotVal = new float[10];
    private double[] snapshotDist = new double[10];
    private int snapshotCount = 0;
    
    // ==================== PARÁMETROS AJUSTABLES ====================
    
    private double distThresholdPresent = 3.0;
    private double distThresholdHole = 5.0;
    private int requiredReadings = 5;
    
    // ==================== DEBOUNCE ====================
    
    private boolean lastA = false;
    private boolean lastB = false;
    private boolean lastX = false;
    private boolean lastY = false;
    private boolean lastLB = false;
    private boolean lastRB = false;
    private boolean lastDpadUp = false;
    private boolean lastDpadDown = false;
    private boolean lastDpadLeft = false;
    private boolean lastDpadRight = false;
    
    // ==================== TIMING ====================
    
    private ElapsedTime recordingTimer = new ElapsedTime();
    private ElapsedTime loopTimer = new ElapsedTime();
    private double avgLoopTime = 0;
    private int loopCount = 0;
    
    // ==================== LIFECYCLE ====================
    
    @Override
    public void init() {
        // Inicializar sensor
        // Inicializar sensor
        colorSensor = new SensorRevColorV3(hardwareMap, COLOR_SENSOR_NAME, DistanceUnit.CM);
        
        try {
            intake = new IntakeSubsystem(hardwareMap);
        } catch (Exception e) {
            telemetry.addLine("⚠ Intake no encontrado");
        }
        
        resetStats();
        
        telemetry.addLine("=== COLOR CALIBRATION ===");
        telemetry.addLine("Sensor inicializado: " + COLOR_SENSOR_NAME);
        telemetry.addLine("");
        telemetry.addLine("Presiona START para comenzar");
        telemetry.update();
    }
    
    @Override
    public void loop() {
        // Medir tiempo de loop
        double loopTime = loopTimer.milliseconds();
        loopTimer.reset();
        avgLoopTime = (avgLoopTime * loopCount + loopTime) / (loopCount + 1);
        loopCount++;
        
        // Leer sensor
        readSensor();
        
        // Procesar controles
        handleControls();
        handleIntakeControls();
        
        // Si está grabando, acumular estadísticas
        if (isRecording && currentDistance < distThresholdPresent) {
            recordSample();
        }
        
        // Mostrar telemetría
        updateTelemetry();
    }
    
    // ==================== LECTURA DEL SENSOR ====================
    
    private void readSensor() {
        currentR = colorSensor.red();
        currentG = colorSensor.green();
        currentB = colorSensor.blue();
        currentDistance = colorSensor.distance(DistanceUnit.CM);
        
        // Convertir a HSV
        float[] hsv = new float[3];
        colorSensor.RGBtoHSV(currentR, currentG, currentB, hsv);
        currentHue = hsv[0];
        currentSat = hsv[1];
        currentVal = hsv[2];
    }
    
    // ==================== CONTROLES ====================
    
    private void handleControls() {
        // A: Toggle grabación
        if (gamepad1.a && !lastA) {
            isRecording = !isRecording;
            if (isRecording) {
                resetStats();
                recordingTimer.reset();
            }
        }
        lastA = gamepad1.a;
        
        // B: Reset stats
        if (gamepad1.b && !lastB) {
            resetStats();
        }
        lastB = gamepad1.b;
        
        // X: Cambiar color objetivo
        if (gamepad1.x && !lastX) {
            switch (targetColor) {
                case YELLOW: targetColor = CalibrationColor.GREEN; break;
                case GREEN: targetColor = CalibrationColor.PURPLE; break;
                case PURPLE: targetColor = CalibrationColor.YELLOW; break;
            }
            resetStats();
        }
        lastX = gamepad1.x;
        
        // Y: Guardar snapshot
        if (gamepad1.y && !lastY && snapshotCount < 10) {
            snapshotHue[snapshotCount] = currentHue;
            snapshotSat[snapshotCount] = currentSat;
            snapshotVal[snapshotCount] = currentVal;
            snapshotDist[snapshotCount] = currentDistance;
            snapshotCount++;
        }
        lastY = gamepad1.y;
        
        // DPAD: Ajustar umbrales de distancia
        if (gamepad1.dpad_up && !lastDpadUp) {
            distThresholdPresent += 0.5;
        }
        lastDpadUp = gamepad1.dpad_up;
        
        if (gamepad1.dpad_down && !lastDpadDown) {
            distThresholdPresent = Math.max(0.5, distThresholdPresent - 0.5);
        }
        lastDpadDown = gamepad1.dpad_down;
        
        if (gamepad1.dpad_right && !lastDpadRight) {
            distThresholdHole += 0.5;
        }
        lastDpadRight = gamepad1.dpad_right;
        
        if (gamepad1.dpad_left && !lastDpadLeft) {
            distThresholdHole = Math.max(distThresholdPresent + 0.5, distThresholdHole - 0.5);
        }
        lastDpadLeft = gamepad1.dpad_left;
        
        // Bumpers: Ajustar lecturas requeridas
        if (gamepad1.right_bumper && !lastRB) {
            requiredReadings++;
        }
        lastRB = gamepad1.right_bumper;
        
        if (gamepad1.left_bumper && !lastLB) {
            requiredReadings = Math.max(1, requiredReadings - 1);
        }
        lastLB = gamepad1.left_bumper;
    }
    
    private void handleIntakeControls() {
        if (intake == null) return;
        
        if (gamepad1.right_trigger > 0.1) {
            intake.intake();
        } else if (gamepad1.left_trigger > 0.1) {
            intake.outtake();
        } else {
            intake.stop();
        }
    }
    
    // ==================== GRABACIÓN ====================
    
    private void recordSample() {
        sampleCount++;
        
        // RGB
        minR = Math.min(minR, currentR);
        maxR = Math.max(maxR, currentR);
        sumR += currentR;
        
        minG = Math.min(minG, currentG);
        maxG = Math.max(maxG, currentG);
        sumG += currentG;
        
        minB = Math.min(minB, currentB);
        maxB = Math.max(maxB, currentB);
        sumB += currentB;
        
        // HSV (cuidado con el wrap-around del Hue)
        // Detectar si el Hue cruza 0° (para púrpura que puede ser 350°-10°)
        if (sampleCount == 1) {
            minHue = maxHue = currentHue;
        } else {
            // Verificar wrap-around
            if (Math.abs(currentHue - minHue) > 180 || Math.abs(currentHue - maxHue) > 180) {
                hueWrapsAround = true;
            }
            
            if (!hueWrapsAround) {
                minHue = Math.min(minHue, currentHue);
                maxHue = Math.max(maxHue, currentHue);
            } else {
                // Normalizar a 0-360 con offset
                float normalizedHue = currentHue < 180 ? currentHue + 360 : currentHue;
                float normalizedMin = minHue < 180 ? minHue + 360 : minHue;
                float normalizedMax = maxHue < 180 ? maxHue + 360 : maxHue;
                
                normalizedMin = Math.min(normalizedMin, normalizedHue);
                normalizedMax = Math.max(normalizedMax, normalizedHue);
                
                minHue = normalizedMin > 360 ? normalizedMin - 360 : normalizedMin;
                maxHue = normalizedMax > 360 ? normalizedMax - 360 : normalizedMax;
            }
        }
        sumHue += currentHue;
        
        minSat = Math.min(minSat, currentSat);
        maxSat = Math.max(maxSat, currentSat);
        sumSat += currentSat;
        
        minVal = Math.min(minVal, currentVal);
        maxVal = Math.max(maxVal, currentVal);
        sumVal += currentVal;
        
        // Distancia
        minDist = Math.min(minDist, currentDistance);
        maxDist = Math.max(maxDist, currentDistance);
        sumDist += currentDistance;
    }
    
    private void resetStats() {
        sampleCount = 0;
        
        minR = minG = minB = Integer.MAX_VALUE;
        maxR = maxG = maxB = Integer.MIN_VALUE;
        sumR = sumG = sumB = 0;
        
        minHue = minSat = minVal = Float.MAX_VALUE;
        maxHue = maxSat = maxVal = Float.MIN_VALUE;
        sumHue = sumSat = sumVal = 0;
        hueWrapsAround = false;
        
        minDist = Double.MAX_VALUE;
        maxDist = Double.MIN_VALUE;
        sumDist = 0;
        
        snapshotCount = 0;
    }
    
    // ==================== TELEMETRÍA ====================
    
    private void updateTelemetry() {
        telemetry.addLine("════════════════════════════════════");
        telemetry.addLine("       COLOR CALIBRATION");
        telemetry.addLine("════════════════════════════════════");
        
        // Estado
        String recordingStatus = isRecording ? "🔴 GRABANDO" : "⏸ PAUSADO";
        telemetry.addLine(String.format("Estado: %s | Target: %s", recordingStatus, targetColor));
        
        if (isRecording) {
            telemetry.addLine(String.format("Tiempo: %.1fs | Samples: %d", 
                recordingTimer.seconds(), sampleCount));
        }
        
        telemetry.addLine("");
        
        // Valores actuales
        telemetry.addLine("── VALORES ACTUALES ──");
        
        String presenceIndicator = currentDistance < distThresholdPresent ? "✓ PELOTA" : 
                                   (currentDistance < distThresholdHole ? "? AGUJERO" : "✗ VACÍO");
        telemetry.addLine(String.format("Distancia: %.2f cm  [%s]", currentDistance, presenceIndicator));
        
        telemetry.addLine(String.format("RGB: R=%d G=%d B=%d", currentR, currentG, currentB));
        telemetry.addLine(String.format("HSV: H=%.1f° S=%.2f V=%.2f", currentHue, currentSat, currentVal));
        
        // Barra visual de Hue
        telemetry.addLine(getHueBar(currentHue));
        
        telemetry.addLine("");
        
        // Estadísticas de sesión
        if (sampleCount > 0) {
            telemetry.addLine("── ESTADÍSTICAS ──");
            
            float avgHue = sumHue / sampleCount;
            float avgSat = sumSat / sampleCount;
            float avgVal = sumVal / sampleCount;
            double avgDist = sumDist / sampleCount;
            
            telemetry.addLine(String.format("Hue:  min=%.1f  max=%.1f  avg=%.1f  %s", 
                minHue, maxHue, avgHue, hueWrapsAround ? "(wrap)" : ""));
            telemetry.addLine(String.format("Sat:  min=%.2f  max=%.2f  avg=%.2f", 
                minSat, maxSat, avgSat));
            telemetry.addLine(String.format("Val:  min=%.2f  max=%.2f  avg=%.2f", 
                minVal, maxVal, avgVal));
            telemetry.addLine(String.format("Dist: min=%.2f  max=%.2f  avg=%.2f", 
                minDist, maxDist, avgDist));
            
            telemetry.addLine("");
            
            // Sugerencia de umbrales
            telemetry.addLine("── UMBRALES SUGERIDOS ──");
            float hueMargin = (maxHue - minHue) * 0.2f;
            float satMargin = (maxSat - minSat) * 0.2f;
            
            telemetry.addLine(String.format("Hue: %.0f - %.0f", 
                Math.max(0, minHue - hueMargin), 
                Math.min(360, maxHue + hueMargin)));
            telemetry.addLine(String.format("Sat min: %.2f", Math.max(0, minSat - satMargin)));
            telemetry.addLine(String.format("Val min: %.2f", Math.max(0, minVal - 0.05f)));
        }
        
        telemetry.addLine("");
        
        // Parámetros actuales
        telemetry.addLine("── PARÁMETROS ──");
        telemetry.addLine(String.format("Dist presente: %.1f cm [DPAD ↑↓]", distThresholdPresent));
        telemetry.addLine(String.format("Dist agujero: %.1f cm [DPAD ←→]", distThresholdHole));
        telemetry.addLine(String.format("Lecturas req: %d [LB/RB]", requiredReadings));
        
        // Snapshots
        if (snapshotCount > 0) {
            telemetry.addLine("");
            telemetry.addLine(String.format("── SNAPSHOTS (%d/10) ──", snapshotCount));
            for (int i = 0; i < snapshotCount; i++) {
                telemetry.addLine(String.format("  %d: H=%.0f S=%.2f V=%.2f D=%.1f", 
                    i+1, snapshotHue[i], snapshotSat[i], snapshotVal[i], snapshotDist[i]));
            }
        }
        
        telemetry.addLine("");
        telemetry.addLine("── CONTROLES ──");
        telemetry.addLine("A: Grabar | B: Reset | X: Color | Y: Snap");
        if (intake != null) {
            telemetry.addLine(String.format("Intake: %s (RT/LT)", intake.getState()));
        }
        
        // Debug
        telemetry.addLine("");
        telemetry.addLine(String.format("Loop: %.1fms avg", avgLoopTime));
        
        telemetry.update();
    }
    
    /**
     * Genera una barra visual del Hue (0-360°)
     */
    private String getHueBar(float hue) {
        StringBuilder bar = new StringBuilder("Hue: [");
        int position = (int) (hue / 360f * 20);
        for (int i = 0; i < 20; i++) {
            if (i == position) {
                bar.append("█");
            } else {
                // Colores aproximados
                if (i < 2) bar.append("R"); // Rojo
                else if (i < 4) bar.append("O"); // Naranja
                else if (i < 6) bar.append("Y"); // Amarillo
                else if (i < 10) bar.append("G"); // Verde
                else if (i < 13) bar.append("C"); // Cyan
                else if (i < 16) bar.append("B"); // Azul
                else bar.append("P"); // Púrpura/Magenta
            }
        }
        bar.append("]");
        return bar.toString();
    }
}
