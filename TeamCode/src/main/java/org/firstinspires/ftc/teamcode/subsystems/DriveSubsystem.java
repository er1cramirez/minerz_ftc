package org.firstinspires.ftc.teamcode.subsystems;

import com.pedropathing.follower.Follower;
import com.pedropathing.ftc.InvertedFTCCoordinates;
import com.pedropathing.ftc.PoseConverter;
import com.pedropathing.geometry.PedroCoordinates;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.constants.DriveConstants;

import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;

public class DriveSubsystem extends SubsystemBase {

    public enum DriveMode {
        TELEOP,     // Control manual del driver
        AUTO        // Siguiendo un path
    }
    private final Follower follower;
    
    private DriveMode currentMode;
    
    /**
     * @param follower Instancia del Follower configurada
     */
    public DriveSubsystem(Follower follower) {
        this.follower = follower;
        this.currentMode = DriveMode.TELEOP;
    }

    /**
     * Convierte una Pose2D del SDK de FTC a una Pose de PedroPathing.
     * Utiliza los conversores para transformar de coordenadas FTC (invertidas para Decode)
     * a las coordenadas de PedroPathing.
     */
    public static Pose convertToPedroPose(Pose2D ftcPose) {
        // Step 1: Convert FTC Pose2D to Pedro Pose (Intermediate, Inverted FTC)
        Pose ftcStandard = PoseConverter.pose2DToPose(ftcPose, InvertedFTCCoordinates.INSTANCE);
        
        // Step 2: Convert to Pedro Coordinate System
        return ftcStandard.getAsCoordinateSystem(PedroCoordinates.INSTANCE);
    }
    
    public void startTeleOpMode() {
        follower.startTeleopDrive();
        currentMode = DriveMode.TELEOP;
    }
    
    /**
     * Controla el robot en TeleOp.
     * 
     * @param forward Velocidad hacia adelante/atrás (-1.0 a 1.0)
     * @param strafe Velocidad lateral (-1.0 a 1.0)
     * @param rotation Velocidad de rotación (-1.0 a 1.0)
     * @param robotCentric true para robot-centric, false para field-centric
     */
    public void setTeleOpDrive(double forward, double strafe, double rotation, boolean robotCentric) {
        follower.setTeleOpDrive(forward, strafe, rotation, robotCentric);
    }
    
    /**
     * Sigue un Path (para autonomous o automated actions).
     * 
     * @param path El path a seguir
     */
    public void followPath(Path path) {
        follower.followPath(path);
        currentMode = DriveMode.AUTO;
    }

    /**
     *
     * @param x //inch
     * @param y //inch
     * @param heading //Radians
     */
    public void setPose(double x, double y, double heading) {
        Pose pose = new Pose(x, y, heading);
        follower.setPose(pose);
    }
    
    /**
     * Sigue un PathChain (secuencia de paths).
     * 
     * @param pathChain El pathChain a seguir
     */
    public void followPath(PathChain pathChain) {
        follower.followPath(pathChain);
        currentMode = DriveMode.AUTO;
    }
    
    /**
     * Detiene el seguimiento de path y regresa a teleop.
     */
    public void breakFollowing() {
        follower.breakFollowing();
        startTeleOpMode();
    }
    
    /**
     * Establece la pose inicial del robot.
     * Útil al inicio de autonomous.
     * 
     * @param pose La pose inicial
     */
    public void setStartingPose(Pose pose) {
        follower.setStartingPose(pose);
    }
    
    // ===== MÉTODOS DE CONSULTA =====
    
    /**
     * Obtiene el modo actual del drivetrain.
     */
    public DriveMode getMode() {
        return currentMode;
    }
    
    /**
     * Verifica si está en modo TeleOp.
     */
    public boolean isTeleOpMode() {
        return currentMode == DriveMode.TELEOP;
    }
    
    /**
     * Verifica si está siguiendo un path.
     */
    public boolean isFollowingPath() {
        return follower.isBusy();
    }
    
    /**
     * Obtiene la pose actual del robot.
     */
    public Pose getPose() {
        return follower.getPose();
    }
    
    /**
     * Obtiene el heading actual (orientación en radianes).
     */
    public double getHeading() {
        return follower.getHeading();
    }
    
    /**
     * Obtiene la velocidad actual del robot.
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
