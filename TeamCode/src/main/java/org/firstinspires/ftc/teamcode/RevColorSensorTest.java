package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * OpMode para probar el sensor REV Color v3 con SolversLib
 * 
 * OBJETIVO: Determinar si el sensor es útil para tu indexer y dónde posicionarlo
 * 
 * PRUEBAS A REALIZAR:
 * 1. Colocar pelota AMARILLA a diferentes distancias
 * 2. Colocar pelota MORADA a diferentes distancias
 * 3. Probar diferentes ubicaciones del sensor:
 *    - En el intake
 *    - En la base del primer slot de intake
 *    - Otras posiciones que se te ocurran
 * 
 * ANOTA LOS VALORES para cada escenario y determina:
 * - ¿Distingue claramente amarillo de morado?
 * - ¿A qué distancia funciona mejor?
 * - ¿Dónde posicionarlo?
 * 
 * CONTROLES:
 * - A: Capturar snapshot de valores actuales (amarillo)
 * - B: Capturar snapshot de valores actuales (morado)
 * - X: Limpiar snapshots
 * - Y: Comparar amarillo vs morado
 */
@TeleOp(name = "REV Color Sensor Test (SolversLib)", group = "Test")
public class RevColorSensorTest extends LinearOpMode {
    
    private SensorRevColorV3 colorSensor;
    
    // Snapshots para comparar
    private int[] yellowSnapshot = null;
    private double yellowDistance = -1;
    private int[] purpleSnapshot = null;
    private double purpleDistance = -1;

    
    // Contador de lecturas para promediar
    private static final int AVERAGE_SAMPLES = 5;
    
    @Override
    public void runOpMode() {
        // Inicializar sensor con SolversLib
        // Asegúrate de que el sensor esté configurado como "colorSensor" en tu Robot Controller
        try {
            colorSensor = new SensorRevColorV3(hardwareMap, "colorSensor", DistanceUnit.CM);
            telemetry.addData("✓", "Sensor inicializado correctamente");
        } catch (Exception e) {
            telemetry.addData("✗", "Error: Sensor no encontrado");
            telemetry.addData("", "Verifica que esté configurado como 'colorSensor'");
            telemetry.update();
            waitForStart();
            return;
        }
        
        telemetry.addData("Status", "Listo para probar");
        telemetry.addData("", "Coloca pelotas frente al sensor");
        telemetry.addData("", "y observa los valores");
        telemetry.update();
        
        waitForStart();
        
        while (opModeIsActive()) {
            
            // ===== LEER VALORES DEL SENSOR =====
            int[] argb = colorSensor.getARGB();
            int alpha = argb[0];
            int red = argb[1];
            int green = argb[2];
            int blue = argb[3];
            double distance = colorSensor.distance(DistanceUnit.CM);
            
            // ===== CONTROLES =====
            handleControls(argb, distance);
            
            // ===== ANÁLISIS AUTOMÁTICO =====
            String detectedColor = analyzeColor(red, green, blue, distance);
            
            // ===== TELEMETRÍA =====
            displayTelemetry(alpha, red, green, blue, distance, detectedColor);
            
            sleep(50); // Actualizar 20 veces por segundo
        }
    }
    
    /**
     * Maneja los controles del gamepad
     */
    private void handleControls(int[] argb, double distance) {
        // A: Capturar snapshot AMARILLO
        if (gamepad1.a) {
            yellowSnapshot = argb.clone();
            yellowDistance = distance;
            telemetry.addData("✓", "Snapshot AMARILLO capturado");
            telemetry.update();
            sleep(300); // Debounce
        }
        
        // B: Capturar snapshot MORADO
        if (gamepad1.b) {
            purpleSnapshot = argb.clone();
            purpleDistance = distance;
            telemetry.addData("✓", "Snapshot MORADO capturado");
            telemetry.update();
            sleep(300);
        }
        
        // X: Limpiar snapshots
        if (gamepad1.x) {
            yellowSnapshot = null;
            purpleSnapshot = null;
            telemetry.addData("✓", "Snapshots limpiados");
            telemetry.update();
            sleep(300);
        }
        
        // Y: Mostrar comparación
        if (gamepad1.y && yellowSnapshot != null && purpleSnapshot != null) {
            showComparison();
            sleep(5000); // Mostrar por 5 segundos
        }
    }
    
