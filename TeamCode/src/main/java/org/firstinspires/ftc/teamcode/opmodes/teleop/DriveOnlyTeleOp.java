package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.drive.TeleOpDrive;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem.DriveMode;

/**
 * Simplified TeleOp OpMode for testing ONLY the drivetrain subsystem.
 * This allows testing the chassis without dependencies on other subsystems.
 */
@TeleOp(name = "🚗 Drive Only TeleOp", group = "Testing")
public class DriveOnlyTeleOp extends CommandOpMode {

    private DriveSubsystem drive;
    private Follower follower;
    private GamepadEx driverGamepad;

    @Override
    public void initialize() {
        // Initialize Follower and Drive Subsystem
        follower = DriveConstants.createFollower(hardwareMap);
        drive = new DriveSubsystem(follower);

        // Initialize Gamepad
        driverGamepad = new GamepadEx(gamepad1);

        // Initialize Starting Pose (Required for Follower)
        drive.setStartingPose(new Pose(0, 0, 0));

        // Create and set default drive command
        TeleOpDrive driveCommand = new TeleOpDrive(
                drive,
                () -> driverGamepad.getLeftY(),
                () -> -driverGamepad.getLeftX(),
                () -> -driverGamepad.getRightX());

        // Register subsystem and set default command
        register(drive);
        drive.setDefaultCommand(driveCommand);

        // Configure driver bindings
        configureDriverBindings();
    }

    @Override
    public void run() {
        super.run();
        // Update telemetry
        updateTelemetry();
    }

    /**
     * Configure driver gamepad bindings for drive controls.
     */
    private void configureDriverBindings() {
        // Left Bumper: Toggle Slow Mode
        // If in SLOW, go to NORMAL. If in any other mode, go to SLOW.
        driverGamepad.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
                .whenPressed(new InstantCommand(() -> {
                    if (drive.getMode() == DriveMode.SLOW) {
                        drive.setNormalMode();
                    } else {
                        drive.setSlowMode();
                    }
                }));

        // Right Bumper: Toggle Fast Mode
        // If in FAST, go to NORMAL. If in any other mode, go to FAST.
        driverGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
                .whenPressed(new InstantCommand(() -> {
                    if (drive.getMode() == DriveMode.FAST) {
                        drive.setNormalMode();
                    } else {
                        drive.setFastMode();
                    }
                }));

        // Back: Toggle Field/Robot Centric
        driverGamepad.getGamepadButton(GamepadKeys.Button.BACK)
                .whenPressed(new InstantCommand(() -> {
                    if (drive.isRobotCentric()) {
                        drive.setFieldCentric();
                    } else {
                        drive.setRobotCentric();
                    }
                }));

        // Start: Reset heading to 0
        driverGamepad.getGamepadButton(GamepadKeys.Button.START)
                .whenPressed(new InstantCommand(() -> {
                    drive.setStartingPose(new Pose(
                            drive.getPose().getX(),
                            drive.getPose().getY(),
                            0));
                }));
    }

    /**
     * Updates the Driver Station telemetry with drive status.
     */
    private void updateTelemetry() {
        telemetry.addData("=== DRIVE ONLY TEST MODE ===", "");
        telemetry.addData("", "");

        // Drive mode and orientation
        telemetry.addData("Drive Mode", drive.getMode());
        telemetry.addData("Orientation", drive.isRobotCentric() ? "Robot Centric" : "Field Centric");
        telemetry.addData("", "");

        // Current pose
        Pose currentPose = drive.getPose();
        telemetry.addData("Position X", String.format("%.2f", currentPose.getX()));
        telemetry.addData("Position Y", String.format("%.2f", currentPose.getY()));
        telemetry.addData("Heading", String.format("%.2f°", Math.toDegrees(currentPose.getHeading())));
        telemetry.addData("", "");

        // Gamepad inputs (for debugging)
        telemetry.addData("Left Stick Y", String.format("%.2f", driverGamepad.getLeftY()));
        telemetry.addData("Left Stick X", String.format("%.2f", driverGamepad.getLeftX()));
        telemetry.addData("Right Stick X", String.format("%.2f", driverGamepad.getRightX()));
        telemetry.addData("", "");

        // Controls guide
        telemetry.addData("=== CONTROLS ===", "");
        telemetry.addData("Left Stick", "Translate (Forward/Strafe)");
        telemetry.addData("Right Stick X", "Rotate");
        telemetry.addData("Left Bumper", "Toggle Slow Mode");
        telemetry.addData("Right Bumper", "Toggle Fast Mode");
        telemetry.addData("Back", "Toggle Field/Robot Centric");
        telemetry.addData("Start", "Reset Heading to 0°");

        telemetry.update();
    }
}
