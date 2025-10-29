package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.constants.IOConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Default drive command for teleoperated control.
 * 
 * Features:
 * - Normal speed: 70% (base)
 * - Precision mode: 35% (hold left bumper)
 * - Turbo mode: 100% (hold right bumper)
 * - Robot-centric control by default
 * - Field-centric option available
 */
public class DefaultDriveCommand extends CommandBase {

    private final DriveSubsystem driveSubsystem;
    private final DoubleSupplier forwardSupplier;
    private final DoubleSupplier strafeSupplier;
    private final DoubleSupplier rotateSupplier;
    private final BooleanSupplier precisionModeSupplier;
    private final BooleanSupplier turboModeSupplier;
    private final BooleanSupplier fieldRelativeSupplier;

    /**
     * Creates a new DefaultDriveCommand.
     * 
     * @param driveSubsystem The drive subsystem to control
     * @param forwardSupplier Supplier for forward/backward input (left stick Y)
     * @param strafeSupplier Supplier for strafe input (left stick X)
     * @param rotateSupplier Supplier for rotation input (right stick X)
     * @param precisionModeSupplier Supplier for precision mode button (left bumper)
     * @param turboModeSupplier Supplier for turbo mode button (right bumper)
     * @param fieldRelativeSupplier Supplier for field-relative mode toggle
     */
    public DefaultDriveCommand(
            DriveSubsystem driveSubsystem,
            DoubleSupplier forwardSupplier,
            DoubleSupplier strafeSupplier,
            DoubleSupplier rotateSupplier,
            BooleanSupplier precisionModeSupplier,
            BooleanSupplier turboModeSupplier,
            BooleanSupplier fieldRelativeSupplier) {
        
        this.driveSubsystem = driveSubsystem;
        this.forwardSupplier = forwardSupplier;
        this.strafeSupplier = strafeSupplier;
        this.rotateSupplier = rotateSupplier;
        this.precisionModeSupplier = precisionModeSupplier;
        this.turboModeSupplier = turboModeSupplier;
        this.fieldRelativeSupplier = fieldRelativeSupplier;
        
        addRequirements(driveSubsystem);
    }

    /**
     * Convenience constructor using GamepadEx.
     * 
     * @param driveSubsystem The drive subsystem to control
     * @param gamepad The gamepad for input
     */
    public DefaultDriveCommand(DriveSubsystem driveSubsystem, GamepadEx gamepad) {
        this(
            driveSubsystem,
            () -> -gamepad.getLeftY(),
            () -> gamepad.getLeftX(),
            () -> gamepad.getRightX(),
            () -> gamepad.getButton(IOConstants.Driver.PRECISION_MODE_BUTTON),
            () -> gamepad.getButton(IOConstants.Driver.TURBO_MODE_BUTTON),
            () -> DriveConstants.DriveMode.DEFAULT_FIELD_RELATIVE
        );
    }

    @Override
    public void execute() {
        // Get raw inputs
        double forward = forwardSupplier.getAsDouble();
        double strafe = strafeSupplier.getAsDouble();
        double rotate = rotateSupplier.getAsDouble();
        
        // Determine speed multiplier based on mode
        double speedMultiplier;
        if (precisionModeSupplier.getAsBoolean()) {
            // Precision mode - slow and accurate
            speedMultiplier = DriveConstants.SpeedMultipliers.PRECISION_SPEED;
        } else if (turboModeSupplier.getAsBoolean()) {
            // Turbo mode - maximum speed
            speedMultiplier = DriveConstants.SpeedMultipliers.TURBO_SPEED;
        } else {
            // Normal mode - balanced speed
            speedMultiplier = DriveConstants.SpeedMultipliers.NORMAL_SPEED;
        }
        
        // Apply speed multiplier
        forward *= speedMultiplier;
        strafe *= speedMultiplier;
        rotate *= speedMultiplier;
        
        // Get field relative setting
        boolean fieldRelative = fieldRelativeSupplier.getAsBoolean();
        
        // Drive the robot
        driveSubsystem.drive(forward, strafe, rotate, fieldRelative);
    }

    @Override
    public void end(boolean interrupted) {
        // Stop the robot when command ends
        driveSubsystem.stop();
    }

    @Override
    public boolean isFinished() {
        // This command never finishes on its own (runs until interrupted)
        return false;
    }
}
