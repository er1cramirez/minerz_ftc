package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.ArrayList;
import java.util.List;

/**
 * OpMode de calibración COMPLETO para el sensor del Spindexer
 * 
 * OBJETIVOS:
 * 1. Determinar tiempo mínimo de lectura necesario
 * 2. Calibrar umbrales óptimos automáticamente
 * 3. Probar votación con diferentes cantidades de samples
 * 4. Validar detección en tiempo real
 * 5. Exportar constantes listas para usar
 * 
 * MODOS DE OPERACIÓN:
 * - CALIBRATION: Capturar samples y calcular umbrales
 * - VALIDATION: Probar detección en tiempo real
 * - VOTING_TEST: Simular flujo de intake con votación
 * - EXPORT: Mostrar código para copiar
 * 
 * CONTROLES:
 * DPAD_UP    - Modo: Calibración
 * DPAD_DOWN  - Modo: Validación
 * DPAD_LEFT  - Modo: Testing de Votación
 * DPAD_RIGHT - Modo: Exportar Constantes
 * 
 * A - Capturar sample AMARILLO
 * B - Capturar sample PÚRPURA
 * X - Limpiar calibración
 * Y - Calcular umbrales óptimos
 * 
 * RIGHT_BUMPER - Aumentar samples por captura (+5)
 * LEFT_BUMPER  - Disminuir samples por captura (-5)
 * 
 * START - Iniciar test de votación (modo VOTING_TEST)
 * BACK  - Toggle: Mostrar valores crudos vs normalizados
 */
@TeleOp(name = "Spindexer Sensor Calibration", group = "Testing")
public class SpindexerSensorCalibration extends LinearOpMode {
    
    // ==================== ENUMS ====================
    
    enum OperationMode {
        CALIBRATION,    // Capturar samples y calibrar
        VALIDATION,     // Validar detección en tiempo real
        VOTING_TEST,    // Probar sistema de votación
        EXPORT          // Mostrar constantes para exportar
    }
    
    enum BallColor {
        YELLOW,
        PURPLE,
        NONE,
        UNKNOWN
    }
    
    // ==================== HARDWARE ====================
    
    private SensorRevColorV3 colorSensor;
    private GamepadEx gamepadEx;
    
    // ==================== ESTADO ====================
    
    private OperationMode currentMode = OperationMode.CALIBRATION;
    private boolean showRawValues = false;
    
    // Configuración de captura
    private int samplesPerCapture = 20;  // Número de lecturas por captura
    private int sampleDelayMs = 50;      // Delay entre lecturas (ms)
    
    // Datos de calibración
    private List<ColorSample> yellowSamples = new ArrayList<>();
    private List<ColorSample> purpleSamples = new ArrayList<>();
    
    // Umbrales calculados
    private ColorThresholds thresholds = null;
    
    // Test de votación
    private ElapsedTime votingTimer = new ElapsedTime();
    private List<BallColor> votingResults = new ArrayList<>();
    private boolean votingInProgress = false;
    
    // ==================== CLASES DE DATOS ====================
    
    /**
     * Sample individual de color
     */
    static class ColorSample {
        int red, green, blue;
        double distance;
        long timestamp;
        
        // Valores normalizados (0-100%)
        double redPercent, greenPercent, bluePercent;
        
        ColorSample(int r, int g, int b, double dist) {
            this.red = r;
            this.green = g;
            this.blue = b;
            this.distance = dist;
            this.timestamp = System.currentTimeMillis();
            
            // Normalizar
            int total = r + g + b;
            if (total > 0) {
                redPercent = (r * 100.0) / total;
                greenPercent = (g * 100.0) / total;
                bluePercent = (b * 100.0) / total;
            }
        }
    }
    
    /**
     * Estadísticas de un conjunto de samples
     */
    static class ColorStats {
        double avgRed, avgGreen, avgBlue;
        double minRed, maxRed, stdDevRed;
        double minGreen, maxGreen, stdDevGreen;
        double minBlue, maxBlue, stdDevBlue;
        double avgDistance;
        int sampleCount;
        
