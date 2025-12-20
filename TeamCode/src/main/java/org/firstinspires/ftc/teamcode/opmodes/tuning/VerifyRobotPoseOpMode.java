package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.FieldPose;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.List;

/**
 * OpMode para verificar que robotPose funciona correctamente.
 * 
 * Este OpMode muestra:
 * 1. robotPose RAW del SDK (pose de la cámara)
 * 2. robotPose compensado (pose del robot considerando torreta)
 * 3. ftcPose para comparación
 * 
 * CÓMO USAR:
 * 1. Coloca el robot en una posición conocida en la cancha
 * 2. Apunta la cámara a un AprilTag de goal (no obelisco)
 * 3. Verifica que las coordenadas mostradas correspondan a tu posición
 * 4. Usa el joystick izquierdo para simular diferentes ángulos de torreta
 * 
 * NOTA: Este OpMode simula el ángulo de la torreta con el joystick.
 * En uso real, el ángulo vendría del TurretSubsystem.
 */
@TeleOp(name = "Tuning - Verify robotPose", group = "Tuning")
public class VerifyRobotPoseOpMode extends LinearOpMode {

    private VisionSubsystem vision;
    
    // Ángulo de torreta simulado (controlado con joystick)
    private double simulatedTurretAngle = 0;

    @Override
    public void runOpMode() {
        vision = new VisionSubsystem(hardwareMap);

        telemetry.addLine("Iniciando cámara...");
        telemetry.update();

        vision.enable();

        // Esperar a que la cámara esté lista
        while (!isStopRequested() && 
               vision.getVisionPortal().getCameraState() != 
               org.firstinspires.ftc.vision.VisionPortal.CameraState.STREAMING) {
            sleep(20);
        }

        telemetry.addLine("=== VERIFY ROBOT POSE ===");
        telemetry.addLine("Apunta la cámara a un AprilTag de GOAL");
        telemetry.addLine("(No funciona con tags de Obelisco)");
        telemetry.addLine();
        telemetry.addLine("Joystick Izq X = simular rotación de torreta");
        telemetry.addLine();
        telemetry.addData(">", "Presiona START");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // Simular ángulo de torreta con joystick
            // Rango: -90° a +90°
            simulatedTurretAngle = -gamepad1.left_stick_x * 90;

            displayTelemetry();
            sleep(50);
        }

        vision.close();
    }

    private void displayTelemetry() {
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine("      VERIFY ROBOT POSE");
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine();

        // Mostrar ángulo de torreta simulado
        telemetry.addData("Torreta (simulada)", "%.1f°", simulatedTurretAngle);
        telemetry.addLine();

        // Obtener detecciones
        List<AprilTagDetection> detections = vision.getAllDetections();

        if (detections.isEmpty()) {
            telemetry.addLine("⚠ NO HAY DETECCIONES");
            telemetry.addLine("Apunta a un AprilTag de goal");
            telemetry.update();
            return;
        }

        // Buscar detección de goal (no obelisco)
        AprilTagDetection goalDetection = null;
        for (AprilTagDetection det : detections) {
            if (det.metadata != null && !det.metadata.name.contains("Obelisk")) {
                goalDetection = det;
                break;
            }
        }

        if (goalDetection == null) {
            telemetry.addLine("⚠ SOLO VEO OBELISCOS");
            telemetry.addLine("robotPose solo funciona con tags de GOAL");
            
            // Mostrar qué vemos
            for (AprilTagDetection det : detections) {
                telemetry.addLine(String.format("  Tag %d: %s", 
                    det.id, det.metadata != null ? det.metadata.name : "unknown"));
            }
            telemetry.update();
            return;
        }

        // ═══════════════════════════════════════
        // SECCIÓN 1: robotPose RAW (pose de la cámara)
        // ═══════════════════════════════════════
        telemetry.addLine("── 📷 CAMERA POSE (robotPose raw) ──");
        
        if (goalDetection.robotPose != null) {
            double camX = goalDetection.robotPose.getPosition().x;
            double camY = goalDetection.robotPose.getPosition().y;
            double camZ = goalDetection.robotPose.getPosition().z;
            double camYaw = goalDetection.robotPose.getOrientation().getYaw(AngleUnit.DEGREES);
            
            telemetry.addLine(String.format("  X: %.1f\"  Y: %.1f\"  Z: %.1f\"", camX, camY, camZ));
            telemetry.addLine(String.format("  Yaw (hacia donde mira): %.1f°", camYaw));
        } else {
            telemetry.addLine("  ⚠ robotPose es NULL");
            telemetry.addLine("  Verifica que setCameraPose esté configurado");
        }
        telemetry.addLine();

        // ═══════════════════════════════════════
        // SECCIÓN 2: Robot Pose (compensado por torreta)
        // ═══════════════════════════════════════
        telemetry.addLine("── 🤖 ROBOT POSE (compensado) ──");
        
        FieldPose robotPose = vision.getRobotPose(simulatedTurretAngle);
        
        if (robotPose != null) {
            telemetry.addLine(String.format("  X: %.1f\"  Y: %.1f\"", robotPose.x, robotPose.y));
            telemetry.addLine(String.format("  Heading: %.1f°", robotPose.heading));
            telemetry.addLine(String.format("  (desde tag %d)", robotPose.sourceTagId));
        } else {
            telemetry.addLine("  ⚠ No se pudo calcular");
        }
        telemetry.addLine();

        // ═══════════════════════════════════════
        // SECCIÓN 3: ftcPose (para comparación)
        // ═══════════════════════════════════════
        telemetry.addLine("── 📐 ftcPose (referencia) ──");
        
        if (goalDetection.ftcPose != null) {
            telemetry.addLine(String.format("  Range: %.1f\"", goalDetection.ftcPose.range));
            telemetry.addLine(String.format("  Bearing: %.1f°", goalDetection.ftcPose.bearing));
            telemetry.addLine(String.format("  Elevation: %.1f°", goalDetection.ftcPose.elevation));
        }
        telemetry.addLine();

        // ═══════════════════════════════════════
        // SECCIÓN 4: Info del tag
        // ═══════════════════════════════════════
        telemetry.addLine("── 🏷 TAG INFO ──");
        telemetry.addLine(String.format("  ID: %d - %s", 
            goalDetection.id, 
            goalDetection.metadata != null ? goalDetection.metadata.name : "unknown"));
        telemetry.addLine(String.format("  DecisionMargin: %.1f", goalDetection.decisionMargin));

        // Posición del tag en cancha (de la librería)
        if (goalDetection.metadata != null && goalDetection.metadata.fieldPosition != null) {
            telemetry.addLine(String.format("  Tag en cancha: X=%.1f Y=%.1f",
                goalDetection.metadata.fieldPosition.get(0),
                goalDetection.metadata.fieldPosition.get(1)));
        }

        telemetry.addLine();
        telemetry.addLine("Joystick Izq X = simular torreta");

        telemetry.update();
    }
}
