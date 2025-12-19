package org.firstinspires.ftc.teamcode.opmodes.test;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.RunCommand;

import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.RBE;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.List;

/**
 * OpMode de monitoreo continuo de visión.
 * 
 * Muestra en tiempo real todas las detecciones de AprilTags
 * sin necesidad de presionar botones. Ideal para:
 * - Verificar que la cámara está funcionando
 * - Calibrar posición de la cámara
 * - Debug rápido en cancha
 * 
 * NO HAY CONTROLES - Solo muestra información.
 */
@TeleOp(name = "Test - Vision Monitor", group = "Test")
public class VisionMonitorOpMode extends CommandOpMode {

    private VisionSubsystem vision;

    // Contadores para estadísticas
    private int totalFrames = 0;
    private int framesWithDetection = 0;

    @Override
    public void initialize() {
        vision = new VisionSubsystem(hardwareMap);
        vision.enable();

        // Actualizar telemetría continuamente
        schedule(new RunCommand(() -> {
            totalFrames++;
            if (vision.hasValidDetection()) {
                framesWithDetection++;
            }
            updateTelemetry();
        }));

        telemetry.addLine("=== VISION MONITOR ===");
        telemetry.addLine("Mostrando detecciones en tiempo real");
        telemetry.update();
    }

    private void updateTelemetry() {
        telemetry.addLine("══════════════════════════════════");
        telemetry.addLine("      VISION MONITOR");
        telemetry.addLine("══════════════════════════════════");

        // Estado general
        telemetry.addLine();
        telemetry.addData("Estado", vision.getState());
        telemetry.addData("Detection Rate", 
            String.format("%.0f%%", (framesWithDetection * 100.0 / totalFrames)));

        // Detecciones actuales
        List<AprilTagDetection> detections = vision.getAllDetections();
        
        telemetry.addLine();
        telemetry.addLine("── DETECCIONES (" + detections.size() + ") ──");

        if (detections.isEmpty()) {
            telemetry.addLine("  (ninguna)");
        } else {
            for (AprilTagDetection det : detections) {
                String tagName = getTagName(det.id);
                
                telemetry.addLine();
                telemetry.addData("  Tag", det.id + " (" + tagName + ")");
                
                if (det.ftcPose != null) {
                    telemetry.addData("    Range", String.format("%.1f\"", det.ftcPose.range));
                    telemetry.addData("    Bearing", String.format("%.1f°", det.ftcPose.bearing));
                    telemetry.addData("    Elevation", String.format("%.1f°", det.ftcPose.elevation));
                    telemetry.addData("    DecisionMargin", String.format("%.1f", det.decisionMargin));
                } else {
                    telemetry.addLine("    (sin pose)");
                }
            }
        }

        // Datos procesados
        telemetry.addLine();
        telemetry.addLine("── DATOS PROCESADOS ──");
        
        // Secuencia
        String seq = vision.getSequence();
        telemetry.addData("Secuencia", seq != null ? seq : "-");
        
        // Goals
        RBE redRBE = vision.getGoalRBE(Alliance.RED);
        RBE blueRBE = vision.getGoalRBE(Alliance.BLUE);
        
        telemetry.addData("Goal Rojo", redRBE != null 
            ? String.format("%.1f\" @ %.1f°", redRBE.range, redRBE.bearing) 
            : "-");
        telemetry.addData("Goal Azul", blueRBE != null 
            ? String.format("%.1f\" @ %.1f°", blueRBE.range, blueRBE.bearing) 
            : "-");

        // Leyenda
        telemetry.addLine();
        telemetry.addLine("── LEYENDA DE TAGS ──");
        telemetry.addLine("20=Goal Rojo  24=Goal Azul");
        telemetry.addLine("21=YYP  22=YPY  23=PYY");

        telemetry.update();
    }

    /**
     * Obtiene nombre descriptivo del tag.
     */
    private String getTagName(int id) {
        switch (id) {
            case VisionConstants.TAG_GOAL_RED:    return "Goal Rojo";
            case VisionConstants.TAG_GOAL_BLUE:   return "Goal Azul";
            case VisionConstants.TAG_SEQUENCE_YYP: return "Seq YYP";
            case VisionConstants.TAG_SEQUENCE_YPY: return "Seq YPY";
            case VisionConstants.TAG_SEQUENCE_PYY: return "Seq PYY";
            default: return "Desconocido";
        }
    }
}
