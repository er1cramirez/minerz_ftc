package org.firstinspires.ftc.teamcode.opmodes.tuning;


import com.pedropathing.ftc.FTCCoordinates;
import com.pedropathing.ftc.PoseConverter;
import com.pedropathing.geometry.PedroCoordinates;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.FieldPose;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import com.pedropathing.geometry.Pose;
import com.seattlesolvers.solverslib.geometry.Pose2d;
import com.seattlesolvers.solverslib.geometry.Rotation2d;
import com.seattlesolvers.solverslib.geometry.Transform2d;
import com.seattlesolvers.solverslib.geometry.Translation2d;

import java.util.List;

/**
 * OpMode para verificar que robotPose funciona correctamente.
 * 
 * Este OpMode muestra:
 * 1. robotPose RAW del SDK (pose de la cámara)
 * 2. robotPose compensado (pose del robot considerando torreta)
 * 3. ftcPose para comparación
 * 4. Pedro Pathing Pose (convertido)
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
public class RobotPoseTuning extends LinearOpMode {

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
        // SECCIÓN 2: Pose de camara en coordenadas de Pedro
        // 

        telemetry.addLine("Pose de camara expresado en el sistema de coordenadas de Pedro");
        
        if (goalDetection.robotPose != null) {
            // Conversion Manual
            // Pedro X = 72 + FTC Y
            // Pedro Y = 72 - FTC X
            // Pedro Heading = FTC Yaw - 90
            
            double ftcX = goalDetection.robotPose.getPosition().x;
            double ftcY = goalDetection.robotPose.getPosition().y;
            double ftcYaw = goalDetection.robotPose.getOrientation().getYaw(AngleUnit.RADIANS);
            // Transformar a Sistema Pedro (manualmente)
            double pedroX = ftcY + 72.0;
            double pedroY = 72.0 - ftcX;
            double pedroYaw = ftcYaw;  // No cambia
            Pose pedroTranformed = new Pose(pedroX, pedroY, pedroYaw);
            telemetry.addLine(String.format("  FTC Raw: X=%.1f  Y=%.1f  Yaw=%.1f°", ftcX, ftcY, ftcYaw));

            telemetry.addLine(String.format("  Manual: "));
            telemetry.addLine("  ---------------------------------");
            telemetry.addLine(String.format("  Ped X: %.1f", pedroTranformed.getX()));
            telemetry.addLine(String.format("  Ped Y: %.1f", pedroTranformed.getY()));
            telemetry.addLine(String.format("  Ped H: %.1f°", pedroTranformed.getHeading()));
        }
        
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
