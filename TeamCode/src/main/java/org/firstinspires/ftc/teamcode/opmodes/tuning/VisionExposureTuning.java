package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.List;

/**
 * OpMode para optimizar la exposición y ganancia de la cámara.
 * Basado en ConceptAprilTagOptimizeExposure del SDK.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * OBJETIVO
 * ═══════════════════════════════════════════════════════════════════════════
 * Encontrar la exposición MÁS BAJA que aún proporcione detección confiable.
 * Menor exposición = menos motion blur = mejor detección en movimiento.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * PROCEDIMIENTO RECOMENDADO
 * ═══════════════════════════════════════════════════════════════════════════
 * 1. Coloca un AprilTag a la distancia de operación típica (2-4 feet)
 * 2. Inicia el OpMode (comienza con exposición mínima, ganancia máxima)
 * 3. Si no detecta el tag, aumenta la exposición gradualmente (Left Bumper)
 * 4. Cuando detecte consistentemente, mueve el robot/cámara para verificar
 *    que no hay motion blur
 * 5. Si hay motion blur, reduce exposición y aumenta ganancia
 * 6. Anota los valores finales y actualiza VisionConstants
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * VER PREVIEW DE CÁMARA
 * ═══════════════════════════════════════════════════════════════════════════
 * Para ver la imagen de la cámara mientras ajustas:
 * - Control Hub: Conecta monitor HDMI o usa scrcpy (https://scrcpy.org/)
 * - Phone RC: Ver directamente en la pantalla
 * - Driver Station: Menú de 3 puntos → Camera Stream
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * CONTROLES
 * ═══════════════════════════════════════════════════════════════════════════
 *   Left Bumper   → Aumentar exposición (+1 ms)
 *   Left Trigger  → Disminuir exposición (-1 ms)
 *   
 *   Right Bumper  → Aumentar ganancia (+1)
 *   Right Trigger → Disminuir ganancia (-1)
 *   
 *   DPAD Up/Down  → Ajuste fino exposición (+/- 1 ms)
 *   DPAD Right    → Salto grande exposición (+5 ms)
 *   DPAD Left     → Salto grande exposición (-5 ms)
 * ═══════════════════════════════════════════════════════════════════════════
 */
@TeleOp(name = "Tuning - Vision Exposure", group = "Tuning")
public class VisionExposureTuning extends LinearOpMode {

    private VisionSubsystem vision;

    // Valores actuales
    private int myExposure;
    private int myGain;

    // Límites de la cámara
    private int minExposure = 1;
    private int maxExposure = 100;
    private int minGain = 0;
    private int maxGain = 255;

    // Estado de botones (para detectar pulsaciones)
    private boolean lastExpUp = false;
    private boolean lastExpDn = false;
    private boolean lastGainUp = false;
    private boolean lastGainDn = false;
    private boolean lastDpadUp = false;
    private boolean lastDpadDn = false;
    private boolean lastDpadLeft = false;
    private boolean lastDpadRight = false;

    @Override
    public void runOpMode() {
        // Inicializar vision subsystem
        vision = new VisionSubsystem(hardwareMap);

        // Esperar a que la cámara esté lista
        telemetry.addData("Estado", "Esperando cámara...");
        telemetry.update();

        // Activar visión y esperar streaming
        vision.enable();
        while (!isStopRequested() && vision.getVisionPortal().getCameraState() 
                != org.firstinspires.ftc.vision.VisionPortal.CameraState.STREAMING) {
            sleep(20);
        }

        // Obtener límites de la cámara
        getCameraLimits();

        // Iniciar con exposición baja y ganancia alta (recomendado)
        myExposure = Math.min(5, minExposure + 4);
        myGain = maxGain;
        vision.setManualExposure(myExposure, myGain);

        // Instrucciones
        telemetry.addData("Estado", "Cámara lista");
        telemetry.addLine();
        telemetry.addLine("=== VISION EXPOSURE TUNING ===");
        telemetry.addLine("Objetivo: Encontrar la exposición más BAJA");
        telemetry.addLine("que detecte tags de forma confiable.");
        telemetry.addLine();
        telemetry.addData(">", "Presiona START para comenzar");
        telemetry.update();

        waitForStart();

        // Loop principal
        while (opModeIsActive()) {
            // Procesar inputs
            processInputs();

            // Mostrar telemetría
            displayTelemetry();

            sleep(20);
        }

        // Limpiar
        vision.close();
    }

    /**
     * Obtiene los límites de exposición y ganancia de la cámara.
     */
    private void getCameraLimits() {
        int[] expLimits = vision.getExposureLimits();
        if (expLimits != null) {
            minExposure = Math.max(1, expLimits[0]);
            maxExposure = expLimits[1];
        }

        int[] gainLimits = vision.getGainLimits();
        if (gainLimits != null) {
            minGain = gainLimits[0];
            maxGain = gainLimits[1];
        }
    }

