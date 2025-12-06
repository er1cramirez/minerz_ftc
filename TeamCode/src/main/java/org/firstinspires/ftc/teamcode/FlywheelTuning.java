package org.firstinspires.ftc.teamcode;

import com.seattlesolvers.solverslib.hardware.motors.MotorEx;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.List;

/**
 * OpMode para ajustar control de velocidad de flywheel
 * 
 * CONTROLES:
 * A: ON/OFF motor
 * DPAD ↑/↓: Velocidad ±100 RPM (mantener para continuo)
 * LB/RB: Velocidad ±500 RPM
 * Y: Velocidad máxima | B: Stop
 * 
 * DPAD ←/→: kP ±0.01
 * LEFT_STICK_BUTTON: kV -0.00001
 * RIGHT_STICK_BUTTON: kV +0.00001
 * X: Reset parámetros
 */
@TeleOp(name = "Flywheel Tuning", group = "Tuning")
public class FlywheelTuning extends LinearOpMode {

    private MotorEx flywheel;
    private GamepadEx gamepadEx;

    // Estado
    private boolean motorOn = false;
    private double targetVelocity = 0.0;  // En RPM
    private double currentVelocity = 0.0; // En RPM

    // Conversión (Rev Hex: 28 CPR)
    private static final double CPR = 28.0;
    private static final double COUNTS_TO_RPM = 60.0 / CPR;
    private static final double RPM_TO_COUNTS = CPR / 60.0;

    // Parámetros de control
    private double kP = 2.5;
    private double kI = 0.0;
    private double kD = 0.0;
    private double kF = 0.0;
    private double kV = 1.4;

    // Incrementos
    private static final double VEL_SMALL = 100.0;
    private static final double VEL_LARGE = 500.0;
    private static final double MAX_VEL = 5500.0;
    private static final double KP_INC = 0.01;
    private static final double KV_INC = 0.01;

    // Hold timer
    private ElapsedTime holdTimer = new ElapsedTime();
    private static final double HOLD_TIME = 0.3;
    private static final double REPEAT_RATE = 0.1;

    @Override
    public void runOpMode() throws InterruptedException {
        gamepadEx = new GamepadEx(gamepad1);

        // Configurar motor Rev Hex
        flywheel = new MotorEx(hardwareMap, "flywheel", 28, 6000);
        flywheel.setRunMode(MotorEx.RunMode.VelocityControl);
        flywheel.setVeloCoefficients(kP, kI, kD);
        flywheel.setFeedforwardCoefficients(kF, kV);

        // Bulk caching
        List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
        hubs.forEach(hub -> hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL));

        telemetry.addLine("Flywheel Tuning Ready");
        telemetry.update();

        waitForStart();
        holdTimer.reset();

