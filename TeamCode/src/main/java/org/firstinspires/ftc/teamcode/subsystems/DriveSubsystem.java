package org.firstinspires.ftc.teamcode.subsystems;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import org.firstinspires.ftc.teamcode.constants.VisionConstants;

public class DriveSubsystem extends SubsystemBase {

    public enum DriveMode {
        AUTO, 
        SLOW,
        NORMAL,
        FAST
    }
    private final Follower follower;
    
    private DriveMode currentMode;

    private Boolean robotCentric;
    
    /**
     * @param follower Instancia del Follower configurada
     */
    public DriveSubsystem(Follower follower) {
        this.follower = follower;
        this.currentMode = DriveMode.NORMAL;
    }

    public void startTeleOpMode() {
        follower.startTeleopDrive();
    }
    
    /**
     * Teleop drive control.
     * 
     * @param forward Speed forward/backward (-1.0 to 1.0)
     * @param strafe Speed lateral (-1.0 to 1.0)
     * @param rotation Speed rotation (-1.0 to 1.0)
     * @param robotCentric true for robot-centric, false for field-centric
     */
    public void setTeleOpDrive(double forward, double strafe, double rotation, boolean robotCentric) {
        follower.setTeleOpDrive(forward, strafe, rotation, robotCentric);
    }

    /**
     * Teleop drive control.
     * 
     * @param forward Speed forward/backward (-1.0 to 1.0)
     * @param strafe Speed lateral (-1.0 to 1.0)
     * @param rotation Speed rotation (-1.0 to 1.0)
     */
    public void setTeleOpDrive(double forward, double strafe, double rotation) {
        follower.setTeleOpDrive(forward, strafe, rotation, false);
    }

    /**
     * Teleop drive field-centric while holding target heading.
     * 
     * @param forward Speed forward/backward (-1.0 to 1.0)
     * @param strafe Speed lateral (-1.0 to 1.0)
     * @param rotation Speed rotation (-1.0 to 1.0)
     */
    public void driveAtAngle(double forward, double strafe, double rotation) {
        
    }
    
    /**
     * Follows a Path (for autonomous or automated actions).
     * 
     * @param path The path to follow
     */
    public void followPath(Path path) {
        follower.followPath(path);
        currentMode = DriveMode.AUTO;
    }

    /**
     * Follows a PathChain (sequence of paths).
     * 
     * @param pathChain The pathChain to follow
     */
    public void followPath(PathChain pathChain) {
        follower.followPath(pathChain);
        currentMode = DriveMode.AUTO;
    }

     /**
     * Stops path following and returns to teleop.
     */
    public void breakFollowing() {
        follower.breakFollowing();
        startTeleOpMode();
    }

    /**
     * Sets the pose of the robot in PedroPathing coordinates.
     * @param x Robot global x position
     * @param y Robot global y position
     * @param heading Robot global heading
     */
    public void setPose(double x, double y, double heading) {
        Pose pose = new Pose(x, y, heading);
        follower.setPose(pose);
    }

    /**
     * Sets the starting pose of the robot in PedroPathing coordinates.
     * @param pose The initial pose
     */
    public void setStartingPose(Pose pose) {
        follower.setStartingPose(pose);
    }

    /**
     * Sets the pose of the robot from a Vision result in FTC coordinates.
     * @param camX_ftc Camera global x position
     * @param camY_ftc Camera global y position
     * @param camYaw_ftc Camera global heading
     * @param turretAngleRad Turret relative angle to robot
     */
    public void setPoseFromVision(double camX_ftc, double camY_ftc, 
                                double camYaw_ftc, double turretAngleRad) {
        // FTC → Pedro
        double camX_pedro = camY_ftc + 72.0;
        double camY_pedro = 72.0 - camX_ftc;
        // Translation of the camera with respect to the robot considering the turret angle
        double cosTheta = Math.cos(turretAngleRad);
        double sinTheta = Math.sin(turretAngleRad);
        double offsetX_robot = VisionConstants.D_R_T + VisionConstants.D_T_C * cosTheta;
        double offsetY_robot = VisionConstants.D_T_C * sinTheta;
        // Rotation of the offset to the robot frame
        double headingRobot = camYaw_ftc - turretAngleRad;
        double cosRobot = Math.cos(headingRobot);
        double sinRobot = Math.sin(headingRobot);
        double offsetX_pedro = offsetX_robot * cosRobot - offsetY_robot * sinRobot;
        double offsetY_pedro = offsetX_robot * sinRobot + offsetY_robot * cosRobot;
        double robotX = camX_pedro - offsetX_pedro;
        double robotY = camY_pedro - offsetY_pedro;
        setPose(robotX, robotY, headingRobot);
    }
    
    
    
    /**
     * Gets the current mode of the drivetrain.
     */
    public DriveMode getMode() {
        return currentMode;
    }

    /**
     * Sets the drive mode to Slow.
     */
    public void setSlowMode() {
        currentMode = DriveMode.SLOW;
    }

    /**
     * Sets the drive mode to Normal.
     */
    public void setNormalMode() {
        currentMode = DriveMode.NORMAL;
    }

    /**
     * Sets the drive mode to Fast.
     */
    public void setFastMode() {
        currentMode = DriveMode.FAST;
    }

    /**
     * Sets Field-Centric mode.
     */
    public void setFieldCentric() {
        robotCentric = false;
    }

    /**
     * Sets Robot-Centric mode.
     */
    public void setRobotCentric() {
        robotCentric = true;
    }

    /**
     * Returns true if in robot-centric mode.
     */
    public boolean isRobotCentric() {
        return robotCentric;
    }   

    
    /**
     * Returns true if following a path.
     */
    public boolean isFollowingPath() {
        return follower.isBusy();
    }
    
    /**
     * Gets the current pose of the robot.
     */
    public Pose getPose() {
        return follower.getPose();
    }
    
    /**
     * Gets the current heading of the robot.
     */
    public double getHeading() {
        return follower.getHeading();
    }
    
    /**
     * Gets the current velocity of the robot.
     */
//    public Vector getVelocity() {
//        return follower.getVelocity();
//    }
//
    public Follower getFollower() {
        return follower;
    }
    
    @Override
    public void periodic() {
        follower.update();
    }
}
