package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.drive.TeleOpDrive;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem.DriveMode;

@TeleOp(name = "🔵 Blue Alliance TeleOp", group = "TeleOp")
public class BlueMainTeleOp extends CommandOpMode {

    private Boolean isRedAlliance = false;//Used mainly for the autoaiming Target Tag ID

    private DriveSubsystem drive;
    private GamepadEx driverGamepad;
    private Follower follower;

    @Override
    public void initialize() {
        // Initialize Follower and Subsystem
        follower = DriveConstants.createFollower(hardwareMap);
        drive = new DriveSubsystem(follower);

        // Initialize Gamepad
        driverGamepad = new GamepadEx(gamepad1);

        // Create and set default drive command
        TeleOpDrive driveCommand = new TeleOpDrive(
                drive,
                () -> driverGamepad.getLeftY(),
                () -> driverGamepad.getLeftX(),
                () -> driverGamepad.getRightX()
        );
        drive.setDefaultCommand(driveCommand);
        register(drive);

        // Configure Bindings
        configureDriverBindings();
    }

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
    }
}
