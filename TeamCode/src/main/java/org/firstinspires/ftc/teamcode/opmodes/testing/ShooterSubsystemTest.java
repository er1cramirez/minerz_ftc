package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.CommandScheduler;
import com.seattlesolvers.solverslib.command.RunCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.shooter.*;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

import java.util.List;
@Disabled
@TeleOp(name = "Shooter Subsystem Test", group = "Testing")
public class ShooterSubsystemTest extends CommandOpMode {

    private ShooterSubsystem shooter;
    private GamepadEx gamepadEx;

    private String lastCommandRun = "None";
    private ElapsedTime commandTimer = new ElapsedTime();

    private List<LynxModule> hubs;

    @Override
    public void initialize() {
        shooter = new ShooterSubsystem(hardwareMap);
        gamepadEx = new GamepadEx(gamepad1);

        hubs = hardwareMap.getAll(LynxModule.class);
        hubs.forEach(hub -> hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL));

        setupButtonBindings();

        schedule(new RunCommand(this::updateTelemetry));

        telemetry.addLine("✅ Shooter Test Ready");
        telemetry.update();
    }

    private void setupButtonBindings() {

        // WARM UP
        gamepadEx.getGamepadButton(GamepadKeys.Button.DPAD_UP)
                .whenPressed(
                        new WarmUpCommand(shooter)
                                .whenFinished(() -> {
                                    lastCommandRun = "WarmUp → IDLE";
                                    commandTimer.reset();
                                })
                );

        // STOP
        gamepadEx.getGamepadButton(GamepadKeys.Button.DPAD_DOWN)
                .whenPressed(
                        new StopShooterCommand(shooter)
                                .whenFinished(() -> {
                                    lastCommandRun = "Stop";
                                    commandTimer.reset();
                                })
                );

        // IDLE
        gamepadEx.getGamepadButton(GamepadKeys.Button.A)
                .whenPressed(
                        new IdleSpinCommand(shooter)
                                .whenFinished(() -> {
                                    lastCommandRun = "→ IDLE";
                                    commandTimer.reset();
                                })
                );

        // SPIN UP CLOSE
        gamepadEx.getGamepadButton(GamepadKeys.Button.X)
                .whenPressed(
                        new SpinUpCloseCommand(shooter)
                                .whenFinished(() -> {
                                    lastCommandRun = "→ CLOSE";
                                    commandTimer.reset();
                                })
                );

        // SPIN UP FAR
        gamepadEx.getGamepadButton(GamepadKeys.Button.Y)
                .whenPressed(
                        new SpinUpFarCommand(shooter)
                                .whenFinished(() -> {
                                    lastCommandRun = "→ FAR";
                                    commandTimer.reset();
                                })
                );

        // HOLD CLOSE
        gamepadEx.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
                .whenPressed(new SpinUpCloseCommand(shooter))
                .whenReleased(
                        new IdleSpinCommand(shooter)
                                .whenFinished(() -> {
                                    lastCommandRun = "Released → IDLE";
                                    commandTimer.reset();
                                })
                );

        // HOLD FAR
        gamepadEx.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
                .whenPressed(new SpinUpFarCommand(shooter))
                .whenReleased(
                        new IdleSpinCommand(shooter)
                                .whenFinished(() -> {
                                    lastCommandRun = "Released → IDLE";
                                    commandTimer.reset();
                                })
                );

        // RESET
        gamepadEx.getGamepadButton(GamepadKeys.Button.BACK)
                .whenPressed(() -> {
                    CommandScheduler.getInstance().reset();
                    lastCommandRun = "⚠️ RESET";
                    commandTimer.reset();
                });
    }

    private void updateTelemetry() {
        hubs.forEach(LynxModule::clearBulkCache);

        telemetry.clear();
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine("      SHOOTER SUBSYSTEM TEST");
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine();

        // Estado (siempre disponible)
        String stateIcon = getStateIcon(shooter.getState());
        telemetry.addData("Estado", "%s %s", stateIcon, shooter.getStateName());
        telemetry.addLine();

        // Velocidades
        telemetry.addData("Target", "%.0f RPM", shooter.getTargetRpm());
        telemetry.addData("Actual", "%.0f RPM", shooter.getCurrentRpm());

        double error = shooter.getVelocityError();
        double errorPercent = shooter.getErrorPercent();
        String errorColor = errorPercent < 5 ? "🟢" : (errorPercent < 10 ? "🟡" : "🔴");
        telemetry.addData("Error", "%s %.0f RPM (%.1f%%)", errorColor, error, errorPercent);
        telemetry.addLine();

        // Ready status
        String readyIcon = shooter.isReady() ? "🟢" : "⚫";
        String readyText = shooter.isReady() ? "READY" : "NOT READY";
        telemetry.addData("Ready", "%s %s", readyIcon, readyText);
        telemetry.addLine();

        // Último comando
        telemetry.addData("Last Action", lastCommandRun);
        telemetry.addData("Time", "%.1f s", commandTimer.seconds());
        telemetry.addLine();

        // Controles
        telemetry.addLine("───────────────────────────────────");
        telemetry.addLine("DPAD↑: WarmUp | DPAD↓: Stop");
        telemetry.addLine("A: IDLE | X: Close | Y: Far");
        telemetry.addLine("LB: Hold Close | RB: Hold Far");
        telemetry.addLine("───────────────────────────────────");

        telemetry.update();
    }

    private String getStateIcon(ShooterSubsystem.ShooterState state) {
        switch (state) {
            case STOPPED: return "⚫";
            case WARMING_UP: return "🟡";
            case IDLE: return "🟢";
            case SPIN_UP_CLOSE: return "🔵";
            case SPIN_UP_FAR: return "🟣";
            default: return "❓";
        }
    }
}