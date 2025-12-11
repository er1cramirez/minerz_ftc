package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;

import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.commands.EjectCycleCommand;
import org.firstinspires.ftc.teamcode.constants.EjectorConstants;

/**
 * OpMode de testing para calibrar y probar el subsistema Ejector.
 * Permite ajustar manualmente las posiciones STOW y EJECT del servo.
 * 
 * CONTROLES - TESTING BÁSICO:
 * - A: Ejecutar comando completo (EJECT → STOW automático)
 * - B: Solo EJECT (mantener en posición de eyección)
 * - X: Solo STOW (regresar a posición guardada)
 * 
 * CONTROLES - CALIBRACIÓN MANUAL:
 * - DPAD UP: Incrementar posición (+0.01)
 * - DPAD DOWN: Decrementar posición (-0.01)
 * - DPAD RIGHT: Incrementar posición (+0.001) [fino]
 * - DPAD LEFT: Decrementar posición (-0.001) [fino]
 * - RIGHT BUMPER: Guardar posición actual como EJECT
 * - LEFT BUMPER: Guardar posición actual como STOW
 * 
 * TELEMETRÍA:
 * - Estado actual del ejector
 * - Posición actual del servo
 * - Posiciones guardadas (STOW y EJECT)
 * - Instrucciones de calibración
 */
@TeleOp(name = "Ejector Test", group = "Testing")
public class EjectorTest extends CommandOpMode {
    
    private EjectorSubsystem ejector;
    
    // Control de botones (debouncing)
    private boolean lastA = false;
    private boolean lastB = false;
    private boolean lastX = false;
    private boolean lastDpadUp = false;
    private boolean lastDpadDown = false;
    private boolean lastDpadRight = false;
    private boolean lastDpadLeft = false;
    private boolean lastRightBumper = false;
    private boolean lastLeftBumper = false;
    
    // Variables de calibración manual
    private double manualPosition = 0.0;
    private boolean manualMode = false;
    private double savedStowPosition = EjectorConstants.Positions.STOW_POSITION;
    private double savedEjectPosition = EjectorConstants.Positions.EJECT_POSITION;
    
    @Override
    public void initialize() {
        // Crear subsistema
        ejector = new EjectorSubsystem(hardwareMap);
        
        // Registrar subsistema
        register(ejector);
        
        // Inicializar posición manual
        manualPosition = savedStowPosition;
        
        telemetry.addLine("Ejector Test & Calibration");
        telemetry.addLine();
        telemetry.addLine("TESTING:");
        telemetry.addLine("  A: Full command (EJECT→STOW)");
        telemetry.addLine("  B: EJECT only");
        telemetry.addLine("  X: STOW only");
        telemetry.addLine();
        telemetry.addLine("CALIBRATION:");
        telemetry.addLine("  DPAD UP/DOWN: ±0.01");
        telemetry.addLine("  DPAD RIGHT/LEFT: ±0.001");
        telemetry.addLine("  RB: Save as EJECT");
        telemetry.addLine("  LB: Save as STOW");
        telemetry.update();
    }
    
    @Override
    public void run() {
        super.run();  // Ejecuta el scheduler
        
        // Controles manuales
        handleTestingControls();
        handleCalibrationControls();
        
        // Actualizar telemetría
        updateTelemetry();
    }
    
    private void handleTestingControls() {
        // A: Comando completo - EJECT y luego STOW automáticamente usando el comando
        if (gamepad1.a && !lastA) {
            manualMode = false;
            new EjectCycleCommand(ejector).schedule();
        }
        lastA = gamepad1.a;        // B: Solo EJECT
        if (gamepad1.b && !lastB) {
            manualMode = false;
            ejector.eject();
        }
        lastB = gamepad1.b;
        
        // X: Solo STOW
        if (gamepad1.x && !lastX) {
            manualMode = false;
            ejector.stow();
        }
        lastX = gamepad1.x;
    }
    
