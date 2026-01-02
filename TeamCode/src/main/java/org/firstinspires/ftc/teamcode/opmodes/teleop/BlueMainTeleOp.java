package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.Command;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.ConditionalCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.ParallelCommandGroup;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.button.Trigger;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.drive.TeleOpDrive;
import org.firstinspires.ftc.teamcode.commands.ejector.EjectCycleCommand;
import org.firstinspires.ftc.teamcode.commands.flywheel.SpinUpCommand;
import org.firstinspires.ftc.teamcode.commands.flywheel.WarmUpToIdleCommand;
import org.firstinspires.ftc.teamcode.commands.sequences.IntakeAndIndexCommand;
import org.firstinspires.ftc.teamcode.commands.sequences.ShootingSequence;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.constants.FlywheelConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem.DriveMode;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.telemetry.TelemetryHelper;
import org.firstinspires.ftc.teamcode.util.UserGamepadFeedback;

@TeleOp(name = "🔵 Blue Alliance TeleOp", group = "TeleOp")
public class BlueMainTeleOp extends CommandOpMode {

    private Boolean isRedAlliance = false;// Used mainly for the autoaiming Target Tag ID

    private DriveSubsystem drive;
    private Follower follower;

    private IntakeSubsystem intake;
    private SpindexerSubsystem spindex;

    // TODO: Add these subsystems when integrated
    // private TurretSubsystem turret;
    private FlywheelSubsystem flywheel;
    private EjectorSubsystem ejector;
    // private VisionSubsystem vision;

