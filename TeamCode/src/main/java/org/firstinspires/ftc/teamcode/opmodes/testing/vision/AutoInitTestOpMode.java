package org.firstinspires.ftc.teamcode.opmodes.test;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.RunCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.vision.DetectSequenceCommand;
import org.firstinspires.ftc.teamcode.commands.vision.UpdateRobotPoseCommand;
import org.firstinspires.ftc.teamcode.commands.turret.RotateToGoalDirectionCommand;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.FieldPose;

/**
 * OpMode de prueba para la secuencia de inicialización del autónomo.
 * 
 * Prueba el flujo completo:
 * 1. Detectar secuencia del obelisco
 * 2. Rotar torreta hacia el goal
 * 3. Actualizar pose del robot desde AprilTag
 * 
 * CONTROLES:
 * ──────────────────────────────────────────
 * Gamepad 1:
 *   A → Ejecutar secuencia completa (alianza ROJA)
 *   B → Ejecutar secuencia completa (alianza AZUL)
 *   
 *   X → Solo paso 1: Detectar secuencia
 *   Y → Solo paso 2-3: Rotar y actualizar pose (ROJO)
 *   
 *   DPAD_LEFT  → Reset de resultados
 *   DPAD_RIGHT → Cancelar secuencia en progreso
 * ──────────────────────────────────────────
 */
@TeleOp(name = "Test - Auto Init Sequence", group = "Test")
public class AutoInitTestOpMode extends CommandOpMode {

    // ===== SUBSYSTEMS =====
    private VisionSubsystem vision;
    private DriveSubsystem drive;
    private TurretSubsystem turret;

    // ===== GAMEPAD =====
    private GamepadEx driver;

    // ===== RESULTADOS =====
    private String detectedSequence = null;
    private FieldPose detectedPose = null;
    private String currentStep = "Esperando...";
    private boolean sequenceRunning = false;
    private long sequenceStartTime = 0;

    // ===== COMANDO ACTUAL =====
    private SequentialCommandGroup currentSequence;

    @Override
    public void initialize() {
        // Inicializar subsystems
        vision = new VisionSubsystem(hardwareMap);
        drive = new DriveSubsystem(hardwareMap);
        turret = new TurretSubsystem(hardwareMap);

        // Inicializar gamepad
        driver = new GamepadEx(gamepad1);

        // ===== BINDINGS =====

        // A → Secuencia completa ROJO
        driver.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new InstantCommand(() -> runFullSequence(Alliance.RED)));

        // B → Secuencia completa AZUL
        driver.getGamepadButton(GamepadKeys.Button.B)
            .whenPressed(new InstantCommand(() -> runFullSequence(Alliance.BLUE)));

        // X → Solo detectar secuencia
        driver.getGamepadButton(GamepadKeys.Button.X)
            .whenPressed(new InstantCommand(() -> runDetectSequenceOnly()));

        // Y → Solo rotar y actualizar pose
        driver.getGamepadButton(GamepadKeys.Button.Y)
            .whenPressed(new InstantCommand(() -> runPoseUpdateOnly(Alliance.RED)));

        // DPAD_LEFT → Reset
        driver.getGamepadButton(GamepadKeys.Button.DPAD_LEFT)
            .whenPressed(new InstantCommand(() -> resetResults()));

        // DPAD_RIGHT → Cancelar
        driver.getGamepadButton(GamepadKeys.Button.DPAD_RIGHT)
            .whenPressed(new InstantCommand(() -> cancelSequence()));

        // Telemetría
        schedule(new RunCommand(() -> updateTelemetry()));

        // Activar visión
        vision.enable();

