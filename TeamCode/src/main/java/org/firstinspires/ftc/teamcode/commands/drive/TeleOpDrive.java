package org.firstinspires.ftc.teamcode.commands.drive;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;

import java.util.function.DoubleSupplier;

public class TeleOpDrive extends CommandBase {
    private final DriveSubsystem drive;

    private final DoubleSupplier forwardSupplier;
    private final DoubleSupplier strafeSupplier;
    private final DoubleSupplier rotationSupplier;

    /**
     * @param drive            DriveSubsystem
     * @param forwardSupplier  DoubleSupplier for forward speed (-1 to 1)
     * @param strafeSupplier   DoubleSupplier for strafe speed (-1 to 1)
     * @param rotationSupplier DoubleSupplier for rotation speed (-1 to 1)
     */
    public TeleOpDrive(
            DriveSubsystem drive,
            DoubleSupplier forwardSupplier,
            DoubleSupplier strafeSupplier,
            DoubleSupplier rotationSupplier) {
        this.drive = drive;
        this.forwardSupplier = forwardSupplier;
        this.strafeSupplier = strafeSupplier;
        this.rotationSupplier = rotationSupplier;

        addRequirements(drive);
    }

    @Override
    public void initialize() {
        drive.startTeleOpMode();
    }

    @Override
    public void execute() {
        double forward = forwardSupplier.getAsDouble();
        double strafe = strafeSupplier.getAsDouble();
        double rotation = rotationSupplier.getAsDouble();

        // Get speed multiplier from DriveSubsystem
        double speedMultiplier = getSpeedMultiplier();
        forward *= speedMultiplier;
        strafe *= speedMultiplier;
        rotation *= speedMultiplier;


        // Get robot/field centric mode from DriveSubsystem
        boolean isRobotCentric = drive.isRobotCentric();

        // Send commands to drivetrain
        drive.setTeleOpDrive(forward, strafe, rotation, isRobotCentric);
    }

    /**
     * Gets the speed multiplier based on the current mode of the subsystem.
     */
    private double getSpeedMultiplier() {
        switch (drive.getMode()) {
            case SLOW:
                return DriveConstants.SLOW_SPEED;
            case FAST:
                return DriveConstants.TURBO_SPEED;
            case NORMAL:
            default:
                return DriveConstants.NORMAL_SPEED;
        }
    }
}
