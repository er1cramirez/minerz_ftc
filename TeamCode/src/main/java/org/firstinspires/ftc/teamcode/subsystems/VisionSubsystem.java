package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Vision Subsystem para detección de AprilTags.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * ENFOQUE: Usar robotPose del SDK con transformación para torreta
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Configuramos setCameraPose con posición (0,0,0) y orientación horizontal 
 * mirando al "frente". Esto hace que:
 * 
 *   robotPose.getPosition()  → Posición de la CÁMARA en cancha
 *   robotPose.getOrientation().getYaw() → Heading hacia donde MIRA la cámara
 * 
 * Como la cámara está en la torreta:
 *   - cameraYaw = robotPose.getOrientation().getYaw()
 *   - robotYaw = cameraYaw - turretAngle
 *   - robotPosition = cameraPosition compensado por offset y rotación
 * 
 * Este enfoque es más simple porque el SDK hace la triangulación por nosotros.
 * Solo necesitamos compensar la rotación de la torreta y el offset físico.
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class VisionSubsystem extends SubsystemBase {

    // ===== ESTADOS =====
    public enum VisionState {
        IDLE,           // VisionPortal no inicializado o cerrado
        DISABLED,       // Streaming pausado
        ACTIVE,         // Detectando, sin detección válida
        TARGET_ACQUIRED // Al menos una detección válida
    }

    // ===== ALIANZA =====
    public enum Alliance {
        RED(VisionConstants.TAG_GOAL_RED),
        BLUE(VisionConstants.TAG_GOAL_BLUE);

        public final int goalTagId;

        Alliance(int goalTagId) {
            this.goalTagId = goalTagId;
        }
    }

    // ===== DATA CLASSES =====

    /**
     * Range-Bearing-Elevation relativo a un target.
     */
    public static class RBE {
        public final double range;
        public final double bearing;
        public final double elevation;
        public final double confidence;

        public RBE(double range, double bearing, double elevation, double confidence) {
            this.range = range;
            this.bearing = bearing;
            this.elevation = elevation;
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return String.format("RBE(R=%.1f\", B=%.1f°, E=%.1f°, C=%.2f)",
                    range, bearing, elevation, confidence);
        }
    }

    /**
     * Pose del robot en coordenadas de cancha.
     */
    public static class FieldPose {
        public final double x;
        public final double y;
        public final double heading;
        public final int sourceTagId;

        public FieldPose(double x, double y, double heading, int sourceTagId) {
            this.x = x;
            this.y = y;
            this.heading = heading;
            this.sourceTagId = sourceTagId;
        }

        @Override
        public String toString() {
            return String.format("Pose(X=%.1f, Y=%.1f, H=%.1f° from tag %d)",
                    x, y, heading, sourceTagId);
        }
    }

    // ===== HARDWARE =====
    private final AprilTagProcessor aprilTagProcessor;
    private final VisionPortal visionPortal;

    // ===== ESTADO =====
    private VisionState currentState;
    private AprilTagDetection lastValidDetection;

    // ===== CONFIGURACIÓN DE CÁMARA PARA robotPose =====
    // Posición (0,0,0) = cámara en "centro del robot" (lo compensamos después)
    // Orientación: pitch=-90 = horizontal, yaw=0 = mirando al "frente"
    private static final Position CAMERA_POSITION = new Position(
            DistanceUnit.INCH, 0, 0, 0, 0);
    private static final YawPitchRollAngles CAMERA_ORIENTATION = new YawPitchRollAngles(
            AngleUnit.DEGREES, 0, -90, 0, 0);

    // ===== CONSTRUCTORES =====

    public VisionSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, VisionConstants.CAMERA_NAME);
    }

    public VisionSubsystem(HardwareMap hardwareMap, String cameraName) {
        // Crear AprilTagProcessor CON setCameraPose
        // Posición (0,0,0) hace que robotPose devuelva la posición de la cámara
        aprilTagProcessor = new AprilTagProcessor.Builder()
                .setDrawTagOutline(true)
                .setDrawTagID(true)
                .setDrawCubeProjection(false)
                .setDrawAxes(false)
                .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                .setTagLibrary(AprilTagGameDatabase.getDecodeTagLibrary())
                .setOutputUnits(VisionConstants.DISTANCE_UNIT, VisionConstants.ANGLE_UNIT)
                .setLensIntrinsics(VisionConstants.FX, VisionConstants.FY,
                        VisionConstants.CX, VisionConstants.CY)
                // Configuramos como si la cámara estuviera en (0,0,0) mirando al frente
                // robotPose nos dará la pose de la cámara, luego compensamos
                .setCameraPose(CAMERA_POSITION, CAMERA_ORIENTATION)
                .build();

        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, cameraName))
                .addProcessor(aprilTagProcessor)
                .enableLiveView(VisionConstants.ENABLE_LIVE_VIEW)
                .setStreamFormat(VisionPortal.StreamFormat.MJPEG)
                .build();

        currentState = VisionState.IDLE;
        lastValidDetection = null;
    }

    // ===== MÉTODOS DE CONTROL =====

    public void enable() {
        if (visionPortal.getCameraState() == VisionPortal.CameraState.STREAMING) {
            aprilTagProcessor.setDecimation(2);
        }
        visionPortal.resumeStreaming();
        currentState = VisionState.ACTIVE;
    }

    public void disable() {
        visionPortal.stopStreaming();
        currentState = VisionState.DISABLED;
        lastValidDetection = null;
    }

    public void close() {
        visionPortal.close();
        currentState = VisionState.IDLE;
    }

    // ===== CONTROL DE EXPOSICIÓN =====

    public boolean setManualExposure(int exposureMs, int gain) {
        if (visionPortal == null ||
                visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            return false;
        }

        try {
            ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
            if (exposureControl.getMode() != ExposureControl.Mode.Manual) {
                exposureControl.setMode(ExposureControl.Mode.Manual);
            }
            exposureControl.setExposure(exposureMs, TimeUnit.MILLISECONDS);

            GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
            gainControl.setGain(gain);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int[] getExposureLimits() {
        if (visionPortal == null ||
                visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            return null;
        }

        try {
            ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
            int min = (int) exposureControl.getMinExposure(TimeUnit.MILLISECONDS) + 1;
            int max = (int) exposureControl.getMaxExposure(TimeUnit.MILLISECONDS);
            return new int[]{min, max};
        } catch (Exception e) {
            return null;
        }
    }

    public int[] getGainLimits() {
        if (visionPortal == null ||
                visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            return null;
        }

        try {
            GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
            int min = gainControl.getMinGain();
            int max = gainControl.getMaxGain();
            return new int[]{min, max};
        } catch (Exception e) {
            return null;
        }
    }

    public int getCurrentExposure() {
        try {
            ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
            return (int) exposureControl.getExposure(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return -1;
        }
    }

    public int getCurrentGain() {
        try {
            GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
            return gainControl.getGain();
        } catch (Exception e) {
            return -1;
        }
    }

    // ===== MÉTODOS DE DETECCIÓN =====

    /**
     * Obtiene la secuencia del obelisco si es visible.
     */
    public String getSequence() {
        AprilTagDetection detection = findDetectionByIds(
                VisionConstants.TAG_SEQUENCE_GPP,
                VisionConstants.TAG_SEQUENCE_PGP,
                VisionConstants.TAG_SEQUENCE_PPG
        );

        if (detection != null && isValidDetection(detection)) {
            return VisionConstants.getSequenceFromTagId(detection.id);
        }
        return null;
    }

    public boolean canSeeSequenceTag() {
        return getSequence() != null;
    }

    /**
     * Obtiene RBE hacia el goal de la alianza especificada.
     * Usa ftcPose (siempre disponible, no depende de robotPose).
     */
    public RBE getGoalRBE(Alliance alliance) {
        AprilTagDetection detection = findDetectionById(alliance.goalTagId);

        if (detection != null && isValidDetection(detection)) {
            return createRBE(detection);
        }
        return null;
    }

    public boolean canSeeGoal(Alliance alliance) {
        return getGoalRBE(alliance) != null;
    }

    public RBE getRBEForTag(int tagId) {
        AprilTagDetection detection = findDetectionById(tagId);

        if (detection != null && isValidDetection(detection)) {
            return createRBE(detection);
        }
        return null;
    }

    /**
     * Obtiene la pose del robot en cancha usando robotPose del SDK.
     * 
     * ENFOQUE SIMPLIFICADO:
     * 1. robotPose del SDK nos da la pose de la "cámara" en cancha
     *    (porque configuramos setCameraPose con posición 0,0,0)
     * 2. El yaw de robotPose es hacia donde MIRA la cámara
     * 3. Compensamos por el ángulo de la torreta para obtener el yaw del robot
     * 4. Compensamos la posición por el offset físico de la cámara
     * 
     * @param turretAngleDeg Ángulo actual de la torreta respecto al robot
     * @return Pose del robot, o null si no hay detección válida
     */
    public FieldPose getRobotPose(double turretAngleDeg) {
        // Buscar detección válida (priorizar goals sobre secuencias)
        AprilTagDetection detection = findDetectionByIds(
                VisionConstants.TAG_GOAL_RED,
                VisionConstants.TAG_GOAL_BLUE
        );

        if (detection == null || !isValidDetection(detection) || detection.robotPose == null) {
            detection = findDetectionByIds(
                    VisionConstants.TAG_SEQUENCE_GPP,
                    VisionConstants.TAG_SEQUENCE_PGP,
                    VisionConstants.TAG_SEQUENCE_PPG
            );
        }

        if (detection == null || !isValidDetection(detection) || detection.robotPose == null) {
            return null;
        }

        return calculateRobotPose(detection, turretAngleDeg);
    }

    /**
     * Obtiene la pose RAW de robotPose sin compensación de torreta.
     * Útil para debug - muestra la pose de la cámara directamente.
     */
    public FieldPose getCameraPoseRaw() {
        for (AprilTagDetection detection : aprilTagProcessor.getDetections()) {
            if (detection.robotPose != null && detection.metadata != null 
                    && !detection.metadata.name.contains("Obelisk")) {
                return new FieldPose(
                        detection.robotPose.getPosition().x,
                        detection.robotPose.getPosition().y,
                        detection.robotPose.getOrientation().getYaw(AngleUnit.DEGREES),
                        detection.id
                );
            }
        }
        return null;
    }

    // ===== MÉTODOS DE ESTADO =====

    public VisionState getState() {
        return currentState;
    }

    public boolean hasValidDetection() {
        return currentState == VisionState.TARGET_ACQUIRED;
    }

    public boolean isDisabled() {
        return currentState == VisionState.DISABLED;
    }

    public AprilTagDetection getLastValidDetection() {
        return lastValidDetection;
    }

    public List<AprilTagDetection> getAllDetections() {
        return aprilTagProcessor.getDetections();
    }

    public VisionPortal getVisionPortal() {
        return visionPortal;
    }

    // ===== PERIODIC =====

    @Override
    public void periodic() {
        if (currentState == VisionState.DISABLED || currentState == VisionState.IDLE) {
            return;
        }

        List<AprilTagDetection> detections = aprilTagProcessor.getDetections();
        boolean hasValid = false;

        for (AprilTagDetection detection : detections) {
            if (isValidDetection(detection)) {
                hasValid = true;
                lastValidDetection = detection;
                break;
            }
        }

        currentState = hasValid ? VisionState.TARGET_ACQUIRED : VisionState.ACTIVE;
    }

    // ===== MÉTODOS PRIVADOS =====

    private AprilTagDetection findDetectionById(int tagId) {
        for (AprilTagDetection detection : aprilTagProcessor.getDetections()) {
            if (detection.id == tagId) {
                return detection;
            }
        }
        return null;
    }

    private AprilTagDetection findDetectionByIds(int... tagIds) {
        for (AprilTagDetection detection : aprilTagProcessor.getDetections()) {
            for (int id : tagIds) {
                if (detection.id == id) {
                    return detection;
                }
            }
        }
        return null;
    }

    private boolean isValidDetection(AprilTagDetection detection) {
        if (detection.ftcPose == null) {
            return false;
        }

        if (detection.decisionMargin < 1.0 / VisionConstants.MAX_AMBIGUITY) {
            return false;
        }

        double range = detection.ftcPose.range;
        if (range < VisionConstants.MIN_DETECTION_RANGE_INCHES ||
                range > VisionConstants.MAX_DETECTION_RANGE_INCHES) {
            return false;
        }

        return true;
    }

    private RBE createRBE(AprilTagDetection detection) {
        double confidence = Math.min(1.0, detection.decisionMargin / 100.0);

        return new RBE(
                detection.ftcPose.range,
                detection.ftcPose.bearing,
                detection.ftcPose.elevation,
                confidence
        );
    }

    /**
     * Calcula la pose del robot a partir de robotPose del SDK.
     * 
     * TRANSFORMACIÓN DE COORDENADAS:
     * El SDK (robotPose) nos da la pose global de la CÁMARA porque configuramos
     * setCameraPose con (0,0,0).
     * 
     * Cadena cinemática invertida:
     * P_robot = P_cam - Rot(H_cam) * V_cam_offset - Rot(H_robot) * V_turret_offset
     * 
     * Donde:
     * 1. H_cam: Heading global de la cámara (dado por SDK)
     * 2. H_robot: Heading global del robot
     *    H_robot = H_cam - H_turret_rel - H_cam_offset
     * 3. V_cam_offset: Offset cámara -> eje torreta (VisionConstants)
     * 4. V_turret_offset: Offset eje torreta -> centro robot (VisionConstants)
     */
    private FieldPose calculateRobotPose(AprilTagDetection detection, double turretAngleDeg) {
        // 1. OBTENER POSE DE LA CÁMARA (GLOBAL)
        double cameraX = detection.robotPose.getPosition().x;
        double cameraY = detection.robotPose.getPosition().y;
        double cameraYaw = detection.robotPose.getOrientation().getYaw(AngleUnit.DEGREES);

        // 2. CALCULAR HEADING DEL ROBOT
        // robotYaw = cameraYaw - turretAngle - cameraOffset
        double robotYaw = cameraYaw - turretAngleDeg - VisionConstants.CAMERA_HEADING_OFFSET_DEG;
        
        // Normalizar a [-180, 180]
        while (robotYaw > 180) robotYaw -= 360;
        while (robotYaw < -180) robotYaw += 360;

        // 3. CALCULAR VECTOR DE OFFSET DE CÁMARA (en frame global)
        // Este offset rota con la cámara (cameraYaw)
        // V_cam_global = Rot(cameraYaw) * V_cam_local
        double cameraOffsetForward = VisionConstants.CAMERA_FORWARD_OFFSET_INCHES;
        double cameraOffsetLeft = VisionConstants.CAMERA_LEFT_OFFSET_INCHES;
        double cameraYawRad = Math.toRadians(cameraYaw);
        
        double camOffsetX_Global = cameraOffsetForward * Math.cos(cameraYawRad) - cameraOffsetLeft * Math.sin(cameraYawRad);
        double camOffsetY_Global = cameraOffsetForward * Math.sin(cameraYawRad) + cameraOffsetLeft * Math.cos(cameraYawRad);

        // 4. CALCULAR VECTOR DE OFFSET DE TORRETA (en frame global)
        // Este offset rota con el robot (robotYaw)
        // V_turret_global = Rot(robotYaw) * V_turret_local
        double turretOffsetForward = VisionConstants.TURRET_FORWARD_OFFSET_INCHES;
        double turretOffsetLeft = VisionConstants.TURRET_LEFT_OFFSET_INCHES;
        double robotYawRad = Math.toRadians(robotYaw);

        double turretOffsetX_Global = turretOffsetForward * Math.cos(robotYawRad) - turretOffsetLeft * Math.sin(robotYawRad);
        double turretOffsetY_Global = turretOffsetForward * Math.sin(robotYawRad) + turretOffsetLeft * Math.cos(robotYawRad);

        // 5. CALCULAR POSICIÓN FINAL DEL ROBOT
        // P_robot = P_cam - V_cam_global - V_turret_global
        double robotX = cameraX - camOffsetX_Global - turretOffsetX_Global;
        double robotY = cameraY - camOffsetY_Global - turretOffsetY_Global;

        return new FieldPose(robotX, robotY, robotYaw, detection.id);
    }
}