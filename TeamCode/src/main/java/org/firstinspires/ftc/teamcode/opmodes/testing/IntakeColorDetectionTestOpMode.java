package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.intake.IntakeWithColorDetectionCommand;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * OpMode para probar IntakeWithColorDetectionCommand.
 * 
 * CONTROLES (Gamepad 2):
 * - RT (mantener): Ejecutar intake con detección automática
 * - LT: Outtake manual
 * - A: Siguiente slot (intake position)
 * - DPAD UP: Slot actual a intake position
 * - LB: Limpiar slot actual
 * - RB: Limpiar todos los slots
 */
@TeleOp(name = "Test: Intake Color Detection", group = "Tuning")
public class IntakeColorDetectionTestOpMode extends CommandOpMode {

    private IntakeSubsystem intake;
    private SpindexerSubsystem spindexer;
    private GamepadEx operatorGamepad;

    private IntakeWithColorDetectionCommand intakeCommand;
    private boolean commandRunning = false;

    @Override
    public void initialize() {
        // Subsystems
        intake = new IntakeSubsystem(hardwareMap);
        spindexer = new SpindexerSubsystem(hardwareMap);
        register(intake, spindexer);

        // Gamepad
        operatorGamepad = new GamepadEx(gamepad2);

        // Comando de intake
        intakeCommand = new IntakeWithColorDetectionCommand(intake, spindexer);

        // Bindings
        configureBindings();

        telemetry.addLine("=== INTAKE COLOR DETECTION TEST ===");
        telemetry.addLine("RT: Intake Auto | LT: Outtake");
        telemetry.addLine("A: Next Slot | LB: Clear | RB: Clear All");
        telemetry.update();

        spindexer.moveToIntakePosition(0);//Only for testing
    }

    private void configureBindings() {
        // A: Siguiente slot
        operatorGamepad.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new InstantCommand(() -> {
                int next = (spindexer.getCurrentSlotIndex() + 1) % 3;
                spindexer.moveToIntakePosition(next);
            }));

        // DPAD UP: Slot actual a intake
        operatorGamepad.getGamepadButton(GamepadKeys.Button.DPAD_UP)
            .whenPressed(new InstantCommand(() -> 
                spindexer.moveToIntakePosition(spindexer.getCurrentSlotIndex())));

        // LB: Limpiar slot actual
        operatorGamepad.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
            .whenPressed(new InstantCommand(() -> 
                spindexer.clearSlot(spindexer.getCurrentSlotIndex())));

        // RB: Limpiar todos
        operatorGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
            .whenPressed(new InstantCommand(spindexer::clearAllSlots));
    }

    @Override
    public void run() {
        super.run();

        // Manejar triggers
        handleTriggers();

        // Telemetría
        updateTelemetry();
    }

    private void handleTriggers() {
        float rt = gamepad2.right_trigger;
        float lt = gamepad2.left_trigger;

        if (rt > 0.1) {
            // Verificar si podemos hacer intake
            if (spindexer.isFull()) {
                // Rumble indicando que está lleno
                if (!commandRunning) {
                    gamepad2.rumble(300);
                }
                commandRunning = false;
            } else if (!commandRunning) {
                // Iniciar comando
                intakeCommand = new IntakeWithColorDetectionCommand(intake, spindexer);
                schedule(intakeCommand);
                commandRunning = true;
            } else if (intakeCommand.isFinished()) {
                // Comando terminó, re-ejecutar si sigue presionado y hay espacio
                if (!spindexer.isFull()) {
                    intakeCommand = new IntakeWithColorDetectionCommand(intake, spindexer);
                    schedule(intakeCommand);
                }
            }
        } else {
            // Soltó trigger
            if (commandRunning) {
                intakeCommand.cancel();
                commandRunning = false;
            }
        }

        // Outtake manual (cancela intake)
        if (lt > 0.1) {
            if (commandRunning) {
                intakeCommand.cancel();
                commandRunning = false;
            }
            intake.outtake();
        } else if (rt <= 0.1) {
            intake.stop();
        }
    }

    private void updateTelemetry() {
        telemetry.addLine("════════════════════════════════");
        telemetry.addLine("   INTAKE COLOR DETECTION TEST");
        telemetry.addLine("════════════════════════════════");

        // Estado del comando
        telemetry.addLine();
        telemetry.addData("Comando", commandRunning ? "▶ ACTIVO" : "⏹ INACTIVO");
        if (commandRunning) {
            telemetry.addData("  Detectado", intakeCommand.isBallDetected() ? "SI" : "NO");
            telemetry.addData("  Color", intakeCommand.getDetectedColor());
            telemetry.addData("  Consec G/P", "%d / %d", 
                intakeCommand.getConsecutiveGreen(), 
                intakeCommand.getConsecutivePurple());
        }

        // Sensor
        telemetry.addLine();
        telemetry.addLine("── SENSOR ──");
        telemetry.addData("Distancia", "%.2f cm", spindexer.getDistance());
        telemetry.addData("HSV", "H=%.0f S=%.2f V=%.2f",
            spindexer.getHue(), spindexer.getSaturation(), spindexer.getValue());
        telemetry.addData("RGB", "%d, %d, %d",
            spindexer.getRed(), spindexer.getGreen(), spindexer.getBlue());

        // Spindexer
        telemetry.addLine();
        telemetry.addLine("── SPINDEXER ──");
        telemetry.addData("Slot", "%d | %s", 
            spindexer.getCurrentSlotIndex(),
            spindexer.isAtIntake() ? "INTAKE" : "OUTTAKE");

        // Visual de slots
        StringBuilder slots = new StringBuilder("Slots: ");
        for (int i = 0; i < 3; i++) {
            String icon = spindexer.getSlotEmoji(i);
            if (i == spindexer.getCurrentSlotIndex()) {
                slots.append("[").append(icon).append("]");
            } else {
                slots.append(" ").append(icon).append(" ");
            }
        }
        telemetry.addLine(slots.toString());
        telemetry.addData("Llenos", "%d/3", spindexer.getFilledSlotCount());

        // Intake
        telemetry.addLine();
        telemetry.addData("Intake", intake.getState());
        telemetry.addData("Triggers", "RT=%.2f LT=%.2f", 
            gamepad2.right_trigger, gamepad2.left_trigger);

        telemetry.update();
    }
}
