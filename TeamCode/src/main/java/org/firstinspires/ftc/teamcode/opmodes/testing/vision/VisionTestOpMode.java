package org.firstinspires.ftc.teamcode.opmodes.test;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.RunCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.FieldPose;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.RBE;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.List;

/**
 * OpMode de prueba para VisionSubsystem.
 * 
 * CONTROLES:
 * ──────────────────────────────────────────
 * Gamepad 1:
 *   A          → Detectar secuencia del obelisco
 *   B          → Obtener RBE al goal ROJO
 *   X          → Obtener RBE al goal AZUL
 *   Y          → Calcular pose del robot en cancha
 *   
 *   DPAD_UP    → Activar visión
 *   DPAD_DOWN  → Desactivar visión
 *   
 *   LEFT_BUMPER  → Mostrar todas las detecciones raw
 *   RIGHT_BUMPER → Limpiar pantalla
 * ──────────────────────────────────────────
 * 
 * NOTA: Para prueba de pose, se usan valores dummy de torreta y heading.
 *       Ajustar MOCK_TURRET_ANGLE y MOCK_ROBOT_HEADING según tu setup.
 */
@TeleOp(name = "Test - Vision Subsystem", group = "Test")
public class VisionTestOpMode extends CommandOpMode {

    // ===== SUBSYSTEMS =====
    private VisionSubsystem vision;

    // ===== GAMEPAD =====
    private GamepadEx driver;

    // ===== VALORES MOCK PARA PRUEBAS =====
    // Cambiar estos valores según tu posición de prueba
    private static final double MOCK_TURRET_ANGLE = 0.0;      // Torreta al frente
    private static final double MOCK_ROBOT_HEADING = 0.0;     // Robot mirando "arriba" en cancha

    // ===== RESULTADOS DE PRUEBAS =====
    private String lastSequence = "No probado";
    private String lastRBERed = "No probado";
    private String lastRBEBlue = "No probado";
    private String lastPose = "No probado";
    private String lastRawDetections = "";

    @Override
    public void initialize() {
        // Inicializar subsystem
        vision = new VisionSubsystem(hardwareMap);

        // Inicializar gamepad
        driver = new GamepadEx(gamepad1);

        // ===== BINDINGS =====

        // A → Detectar secuencia
        driver.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new InstantCommand(() -> testSequenceDetection()));

        // B → RBE a goal rojo
        driver.getGamepadButton(GamepadKeys.Button.B)
            .whenPressed(new InstantCommand(() -> testGoalRBE(Alliance.RED)));

        // X → RBE a goal azul
        driver.getGamepadButton(GamepadKeys.Button.X)
            .whenPressed(new InstantCommand(() -> testGoalRBE(Alliance.BLUE)));

        // Y → Pose del robot
        driver.getGamepadButton(GamepadKeys.Button.Y)
            .whenPressed(new InstantCommand(() -> testRobotPose()));

        // DPAD_UP → Activar visión
        driver.getGamepadButton(GamepadKeys.Button.DPAD_UP)
            .whenPressed(new InstantCommand(() -> {
                vision.enable();
                telemetry.speak("Vision enabled");
            }));

        // DPAD_DOWN → Desactivar visión
        driver.getGamepadButton(GamepadKeys.Button.DPAD_DOWN)
            .whenPressed(new InstantCommand(() -> {
                vision.disable();
                telemetry.speak("Vision disabled");
            }));

        // LEFT_BUMPER → Mostrar detecciones raw
        driver.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
            .whenPressed(new InstantCommand(() -> showRawDetections()));

        // RIGHT_BUMPER → Limpiar resultados
        driver.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
            .whenPressed(new InstantCommand(() -> clearResults()));

        // ===== DEFAULT COMMAND: Actualizar telemetría =====
        schedule(new RunCommand(() -> updateTelemetry()));

        // Activar visión al inicio
        vision.enable();