    // Commands
    private Command intakeCommand;
    private Command shotSeq;
    private Command fastShootSeq;

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
        flywheel = new FlywheelSubsystem(hardwareMap);
        ejector = new EjectorSubsystem(hardwareMap);
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
                () -> driverGamepad.getRightX());
        drive.setDefaultCommand(driveCommand);
        register(drive, intake, spindex, flywheel, ejector);
        // TODO: register(turret, flywheel, ejector, vision);

        // Configure Bindings
        configureDriverBindings();
        configureOperatorBindings();
    }


    @Override
    public void run() {
        super.run();
        // Update telemetry
        updateTelemetry();
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
        configureIntakeBindings();
        configureShootingBindings();
    }

    private void configureIntakeBindings() {
        Trigger autoIntaketrigger = new Trigger(
                () -> operatorGamepad.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER) > 0.2);
        autoIntaketrigger.whileActiveContinuous(
                new ConditionalCommand(
                        new SequentialCommandGroup(
                            intakeCommand = new IntakeAndIndexCommand(intake, spindex),
                            new ConditionalCommand(
                                    new ConditionalCommand(
                                            new InstantCommand(() -> UserGamepadFeedback.playGreenBall(gamepad2)),
                                            new InstantCommand(() -> UserGamepadFeedback.playPurpleBall(gamepad2)),
                                            () -> spindex.getCurrentSlotState() == SpindexerSubsystem.SlotState.GREEN),
                                    new InstantCommand(),
                                    () -> spindex.getCurrentSlotState() != SpindexerSubsystem.SlotState.UNKNOWN)),
                        new ParallelCommandGroup(
                                new InstantCommand(() -> intakeCommand.cancel()),
                                new InstantCommand(() -> UserGamepadFeedback.playWarning(gamepad2))),
                        () -> (spindex.getState() == SpindexerSubsystem.SpindexerState.AT_INTAKE
                                && (intakeCommand == null || !intakeCommand.isScheduled())
                                && !spindex.isFull())))
                .whenInactive(new InstantCommand(intake::stop));

        Trigger manualOuttakeTrigger = new Trigger(
                () -> operatorGamepad.getTrigger(GamepadKeys.Trigger.LEFT_TRIGGER) > 0.2);
        manualOuttakeTrigger
                .whenActive(new InstantCommand(intake::outtake))
                .whenInactive(new InstantCommand(intake::stop));

        operatorGamepad.getGamepadButton(GamepadKeys.Button.A)
                .whenPressed(new InstantCommand(
                        () -> spindex.moveToIntakePosition((spindex.getCurrentSlotIndex() + 1) % 3)));
    }

    private void configureShootingBindings() {
        // Fast shoot sequence
        fastShootSeq = new ConditionalCommand(
                // new SequentialCommandGroup(
                // new InstantCommand(() -> spindex.moveToOuttakePosition(1)),
                // new ConditionalCommand(
                // new SpinUpCommand(flywheel,
                // FlywheelConstants.interpolateRPMForDistance(100)),
                // new SequentialCommandGroup(
                // new WarmUpToIdleCommand(flywheel),
                // new SpinUpCommand(flywheel,
                // FlywheelConstants.interpolateRPMForDistance(100))),
                // () -> (flywheel.isIdleSpin() || flywheel.isAtSpeed())),
                // DEBUG: Assign and track placeholder
                shotSeq = new ShootingSequence(spindex, flywheel, ejector, SpindexerSubsystem.ShootingStrategy.FASTEST),
                // Spin down to IDLE_SPIN after shooting
                // new SpinUpCommand(flywheel, FlywheelConstants.IDLE_SPIN_RPM, false),
                // new InstantCommand(flywheel::setIdleSpin)),
                // If spindexer is empty, play warning
                new InstantCommand(() -> UserGamepadFeedback.playWarning(gamepad2)),
                // Only shoot if there are balls
                () -> (spindex.getFilledSlotCount() > 0)
         && (shotSeq == null || !shotSeq.isScheduled()));
        operatorGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
                .whenPressed(fastShootSeq);
//                        new ShootingSequence(spindex, flywheel, ejector, SpindexerSubsystem.ShootingStrategy.FASTEST));

        // Left Bumper for ordered sequence

        // BackUp Manual Mode
        operatorGamepad.getGamepadButton(GamepadKeys.Button.B)
                .whenPressed(new InstantCommand(
                        () -> spindex.moveToOuttakePosition((spindex.getCurrentSlotIndex() + 1) % 3)));

        operatorGamepad.getGamepadButton(GamepadKeys.Button.Y)
                .whenPressed(
                        new ConditionalCommand(
                                new EjectCycleCommand(ejector),
                                new InstantCommand(() -> UserGamepadFeedback.playWarning(gamepad2)),
                                () -> (spindex.getState() == SpindexerSubsystem.SpindexerState.AT_OUTTAKE
                                        && flywheel.isAtSpeed())));

        // Flywheel Manual controls
        // Back: Toggle between IDLE_SPIN and 2600 RPM
        // Start: Toggle between IDLE_SPIN and 3300 RPM
        // Back && Start: Emergency stop
        operatorGamepad.getGamepadButton(GamepadKeys.Button.BACK)
                .whenPressed(new ConditionalCommand(
                        new WarmUpToIdleCommand(flywheel), // If IDLE -> WarmUp to IDLE_SPIN
                        new ConditionalCommand(
                                new SpinUpCommand(flywheel, 2600), // If IDLE_SPIN -> SpinUp to 2600
                                new SequentialCommandGroup( // If AT_SPEED -> SpinDown to IDLE_SPIN
                                        new SpinUpCommand(flywheel, FlywheelConstants.IDLE_SPIN_RPM, false),
                                        new InstantCommand(flywheel::setIdleSpin)),
                                () -> flywheel.isIdleSpin()),
                        () -> flywheel.isIdle()));

        operatorGamepad.getGamepadButton(GamepadKeys.Button.START)
                .whenPressed(new ConditionalCommand(
                        new WarmUpToIdleCommand(flywheel), // If IDLE -> WarmUp to IDLE_SPIN
                        new ConditionalCommand(
                                new SpinUpCommand(flywheel, 3300), // If IDLE_SPIN -> SpinUp to 3300
                                new SequentialCommandGroup( // If AT_SPEED -> SpinDown to IDLE_SPIN
                                        new SpinUpCommand(flywheel, FlywheelConstants.IDLE_SPIN_RPM, false),
                                        new InstantCommand(flywheel::setIdleSpin)),
                                () -> flywheel.isIdleSpin()),
                        () -> flywheel.isIdle()));

        operatorGamepad.getGamepadButton(GamepadKeys.Button.BACK)
                .and(operatorGamepad.getGamepadButton(GamepadKeys.Button.START))
                .whenActive(new InstantCommand(flywheel::stop));

    }

    /**
     * Updates the Driver Station telemetry with robot status.
     */
    private void updateTelemetry() {
        telemetryHelper.begin();

        // ====== HEADER ======
        telemetryHelper.addHeader("🔵 BLUE TELEOP MinerZ");
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
        // Debug: Show filled slot count
        telemetryHelper.addLine("   📊 Filled Slots: " + spindex.getFilledSlotCount());
        telemetryHelper.addSpace();

        // ====== SHOOTER SECTION ======
        telemetryHelper.addSectionHeader("SHOOTER", "#FF6347");
        // TODO: Uncomment when Turret is integrated
        // telemetryHelper.addBigLine(turret.getCompactStatus());
        telemetryHelper.addLine("🔄 Turret: [NOT INTEGRATED]", "gray");

        telemetryHelper.addBigLine(flywheel.getCompactStatus());
        // Debug: Detailed flywheel state
        telemetryHelper.addLine("   State: " + flywheel.getStateName() +
                " | Ready: " + flywheel.isReadyToShoot() +
                " | Stable: " + (flywheel.getStabilityTimeMs() > 0));
        telemetryHelper.addLine("📤 Ejector: " + ejector.getState());
        // telemetryHelper.addLine("📤 Ejector: [NOT INTEGRATED]", "gray");
        telemetryHelper.addSpace();

        // ====== DEBUG SECTION ======
        telemetryHelper.addSectionHeader("DEBUG", "#FF00FF");

        // Debug command status
        String seqStatus = (fastShootSeq == null) ? "NULL" : (fastShootSeq.isScheduled() ? "RUNNING" : "IDLE");
        telemetryHelper.addLine("🎯 DebugCmd: " + seqStatus);

        // Debug shooting order
        java.util.List<Integer> order = spindex.getShootingOrder(SpindexerSubsystem.ShootingStrategy.FASTEST);
        telemetryHelper.addLine("📋 ShootOrder: " + order.toString() + " (size=" + order.size() + ")");

        // Debug slot states
        telemetryHelper.addLine("🔢 Slots: [" +
                spindex.getSlotState(0) + ", " +
                spindex.getSlotState(1) + ", " +
                spindex.getSlotState(2) + "]");

        // ====== SYSTEM SECTION ======
        telemetryHelper.addSectionHeader("SYSTEM", "#808080");
        double voltage = flywheel.getBatteryVoltage();
        telemetryHelper.addSystemLine(voltage);

        telemetryHelper.end();
    }
}
