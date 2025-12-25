package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.intake.IntakeWithColorDetectionCommand;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

/**
 * OpMode para probar IntakeWithColorDetectionCommand.
 * 
 * CONTROLES (Gamepad 2):
 * - RT (mantener): Ejecutar intake con detección automática
 * - LT: Outtake manual
 * - A: Siguiente slot
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
    private boolean triggerWasPressed = false;

    @Override
    public void initialize() {
        intake = new IntakeSubsystem(hardwareMap);
        spindexer = new SpindexerSubsystem(hardwareMap);
        register(intake, spindexer);

        operatorGamepad = new GamepadEx(gamepad2);
        configureBindings();

        telemetry.addLine("=== INTAKE COLOR DETECTION TEST ===");
        telemetry.addLine("RT: Intake Auto | LT: Outtake");
        telemetry.update();
    }

    private void configureBindings() {
        operatorGamepad.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new InstantCommand(() -> {
                int next = (spindexer.getCurrentSlotIndex() + 1) % 3;
                spindexer.moveToIntakePosition(next);
            }));

        operatorGamepad.getGamepadButton(GamepadKeys.Button.DPAD_UP)
            .whenPressed(new InstantCommand(() -> 
                spindexer.moveToIntakePosition(spindexer.getCurrentSlotIndex())));

        operatorGamepad.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
            .whenPressed(new InstantCommand(() -> 
                spindexer.clearSlot(spindexer.getCurrentSlotIndex())));

        operatorGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
            .whenPressed(new InstantCommand(spindexer::clearAllSlots));
    }

    @Override
    public void run() {
        super.run();
        handleTriggers();
        updateTelemetry();
    }

    private void handleTriggers() {
        float rt = gamepad2.right_trigger;
        float lt = gamepad2.left_trigger;

        boolean triggerPressed = rt > 0.1;

        if (triggerPressed) {
            if (spindexer.isFull()) {
                if (!triggerWasPressed) {
                    gamepad2.rumble(300);
                }
            } else {
                boolean commandFinished = intakeCommand == null || !intakeCommand.isScheduled();
                
                if (commandFinished) {
                    intakeCommand = new IntakeWithColorDetectionCommand(intake, spindexer);
                    schedule(intakeCommand);
                }
            }
        } else {
            if (intakeCommand != null && intakeCommand.isScheduled()) {
                intakeCommand.cancel();
            }
        }

        triggerWasPressed = triggerPressed;

        // Outtake manual
        if (lt > 0.1) {
            if (intakeCommand != null && intakeCommand.isScheduled()) {
                intakeCommand.cancel();
            }
            intake.outtake();
        } else if (!triggerPressed) {
            intake.stop();
        }
    }

    private void updateTelemetry() {
        telemetry.addLine("════════════════════════════════");
        telemetry.addLine("   INTAKE COLOR DETECTION TEST");
        telemetry.addLine("════════════════════════════════");

        // Sensor
        telemetry.addLine();
        telemetry.addLine("── SENSOR ──");
        telemetry.addData("Distancia", "%.2f cm", spindexer.getDistance());
        telemetry.addData("HSV", "H=%.0f S=%.2f V=%.2f",
            spindexer.getHue(), spindexer.getSaturation(), spindexer.getValue());

        // Spindexer
        telemetry.addLine();
        telemetry.addLine("── SPINDEXER ──");
        telemetry.addData("Slot", "%d | %s", 
            spindexer.getCurrentSlotIndex(),
            spindexer.isAtIntake() ? "INTAKE" : "OUTTAKE");

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

        // Comando
        telemetry.addLine();
        boolean isRunning = intakeCommand != null && intakeCommand.isScheduled();
        telemetry.addData("Comando", isRunning ? "▶ ACTIVO" : "⏹ INACTIVO");

        telemetry.update();
    }
}