        telemetry.addLine("=== VISION TEST OPMODE ===");
        telemetry.addLine("Presiona START para comenzar");
        telemetry.update();
    }

    // ===== MÉTODOS DE PRUEBA =====

    /**
     * Prueba detección de secuencia del obelisco.
     */
    private void testSequenceDetection() {
        String sequence = vision.getSequence();
        
        if (sequence != null) {
            lastSequence = "✓ Secuencia: " + sequence;
            telemetry.speak("Sequence " + sequence);
        } else {
            lastSequence = "✗ No detectada";
            
            // Intentar dar más info
            if (vision.isDisabled()) {
                lastSequence += " (Vision desactivada)";
            } else if (!vision.hasValidDetection()) {
                lastSequence += " (Sin detecciones válidas)";
            } else {
                lastSequence += " (Tag de secuencia no visible)";
            }
        }
    }

    /**
     * Prueba obtención de RBE hacia un goal.
     */
    private void testGoalRBE(Alliance alliance) {
        RBE rbe = vision.getGoalRBE(alliance);
        String result;
        
        if (rbe != null) {
            result = String.format("✓ R=%.1f\" B=%.1f° E=%.1f° (C=%.0f%%)",
                    rbe.range, rbe.bearing, rbe.elevation, rbe.confidence * 100);
            telemetry.speak("Goal found at " + Math.round(rbe.range) + " inches");
        } else {
            result = "✗ No detectado";
            
            if (vision.isDisabled()) {
                result += " (Vision desactivada)";
            }
        }
        
        if (alliance == Alliance.RED) {
            lastRBERed = result;
        } else {
            lastRBEBlue = result;
        }
    }

    /**
     * Prueba cálculo de pose del robot en cancha.
     */
    private void testRobotPose() {
        FieldPose pose = vision.getRobotPose(MOCK_TURRET_ANGLE, MOCK_ROBOT_HEADING);
        
        if (pose != null) {
            lastPose = String.format("✓ X=%.1f Y=%.1f H=%.1f° (tag %d)",
                    pose.x, pose.y, pose.heading, pose.sourceTagId);
            telemetry.speak("Pose calculated");
        } else {
            lastPose = "✗ No calculada";
            
            if (vision.isDisabled()) {
                lastPose += " (Vision desactivada)";
            } else if (!vision.hasValidDetection()) {
                lastPose += " (Sin detecciones válidas)";
            }
        }
    }

    /**
     * Muestra todas las detecciones raw para debug.
     */
    private void showRawDetections() {
        List<AprilTagDetection> detections = vision.getAllDetections();
        StringBuilder sb = new StringBuilder();
        
        if (detections.isEmpty()) {
            sb.append("Sin detecciones");
        } else {
            sb.append(detections.size()).append(" detecciones:\n");
            
            for (AprilTagDetection det : detections) {
                sb.append(String.format("  ID %d: ", det.id));
                
                if (det.ftcPose != null) {
                    sb.append(String.format("R=%.1f\" B=%.1f° DM=%.1f",
                            det.ftcPose.range, det.ftcPose.bearing, det.decisionMargin));
                } else {
                    sb.append("(sin pose)");
                }
                sb.append("\n");
            }
        }
        
        lastRawDetections = sb.toString();
    }

    /**
     * Limpia todos los resultados.
     */
    private void clearResults() {
        lastSequence = "No probado";
        lastRBERed = "No probado";
        lastRBEBlue = "No probado";
        lastPose = "No probado";
        lastRawDetections = "";
        telemetry.speak("Cleared");
    }

    /**
     * Actualiza la telemetría con estado actual.
     */
    private void updateTelemetry() {
        telemetry.addLine("══════════════════════════════════");
        telemetry.addLine("   VISION SUBSYSTEM TEST");
        telemetry.addLine("══════════════════════════════════");
        
        // Estado del subsystem
        telemetry.addLine();
        telemetry.addData("Estado", vision.getState());
        telemetry.addData("Detección válida", vision.hasValidDetection() ? "✓ SÍ" : "✗ NO");
        
        // Controles
        telemetry.addLine();
        telemetry.addLine("── CONTROLES ──");
        telemetry.addLine("A=Secuencia  B=Goal Rojo  X=Goal Azul");
        telemetry.addLine("Y=Pose  ↑=Enable  ↓=Disable");
        telemetry.addLine("LB=Raw  RB=Clear");
        
        // Resultados de pruebas
        telemetry.addLine();
        telemetry.addLine("── RESULTADOS ──");
        telemetry.addData("Secuencia (A)", lastSequence);
        telemetry.addData("RBE Rojo (B)", lastRBERed);
        telemetry.addData("RBE Azul (X)", lastRBEBlue);
        telemetry.addData("Pose (Y)", lastPose);
        
        // Detecciones raw si hay
        if (!lastRawDetections.isEmpty()) {
            telemetry.addLine();
            telemetry.addLine("── RAW DETECTIONS ──");
            telemetry.addLine(lastRawDetections);
        }
        
        // Info de configuración
        telemetry.addLine();
        telemetry.addLine("── CONFIG ──");
        telemetry.addData("Mock Turret Angle", MOCK_TURRET_ANGLE + "°");
        telemetry.addData("Mock Robot Heading", MOCK_ROBOT_HEADING + "°");
        
        telemetry.update();
    }
}
