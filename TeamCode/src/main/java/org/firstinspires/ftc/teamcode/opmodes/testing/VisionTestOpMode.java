// opmodes/testing/VisionTestOpMode.java
package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.util.RobotState;
import org.firstinspires.ftc.teamcode.util.VisionTarget;

/**
 * OpMode de testing para VisionSubsystem.
 * 
 * CONTROLES:
 * - A: Iniciar tracking de target (20 para azul)
 * - B: Detener tracking
 * - X: Cambiar a target rojo (24)
 * - Y: Cambiar a target azul (20)
 * - START: Detectar secuencia (21-23)
 * 
 * TELEMETRÍA:
 * - Estado de Vision
 * - Target detectado (range, bearing)
 * - FPS
 * - Alineación
 */
@TeleOp(name = "Vision Test", group = "Testing")
//@Disabled  // Remover cuando esté listo para usar
public class VisionTestOpMode extends CommandOpMode {
    
    private VisionSubsystem vision;
    
    @Override
    public void initialize() {
        // Crear VisionSubsystem con target azul por defecto
        vision = new VisionSubsystem(hardwareMap, VisionConstants.TARGET_ID_BLUE);
        
        // Configurar RobotState
        RobotState.getInstance().setAlliance(RobotState.Alliance.BLUE);
        RobotState.getInstance().setMode(RobotState.RobotMode.TELEOP);
        
        // Registrar subsystem
        register(vision);
        
        telemetry.addLine("Vision Test OpMode");
        telemetry.addLine("Controls:");
        telemetry.addLine("  A: Start tracking");
        telemetry.addLine("  B: Stop tracking");
        telemetry.addLine("  X: Switch to RED (24)");
        telemetry.addLine("  Y: Switch to BLUE (20)");
        telemetry.addLine("  START: Detect sequence");
        telemetry.update();
    }
    
    @Override
    public void run() {
        super.run();  // Ejecuta el scheduler
        
        // Controles manuales
        handleControls();
        
        // Actualizar telemetría
        updateTelemetry();
    }
    
    private void handleControls() {
        // A: Iniciar tracking
        if (gamepad1.a) {
            vision.startTargetTracking();
        }
        
        // B: Detener tracking
        if (gamepad1.b) {
            vision.stopTracking();
        }
        
        // X: Cambiar a rojo (24)
        if (gamepad1.x) {
            vision.setTargetId(VisionConstants.TARGET_ID_RED);
            RobotState.getInstance().setAlliance(RobotState.Alliance.RED);
        }
        
        // Y: Cambiar a azul (20)
        if (gamepad1.y) {
            vision.setTargetId(VisionConstants.TARGET_ID_BLUE);
            RobotState.getInstance().setAlliance(RobotState.Alliance.BLUE);
        }
        
        // START: Detectar secuencia
        if (gamepad1.start) {
            vision.startSequenceDetection();
        }
    }
    
    private void updateTelemetry() {
        telemetry.clear();
        
        // Encabezado
        telemetry.addLine("=== VISION TEST ===");
        telemetry.addLine();
        
        // Estado
        telemetry.addData("State", vision.getStateName());
        telemetry.addData("Alliance", RobotState.getInstance().isBlue() ? "BLUE (20)" : "RED (24)");
        telemetry.addLine();
        
        // Target actual
        VisionTarget target = vision.getLastValidTarget();
        if (target.isValid()) {
            telemetry.addData("Target ID", target.id);
            telemetry.addData("Range", "%.1f inches", target.range);
            telemetry.addData("Bearing", "%.1f°", target.bearing);
            telemetry.addData("Elevation", "%.1f°", target.elevation);
            telemetry.addData("Age", "%d ms", target.getAge());
            telemetry.addData("Aligned", vision.isAligned() ? "✓ YES" : "✗ NO");
        } else {
            telemetry.addData("Target", "NOT DETECTED");
        }
        telemetry.addLine();
        
        // Secuencia
        Integer sequenceId = vision.getDetectedSequenceId();
        if (sequenceId != null) {
            String pattern = getSequencePattern(sequenceId);
            telemetry.addData("Sequence", "ID %d (%s)", sequenceId, pattern);
        } else {
            telemetry.addData("Sequence", "NOT DETECTED");
        }
        telemetry.addLine();
        
        // Performance
        telemetry.addData("FPS", "%.1f", vision.getCurrentFps());
        
        // Configuración
        telemetry.addLine();
        telemetry.addLine("=== CONFIG ===");
        telemetry.addData("Resolution", "%dx%d", VisionConstants.CAMERA_WIDTH, VisionConstants.CAMERA_HEIGHT);
        telemetry.addData("Decimation", "%.1f", VisionConstants.DECIMATION);
        telemetry.addData("Custom Calib", VisionConstants.USE_CUSTOM_CALIBRATION ? "YES" : "NO");
        
        telemetry.update();
    }
    
    private String getSequencePattern(int id) {
        switch (id) {
            case 21: return "G P P";
            case 22: return "P G P";
            case 23: return "P P G";
            default: return "Unknown";
        }
    }
}