        ColorStats(List<ColorSample> samples) {
            if (samples.isEmpty()) {
                sampleCount = 0;
                return;
            }
            
            sampleCount = samples.size();
            
            // Calcular promedios
            double sumRed = 0, sumGreen = 0, sumBlue = 0, sumDist = 0;
            minRed = minGreen = minBlue = 100;
            maxRed = maxGreen = maxBlue = 0;
            
            for (ColorSample s : samples) {
                sumRed += s.redPercent;
                sumGreen += s.greenPercent;
                sumBlue += s.bluePercent;
                sumDist += s.distance;
                
                minRed = Math.min(minRed, s.redPercent);
                maxRed = Math.max(maxRed, s.redPercent);
                minGreen = Math.min(minGreen, s.greenPercent);
                maxGreen = Math.max(maxGreen, s.greenPercent);
                minBlue = Math.min(minBlue, s.bluePercent);
                maxBlue = Math.max(maxBlue, s.bluePercent);
            }
            
            avgRed = sumRed / sampleCount;
            avgGreen = sumGreen / sampleCount;
            avgBlue = sumBlue / sampleCount;
            avgDistance = sumDist / sampleCount;
            
            // Calcular desviación estándar
            double varRed = 0, varGreen = 0, varBlue = 0;
            for (ColorSample s : samples) {
                varRed += Math.pow(s.redPercent - avgRed, 2);
                varGreen += Math.pow(s.greenPercent - avgGreen, 2);
                varBlue += Math.pow(s.bluePercent - avgBlue, 2);
            }
            
            stdDevRed = Math.sqrt(varRed / sampleCount);
            stdDevGreen = Math.sqrt(varGreen / sampleCount);
            stdDevBlue = Math.sqrt(varBlue / sampleCount);
        }
    }
    
    /**
     * Umbrales calculados para detección
     */
    static class ColorThresholds {
        // Distancia para detectar presencia de pelota
        double ballDetectionDistance;
        
        // Umbrales para amarillo (%)
        double yellowRedMin, yellowGreenMin, yellowBlueMax;
        
        // Umbrales para púrpura (%)
        double purpleRedMin, purpleBlueMin, purpleGreenMax;
        
        // Margen de seguridad aplicado (0.0-1.0)
        double safetyMargin = 0.1;  // 10% de margen
        
        ColorThresholds(ColorStats yellowStats, ColorStats purpleStats) {
            // Distancia: promedio de ambos colores
            ballDetectionDistance = (yellowStats.avgDistance + purpleStats.avgDistance) / 2.0 + 1.0;
            
            // Amarillo: alto rojo y verde, bajo azul
            yellowRedMin = yellowStats.avgRed - (yellowStats.stdDevRed * 2) - (yellowStats.avgRed * safetyMargin);
            yellowGreenMin = yellowStats.avgGreen - (yellowStats.stdDevGreen * 2) - (yellowStats.avgGreen * safetyMargin);
            yellowBlueMax = yellowStats.avgBlue + (yellowStats.stdDevBlue * 2) + (yellowStats.avgBlue * safetyMargin);
            
            // Púrpura: alto azul y algo de rojo, bajo verde
            purpleRedMin = purpleStats.avgRed - (purpleStats.stdDevRed * 2) - (purpleStats.avgRed * safetyMargin);
            purpleBlueMin = purpleStats.avgBlue - (purpleStats.stdDevBlue * 2) - (purpleStats.avgBlue * safetyMargin);
            purpleGreenMax = purpleStats.avgGreen + (purpleStats.stdDevGreen * 2) + (purpleStats.avgGreen * safetyMargin);
            
            // Asegurar valores en rango [0, 100]
            yellowRedMin = Math.max(0, Math.min(100, yellowRedMin));
            yellowGreenMin = Math.max(0, Math.min(100, yellowGreenMin));
            yellowBlueMax = Math.max(0, Math.min(100, yellowBlueMax));
            purpleRedMin = Math.max(0, Math.min(100, purpleRedMin));
            purpleBlueMin = Math.max(0, Math.min(100, purpleBlueMin));
            purpleGreenMax = Math.max(0, Math.min(100, purpleGreenMax));
        }
        
