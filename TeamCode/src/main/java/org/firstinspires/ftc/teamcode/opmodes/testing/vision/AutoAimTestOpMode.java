package org.firstinspires.ftc.teamcode.opmodes.test;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.RunCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.vision.AimAtGoalCommand;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.RBE;

/**
 * OpMode de prueba para Auto-Aiming con torreta real.
 * 
 * REQUISITOS:
 * - VisionSubsystem configurado
 * - TurretSubsystem configurado
 * 
 * CONTROLES:
 * ──────────────────────────────────────────
 * Gamepad 1:
 *   A             → Activar auto-aim a goal ROJO
 *   B             → Activar auto-aim a goal AZUL
 *   X             → Detener auto-aim
 *   
 *   LEFT_STICK_X  → Control manual de torreta
 *   
 *   DPAD_UP       → Activar visión
 *   DPAD_DOWN     → Desactivar visión
 *   
 *   Y             → Toggle: mostrar detalles extra
 * ──────────────────────────────────────────
 */
@TeleOp(name = "Test - Auto Aim", group = "Test")
public class AutoAimTestOpMode extends CommandOpMode {

    // ===== SUBSYSTEMS =====
    private VisionSubsystem vision;
    private TurretSubsystem turret;

    // ===== GAMEPAD =====
    private GamepadEx driver;

    // ===== COMANDOS =====
    private AimAtGoalCommand currentAimCommand;

    // ===== ESTADO =====
    private Alliance targetAlliance = null;
    private boolean showDetails = false;

    @Override
    public void initialize() {
        // Inicializar subsystems
        vision = new VisionSubsystem(hardwareMap);
        turret = new TurretSubsystem(hardwareMap);  // Asegúrate de tener este subsystem

        // Inicializar gamepad
        driver = new GamepadEx(gamepad1);

        // ===== BINDINGS =====

        // A → Auto-aim a goal ROJO
        driver.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new InstantCommand(() -> startAutoAim(Alliance.RED)));

        // B → Auto-aim a goal AZUL
        driver.getGamepadButton(GamepadKeys.Button.B)
            .whenPressed(new InstantCommand(() -> startAutoAim(Alliance.BLUE)));

        // X → Detener auto-aim
        driver.getGamepadButton(GamepadKeys.Button.X)
            .whenPressed(new InstantCommand(() -> stopAutoAim()));

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

        // Y → Toggle detalles
        driver.getGamepadButton(GamepadKeys.Button.Y)
            .whenPressed(new InstantCommand(() -> {
                showDetails = !showDetails;
            }));

        // ===== DEFAULT COMMAND PARA TORRETA =====
        // Control manual cuando no hay auto-aim activo
        turret.setDefaultCommand(
            new RunCommand(() -> {
                double manualInput = driver.getLeftX();
                if (Math.abs(manualInput) > 0.1) {
                    turret.adjustAngle(manualInput * 2.0);  // Escalar según necesidad
                } else {
                    turret.hold();
                }
            }, turret)
        );

        // ===== TELEMETRÍA =====
        schedule(new RunCommand(() -> updateTelemetry()));

        // Activar visión
        vision.enable();

        telemetry.addLine("=== AUTO AIM TEST ===");
        telemetry.addLine("Presiona START para comenzar");
        telemetry.update();
    }

    // ===== MÉTODOS DE CONTROL =====

    /**
     * Inicia auto-aim hacia el goal de la alianza especificada.
     */
    private void startAutoAim(Alliance alliance) {
        // Cancelar comando anterior si existe
        if (currentAimCommand != null) {
            currentAimCommand.cancel();
        }

        targetAlliance = alliance;
        currentAimCommand = new AimAtGoalCommand(vision, turret, alliance);
        currentAimCommand.schedule();

        String color = (alliance == Alliance.RED) ? "red" : "blue";
        telemetry.speak("Aiming at " + color + " goal");
    }

    /**
     * Detiene el auto-aim.
     */
    private void stopAutoAim() {
        if (currentAimCommand != null) {
            currentAimCommand.cancel();
            currentAimCommand = null;
        }
        targetAlliance = null;
        turret.hold();
        telemetry.speak("Auto aim stopped");
    }

    /**
     * Actualiza la telemetría.
     */
    private void updateTelemetry() {
        telemetry.addLine("══════════════════════════════════");
        telemetry.addLine("      AUTO AIM TEST");
        telemetry.addLine("══════════════════════════════════");

        // Estado de visión
        telemetry.addLine();
        telemetry.addData("Vision", vision.getState());

        // Estado de auto-aim
        telemetry.addLine();
        telemetry.addLine("── AUTO AIM ──");
        
        if (currentAimCommand != null && targetAlliance != null) {
            String color = (targetAlliance == Alliance.RED) ? "🔴 ROJO" : "🔵 AZUL";
            telemetry.addData("Target", color);
            telemetry.addData("Locked", currentAimCommand.isLocked() ? "✓ SÍ" : "✗ NO");
            
            RBE rbe = currentAimCommand.getLastRBE();
            if (rbe != null) {
                telemetry.addData("Range", String.format("%.1f\"", rbe.range));
                telemetry.addData("Bearing", String.format("%.1f°", rbe.bearing));
                telemetry.addData("Elevation", String.format("%.1f°", rbe.elevation));
            } else {
                telemetry.addData("RBE", "Sin detección");
            }
        } else {
            telemetry.addData("Estado", "INACTIVO (control manual)");
        }

        // Estado de torreta
        telemetry.addLine();
        telemetry.addLine("── TORRETA ──");
        telemetry.addData("Ángulo", String.format("%.1f°", turret.getAngle()));
        telemetry.addData("Estado", turret.getState());

        // Controles
        telemetry.addLine();
        telemetry.addLine("── CONTROLES ──");
        telemetry.addLine("A=Aim Rojo  B=Aim Azul  X=Stop");
        telemetry.addLine("Stick Izq = Manual  Y=Detalles");

        // Detalles extra
        if (showDetails) {
            telemetry.addLine();
            telemetry.addLine("── DETALLES ──");
            
            // RBE a ambos goals
            RBE redRBE = vision.getGoalRBE(Alliance.RED);
            RBE blueRBE = vision.getGoalRBE(Alliance.BLUE);
            
            telemetry.addData("Goal Rojo", 
                redRBE != null ? String.format("R=%.1f\" B=%.1f°", redRBE.range, redRBE.bearing) : "No visible");
            telemetry.addData("Goal Azul", 
                blueRBE != null ? String.format("R=%.1f\" B=%.1f°", blueRBE.range, blueRBE.bearing) : "No visible");
            
            // Secuencia si es visible
            String seq = vision.getSequence();
            telemetry.addData("Secuencia", seq != null ? seq : "No visible");
        }

        telemetry.update();
    }
}