    private void handleCalibrationControls() {
        // DPAD UP: Incrementar posición (+0.01)
        if (gamepad1.dpad_up && !lastDpadUp) {
            manualMode = true;
            manualPosition = Math.min(1.0, manualPosition + 0.01);
            setManualPosition(manualPosition);
        }
        lastDpadUp = gamepad1.dpad_up;
        
        // DPAD DOWN: Decrementar posición (-0.01)
        if (gamepad1.dpad_down && !lastDpadDown) {
            manualMode = true;
            manualPosition = Math.max(0.0, manualPosition - 0.01);
            setManualPosition(manualPosition);
        }
        lastDpadDown = gamepad1.dpad_down;
        
        // DPAD RIGHT: Incrementar posición (+0.001)
        if (gamepad1.dpad_right && !lastDpadRight) {
            manualMode = true;
            manualPosition = Math.min(1.0, manualPosition + 0.001);
            setManualPosition(manualPosition);
        }
        lastDpadRight = gamepad1.dpad_right;
        
        // DPAD LEFT: Decrementar posición (-0.001)
        if (gamepad1.dpad_left && !lastDpadLeft) {
            manualMode = true;
            manualPosition = Math.max(0.0, manualPosition - 0.001);
            setManualPosition(manualPosition);
        }
        lastDpadLeft = gamepad1.dpad_left;
        
        // RIGHT BUMPER: Guardar como EJECT
        if (gamepad1.right_bumper && !lastRightBumper) {
            savedEjectPosition = manualPosition;
        }
        lastRightBumper = gamepad1.right_bumper;
        
        // LEFT BUMPER: Guardar como STOW
        if (gamepad1.left_bumper && !lastLeftBumper) {
            savedStowPosition = manualPosition;
        }
        lastLeftBumper = gamepad1.left_bumper;
    }
    
    private void setManualPosition(double position) {
        // Acceso directo al servo para testing manual
        ejector.setPosition(position);
    }
    
    private void updateTelemetry() {
        telemetry.clear();
        
        // Encabezado
        telemetry.addLine("=== EJECTOR TEST ===");
        telemetry.addLine();
        
        // === ESTADO ===
        telemetry.addLine("--- STATUS ---");
        telemetry.addData("State", ejector.getState().name());
        telemetry.addData("Is Stowed", ejector.isStowed() ? "✓" : "✗");
        telemetry.addData("Mode", manualMode ? "MANUAL CALIBRATION" : "AUTO TESTING");
        telemetry.addLine();
        
        // === POSICIONES ===
        telemetry.addLine("--- POSITIONS ---");
        if (manualMode) {
            telemetry.addData("Current Position", "%.3f", manualPosition);
        }
        telemetry.addData("STOW Position", "%.3f %s", 
                         savedStowPosition,
                         savedStowPosition == EjectorConstants.Positions.STOW_POSITION ? "(default)" : "(custom)");
        telemetry.addData("EJECT Position", "%.3f %s", 
                         savedEjectPosition,
                         savedEjectPosition == EjectorConstants.Positions.EJECT_POSITION ? "(default)" : "(custom)");
        telemetry.addLine();
        
        // === INSTRUCCIONES ===
        telemetry.addLine("--- CONTROLS ---");
        if (manualMode) {
            telemetry.addLine("CALIBRATING:");
            telemetry.addLine("  DPAD ↑↓: ±0.01");
            telemetry.addLine("  DPAD ←→: ±0.001");
            telemetry.addLine("  RB: Save as EJECT");
            telemetry.addLine("  LB: Save as STOW");
            telemetry.addLine("  Press A/B/X to exit calibration");
        } else {
            telemetry.addLine("TESTING:");
            telemetry.addLine("  A: Full command");
            telemetry.addLine("  B: EJECT only");
            telemetry.addLine("  X: STOW only");
            telemetry.addLine("  DPAD: Enter calibration mode");
        }
        telemetry.addLine();
        
        // === NOTAS DE CALIBRACIÓN ===
        if (savedStowPosition != EjectorConstants.Positions.STOW_POSITION ||
            savedEjectPosition != EjectorConstants.Positions.EJECT_POSITION) {
            telemetry.addLine("--- CALIBRATION NOTES ---");
            telemetry.addLine("⚠️ Custom positions detected!");
            telemetry.addLine("Update EjectorConstants.java with:");
            telemetry.addLine(String.format("  STOW_POSITION = %.3f;", savedStowPosition));
            telemetry.addLine(String.format("  EJECT_POSITION = %.3f;", savedEjectPosition));
        }
        
        telemetry.update();
    }
}
