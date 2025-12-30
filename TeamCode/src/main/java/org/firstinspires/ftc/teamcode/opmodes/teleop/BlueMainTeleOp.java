package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
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
import org.firstinspires.ftc.teamcode.telemetry.TelemetryHelper;
import org.firstinspires.ftc.teamcode.util.UserGamepadFeedback;

@TeleOp(name = "🔵 Blue Alliance TeleOp", group = "TeleOp")
public class BlueMainTeleOp extends CommandOpMode {

    private Boolean isRedAlliance = false;//Used mainly for the autoaiming Target Tag ID

    private DriveSubsystem drive;
    private Follower follower;

    private IntakeSubsystem intake;
    private SpindexerSubsystem spindex;
    
    // TODO: Add these subsystems when integrated
    // private TurretSubsystem turret;
    // private FlywheelSubsystem flywheel;
    // private EjectorSubsystem ejector;
    // private VisionSubsystem vision;

    //Commands
    private IntakeAndIndexCommand intakeCommand;

    private GamepadEx driverGamepad;
    private GamepadEx operatorGamepad;
    
    // Telemetry
    private TelemetryHelper telemetryHelper;

    @Override
    public void initialize() {
        // Initialize Follower and Subsystem
        follower = DriveConstants.createFollower(hardwareMap);
        drive = new DriveSubsystem(follower);

        intake = new IntakeSubsystem(hardwareMap);
        spindex = new SpindexerSubsystem(hardwareMap);
        
        // TODO: Initialize when integrated
        // turret = new TurretSubsystem(hardwareMap);
        // flywheel = new FlywheelSubsystem(hardwareMap);
        // ejector = new EjectorSubsystem(hardwareMap);
        // vision = new VisionSubsystem(hardwareMap);

        // Initialize Gamepad
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);
        
        // Initialize Telemetry with HTML mode
        telemetryHelper = new TelemetryHelper(telemetry);
        telemetryHelper.setDisplayMode(TelemetryHelper.DisplayMode.HTML);

        // Create and set default drive command
        TeleOpDrive driveCommand = new TeleOpDrive(
                drive,
                () -> driverGamepad.getLeftY(),
                () -> driverGamepad.getLeftX(),
                () -> driverGamepad.getRightX()
        );
        drive.setDefaultCommand(driveCommand);
        register(drive, intake, spindex);
        // TODO: register(turret, flywheel, ejector, vision);

        spindex.moveToIntakePosition(0);
        // Configure Bindings
        configureDriverBindings();
        configureOperatorBindings();
    }
    
    @Override
    public void run() {
        super.run();

        // Update telemetry
        updateTelemetry();
//        telemetry.update();
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
                        new InstantCommand(() -> UserGamepadFeedback.playWarning(gamepad2))
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
    
    /**
     * Updates the Driver Station telemetry with robot status.
     */
    private void updateTelemetry() {
        telemetryHelper.begin();
        
        // ====== HEADER ======
        telemetryHelper.addHeader("🔵 BLUE TELEOP MineZ");
        telemetryHelper.addSpace();
        
        // ====== DRIVE SECTION ======
        telemetryHelper.addSectionHeader("DRIVE", "#00BFFF");
        telemetryHelper.addBigLine(drive.getCompactStatus());
        telemetryHelper.addSpace();
        
        // ====== INDEXER SECTION ======
        telemetryHelper.addSectionHeader("INDEXER", "#FFD700");
        telemetryHelper.addBigLine(spindex.getCompactStatus());
        // Intake state
        String intakeIcon = intake.isActive() ? "▶" : (intake.isIdle() ? "⏹" : "◀");
        telemetryHelper.addLine("   " + intakeIcon + " Intake: " + intake.getState());
        telemetryHelper.addSpace();
        
        // ====== SHOOTER SECTION ======
        telemetryHelper.addSectionHeader("SHOOTER", "#FF6347");
        // TODO: Uncomment when Turret is integrated
        // telemetryHelper.addBigLine(turret.getCompactStatus());
        telemetryHelper.addLine("🔄 Turret: [NOT INTEGRATED]", "gray");
        
        // TODO: Uncomment when Flywheel is integrated
        // telemetryHelper.addBigLine(flywheel.getCompactStatus());
        telemetryHelper.addLine("🔥 Flywheel: [NOT INTEGRATED]", "gray");
        
        // TODO: Uncomment when Ejector is integrated
        // telemetryHelper.addLine("📤 Ejector: " + ejector.getState());
        telemetryHelper.addLine("📤 Ejector: [NOT INTEGRATED]", "gray");
        telemetryHelper.addSpace();
        
        // ====== SYSTEM SECTION ======
        telemetryHelper.addSectionHeader("SYSTEM", "#808080");
        // TODO: Get actual voltage from flywheel.getBatteryVoltage() when integrated
        double voltage = 12.5; // Placeholder
        telemetryHelper.addSystemLine(voltage);
        
        telemetryHelper.end();
    }
}