    /**
     * Procesa inputs del gamepad.
     */
    private void processInputs() {
        // === EXPOSICIÓN ===

        // Left bumper = aumentar exposición
        boolean expUp = gamepad1.left_bumper;
        if (expUp && !lastExpUp) {
            myExposure = Range.clip(myExposure + 1, minExposure, maxExposure);
            vision.setManualExposure(myExposure, myGain);
        }
        lastExpUp = expUp;

        // Left trigger = disminuir exposición
        boolean expDn = gamepad1.left_trigger > 0.25;
        if (expDn && !lastExpDn) {
            myExposure = Range.clip(myExposure - 1, minExposure, maxExposure);
            vision.setManualExposure(myExposure, myGain);
        }
        lastExpDn = expDn;

        // DPAD up = +1 exposición
        boolean dpadUp = gamepad1.dpad_up;
        if (dpadUp && !lastDpadUp) {
            myExposure = Range.clip(myExposure + 1, minExposure, maxExposure);
            vision.setManualExposure(myExposure, myGain);
        }
        lastDpadUp = dpadUp;

        // DPAD down = -1 exposición
        boolean dpadDn = gamepad1.dpad_down;
        if (dpadDn && !lastDpadDn) {
            myExposure = Range.clip(myExposure - 1, minExposure, maxExposure);
            vision.setManualExposure(myExposure, myGain);
        }
        lastDpadDn = dpadDn;

        // DPAD right = +5 exposición
        boolean dpadRight = gamepad1.dpad_right;
        if (dpadRight && !lastDpadRight) {
            myExposure = Range.clip(myExposure + 5, minExposure, maxExposure);
            vision.setManualExposure(myExposure, myGain);
        }
        lastDpadRight = dpadRight;

        // DPAD left = -5 exposición
        boolean dpadLeft = gamepad1.dpad_left;
        if (dpadLeft && !lastDpadLeft) {
            myExposure = Range.clip(myExposure - 5, minExposure, maxExposure);
            vision.setManualExposure(myExposure, myGain);
        }
        lastDpadLeft = dpadLeft;

        // === GANANCIA ===

        // Right bumper = aumentar ganancia
        boolean gainUp = gamepad1.right_bumper;
        if (gainUp && !lastGainUp) {
            myGain = Range.clip(myGain + 1, minGain, maxGain);
            vision.setManualExposure(myExposure, myGain);
        }
        lastGainUp = gainUp;

        // Right trigger = disminuir ganancia
        boolean gainDn = gamepad1.right_trigger > 0.25;
        if (gainDn && !lastGainDn) {
            myGain = Range.clip(myGain - 1, minGain, maxGain);
            vision.setManualExposure(myExposure, myGain);
        }
        lastGainDn = gainDn;
    }

    /**
     * Muestra la telemetría.
     */
    private void displayTelemetry() {
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine("   VISION EXPOSURE TUNING");
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine();

        // === CONFIGURACIÓN ACTUAL ===
        telemetry.addLine("── ⚙️ CONFIGURACIÓN ──");
        telemetry.addData("Exposición", "%d ms  (rango: %d - %d)",
                myExposure, minExposure, maxExposure);
        telemetry.addData("Ganancia", "%d  (rango: %d - %d)",
                myGain, minGain, maxGain);
        telemetry.addLine();

        // === DETECCIONES ===
        telemetry.addLine("── 📷 DETECCIONES ──");

        List<AprilTagDetection> detections = vision.getAllDetections();
        int numTags = detections.size();

        if (numTags > 0) {
            telemetry.addData("Tags", "####### %d DETECTADO(S) #######", numTags);

            for (AprilTagDetection det : detections) {
                String name = getTagName(det.id);

                if (det.ftcPose != null) {
                    // Mostrar info detallada
                    telemetry.addLine(String.format("  ID %d (%s):", det.id, name));
                    telemetry.addLine(String.format("    Range: %.1f\"", det.ftcPose.range));
                    telemetry.addLine(String.format("    Bearing: %.1f°", det.ftcPose.bearing));
                    telemetry.addLine(String.format("    DecisionMargin: %.1f", det.decisionMargin));

                    // Indicar si pasa validación
                    boolean valid = det.decisionMargin >= 1.0 / VisionConstants.MAX_AMBIGUITY
                            && det.ftcPose.range >= VisionConstants.MIN_DETECTION_RANGE_INCHES
                            && det.ftcPose.range <= VisionConstants.MAX_DETECTION_RANGE_INCHES;
                    telemetry.addLine(String.format("    Válido: %s", valid ? "✅" : "❌"));
                } else {
                    telemetry.addLine(String.format("  ID %d (%s): sin pose", det.id, name));
                }
            }
        } else {
            telemetry.addData("Tags", "----------- NINGUNO -----------");
            telemetry.addLine("  → Aumenta exposición o acerca un tag");
        }

        telemetry.addLine();

        // === CONTROLES ===
        telemetry.addLine("── 🎮 CONTROLES ──");
        telemetry.addLine("LB/LT = Exposición +/-");
        telemetry.addLine("RB/RT = Ganancia +/-");
        telemetry.addLine("DPAD ↑↓ = Exp +/-1   DPAD ←→ = Exp +/-5");
        telemetry.addLine();

        // === NOTA IMPORTANTE ===
        telemetry.addLine("── 💡 CUANDO TERMINES ──");
        telemetry.addLine("Actualiza VisionConstants.java con:");
        telemetry.addLine(String.format("  EXPOSURE_MS = %d", myExposure));
        telemetry.addLine(String.format("  GAIN = %d", myGain));

        telemetry.update();
    }

    /**
     * Obtiene el nombre legible de un tag ID.
     */
    private String getTagName(int id) {
        if (id == VisionConstants.TAG_GOAL_RED) return "ROJO";
        if (id == VisionConstants.TAG_GOAL_BLUE) return "AZUL";
        if (id == VisionConstants.TAG_SEQUENCE_GPP) return "SEQ-GPP";
        if (id == VisionConstants.TAG_SEQUENCE_PGP) return "SEQ-PGP";
        if (id == VisionConstants.TAG_SEQUENCE_PPG) return "SEQ-PPG";
        return "UNKNOWN";
    }
}
