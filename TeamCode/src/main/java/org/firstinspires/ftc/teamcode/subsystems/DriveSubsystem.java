package org.firstinspires.ftc.teamcode.subsystems;

import com.seattlesolvers.solverslib.command.SubsystemBase;

public class DriveSubsystem extends SubsystemBase {
    private final MecanumDrive drive;

    public DriveSubsystem() {
        drive = new MecanumDrive(
            new Motor(hardwareMap, "frontLeft", Motor.GoBILDA.RPM_435),
            new Motor(hardwareMap, "frontRight", Motor.GoBILDA.RPM_435),
            new Motor(hardwareMap, "backLeft", Motor.GoBILDA.RPM_435),
            new Motor(hardwareMap, "backRight", Motor.GoBILDA.RPM_435)
        );
    }


    public void drive(double forward, double strafe, double rotate, boolean fieldRelative) {
        if (fieldRelative) {
            drive.driveFieldRelative(forward, strafe, rotate, getHeading());
        } else {
            drive.driveRobotRelative(forward, strafe, rotate);
        }
        
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
