package org.firstinspires.ftc.teamcode.subsystems;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.drivebase.MecanumDrive;
import com.seattlesolvers.solverslib.geometry.Pose2d;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.Motor.ZeroPowerBehavior;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

public class DriveSubsystem extends SubsystemBase {
    private final MotorEx frontRightMotor;
    private final MotorEx backLeftMotor;
    private final MotorEx backRightMotor;   

    private final MecanumDrive drive;


    public DriveSubsystem() {
        // Individual motors for configuration
        MotorEx frontLeftMotor = new MotorEx(hardwareMap, "frontLeft", Motor.GoBILDA.RPM_435);
        frontLeftMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
        frontRightMotor = new MotorEx(hardwareMap, "frontRight", Motor.GoBILDA.RPM_435);
        frontRightMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
        backLeftMotor = new MotorEx(hardwareMap, "backLeft", Motor.GoBILDA.RPM_435);
        backLeftMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
        backRightMotor = new MotorEx(hardwareMap, "backRight", Motor.GoBILDA.RPM_435);
        backRightMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
        drive = new MecanumDrive(frontLeftMotor, frontRightMotor, backLeftMotor, backRightMotor);
        configureDrivetrain();
    }


    public void drive(double forward, double strafe, double rotate, boolean fieldRelative) {
        if (fieldRelative) {
            drive.driveFieldCentric(forward, strafe, rotate, getHeading(), true);
        } else {
            drive.driveRobotCentric(forward, strafe, rotate, true);
        }
        
    }

    void configureDrivetrain() {

    }

    public void stop() {
        drive.stop();
    }

    public void resetGyro() {
        // Placeholder for actual gyro reset logic
    }

    public double getHeading() {
        // Placeholder for actual heading retrieval logic
        return 0.0;
    }

    public Pose2d getPose() {
        // Placeholder for actual pose retrieval logic
        return new Pose2d(0, 0, 0);
    }

}