    /**
     * Analiza el color basándose en valores RGB y distancia
     */
    private String analyzeColor(int red, int green, int blue, double distance) {
        // Si está muy lejos, probablemente no hay pelota
        if (distance > 8.0) {
            return "NADA (muy lejos)";
        }
        
        // Calcular el total y valores normalizados
        int total = red + green + blue;
        if (total == 0) {
            return "ERROR (sin luz)";
        }
        
        double redPercent = (red * 100.0) / total;
        double greenPercent = (green * 100.0) / total;
        double bluePercent = (blue * 100.0) / total;
        
        // ANÁLISIS HEURÍSTICO (ajustarás estos valores después de las pruebas)
        
        // Amarillo típicamente tiene más rojo y verde, menos azul
        if (redPercent > 30 && greenPercent > 30 && bluePercent < 30) {
            return "AMARILLO (probable)";
        }
        
        // Morado típicamente tiene más azul y algo de rojo, menos verde
        if (bluePercent > 35 && redPercent > 20 && greenPercent < 30) {
            return "MORADO (probable)";
        }
        
        // Si no coincide con ningún patrón claro
        return "DESCONOCIDO";
    }
    
    /**
     * Muestra telemetría principal
     */
    private void displayTelemetry(int alpha, int red, int green, int blue, 
                                   double distance, String detectedColor) {
        // Calcular total para porcentajes
        int total = red + green + blue;
        double redPercent = total > 0 ? (red * 100.0) / total : 0;
        double greenPercent = total > 0 ? (green * 100.0) / total : 0;
        double bluePercent = total > 0 ? (blue * 100.0) / total : 0;
        
        // Valores crudos
        telemetry.addData("════ VALORES CRUDOS ════", "");
        telemetry.addData("Alpha (intensidad)", alpha);
        telemetry.addData("Red", red);
        telemetry.addData("Green", green);
        telemetry.addData("Blue", blue);
        telemetry.addData("Distancia (cm)", "%.2f", distance);
        
        // Porcentajes (más útiles para comparar)
        telemetry.addData("", "");
        telemetry.addData("════ PORCENTAJES ════", "");
        telemetry.addData("Red %", "%.1f%%", redPercent);
        telemetry.addData("Green %", "%.1f%%", greenPercent);
        telemetry.addData("Blue %", "%.1f%%", bluePercent);
        
        // Detección automática
        telemetry.addData("", "");
        telemetry.addData("════ DETECCIÓN ════", "");
        telemetry.addData("Color Detectado", detectedColor);
        
        // Indicador visual de qué tan cerca está
        String proximityIndicator = getProximityIndicator(distance);
        telemetry.addData("Proximidad", proximityIndicator);
        
        // Snapshots capturados
        telemetry.addData("", "");
        telemetry.addData("════ SNAPSHOTS ════", "");
        telemetry.addData("Amarillo", yellowSnapshot != null ? "✓ Capturado" : "✗ No capturado");
        telemetry.addData("Morado", purpleSnapshot != null ? "✓ Capturado" : "✗ No capturado");
        
        // Controles
        telemetry.addData("", "");
        telemetry.addData("════ CONTROLES ════", "");
        telemetry.addData("A", "Capturar AMARILLO");
        telemetry.addData("B", "Capturar MORADO");
        telemetry.addData("X", "Limpiar snapshots");
        telemetry.addData("Y", "Comparar A vs B");
        
        // Consejos según distancia
        telemetry.addData("", "");
        telemetry.addData("════ CONSEJO ════", "");
        if (distance > 10) {
            telemetry.addData("", "⚠ Muy lejos - acerca la pelota");
        } else if (distance > 5) {
            telemetry.addData("", "⚠ Un poco lejos - puede funcionar");
        } else if (distance > 1) {
            telemetry.addData("", "✓ Distancia buena");
        } else {
            telemetry.addData("", "⚠ Muy cerca - puede saturar");
        }
    }
    
