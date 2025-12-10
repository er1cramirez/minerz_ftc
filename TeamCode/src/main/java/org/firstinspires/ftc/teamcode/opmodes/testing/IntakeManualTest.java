package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;

/**
 * OpMode de testing para IntakeSubsystem.
 * 
 * CONTROLES:
 * - Right Trigger: Intake (recoger piezas)
 * - Left Trigger: Outtake (expulsar piezas)
 * - Sin triggers: Stop (motor detenido)
 * 
 * TELEMETRÍA:
 * - Estado actual del intake
 * - Corriente del motor (Amps)
 */
@TeleOp(name = "Intake Manual Test", group = "Testing")
public class IntakeManualTest extends CommandOpMode {
    
    private IntakeSubsystem intake;
    
    @Override
    public void initialize() {
        // Crear IntakeSubsystem
        intake = new IntakeSubsystem(hardwareMap);
        
        // Registrar subsystem
        register(intake);
        
        telemetry.addLine("Intake Manual Test");
        telemetry.addLine("Controls:");
        telemetry.addLine("  RT: Intake");
        telemetry.addLine("  LT: Outtake");
        telemetry.addLine("  Release: Stop");
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
        float rightTrigger = gamepad1.right_trigger;
        float leftTrigger = gamepad1.left_trigger;
        
        // Prioridad: Right trigger > Left trigger > Stop
        if (rightTrigger > 0.1) {
            // Intaking
            intake.intake();
        } else if (leftTrigger > 0.1) {
            // Outtaking
            intake.outtake();
        } else {
            // Stop
            intake.stop();
        }
    }
    
    private void updateTelemetry() {
        telemetry.clear();
        
        // Encabezado
        telemetry.addLine("=== INTAKE TEST ===");
        telemetry.addLine();
        
        // Estado
        telemetry.addData("State", intake.getState().name());
        telemetry.addData("Active", intake.isActive() ? "YES" : "NO");
        telemetry.addData("Idle", intake.isIdle() ? "YES" : "NO");
        telemetry.addLine();
        
        // Corriente del motor
        telemetry.addData("Motor Current", "%.2f A", intake.getCurrentValue());
        telemetry.addLine();
        
        // Controles actuales
        telemetry.addData("RT (Intake)", "%.2f", gamepad1.right_trigger);
        telemetry.addData("LT (Outtake)", "%.2f", gamepad1.left_trigger);
        
        telemetry.update();
    }
}