        /**
         * Detecta color basándose en los umbrales
         */
        BallColor detectColor(double redPercent, double greenPercent, double bluePercent, double distance) {
            // Verificar si hay pelota
            if (distance > ballDetectionDistance) {
                return BallColor.NONE;
            }
            
            // Verificar amarillo
            boolean isYellow = redPercent >= yellowRedMin && 
                              greenPercent >= yellowGreenMin && 
                              bluePercent <= yellowBlueMax;
            
            // Verificar púrpura
            boolean isPurple = redPercent >= purpleRedMin && 
                              bluePercent >= purpleBlueMin && 
                              greenPercent <= purpleGreenMax;
            
            if (isYellow && !isPurple) return BallColor.YELLOW;
            if (isPurple && !isYellow) return BallColor.PURPLE;
            if (isYellow && isPurple) return BallColor.UNKNOWN;  // Ambiguo
            
            return BallColor.UNKNOWN;
        }
    }
    
    // ==================== MAIN LOOP ====================
    
    @Override
    public void runOpMode() {
        // Inicializar gamepad
        gamepadEx = new GamepadEx(gamepad1);
        
        // Inicializar sensor
        try {
            colorSensor = new SensorRevColorV3(hardwareMap, "colorSensor", DistanceUnit.CM);
            telemetry.addData("✓", "Sensor inicializado");
        } catch (Exception e) {
            telemetry.addData("✗", "ERROR: Sensor no encontrado");
            telemetry.addData("", "Verifica hardware config");
            telemetry.update();
            waitForStart();
            return;
        }
        
        telemetry.addData("Status", "Listo para calibrar");
        telemetry.addData("Modo", currentMode.name());
        telemetry.update();
        
        waitForStart();
        
        while (opModeIsActive()) {
            gamepadEx.readButtons();
            
            handleModeSwitch();
            handleControls();
            
            switch (currentMode) {
                case CALIBRATION:
                    runCalibrationMode();
                    break;
                case VALIDATION:
                    runValidationMode();
                    break;
                case VOTING_TEST:
                    runVotingTestMode();
                    break;
                case EXPORT:
                    runExportMode();
                    break;
            }
            
            sleep(50);
        }
    }
    
    // ==================== CAMBIO DE MODOS ====================
    
