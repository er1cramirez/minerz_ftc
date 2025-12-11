package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.constants.ShooterConstants;

import java.util.List;

/**
 * Prueba MÍNIMA para diagnosticar el problema.
 * Replica exactamente la estructura de FlywheelTuning pero simplificado.
 */
@TeleOp(name = "Test Flywheel Direct", group = "Testing")
public class TestFlywheelDirect extends LinearOpMode {
    
    private MotorEx flywheel;
    private GamepadEx gamepadEx;
    
    private double targetRpm = 0;
    private boolean motorOn = false;
    
    @Override
    public void runOpMode() throws InterruptedException {
        gamepadEx = new GamepadEx(gamepad1);
        
        flywheel = new MotorEx(hardwareMap, "flyWheel", 28, 6000);
        flywheel.setRunMode(MotorEx.RunMode.VelocityControl);
        flywheel.setVeloCoefficients(
            ShooterConstants.kP,
            ShooterConstants.kI,
            ShooterConstants.kD
        );
        flywheel.setFeedforwardCoefficients(
            ShooterConstants.kF,
            ShooterConstants.kV
        );
        
        List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
        hubs.forEach(hub -> hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL));
        
        telemetry.addLine("Ready - Test Flywheel Direct");
        telemetry.addLine("A: ON | B: OFF");
        telemetry.addLine("X: 2600 RPM | Y: 3300 RPM");
        telemetry.update();
        
        waitForStart();
        
        while (!isStopRequested() && opModeIsActive()) {
            hubs.forEach(LynxModule::clearBulkCache);
            gamepadEx.readButtons();
            
            // ON/OFF
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.A)) {
                motorOn = true;
            }
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.B)) {
                motorOn = false;
                targetRpm = 0;
            }
            
            // Cambiar velocidad
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.X)) {
                targetRpm = 2600;
            }
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.Y)) {
                targetRpm = 3300;
            }
            
            // APLICAR VELOCIDAD CADA CICLO (como en FlywheelTuning)
            if (motorOn) {
                double countsPerSecond = targetRpm * ShooterConstants.RPM_TO_COUNTS;
                flywheel.setVelocity(countsPerSecond);
            } else {
                flywheel.setVelocity(0);
            }
            
            // Telemetría
            double currentRpm = flywheel.getVelocity() * ShooterConstants.COUNTS_TO_RPM;
            
            telemetry.addData("Motor", motorOn ? "ON" : "OFF");
            telemetry.addData("Target", "%.0f RPM", targetRpm);
            telemetry.addData("Actual", "%.0f RPM", currentRpm);
            telemetry.addData("Error", "%.0f RPM", targetRpm - currentRpm);
            telemetry.update();
        }
        
        flywheel.setVelocity(0);
    }
}