        while (!isStopRequested() && opModeIsActive()) {
            hubs.forEach(LynxModule::clearBulkCache);
            gamepadEx.readButtons();

            // === CONTROLES ===
            
            // ON/OFF
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.A)) {
                motorOn = !motorOn;
                if (!motorOn) targetVelocity = 0;
            }

            if (motorOn) {
                // Velocidad
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_UP)) {
                    targetVelocity = Math.min(targetVelocity + VEL_SMALL, MAX_VEL);
                    holdTimer.reset();
                }
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_DOWN)) {
                    targetVelocity = Math.max(targetVelocity - VEL_SMALL, 0);
                    holdTimer.reset();
                }
                
                // Hold mode
                if (gamepadEx.isDown(GamepadKeys.Button.DPAD_UP) && holdTimer.seconds() > HOLD_TIME) {
                    targetVelocity = Math.min(targetVelocity + VEL_SMALL, MAX_VEL);
                    sleep((long)(REPEAT_RATE * 1000));
                }
                if (gamepadEx.isDown(GamepadKeys.Button.DPAD_DOWN) && holdTimer.seconds() > HOLD_TIME) {
                    targetVelocity = Math.max(targetVelocity - VEL_SMALL, 0);
                    sleep((long)(REPEAT_RATE * 1000));
                }

                // Saltos grandes
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.LEFT_BUMPER)) {
                    targetVelocity = Math.min(targetVelocity + VEL_LARGE, MAX_VEL);
                }
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.RIGHT_BUMPER)) {
                    targetVelocity = Math.max(targetVelocity - VEL_LARGE, 0);
                }

                // Atajos
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.Y)) {
                    targetVelocity = MAX_VEL;
                }
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.B)) {
                    targetVelocity = 0;
                }
            }

            // Ajustar kP
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_RIGHT)) {
                kP += KP_INC;
                flywheel.setVeloCoefficients(kP, kI, kD);
                holdTimer.reset();
            }
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_LEFT)) {
                kP = Math.max(0, kP - KP_INC);
                flywheel.setVeloCoefficients(kP, kI, kD);
                holdTimer.reset();
            }
            
            // Hold kP
            if (gamepadEx.isDown(GamepadKeys.Button.DPAD_RIGHT) && holdTimer.seconds() > HOLD_TIME) {
                kP += KP_INC;
                flywheel.setVeloCoefficients(kP, kI, kD);
                sleep((long)(REPEAT_RATE * 1000));
            }
            if (gamepadEx.isDown(GamepadKeys.Button.DPAD_LEFT) && holdTimer.seconds() > HOLD_TIME) {
                kP = Math.max(0, kP - KP_INC);
                flywheel.setVeloCoefficients(kP, kI, kD);
                sleep((long)(REPEAT_RATE * 1000));
            }

            // Ajustar kV con stick buttons
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.RIGHT_STICK_BUTTON)) {
                kV += KV_INC;
                flywheel.setFeedforwardCoefficients(kF, kV);
                holdTimer.reset();
            }
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.LEFT_STICK_BUTTON)) {
                kV = Math.max(0, kV - KV_INC);
                flywheel.setFeedforwardCoefficients(kF, kV);
                holdTimer.reset();
            }
            
            // Hold kV
            if (gamepadEx.isDown(GamepadKeys.Button.RIGHT_STICK_BUTTON) && holdTimer.seconds() > HOLD_TIME) {
                kV += KV_INC;
                flywheel.setFeedforwardCoefficients(kF, kV);
                sleep((long)(REPEAT_RATE * 1000));
            }
            if (gamepadEx.isDown(GamepadKeys.Button.LEFT_STICK_BUTTON) && holdTimer.seconds() > HOLD_TIME) {
                kV = Math.max(0, kV - KV_INC);
                flywheel.setFeedforwardCoefficients(kF, kV);
                sleep((long)(REPEAT_RATE * 1000));
            }

            // Reset
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.X)) {
                kP = 0.1;
                kV = 0.00017;
                flywheel.setVeloCoefficients(kP, kI, kD);
                flywheel.setFeedforwardCoefficients(kF, kV);
            }

            // === CONTROL MOTOR ===
            if (motorOn) {
                flywheel.setVelocity(targetVelocity * RPM_TO_COUNTS);
            } else {
                flywheel.stopMotor();
            }

            // Leer velocidad (convertir counts/s a RPM)
            currentVelocity = flywheel.getVelocity() * COUNTS_TO_RPM;

            // === TELEMETRÍA ===
            double error = targetVelocity - currentVelocity;
            double errorPercent = targetVelocity > 0 ? Math.abs(error / targetVelocity * 100) : 0;

            telemetry.addData("Motor", motorOn ? "🟢 ON" : "⚫ OFF");
            telemetry.addLine();
            
            telemetry.addData("Target", "%.0f RPM", targetVelocity);
            telemetry.addData("Actual", "%.0f RPM", currentVelocity);
            telemetry.addData("Error", "%.0f RPM (%.1f%%)", error, errorPercent);
            
            // Status
            String status;
            if (!motorOn) status = "⚫ OFF";
            else if (errorPercent < 2) status = "🟢 Perfecto";
            else if (errorPercent < 5) status = "🟡 Aceptable";
            else status = "🔴 Ajustar";
            telemetry.addData("Status", status);
            telemetry.addLine();
            
            telemetry.addData("kP", "%.3f", kP);
            telemetry.addData("kV", "%.5f", kV);
            telemetry.addLine();
            
            telemetry.addLine("DPAD↑↓: Vel | DPAD←→: kP");
            telemetry.addLine("L3/R3: kV | X: Reset");
            
            telemetry.update();
        }

        flywheel.stopMotor();
    }
}
