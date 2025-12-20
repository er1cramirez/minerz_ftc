package org.firstinspires.ftc.teamcode.commands.vision;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.FieldPose;

import java.util.function.Consumer;

/**
 * Comando para actualizar la pose del robot en el Follower de Pedro Pathing
 * usando la pose calculada desde AprilTags.
 * 
 * Este comando:
 * 1. Lee la pose actual desde VisionSubsystem
 * 2. Si es válida, la pasa al DriveSubsystem para setear en el Follower
 * 
 * Útil al inicio del autónomo para corregir la estimación inicial.
 */
public class UpdateRobotPoseCommand extends CommandBase {

    private final VisionSubsystem vision;
    private final DriveSubsystem drive;
    private final TurretSubsystem turret;
    private final Consumer<FieldPose> onPoseUpdated;
    private final long timeoutMs;

    private ElapsedTime timer;
    private FieldPose detectedPose;
    private boolean poseUpdated;

    /**
     * Crea el comando con timeout default.
     * 
     * @param vision Subsystem de visión
     * @param drive Subsystem de drivetrain (para setear pose)
     * @param turret Subsystem de torreta (para obtener ángulo actual)
     */
    public UpdateRobotPoseCommand(VisionSubsystem vision,
                                   DriveSubsystem drive,
                                   TurretSubsystem turret) {
        this(vision, drive, turret, null, VisionConstants.DETECTION_TIMEOUT_MS);
    }

    /**
     * Crea el comando con callback opcional.
     * 
     * @param vision Subsystem de visión
     * @param drive Subsystem de drivetrain
     * @param turret Subsystem de torreta
     * @param onPoseUpdated Callback opcional cuando se actualiza pose
     * @param timeoutMs Timeout en milisegundos
     */
    public UpdateRobotPoseCommand(VisionSubsystem vision,
                                   DriveSubsystem drive,
                                   TurretSubsystem turret,
                                   Consumer<FieldPose> onPoseUpdated,
                                   long timeoutMs) {
        this.vision = vision;
        this.drive = drive;
        this.turret = turret;
        this.onPoseUpdated = onPoseUpdated;
        this.timeoutMs = timeoutMs;

        // Solo requerimos vision - drive y turret son solo para lectura/escritura puntual
        addRequirements(vision);
    }

    @Override
    public void initialize() {
        timer = new ElapsedTime();
        detectedPose = null;
        poseUpdated = false;
        vision.enable();
    }

    @Override
    public void execute() {
        if (detectedPose != null) {
            return;  // Ya tenemos pose, esperando terminar
        }

        // Obtener ángulo de torreta y heading actual del robot
        double turretAngle = turret.getAngle();
        double currentHeading = drive.getHeading();

        // Intentar obtener pose desde visión
        detectedPose = vision.getRobotPose(turretAngle, currentHeading);

        if (detectedPose != null) {
            // Actualizar pose en el drivetrain/follower
            drive.setPose(detectedPose.x, detectedPose.y, detectedPose.heading);
            poseUpdated = true;

            // Callback si existe
            if (onPoseUpdated != null) {
                onPoseUpdated.accept(detectedPose);
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        // El comando termina, pero dejamos visión activa por si se necesita después
    }

    @Override
    public boolean isFinished() {
        // Terminar si actualizamos pose o timeout
        return poseUpdated || timer.milliseconds() >= timeoutMs;
    }

    /**
     * Verifica si la pose fue actualizada exitosamente.
     */
    public boolean wasPoseUpdated() {
        return poseUpdated;
    }

    /**
     * Obtiene la pose detectada (puede ser null si falló).
     */
    public FieldPose getDetectedPose() {
        return detectedPose;
    }
}
