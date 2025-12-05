package org.firstinspires.ftc.teamcode.opModes;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.button.GamepadButton;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.DefaultDriveCommand;
import org.firstinspires.ftc.teamcode.commands.IntakeCommand;
import org.firstinspires.ftc.teamcode.constants.IOConstants;
import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Main TeleOp mode for the robot.
 * 
 * Driver (Gamepad 1):
 * - Left stick: Drive forward/backward and strafe
 * - Right stick X: Rotate
 * - Left bumper (hold): Precision mode (35% speed)
 * - Right bumper (hold): Turbo mode (100% speed)
 * - Back button: Reset gyro
 * 
 * Operator (Gamepad 2):
 * - Right trigger: Run intake
 * - Left trigger: Run outtake
 * - X button: Indexer advance (cycles through all 6 positions)
 * - Y button: Indexer reverse
 * - A button: Toggle shooter idle/stop
 * - B button: Shooter spin up to full speed
 * - Right bumper: Trigger ejector
 */
@TeleOp(name = "Main TeleOp", group = "Competition")
public class MainTeleOp extends CommandOpMode {

    // Subsystems
    private DriveSubsystem driveSubsystem;
    private IntakeSubsystem intakeSubsystem;
    private ArtifactIndexer indexer;
    private EjectorSubsystem ejector;
    private ShooterSubsystem shooter;

    // Commands
    private DefaultDriveCommand defaultDriveCommand;
    private IntakeCommand intakeCommand;

    // Gamepads
    private GamepadEx driverGamepad;
    private GamepadEx operatorGamepad;

    @Override
    public void initialize() {
        // Initialize gamepads
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);

        // Initialize subsystems
        driveSubsystem = new DriveSubsystem(hardwareMap);
        intakeSubsystem = new IntakeSubsystem(hardwareMap);
        indexer = new ArtifactIndexer(hardwareMap, "indexer_servo");
        ejector = new EjectorSubsystem(hardwareMap);
        shooter = new ShooterSubsystem(hardwareMap);

        // Create commands
        defaultDriveCommand = new DefaultDriveCommand(driveSubsystem, driverGamepad);
        intakeCommand = new IntakeCommand(intakeSubsystem, operatorGamepad);

        // Configure button bindings
        configureButtonBindings();

        // Register subsystems
        register(driveSubsystem, intakeSubsystem, indexer, ejector, shooter);

        // Set default commands
        driveSubsystem.setDefaultCommand(defaultDriveCommand);
        intakeSubsystem.setDefaultCommand(intakeCommand);