        telemetry.addLine("=== AUTO INIT SEQUENCE TEST ===");
        telemetry.addLine("Presiona START para comenzar");
        telemetry.update();
    }

    // ===== SECUENCIAS DE PRUEBA =====

    /**
     * Ejecuta la secuencia completa de inicialización.
     */
    private void runFullSequence(Alliance alliance) {
        if (sequenceRunning) {
            telemetry.speak("Sequence already running");
            return;
        }

        resetResults();
        sequenceRunning = true;
        sequenceStartTime = System.currentTimeMillis();
        String allianceName = (alliance == Alliance.RED) ? "ROJA" : "AZUL";

        currentSequence = new SequentialCommandGroup(
            // Paso 1: Detectar secuencia
            new InstantCommand(() -> currentStep = "1/3: Detectando secuencia..."),
            new DetectSequenceCommand(vision, seq -> {
                detectedSequence = seq;
                if (seq != null) {
                    telemetry.speak("Sequence " + seq);
                }
            }),
            new WaitCommand(200),

            // Paso 2: Rotar torreta hacia goal
            new InstantCommand(() -> currentStep = "2/3: Rotando torreta hacia goal " + allianceName + "..."),
            new RotateToGoalDirectionCommand(turret, alliance),
            new WaitCommand(300),

            // Paso 3: Actualizar pose
            new InstantCommand(() -> currentStep = "3/3: Calculando pose del robot..."),
            new UpdateRobotPoseCommand(vision, drive, turret, pose -> {
                detectedPose = pose;
                if (pose != null) {
                    telemetry.speak("Pose updated");
                }
            }, 3000),

            // Finalizar
            new InstantCommand(() -> {
                sequenceRunning = false;
                long elapsed = System.currentTimeMillis() - sequenceStartTime;
                currentStep = "✓ Completado en " + elapsed + "ms";
                telemetry.speak("Sequence complete");
            })
        );

        currentSequence.schedule();
    }

    /**
     * Ejecuta solo la detección de secuencia.
     */
    private void runDetectSequenceOnly() {
        if (sequenceRunning) return;

        resetResults();
        sequenceRunning = true;
        sequenceStartTime = System.currentTimeMillis();
        currentStep = "Detectando secuencia...";

        new SequentialCommandGroup(
            new DetectSequenceCommand(vision, seq -> {
                detectedSequence = seq;
            }),
            new InstantCommand(() -> {
                sequenceRunning = false;
                long elapsed = System.currentTimeMillis() - sequenceStartTime;
                currentStep = detectedSequence != null 
                    ? "✓ Secuencia: " + detectedSequence + " (" + elapsed + "ms)"
                    : "✗ No detectada (" + elapsed + "ms)";
            })
        ).schedule();
    }

    /**
     * Ejecuta solo rotación de torreta y actualización de pose.
     */
    private void runPoseUpdateOnly(Alliance alliance) {
        if (sequenceRunning) return;

        // Mantener secuencia detectada anteriormente
        FieldPose previousPose = detectedPose;
        detectedPose = null;
        
        sequenceRunning = true;
        sequenceStartTime = System.currentTimeMillis();

        new SequentialCommandGroup(
            new InstantCommand(() -> currentStep = "Rotando torreta..."),
            new RotateToGoalDirectionCommand(turret, alliance),
            new WaitCommand(300),
            new InstantCommand(() -> currentStep = "Calculando pose..."),
            new UpdateRobotPoseCommand(vision, drive, turret, pose -> {
                detectedPose = pose;
            }, 3000),
            new InstantCommand(() -> {
                sequenceRunning = false;
                long elapsed = System.currentTimeMillis() - sequenceStartTime;
                currentStep = detectedPose != null 
                    ? "✓ Pose actualizada (" + elapsed + "ms)"
                    : "✗ Pose no calculada (" + elapsed + "ms)";
            })
        ).schedule();
    }

    /**
     * Resetea todos los resultados.
     */
    private void resetResults() {
        detectedSequence = null;
        detectedPose = null;
        currentStep = "Esperando...";
    }

    /**
     * Cancela la secuencia en progreso.
     */
    private void cancelSequence() {
        if (currentSequence != null) {
            currentSequence.cancel();
        }
        sequenceRunning = false;
        currentStep = "⚠ Cancelado";
        telemetry.speak("Cancelled");
    }

    /**
     * Actualiza la telemetría.
     */
    private void updateTelemetry() {
        telemetry.addLine("══════════════════════════════════");
        telemetry.addLine("   AUTO INIT SEQUENCE TEST");
        telemetry.addLine("══════════════════════════════════");

        // Estado actual
        telemetry.addLine();
        telemetry.addData("Estado", currentStep);
        
        if (sequenceRunning) {
            long elapsed = System.currentTimeMillis() - sequenceStartTime;
            telemetry.addData("Tiempo", elapsed + "ms");
        }

        // Resultados
        telemetry.addLine();
        telemetry.addLine("── RESULTADOS ──");
        
        // Secuencia
        if (detectedSequence != null) {
            telemetry.addData("Secuencia", "✓ " + detectedSequence);
        } else {
            telemetry.addData("Secuencia", "No detectada");
        }
        
        // Pose
        if (detectedPose != null) {
            telemetry.addData("Pose X", String.format("%.1f\"", detectedPose.x));
            telemetry.addData("Pose Y", String.format("%.1f\"", detectedPose.y));
            telemetry.addData("Pose Heading", String.format("%.1f°", detectedPose.heading));
            telemetry.addData("Desde tag", detectedPose.sourceTagId);
        } else {
            telemetry.addData("Pose", "No calculada");
        }

        // Estado de subsystems
        telemetry.addLine();
        telemetry.addLine("── SUBSYSTEMS ──");
        telemetry.addData("Vision", vision.getState());
        telemetry.addData("Turret Angle", String.format("%.1f°", turret.getAngle()));
        telemetry.addData("Drive Heading", String.format("%.1f°", drive.getHeading()));

        // Controles
        telemetry.addLine();
        telemetry.addLine("── CONTROLES ──");
        telemetry.addLine("A=Full Rojo  B=Full Azul");
        telemetry.addLine("X=Solo Secuencia  Y=Solo Pose");
        telemetry.addLine("←=Reset  →=Cancelar");

        telemetry.update();
    }
}