    private void handleModeSwitch() {
        if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_UP)) {
            currentMode = OperationMode.CALIBRATION;
        } else if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_DOWN)) {
            currentMode = OperationMode.VALIDATION;
        } else if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_LEFT)) {
            currentMode = OperationMode.VOTING_TEST;
        } else if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_RIGHT)) {
            currentMode = OperationMode.EXPORT;
        }
        
        if (gamepadEx.wasJustPressed(GamepadKeys.Button.BACK)) {
            showRawValues = !showRawValues;
        }
    }
    
    // ==================== CONTROLES GENERALES ====================
    
    private void handleControls() {
        // Ajustar samples por captura
        if (gamepadEx.wasJustPressed(GamepadKeys.Button.RIGHT_BUMPER)) {
            samplesPerCapture += 5;
        } else if (gamepadEx.wasJustPressed(GamepadKeys.Button.LEFT_BUMPER) && samplesPerCapture > 5) {
            samplesPerCapture -= 5;
        }
    }
    
    // ==================== MODO: CALIBRATION ====================
    
    private void runCalibrationMode() {
        telemetry.clear();
        telemetry.addLine("═══ MODO: CALIBRACIÓN ═══");
        telemetry.addLine();
        
        // Valores actuales
        ColorSample current = readCurrentSample();
        displayCurrentReading(current);
        
        // Estadísticas de samples capturados
        telemetry.addLine("─── SAMPLES CAPTURADOS ───");
        telemetry.addData("Amarillo", yellowSamples.size());
        telemetry.addData("Púrpura", purpleSamples.size());
        telemetry.addData("Samples/captura", samplesPerCapture);
        telemetry.addLine();
        
        // Controles de calibración
        if (gamepadEx.wasJustPressed(GamepadKeys.Button.A)) {
            captureColorSamples(BallColor.YELLOW);
        } else if (gamepadEx.wasJustPressed(GamepadKeys.Button.B)) {
            captureColorSamples(BallColor.PURPLE);
        } else if (gamepadEx.wasJustPressed(GamepadKeys.Button.X)) {
            yellowSamples.clear();
            purpleSamples.clear();
            thresholds = null;
            telemetry.addData("✓", "Calibración limpiada");
        } else if (gamepadEx.wasJustPressed(GamepadKeys.Button.Y) && yellowSamples.size() > 0 && purpleSamples.size() > 0) {
            calculateThresholds();
        }
        
        // Mostrar estadísticas si hay datos
        if (yellowSamples.size() > 0 || purpleSamples.size() > 0) {
            displayCalibrationStats();
        }
        
        // Controles
        telemetry.addLine("─── CONTROLES ───");
        telemetry.addData("A", "Capturar AMARILLO");
        telemetry.addData("B", "Capturar PÚRPURA");
        telemetry.addData("X", "Limpiar todo");
        if (yellowSamples.size() > 0 && purpleSamples.size() > 0) {
            telemetry.addData("Y", "Calcular umbrales ★");
        }
        telemetry.addData("BUMPERS", "±5 samples");
        
        telemetry.update();
    }
    
    private void captureColorSamples(BallColor color) {
        telemetry.clear();
        telemetry.addLine("CAPTURANDO " + color.name() + "...");
        telemetry.addData("Samples", samplesPerCapture);
        telemetry.update();
        
        List<ColorSample> samples = new ArrayList<>();
        
        for (int i = 0; i < samplesPerCapture; i++) {
            ColorSample sample = readCurrentSample();
            samples.add(sample);
            
            telemetry.addData("Progreso", "%d/%d", i + 1, samplesPerCapture);
            telemetry.update();
            
            sleep(sampleDelayMs);
        }
        
        if (color == BallColor.YELLOW) {
            yellowSamples.addAll(samples);
        } else {
            purpleSamples.addAll(samples);
        }
        
        telemetry.addData("✓", "Captura completada");
        telemetry.update();
        sleep(500);
    }
    
    private void calculateThresholds() {
        ColorStats yellowStats = new ColorStats(yellowSamples);
        ColorStats purpleStats = new ColorStats(purpleSamples);
        
        thresholds = new ColorThresholds(yellowStats, purpleStats);
        
        telemetry.clear();
        telemetry.addLine("✓ UMBRALES CALCULADOS");
        telemetry.addLine();
        telemetry.addData("Samples amarillo", yellowSamples.size());
        telemetry.addData("Samples púrpura", purpleSamples.size());
        telemetry.addLine();
        telemetry.addLine("Cambia a modo VALIDATION");
        telemetry.addLine("o EXPORT para ver valores");
        telemetry.update();
        sleep(2000);
    }
    
    private void displayCalibrationStats() {
        if (yellowSamples.size() > 0) {
            ColorStats stats = new ColorStats(yellowSamples);
            telemetry.addLine("─── AMARILLO ───");
            telemetry.addData("R", "%.1f%% (±%.1f)", stats.avgRed, stats.stdDevRed);
            telemetry.addData("G", "%.1f%% (±%.1f)", stats.avgGreen, stats.stdDevGreen);
            telemetry.addData("B", "%.1f%% (±%.1f)", stats.avgBlue, stats.stdDevBlue);
            telemetry.addLine();
        }
        
        if (purpleSamples.size() > 0) {
            ColorStats stats = new ColorStats(purpleSamples);
            telemetry.addLine("─── PÚRPURA ───");
            telemetry.addData("R", "%.1f%% (±%.1f)", stats.avgRed, stats.stdDevRed);
            telemetry.addData("G", "%.1f%% (±%.1f)", stats.avgGreen, stats.stdDevGreen);
            telemetry.addData("B", "%.1f%% (±%.1f)", stats.avgBlue, stats.stdDevBlue);
            telemetry.addLine();
        }
    }
    
    // ==================== MODO: VALIDATION ====================
    
    private void runValidationMode() {
        if (thresholds == null) {
            telemetry.clear();
            telemetry.addLine("═══ MODO: VALIDACIÓN ═══");
            telemetry.addLine();
            telemetry.addData("✗", "No hay umbrales");
            telemetry.addLine("Calibra primero en modo CALIBRATION");
            telemetry.update();
            return;
        }
        
        telemetry.clear();
        telemetry.addLine("═══ MODO: VALIDACIÓN ═══");
        telemetry.addLine();
        
        ColorSample current = readCurrentSample();
        BallColor detected = thresholds.detectColor(
            current.redPercent, 
            current.greenPercent, 
            current.bluePercent, 
            current.distance
        );
        
        // Mostrar detección con énfasis
        telemetry.addLine("─── DETECCIÓN ───");
        String detectionEmoji = getColorEmoji(detected);
        telemetry.addData("Color", "%s %s", detectionEmoji, detected.name());
        telemetry.addData("Confianza", getConfidence(current, detected));
        telemetry.addLine();
        
        // Valores actuales
        displayCurrentReading(current);
        
        // Umbrales activos
        telemetry.addLine("─── UMBRALES ACTIVOS ───");
        telemetry.addData("Distancia max", "%.2f cm", thresholds.ballDetectionDistance);
        telemetry.addLine();
        telemetry.addData("Amarillo R≥", "%.1f%%", thresholds.yellowRedMin);
        telemetry.addData("Amarillo G≥", "%.1f%%", thresholds.yellowGreenMin);
        telemetry.addData("Amarillo B≤", "%.1f%%", thresholds.yellowBlueMax);
        telemetry.addLine();
        telemetry.addData("Púrpura R≥", "%.1f%%", thresholds.purpleRedMin);
        telemetry.addData("Púrpura B≥", "%.1f%%", thresholds.purpleBlueMin);
        telemetry.addData("Púrpura G≤", "%.1f%%", thresholds.purpleGreenMax);
        
        telemetry.update();
    }
    
    private String getConfidence(ColorSample sample, BallColor detected) {
        if (detected == BallColor.NONE) {
            return "N/A (sin pelota)";
        }
        if (detected == BallColor.UNKNOWN) {
            return "Baja (ambiguo)";
        }
        
        // Calcular qué tan dentro de los rangos está
        double confidence = 100.0;
        
        if (detected == BallColor.YELLOW) {
            double redMargin = (sample.redPercent - thresholds.yellowRedMin) / thresholds.yellowRedMin;
            double greenMargin = (sample.greenPercent - thresholds.yellowGreenMin) / thresholds.yellowGreenMin;
            double blueMargin = (thresholds.yellowBlueMax - sample.bluePercent) / thresholds.yellowBlueMax;
            confidence = Math.min(100, (redMargin + greenMargin + blueMargin) * 100 / 3);
        } else {
            double redMargin = (sample.redPercent - thresholds.purpleRedMin) / thresholds.purpleRedMin;
            double blueMargin = (sample.bluePercent - thresholds.purpleBlueMin) / thresholds.purpleBlueMin;
            double greenMargin = (thresholds.purpleGreenMax - sample.greenPercent) / thresholds.purpleGreenMax;
            confidence = Math.min(100, (redMargin + blueMargin + greenMargin) * 100 / 3);
        }
        
        return String.format("%.0f%%", Math.max(0, confidence));
    }
    
    // ==================== MODO: VOTING TEST ====================
    
    private void runVotingTestMode() {
        telemetry.clear();
        telemetry.addLine("═══ MODO: TEST DE VOTACIÓN ═══");
        telemetry.addLine();
        
        if (thresholds == null) {
            telemetry.addData("✗", "No hay umbrales");
            telemetry.addLine("Calibra primero");
            telemetry.update();
            return;
        }
        
        if (!votingInProgress) {
            telemetry.addLine("Simula el flujo de intake:");
            telemetry.addLine("1. Coloca pelota frente al sensor");
            telemetry.addLine("2. Presiona START");
            telemetry.addLine("3. Sistema toma N lecturas");
            telemetry.addLine("4. Muestra resultado de votación");
            telemetry.addLine();
            telemetry.addData("Samples a tomar", samplesPerCapture);
            telemetry.addData("Delay entre samples", "%d ms", sampleDelayMs);
            telemetry.addLine();
            telemetry.addData("START", "Iniciar votación");
            telemetry.addData("BUMPERS", "Ajustar samples");
            
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.START)) {
                startVotingTest();
            }
        } else {
            // Mostrar progreso
            int elapsed = (int) votingTimer.milliseconds();
            int totalTime = samplesPerCapture * sampleDelayMs;
            int progress = Math.min(100, (elapsed * 100) / totalTime);
            
            telemetry.addData("Progreso", "%d%% (%d/%d)", 
                progress, 
                votingResults.size(), 
                samplesPerCapture);
            
            // Barra de progreso
            telemetry.addData("", getProgressBar(progress));
            telemetry.addLine();
            
            // Votos parciales
            if (votingResults.size() > 0) {
                int yellowVotes = 0, purpleVotes = 0, unknownVotes = 0;
                for (BallColor c : votingResults) {
                    if (c == BallColor.YELLOW) yellowVotes++;
                    else if (c == BallColor.PURPLE) purpleVotes++;
                    else unknownVotes++;
                }
                
                telemetry.addLine("─── VOTOS PARCIALES ───");
                telemetry.addData("🟡 Amarillo", yellowVotes);
                telemetry.addData("🟣 Púrpura", purpleVotes);
                telemetry.addData("❓ Unknown", unknownVotes);
            }
            
            // Verificar si terminó
            if (votingResults.size() >= samplesPerCapture) {
                finishVotingTest();
            }
        }
        
        telemetry.update();
    }
    
    private void startVotingTest() {
        votingInProgress = true;
        votingResults.clear();
        votingTimer.reset();
        
        // Thread para capturar samples
        new Thread(() -> {
            for (int i = 0; i < samplesPerCapture && opModeIsActive(); i++) {
                ColorSample sample = readCurrentSample();
                BallColor detected = thresholds.detectColor(
                    sample.redPercent, 
                    sample.greenPercent, 
                    sample.bluePercent, 
                    sample.distance
                );
                
                synchronized (votingResults) {
                    votingResults.add(detected);
                }
                
                try {
                    Thread.sleep(sampleDelayMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
    
    private void finishVotingTest() {
        // Contar votos
        int yellowVotes = 0, purpleVotes = 0, noneVotes = 0, unknownVotes = 0;
        for (BallColor c : votingResults) {
            switch (c) {
                case YELLOW: yellowVotes++; break;
                case PURPLE: purpleVotes++; break;
                case NONE: noneVotes++; break;
                case UNKNOWN: unknownVotes++; break;
            }
        }
        
        // Determinar ganador
        BallColor winner = BallColor.UNKNOWN;
        int maxVotes = Math.max(Math.max(yellowVotes, purpleVotes), unknownVotes);
        
        if (noneVotes > samplesPerCapture / 2) {
            winner = BallColor.NONE;
        } else if (yellowVotes == maxVotes && yellowVotes > purpleVotes) {
            winner = BallColor.YELLOW;
        } else if (purpleVotes == maxVotes && purpleVotes > yellowVotes) {
            winner = BallColor.PURPLE;
        }
        
        // Mostrar resultados
        telemetry.clear();
        telemetry.addLine("═══ RESULTADO DE VOTACIÓN ═══");
        telemetry.addLine();
        
        String emoji = getColorEmoji(winner);
        telemetry.addData("GANADOR", "%s %s", emoji, winner.name());
        telemetry.addLine();
        
        telemetry.addLine("─── VOTOS ───");
        telemetry.addData("🟡 Amarillo", "%d (%.0f%%)", yellowVotes, yellowVotes * 100.0 / samplesPerCapture);
        telemetry.addData("🟣 Púrpura", "%d (%.0f%%)", purpleVotes, purpleVotes * 100.0 / samplesPerCapture);
        telemetry.addData("⚫ Nada", "%d (%.0f%%)", noneVotes, noneVotes * 100.0 / samplesPerCapture);
        telemetry.addData("❓ Unknown", "%d (%.0f%%)", unknownVotes, unknownVotes * 100.0 / samplesPerCapture);
        telemetry.addLine();
        
        int totalTime = (int) votingTimer.milliseconds();
        telemetry.addData("Tiempo total", "%d ms", totalTime);
        telemetry.addData("Tiempo/sample", "%.1f ms", totalTime / (double) samplesPerCapture);
        telemetry.addLine();
        
        // Evaluación
        double winnerPercent = maxVotes * 100.0 / samplesPerCapture;
        if (winnerPercent >= 80) {
            telemetry.addData("Evaluación", "✓ EXCELENTE - Alta confianza");
        } else if (winnerPercent >= 60) {
            telemetry.addData("Evaluación", "✓ BUENO - Confianza aceptable");
        } else {
            telemetry.addData("Evaluación", "⚠ POBRE - Baja confianza");
            telemetry.addLine("Considera re-calibrar o ajustar posición");
        }
        
        telemetry.addLine();
        telemetry.addData("START", "Repetir test");
        telemetry.update();
        
        votingInProgress = false;
        
        // Esperar a que se suelte el botón
        while (opModeIsActive() && gamepadEx.isDown(GamepadKeys.Button.START)) {
            gamepadEx.readButtons();
            sleep(50);
        }
    }
    
    // ==================== MODO: EXPORT ====================
    
    private void runExportMode() {
        telemetry.clear();
        telemetry.addLine("═══ EXPORTAR CONSTANTES ═══");
        telemetry.addLine();
        
        if (thresholds == null) {
            telemetry.addData("✗", "No hay umbrales calculados");
            telemetry.addLine("Calibra primero");
            telemetry.update();
            return;
        }
        
        telemetry.addLine("Copia este código a");
        telemetry.addLine("SpindexerConstants.java:");
        telemetry.addLine();
        telemetry.addLine("─────────────────────────");
        telemetry.addLine();
        
        // Generar código
        telemetry.addLine("// Detección de pelota");
        telemetry.addData("BALL_DETECTION_DISTANCE", 
            String.format("%.2f", thresholds.ballDetectionDistance));
        telemetry.addLine();
        
        telemetry.addLine("// Umbrales AMARILLO (%)");
        telemetry.addData("YELLOW_RED_MIN", 
            String.format("%.2f", thresholds.yellowRedMin));
        telemetry.addData("YELLOW_GREEN_MIN", 
            String.format("%.2f", thresholds.yellowGreenMin));
        telemetry.addData("YELLOW_BLUE_MAX", 
            String.format("%.2f", thresholds.yellowBlueMax));
        telemetry.addLine();
        
        telemetry.addLine("// Umbrales PÚRPURA (%)");
        telemetry.addData("PURPLE_RED_MIN", 
            String.format("%.2f", thresholds.purpleRedMin));
        telemetry.addData("PURPLE_BLUE_MIN", 
            String.format("%.2f", thresholds.purpleBlueMin));
        telemetry.addData("PURPLE_GREEN_MAX", 
            String.format("%.2f", thresholds.purpleGreenMax));
        
        telemetry.addLine();
        telemetry.addLine("─────────────────────────");
        telemetry.addLine();
        telemetry.addLine("Recomendaciones:");
        telemetry.addData("Samples óptimos", "%d-%d", 
            Math.max(10, samplesPerCapture - 5), 
            samplesPerCapture + 5);
        telemetry.addData("Delay óptimo", "%d ms", sampleDelayMs);
        
        telemetry.update();
    }
    
    // ==================== UTILIDADES ====================
    
    private ColorSample readCurrentSample() {
        int[] argb = colorSensor.getARGB();
        double distance = colorSensor.distance();
        return new ColorSample(argb[1], argb[2], argb[3], distance);
    }
    
    private void displayCurrentReading(ColorSample sample) {
        telemetry.addLine("─── LECTURA ACTUAL ───");
        
        if (showRawValues) {
            telemetry.addData("Red", sample.red);
            telemetry.addData("Green", sample.green);
            telemetry.addData("Blue", sample.blue);
        } else {
            telemetry.addData("Red", "%.1f%%", sample.redPercent);
            telemetry.addData("Green", "%.1f%%", sample.greenPercent);
            telemetry.addData("Blue", "%.1f%%", sample.bluePercent);
        }
        
        telemetry.addData("Distancia", "%.2f cm", sample.distance);
        telemetry.addData("", getProximityBar(sample.distance));
        telemetry.addLine();
    }
    
    private String getColorEmoji(BallColor color) {
        switch (color) {
            case YELLOW: return "🟡";
            case PURPLE: return "🟣";
            case NONE: return "⚫";
            case UNKNOWN: return "❓";
            default: return "❓";
        }
    }
    
    private String getProgressBar(int percent) {
        int bars = percent / 5;  // 20 barras total
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 20; i++) {
            sb.append(i < bars ? "█" : "░");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String getProximityBar(double distance) {
        if (distance > 10) return "[          ] Muy lejos";
        if (distance > 8) return "[•         ] Lejos";
        if (distance > 6) return "[••        ] ";
        if (distance > 4) return "[•••       ] ✓ Bueno";
        if (distance > 2) return "[••••      ] ✓ Óptimo";
        if (distance > 1) return "[•••••     ] ✓ Bueno";
        return "[••••••    ] Muy cerca";
    }
}