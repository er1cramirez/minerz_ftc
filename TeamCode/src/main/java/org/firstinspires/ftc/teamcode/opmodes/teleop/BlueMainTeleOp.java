package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.ConditionalCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.ParallelCommandGroup;
import com.seattlesolvers.solverslib.command.button.Trigger;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.drive.TeleOpDrive;
import org.firstinspires.ftc.teamcode.commands.sequences.IntakeAndIndexCommand;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem.DriveMode;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.util.UserGamepadFeedback;

@TeleOp(name = "🔵 Blue Alliance TeleOp", group = "TeleOp")
public class BlueMainTeleOp extends CommandOpMode {

    private Boolean isRedAlliance = false;//Used mainly for the autoaiming Target Tag ID

    private DriveSubsystem drive;
    private Follower follower;

    private IntakeSubsystem intake;
    private SpindexerSubsystem spindex;



    //Commands
    private IntakeAndIndexCommand intakeCommand;


    private GamepadEx driverGamepad;
    private GamepadEx operatorGamepad;

    @Override
    public void initialize() {
        // Initialize Follower and Subsystem
        follower = DriveConstants.createFollower(hardwareMap);
        drive = new DriveSubsystem(follower);

        intake = new IntakeSubsystem(hardwareMap);
        spindex = new SpindexerSubsystem(hardwareMap);

        // Initialize Gamepad
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);

        // Create and set default drive command
        TeleOpDrive driveCommand = new TeleOpDrive(
                drive,
                () -> driverGamepad.getLeftY(),
                () -> driverGamepad.getLeftX(),
                () -> driverGamepad.getRightX()
        );
        drive.setDefaultCommand(driveCommand);
        register(drive, intake, spindex);

        spindex.moveToIntakePosition(0);
        // Configure Bindings
        configureDriverBindings();
        configureOperatorBindings();
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

    private void configureOperatorBindings() {
        Trigger autoIntaketrigger = new Trigger(() -> operatorGamepad.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER)>0.2);
        autoIntaketrigger
                .whileActiveContinuous(
                new ConditionalCommand(
                    intakeCommand = new IntakeAndIndexCommand(intake, spindex),
                    new ParallelCommandGroup(
                        new InstantCommand(() -> intakeCommand.cancel()),
                        new InstantCommand(() -> gamepad2.runRumbleEffect(UserGamepadFeedback.test)),
                        new InstantCommand(() -> gamepad2.runLedEffect(UserGamepadFeedback.readyToShootLedEff))
                    ),
                    () -> (spindex.getState() == SpindexerSubsystem.SpindexerState.AT_INTAKE
                            && (intakeCommand == null || !intakeCommand.isScheduled())
                            && !spindex.isFull())
            ))
            .whenInactive(new InstantCommand(intake::stop));

        Trigger manualOuttakeTrigger = new Trigger(() -> operatorGamepad.getTrigger(GamepadKeys.Trigger.LEFT_TRIGGER)>0.2);
        manualOuttakeTrigger
                .whenActive(new InstantCommand(intake::outtake))
                .whenInactive(new InstantCommand(intake::stop));

        operatorGamepad.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new InstantCommand(
                    () ->
                        spindex.moveToIntakePosition((spindex.getCurrentSlotIndex()+1)% 3)
            ));
    }
}
