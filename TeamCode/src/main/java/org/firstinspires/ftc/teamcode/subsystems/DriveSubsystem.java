package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.geometry.Rotation2d;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.Motor.ZeroPowerBehavior;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.constants.RobotConstants;

/**
 * Subsystem for mecanum drive with GoBilda Pinpoint odometry.
 * Uses INCHES for distance and RADIANS for angles.
 * Prepared for both TeleOp manual control and autonomous path following with Pedro Pathing.
 */
public class DriveSubsystem extends SubsystemBase {

    private final MecanumDrive drive;
    private final GoBildaPinpointDriver odo;

    // Individual motor references for potential direct control
    private final MotorEx frontLeftMotor;
    private final MotorEx frontRightMotor;
    private final MotorEx backLeftMotor;
    private final MotorEx backRightMotor;

    /**
     * Creates a new DriveSubsystem.
     *
     * @param hardwareMap The hardware map from the OpMode
     */
    public DriveSubsystem(HardwareMap hardwareMap) {
        // Initialize motors with proper configuration
        frontLeftMotor = new MotorEx(hardwareMap,
                RobotConstants.HardwareNames.FRONT_LEFT_MOTOR,
                Motor.GoBILDA.RPM_312);
        frontLeftMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);

        frontRightMotor = new MotorEx(hardwareMap,
                RobotConstants.HardwareNames.FRONT_RIGHT_MOTOR,
                Motor.GoBILDA.RPM_312);
        frontRightMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);

        backLeftMotor = new MotorEx(hardwareMap,
                RobotConstants.HardwareNames.BACK_LEFT_MOTOR,
                Motor.GoBILDA.RPM_312);
        backLeftMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);

        backRightMotor = new MotorEx(hardwareMap,
                RobotConstants.HardwareNames.BACK_RIGHT_MOTOR,
                Motor.GoBILDA.RPM_312);
        backRightMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);

        // Create mecanum drive
        drive = new MecanumDrive(frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor);

        // Initialize Pinpoint odometry
        odo = hardwareMap.get(GoBildaPinpointDriver.class,
                RobotConstants.HardwareNames.PINPOINT_ODOMETRY);

        configureOdometry();
    }

    /**
     * Configures the Pinpoint odometry system.
     */
    private void configureOdometry() {
        // Reset position and IMU
        odo.resetPosAndIMU();

        // Set encoder directions (adjust these if odometry is reading backwards)
        odo.setEncoderDirections(
                DriveConstants.Odometry.X_DIRECTION,
                DriveConstants.Odometry.Y_DIRECTION);

        // Set encoder resolution (pod type)
        odo.setEncoderResolution(DriveConstants.Odometry.POD_TYPE);

        // Set offsets from robot center (in inches)
        odo.setOffsets(
                DriveConstants.Odometry.X_OFFSET_INCHES,
                DriveConstants.Odometry.Y_OFFSET_INCHES,
                DriveConstants.Odometry.DISTANCE_UNIT);
    }

    /**
     * Drives the robot with specified inputs.
     *
     * @param forward The forward/backward speed (-1.0 to 1.0)
     * @param strafe The left/right strafe speed (-1.0 to 1.0)
     * @param rotate The rotation speed (-1.0 to 1.0)
     * @param fieldRelative Whether to use field-relative control
     */
    public void drive(double forward, double strafe, double rotate, boolean fieldRelative) {
        if (fieldRelative) {
            // SolversLib order: (strafe, forward, turn, gyroAngle, squareInputs)
            drive.driveFieldCentric(strafe, forward, rotate, getHeading(), true);
        } else {
            // SolversLib order: (strafe, forward, turn, squareInputs)
            drive.driveRobotCentric(strafe, forward, rotate, true);
        }
    }

    /**
     * Sets individual motor powers directly.
     * Useful for path following libraries like Pedro Pathing.
     *
     * @param frontLeft Front left motor power
     * @param frontRight Front right motor power
     * @param backLeft Back left motor power
     * @param backRight Back right motor power
     */
    public void setMotorPowers(double frontLeft, double frontRight, double backLeft, double backRight) {
        frontLeftMotor.set(frontLeft);
        frontRightMotor.set(frontRight);
        backLeftMotor.set(backLeft);
        backRightMotor.set(backRight);
    }

    /**
     * Stops all drive motors.
     */
    public void stop() {
        drive.stop();
    }

    /**
     * Resets the IMU/gyro heading to zero.
     */
    public void resetGyro() {
//        odo.resetIMU();
    }

    /**
     * Gets the robot's current heading in radians.
     *
     * @return The heading in radians (-π to π)
     */
    public double getHeading() {
        return odo.getHeading(AngleUnit.RADIANS);
    }

    /**
     * Gets the robot's current heading as a Rotation2d.
     *
     * @return The heading as Rotation2d
     */
    public Rotation2d getRotation2d() {
        return new Rotation2d(getHeading());
    }

    /**
     * Gets the robot's current pose from odometry.
     * Returns position in INCHES and heading in RADIANS.
     *
     * @return The current Pose2D (in inches and radians)
     */
    public Pose2D getPose() {
        return odo.getPosition();
    }

    /**
     * Gets the X position in inches.
     *
     * @return X position in inches
     */
    public double getX() {
        return getPose().getX(DistanceUnit.INCH);
    }

    /**
     * Gets the Y position in inches.
     *
     * @return Y position in inches
     */
    public double getY() {
        return getPose().getY(DistanceUnit.INCH);
    }

    /**
     * Sets the robot's pose for odometry.
     * Useful for initializing autonomous routines or vision-based relocalization.
     *
     * @param x X position in inches
     * @param y Y position in inches
     * @param heading Heading in radians
     */
    public void setPose(double x, double y, double heading) {
        Pose2D newPose = new Pose2D(
                DistanceUnit.INCH,
                x,
                y,
                AngleUnit.RADIANS,
                heading);
        odo.setPosition(newPose);
    }

    /**
     * Sets the robot's pose using a Pose2D object.
     *
     * @param pose The new pose
     */
    public void setPose(Pose2D pose) {
        odo.setPosition(pose);
    }

    /**
     * Resets the pose to origin (0, 0, 0).
     */
    public void resetPose() {
        setPose(0, 0, 0);
    }

    /**
     * Gets the velocity in the X direction (forward/backward).
     *
     * @return X velocity in inches per second
     */
    public double getVelocityX() {
        return odo.getVelX(DistanceUnit.INCH);
    }

    /**
     * Gets the velocity in the Y direction (strafe).
     *
     * @return Y velocity in inches per second
     */
    public double getVelocityY() {
        return odo.getVelY(DistanceUnit.INCH);
    }

    /**
     * Gets the angular velocity.
     *
     * @return Angular velocity in radians per second
     */
    public double getAngularVelocity() {
        return odo.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS);
    }

    /**
     * Periodic method called every loop cycle.
     * Updates odometry data.
     */
    @Override
    public void periodic() {
        // Update Pinpoint odometry
        odo.update();
    }
}