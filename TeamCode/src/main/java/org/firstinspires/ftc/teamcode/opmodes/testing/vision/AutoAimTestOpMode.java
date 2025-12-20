package org.firstinspires.ftc.teamcode.opmodes.testing.vision;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.RunCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.vision.nuevo.AimAtGoalCommand;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.vision.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.vision.VisionSubsystem.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.vision.VisionSubsystem.RBE;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.List;

/**
 * OpMode de prueba para Auto-Aiming con diagnóstico completo.
 * 
 * CONTROLES:
 * ──────────────────────────────────────────
 *   A             → Auto-aim a goal ROJO (tag 24)
 *   B             → Auto-aim a goal AZUL (tag 20)
 *   X             → Detener auto-aim
 *   
 *   LEFT_STICK_X  → Control manual de torreta
 *   
 *   DPAD_UP       → Activar visión
 *   DPAD_DOWN     → Desactivar visión
 *   
 *   Y             → Toggle: modo debug detallado
 * ──────────────────────────────────────────
 * 
 * DIAGNÓSTICO:
 * Este OpMode muestra claramente:
 * - Qué tags ve la cámara actualmente
 * - Qué tag está buscando el auto-aim
 * - Si hay mismatch entre lo que ve y lo que busca
 */
@TeleOp(name = "Test - Auto Aim v3", group = "Test")
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
    private boolean debugMode = true;

    @Override
    public void initialize() {
        // Inicializar subsystems
        vision = new VisionSubsystem(hardwareMap);
        turret = new TurretSubsystem(hardwareMap);

        // Inicializar gamepad
        driver = new GamepadEx(gamepad1);

        // ===== BINDINGS =====

        // A → Auto-aim a goal ROJO (tag 24)
        driver.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new InstantCommand(() -> startAutoAim(Alliance.RED)));

        // B → Auto-aim a goal AZUL (tag 20)
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

        // Y → Toggle debug mode
        driver.getGamepadButton(GamepadKeys.Button.Y)
            .whenPressed(new InstantCommand(() -> debugMode = !debugMode));

        // ===== DEFAULT COMMAND PARA TORRETA =====
        turret.setDefaultCommand(
            new RunCommand(() -> {
                double manualInput = driver.getLeftX();
                if (Math.abs(manualInput) > 0.1) {
                    // Control manual - usar adjustAngle para movimiento suave
                    turret.adjustAngle(manualInput * 3.0);
                }
                // Si no hay input, el turret mantiene su posición actual
            }, turret)
        );

        // ===== TELEMETRÍA =====
        schedule(new RunCommand(() -> updateTelemetry()));

        // Activar visión
        vision.enable();

        telemetry.addLine("=== AUTO AIM TEST v3 ===");
        telemetry.update();
    }

    // ===== MÉTODOS DE CONTROL =====

    private void startAutoAim(Alliance alliance) {
        if (currentAimCommand != null) {
            currentAimCommand.cancel();
        }

        targetAlliance = alliance;
        currentAimCommand = new AimAtGoalCommand(vision, turret, alliance);
        currentAimCommand.schedule();

        String color = (alliance == Alliance.RED) ? "rojo" : "azul";
        telemetry.speak("Buscando goal " + color);
    }

    private void stopAutoAim() {
        if (currentAimCommand != null) {
            currentAimCommand.cancel();
            currentAimCommand = null;
        }
        targetAlliance = null;
        telemetry.speak("Auto aim detenido");
    }

    private void updateTelemetry() {
        // ═══════════════════════════════════════
        // SECCIÓN 1: HEADER
        // ═══════════════════════════════════════
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine("      AUTO AIM TEST v3");
        telemetry.addLine("═══════════════════════════════════");

        // ═══════════════════════════════════════
        // SECCIÓN 2: DIAGNÓSTICO DE VISIÓN
        // ═══════════════════════════════════════
        telemetry.addLine();
        telemetry.addLine("── 👁 CÁMARA VE ──");
        
        List<AprilTagDetection> detections = vision.getAllDetections();
        
        // Detectar qué goals están visibles
        boolean seesRedGoal = false;
        boolean seesBlueGoal = false;
        
        if (detections.isEmpty()) {
            telemetry.addLine("  ⚠ NINGÚN TAG DETECTADO");
        } else {
            for (AprilTagDetection det : detections) {
                String emoji = "";
                String name = "";
                
                if (det.id == VisionConstants.TAG_GOAL_RED) {
                    emoji = "🔴";
                    name = "GOAL ROJO";
                    seesRedGoal = true;
                } else if (det.id == VisionConstants.TAG_GOAL_BLUE) {
                    emoji = "🔵";
                    name = "GOAL AZUL";
                    seesBlueGoal = true;
                } else if (det.id == VisionConstants.TAG_SEQUENCE_GPP ||
                           det.id == VisionConstants.TAG_SEQUENCE_PGP ||
                           det.id == VisionConstants.TAG_SEQUENCE_PPG) {
                    emoji = "🟢";
                    name = "SECUENCIA";
                } else {
                    emoji = "⚪";
                    name = "OTRO";
                }
                
                if (det.ftcPose != null) {
                    telemetry.addLine(String.format("  %s Tag %d (%s): %.1f\" @ %.1f°", 
                        emoji, det.id, name, det.ftcPose.range, det.ftcPose.bearing));
                } else {
                    telemetry.addLine(String.format("  %s Tag %d (%s): sin pose", 
                        emoji, det.id, name));
                }
            }
        }

        // ═══════════════════════════════════════
        // SECCIÓN 3: ESTADO DEL AUTO-AIM
        // ═══════════════════════════════════════
        telemetry.addLine();
        telemetry.addLine("── 🎯 AUTO-AIM ──");
        
        if (currentAimCommand != null && targetAlliance != null) {
            // Mostrar qué está buscando
            String targetEmoji = (targetAlliance == Alliance.RED) ? "🔴" : "🔵";
            String targetName = (targetAlliance == Alliance.RED) ? "ROJO" : "AZUL";
            int targetTagId = currentAimCommand.getTargetTagId();
            
            telemetry.addData("Buscando", String.format("%s Goal %s (tag %d)", 
                targetEmoji, targetName, targetTagId));
            
            // Verificar si el tag buscado está visible
            boolean targetVisible = (targetAlliance == Alliance.RED && seesRedGoal) ||
                                   (targetAlliance == Alliance.BLUE && seesBlueGoal);
            
            if (!targetVisible) {
                telemetry.addLine("  ⚠️ ¡TAG " + targetTagId + " NO ESTÁ VISIBLE!");
                if (targetAlliance == Alliance.RED && seesBlueGoal) {
                    telemetry.addLine("  💡 Veo el AZUL - ¿Presionar B?");
                } else if (targetAlliance == Alliance.BLUE && seesRedGoal) {
                    telemetry.addLine("  💡 Veo el ROJO - ¿Presionar A?");
                }
            }
            
            // Estado del tracking
            telemetry.addData("Locked", currentAimCommand.isLocked() ? "✅ SÍ" : "❌ NO");
            telemetry.addData("Viendo goal", currentAimCommand.canSeeGoal() ? "✅" : "❌");
            
            // RBE si está disponible
            RBE rbe = currentAimCommand.getLastRBE();
            if (rbe != null) {
                telemetry.addData("Range", String.format("%.1f\"", rbe.range));
                telemetry.addData("Bearing", String.format("%.1f° %s", 
                    rbe.bearing, 
                    rbe.bearing > 0 ? "(←izq)" : rbe.bearing < 0 ? "(der→)" : "(centro)"));
            }
        } else {
            telemetry.addLine("  INACTIVO");
            telemetry.addLine("  A = Buscar ROJO (tag " + VisionConstants.TAG_GOAL_RED + ")");
            telemetry.addLine("  B = Buscar AZUL (tag " + VisionConstants.TAG_GOAL_BLUE + ")");
        }

        // ═══════════════════════════════════════
        // SECCIÓN 4: TORRETA
        // ═══════════════════════════════════════
        telemetry.addLine();
        telemetry.addLine("── 🔄 TORRETA ──");
        telemetry.addData("Ángulo actual", String.format("%.1f°", turret.getCurrentAngleDeg()));
        telemetry.addData("Target", String.format("%.1f°", turret.getTargetAngleDeg()));
        telemetry.addData("Error", String.format("%.1f°", turret.getPositionErrorDeg()));
        telemetry.addData("Estado", turret.getStateName());
        telemetry.addData("En target", turret.isAtTarget() ? "✅" : "❌");

        // ═══════════════════════════════════════
        // SECCIÓN 5: DEBUG (opcional)
        // ═══════════════════════════════════════
        if (debugMode) {
            telemetry.addLine();
            telemetry.addLine("── 🔧 DEBUG ──");
            telemetry.addData("Vision State", vision.getState());
            telemetry.addData("Has Valid Detection", vision.hasValidDetection());
            telemetry.addData("Motor Power", String.format("%.2f", turret.getMotorPower()));
            telemetry.addData("Velocity", String.format("%.1f°/s", turret.getVelocityDegPerSec()));
            
            // RBE directo a cada goal
            RBE redRBE = vision.getGoalRBE(Alliance.RED);
            RBE blueRBE = vision.getGoalRBE(Alliance.BLUE);
            telemetry.addData("RBE→Rojo", redRBE != null ? 
                String.format("R=%.1f\" B=%.1f°", redRBE.range, redRBE.bearing) : "null");
            telemetry.addData("RBE→Azul", blueRBE != null ? 
                String.format("R=%.1f\" B=%.1f°", blueRBE.range, blueRBE.bearing) : "null");
        }

        // ═══════════════════════════════════════
        // SECCIÓN 6: CONTROLES
        // ═══════════════════════════════════════
        telemetry.addLine();
        telemetry.addLine("── CONTROLES ──");
        telemetry.addLine("A=🔴Rojo  B=🔵Azul  X=Stop  Y=Debug");
        telemetry.addLine("Stick Izq=Manual  ↑↓=Vision on/off");

        telemetry.update();
    }
}