    /**
     * Muestra comparación entre snapshots de amarillo y morado
     */
    private void showComparison() {
        telemetry.clear();
        telemetry.addData("════ COMPARACIÓN ════", "");
        telemetry.addData("", "");
        
        // Extraer valores
        int yAlpha = yellowSnapshot[0], yRed = yellowSnapshot[1];
        int yGreen = yellowSnapshot[2], yBlue = yellowSnapshot[3];
        int pAlpha = purpleSnapshot[0], pRed = purpleSnapshot[1];
        int pGreen = purpleSnapshot[2], pBlue = purpleSnapshot[3];
        
        int yTotal = yRed + yGreen + yBlue;
        int pTotal = pRed + pGreen + pBlue;
        
        // Calcular porcentajes
        double yRedP = (yRed * 100.0) / yTotal;
        double yGreenP = (yGreen * 100.0) / yTotal;
        double yBlueP = (yBlue * 100.0) / yTotal;
        
        double pRedP = (pRed * 100.0) / pTotal;
        double pGreenP = (pGreen * 100.0) / pTotal;
        double pBlueP = (pBlue * 100.0) / pTotal;
        
        // Mostrar comparación
        telemetry.addData("", "AMARILLO vs MORADO");
        telemetry.addData("─────────────────", "");
        telemetry.addData("Red %", "%.1f%% vs %.1f%%", yRedP, pRedP);
        telemetry.addData("Green %", "%.1f%% vs %.1f%%", yGreenP, pGreenP);
        telemetry.addData("Blue %", "%.1f%% vs %.1f%%", yBlueP, pBlueP);
        telemetry.addData("─────────────────", "");
        telemetry.addData("Distancia", "%.2f cm vs %.2f cm", yellowDistance, purpleDistance);
        
        telemetry.addData("", "");
        telemetry.addData("════ ANÁLISIS ════", "");
        
        // Calcular diferencias
        double redDiff = Math.abs(yRedP - pRedP);
        double greenDiff = Math.abs(yGreenP - pGreenP);
        double blueDiff = Math.abs(yBlueP - pBlueP);
        
        // Determinar qué canal es más distintivo
        String bestChannel = "Red";
        double maxDiff = redDiff;
        if (greenDiff > maxDiff) {
            bestChannel = "Green";
            maxDiff = greenDiff;
        }
        if (blueDiff > maxDiff) {
            bestChannel = "Blue";
            maxDiff = blueDiff;
        }
        
        telemetry.addData("Canal más distintivo", bestChannel);
        telemetry.addData("Diferencia máxima", "%.1f%%", maxDiff);
        
        // Evaluación de utilidad
        telemetry.addData("", "");
        if (maxDiff > 15) {
            telemetry.addData("Evaluación", "✓ EXCELENTE - Fácil distinguir");
            telemetry.addData("", "El sensor es MUY útil");
        } else if (maxDiff > 10) {
            telemetry.addData("Evaluación", "✓ BUENO - Se puede distinguir");
            telemetry.addData("", "El sensor es útil");
        } else if (maxDiff > 5) {
            telemetry.addData("Evaluación", "⚠ ACEPTABLE - Distingue con dificultad");
            telemetry.addData("", "Prueba mejor iluminación/posición");
        } else {
            telemetry.addData("Evaluación", "✗ POBRE - No distingue bien");
            telemetry.addData("", "Sensor puede no ser útil aquí");
        }
        
        telemetry.addData("", "");
        telemetry.addData("Presiona cualquier botón", "para volver");
    }
    
    /**
     * Genera indicador visual de proximidad
     */
    private String getProximityIndicator(double distance) {
        if (distance > 10) {
            return "│          │ (muy lejos)";
        } else if (distance > 8) {
            return "│•         │";
        } else if (distance > 6) {
            return "│ •        │";
        } else if (distance > 4) {
            return "│  •       │ (bueno)";
        } else if (distance > 2) {
            return "│   •      │ (muy bueno)";
        } else if (distance > 1) {
            return "│    •     │ (bueno)";
        } else {
            return "│     •    │ (muy cerca)";
        }
    }
}