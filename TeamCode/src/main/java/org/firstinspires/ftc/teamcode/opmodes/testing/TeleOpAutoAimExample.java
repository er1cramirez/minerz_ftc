package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.RunCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.AutoAimCommand;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;

/**
 * Example TeleOp demonstrating AutoAimCommand usage.
 * 
 * <h2>Control Scheme (Operator Gamepad):</h2>
 * <ul>
 *   <li><b>X Button:</b> Toggle Auto-Aim ON/OFF</li>
 *   <li><b>Left Bumper (hold):</b> Manual turret left (interrupts auto-aim)</li>
 *   <li><b>Right Bumper (hold):</b> Manual turret right (interrupts auto-aim)</li>
 *   <li><b>Y Button:</b> Return turret to home (0°)</li>
 * </ul>
 */
@TeleOp(name = "TeleOp - AutoAim Example", group = "Examples")
public class TeleOpAutoAimExample extends CommandOpMode {
    
    // Subsystems
    private TurretSubsystem turret;
    private VisionSubsystem vision;
    
    // Gamepads
    private GamepadEx driverGamepad;
    private GamepadEx operatorGamepad;
    
    // Alliance selection
    private Alliance alliance = Alliance.BLUE;
    
    // Feedback state
    private boolean wasLockedLastFrame = false;
    
    @Override
    public void initialize() {
        // ===== SUBSYSTEMS =====
        turret = new TurretSubsystem(hardwareMap);
        vision = new VisionSubsystem(hardwareMap);
        
        vision.enable();
        turret.setCurrentPositionAsHome();
        
        // ===== GAMEPADS =====
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);
        
        // ===== BUTTON BINDINGS =====
        configureButtonBindings();
        
        // ===== TELEMETRY =====
        telemetry.addLine("=== TeleOp Ready ===");
        telemetry.addData("Alliance", alliance.name());
        telemetry.update();
    }
    
    private void configureButtonBindings() {
        // ----- AUTO-AIM TOGGLE -----
        operatorGamepad.getGamepadButton(GamepadKeys.Button.X)
            .toggleWhenPressed(new AutoAimCommand(turret, vision, alliance));
        
        // ----- MANUAL TURRET CONTROL -----
        // These require turret, so they automatically interrupt auto-aim
        operatorGamepad.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
            .whileHeld(new RunCommand(() -> turret.setManualPower(0.4), turret))
            .whenReleased(new InstantCommand(turret::stop, turret));
        
        operatorGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
            .whileHeld(new RunCommand(() -> turret.setManualPower(-0.4), turret))
            .whenReleased(new InstantCommand(turret::stop, turret));
        
        // ----- TURRET HOME -----
        operatorGamepad.getGamepadButton(GamepadKeys.Button.Y)
            .whenPressed(new InstantCommand(() -> turret.setTargetPosition(0), turret));
    }
    
    @Override
    public void run() {
        super.run();
        updateFeedback();
        updateTelemetry();
    }
    
    private void updateFeedback() {
        boolean isLocked = turret.isLockedOnTarget();
        
        if (isLocked && !wasLockedLastFrame) {
            operatorGamepad.gamepad.rumble(150, 150, 100);
        }
        
        wasLockedLastFrame = isLocked;
    }
    
    private void updateTelemetry() {
        telemetry.addLine("=== TURRET ===");
        telemetry.addData("State", turret.getStateName());
        telemetry.addData("Vision Tracking", turret.getVisionTrackingStateName());
        telemetry.addData("Locked", turret.isLockedOnTarget() ? "✓ YES" : "✗ NO");
        telemetry.addData("Position", "%.1f°", turret.getCurrentAngleDeg());
        telemetry.addData("Target", "%.1f°", turret.getTargetAngleDeg());
        telemetry.addData("Bearing", "%.1f°", turret.getLastValidBearing());
        
        telemetry.addLine("=== VISION ===");
        VisionSubsystem.RBE rbe = vision.getGoalRBE(alliance);
        if (rbe != null) {
            telemetry.addData("Range", "%.1f in", rbe.range);
            telemetry.addData("Bearing", "%.1f°", rbe.bearing);
        } else {
            telemetry.addData("Target", "NOT VISIBLE");
        }
        
        telemetry.addLine("=== CONTROLS ===");
        telemetry.addData("X", "Toggle Auto-Aim");
        telemetry.addData("LB/RB", "Manual Turret");
        telemetry.addData("Y", "Turret Home");
        
        telemetry.update();
    }
}
