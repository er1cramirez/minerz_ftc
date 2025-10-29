package org.firstinspires.ftc.teamcode.opModes;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.button.Button;
import com.seattlesolvers.solverslib.command.button.GamepadButton;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.commands.DefaultDriveCommand;
import org.firstinspires.ftc.teamcode.commands.IntakeCommand;
import org.firstinspires.ftc.teamcode.constants.IOConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;

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
 */
@TeleOp(name = "Main TeleOp", group = "Competition")
public class MainTeleOp extends CommandOpMode {

    // Subsystems
    private DriveSubsystem driveSubsystem;
    private IntakeSubsystem intakeSubsystem;

    // Commands
    private DefaultDriveCommand defaultDriveCommand;
    private IntakeCommand intakeCommand;

    // Gamepads
    private GamepadEx driverGamepad;
    private GamepadEx operatorGamepad;

    // Buttons
    private Button resetGyroButton;

    @Override
    public void initialize() {
        // Initialize gamepads
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);

        // Initialize subsystems
        driveSubsystem = new DriveSubsystem(hardwareMap);
        intakeSubsystem = new IntakeSubsystem(hardwareMap);

        // Create commands
        defaultDriveCommand = new DefaultDriveCommand(driveSubsystem, driverGamepad);
        intakeCommand = new IntakeCommand(intakeSubsystem, operatorGamepad);

        // Configure button bindings
        configureButtonBindings();

        // Register subsystems
        register(driveSubsystem, intakeSubsystem);

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
        // Reset gyro when back button is pressed
        resetGyroButton = new GamepadButton(driverGamepad, IOConstants.Driver.RESET_GYRO_BUTTON)
                .whenPressed(() -> {
                    driveSubsystem.resetGyro();
                    gamepad1.rumble(200); // Haptic feedback
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
        
        telemetry.update();
    }
}