        // Telemetry
        telemetry.addLine("Robot Initialized");
        telemetry.addLine("Ready to start!");
        telemetry.update();
    }

    /**
     * Configures button bindings for commands.
     */
    private void configureButtonBindings() {
        // ==================== Driver Controls ====================
        
        // Reset gyro when back button is pressed
        new GamepadButton(driverGamepad, IOConstants.Driver.RESET_GYRO_BUTTON)
                .whenPressed(() -> {
                    driveSubsystem.resetGyro();
                    gamepad1.rumble(200);
                });
        
        // ==================== Indexer Controls ====================
        
        // OLD SIMPLE VERSION - Advance to next intake position (turnTrigger)
        new GamepadButton(operatorGamepad, IOConstants.Operator.INDEXER_ADVANCE_BUTTON)
                .whenPressed(() -> {
                    indexer.turnTrigger();
                    gamepad2.rumble(50);
                });
        
        // OLD SIMPLE VERSION - Toggle outtake (feed)
        new GamepadButton(operatorGamepad, IOConstants.Operator.INDEXER_REVERSE_BUTTON)
                .whenPressed(() -> {
                    indexer.feed();
                    gamepad2.rumble(50);
                });
        
        // TESTING: Use D-pad to test raw servo positions
        new GamepadButton(operatorGamepad, GamepadKeys.Button.DPAD_UP)
                .whenPressed(() -> {
                    indexer.setRawPosition(0.0);  // 0°
                    gamepad2.rumble(50);
                });
        
        new GamepadButton(operatorGamepad, GamepadKeys.Button.DPAD_RIGHT)
                .whenPressed(() -> {
                    indexer.setRawPosition(0.4);  // 120°
                    gamepad2.rumble(50);
                });
        
        new GamepadButton(operatorGamepad, GamepadKeys.Button.DPAD_DOWN)
                .whenPressed(() -> {
                    indexer.setRawPosition(0.8);  // 240°
                    gamepad2.rumble(50);
                });
        
        new GamepadButton(operatorGamepad, GamepadKeys.Button.DPAD_LEFT)
                .whenPressed(() -> {
                    indexer.setRawPosition(1.0);  // 300°
                    gamepad2.rumble(50);
                });
        
        // ==================== Shooter Controls ====================
        
        // Toggle shooter between idle and stopped (A button)
        new GamepadButton(operatorGamepad, IOConstants.Operator.SHOOTER_TOGGLE)
                .whenPressed(() -> {
                    if (shooter.isRunning()) {
                        shooter.stop();
                        gamepad2.rumble(100);
                    } else {
                        shooter.startIdle();
                        gamepad2.rumble(200);
                    }
                });
        
        // Toggle spin up to full velocity (B button) - changed from hold to toggle for testing
        new GamepadButton(operatorGamepad, IOConstants.Operator.SHOOTER_SPINUP)
                .whenPressed(() -> {
                    if (shooter.isReady() || shooter.getState() == ShooterSubsystem.ShooterState.SPINNING_UP) {
                        // Already at full speed, return to idle
                        shooter.returnToIdle();
                        gamepad2.rumble(100);
                    } else {
                        // Spin up to full speed
                        shooter.spinUp();
                        gamepad2.rumble(200);
                    }
                });
        
        // ==================== Ejector Controls ====================
        
        // Trigger ejector cycle (eject -> stow)
        new GamepadButton(operatorGamepad, IOConstants.Operator.EJECT_BUTTON)
                .whenPressed(() -> {
                    ejector.ejectAndStow();
                    gamepad2.rumble(100);
                });
    }

    @Override
    public void run() {
        // Run the command scheduler
        super.run();

        // Update telemetry
        updateTelemetry();
    }

    /**
     * Updates telemetry with robot status.
     */
    private void updateTelemetry() {
        // Drive information
        telemetry.addData("X Position", "%.2f in", driveSubsystem.getX());
        telemetry.addData("Y Position", "%.2f in", driveSubsystem.getY());
        telemetry.addData("Heading", "%.2f deg", Math.toDegrees(driveSubsystem.getHeading()));
        
        // Speed mode indicator
        String speedMode = "Normal (70%)";
        if (driverGamepad.getButton(IOConstants.Driver.PRECISION_MODE_BUTTON)) {
            speedMode = "Precision (35%)";
        } else if (driverGamepad.getButton(IOConstants.Driver.TURBO_MODE_BUTTON)) {
            speedMode = "Turbo (100%)";
        }
        telemetry.addData("Speed Mode", speedMode);
        
        // Intake status
        telemetry.addData("Intake", intakeSubsystem.isRunning() ? "Running" : "Stopped");
        
        // Indexer status
        telemetry.addData("Indexer", indexer.getDebugInfo());
        telemetry.addData("Balls Loaded", indexer.getBallCount());
        
        // Shooter status
        telemetry.addData("Shooter State", shooter.getState().name());
        telemetry.addData("Shooter Velocity", "%.0f / %.0f", 
                shooter.getVelocity(), shooter.getTargetVelocity());
        telemetry.addData("Shooter Ready", shooter.isReady() ? "YES" : "NO");
        
        // Ejector status
        telemetry.addData("Ejector", ejector.isStowed() ? "Stowed" : "Ejecting");
        
        telemetry.update();
    }
}
