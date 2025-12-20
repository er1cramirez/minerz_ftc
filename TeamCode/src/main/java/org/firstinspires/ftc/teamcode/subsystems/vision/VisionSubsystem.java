package org.firstinspires.ftc.teamcode.subsystems.vision;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
//import org.firstinspires.ftc.teamcode.subsystems.vision.VisionConstants;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

/**
 * Subsystem de visión para detección de AprilTags.
 * 
 * Responsabilidades:
 * - Inicializar y gestionar VisionPortal con AprilTagProcessor
 * - Exponer detecciones procesadas y validadas
 * - Proporcionar datos de pose relativa (RBE) a targets
 * - Proporcionar pose absoluta del robot en cancha
 * 
 * NO es responsabilidad de este subsystem:
 * - Rotar la torreta
 * - Setear pose en drivetrain/follower
 * - Decidir cuándo disparar
 */
public class VisionSubsystem extends SubsystemBase {

    // ===== ENUM DE ESTADOS =====
    public enum VisionState {
        /** VisionPortal no inicializado o detenido */
        DISABLED,
        /** Procesando pero sin detección válida */
        SEARCHING,
        /** Tiene al menos una detección válida */
        TARGET_ACQUIRED
    }

    // ===== ENUM DE ALIANZA =====
    public enum Alliance {
        RED(VisionConstants.TAG_GOAL_RED),
        BLUE(VisionConstants.TAG_GOAL_BLUE);

        public final int goalTagId;

        Alliance(int goalTagId) {
            this.goalTagId = goalTagId;
        }
    }

    // ===== DATA CLASS PARA RBE =====
    /**
     * Range-Bearing-Elevation relativo a un target.
     * Todos los valores desde la perspectiva de la CÁMARA.
     */
    public static class RBE {
        /** Distancia al target en pulgadas */
        public final double range;
        /** Ángulo horizontal al target en grados (positivo = izquierda) */
        public final double bearing;
        /** Ángulo vertical al target en grados (positivo = arriba) */
        public final double elevation;
        /** Calidad de la detección (0-1, mayor = mejor) */
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

    // ===== DATA CLASS PARA POSE EN CANCHA =====
    /**
     * Pose del robot en coordenadas de cancha.
     */
    public static class FieldPose {
        /** Posición X en pulgadas */
        public final double x;
        /** Posición Y en pulgadas */
        public final double y;
        /** Heading en grados */
        public final double heading;
        /** ID del tag usado para calcular esta pose */
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

    // ===== CONSTRUCTOR =====
    /**
     * Crea el subsystem de visión.
     * 
     * @param hardwareMap HardwareMap del OpMode
     */
    public VisionSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, VisionConstants.CAMERA_NAME);
    }

    /**
     * Crea el subsystem de visión con nombre de cámara custom.
     * 
     * @param hardwareMap HardwareMap del OpMode
     * @param cameraName  Nombre de la cámara en la configuración
     */
    public VisionSubsystem(HardwareMap hardwareMap, String camera Name) {
        // Crear AprilTagProcessor con la librería de Decode
        aprilTagProcessor = new AprilTagProcessor.Builder()
                .setDrawTagOutline(true)
                .setDrawTagID(true)
                .setDrawCubeProjection(false)
                .setDrawAxes(false)
                .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                .setTagLibrary(AprilTagGameDatabase.getDecodeTagLibrary())
                .setOutputUnits(org.firstinspires.ftc.teamcode.constants.VisionConstants.DISTANCE_UNIT, AngleUnit.DEGREES)
                .setLensIntrinsics(VisionConstants.FX, VisionConstants.FY, VisionConstants.CX, VisionConstants.CY)
                .build();

        // Crear VisionPortal
        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, cameraName))
                .addProcessor(aprilTagProcessor)
                .enableLiveView(VisionConstants.ENABLE_LIVE_VIEW)
                .setStreamFormat(VisionPortal.StreamFormat.MJPEG)
                .build();

        currentState = VisionState.SEARCHING;
        lastValidDetection = null;
    }

    // ===== MÉTODOS DE CONTROL =====

    /**
     * Activa el procesamiento de visión.
     */
    public void enable() {
        if (visionPortal.getCameraState() == VisionPortal.CameraState.STREAMING) {
            aprilTagProcessor.setDecimation(2);  // Balance velocidad/precisión
        }
        visionPortal.resumeStreaming();
        currentState = VisionState.SEARCHING;
    }

    /**
     * Desactiva el procesamiento para ahorrar CPU.
     */
    public void disable() {
        visionPortal.stopStreaming();
        currentState = VisionState.DISABLED;
        lastValidDetection = null;
    }

    /**
     * Libera recursos. Llamar al final del OpMode.
     */
    public void close() {
        visionPortal.close();
        currentState = VisionState.DISABLED;
    }

    // ===== MÉTODOS DE CONSULTA - DETECCIONES =====

    /**
     * Obtiene la secuencia del obelisco si es visible.
     * 
     * @return String de secuencia ("YYP", "YPY", "PYY") o null si no detectada
     */
    public String getSequence() {
        AprilTagDetection detection = findDetectionByIds(
                VisionConstants.TAG_SEQUENCE_YYP,
                VisionConstants.TAG_SEQUENCE_YPY,
                VisionConstants.TAG_SEQUENCE_PYY
        );

        if (detection != null && isValidDetection(detection)) {
            return VisionConstants.getSequenceFromTagId(detection.id);
        }
        return null;
    }

    /**
     * Verifica si la secuencia del obelisco es visible.
     */
    public boolean canSeeSequenceTag() {
        return getSequence() != null;
    }

    /**
     * Obtiene RBE hacia el goal de la alianza especificada.
     * 
     * @param alliance Alianza cuyo goal buscar
     * @return RBE al goal, o null si no visible
     */
    public RBE getGoalRBE(Alliance alliance) {
        AprilTagDetection detection = findDetectionById(alliance.goalTagId);

        if (detection != null && isValidDetection(detection)) {
            return createRBE(detection);
        }
        return null;
    }

    /**
     * Verifica si el goal de la alianza es visible.
     */
    public boolean canSeeGoal(Alliance alliance) {
        return getGoalRBE(alliance) != null;
    }

    /**
     * Obtiene la pose del robot en cancha basada en cualquier AprilTag visible.
     * Prioriza tags de goal sobre tags de secuencia.
     * 
     * @param turretAngleDeg Ángulo actual de la torreta respecto al robot (grados)
     * @param robotHeadingDeg Heading actual del robot en cancha (grados)
     * @return Pose en cancha, o null si no hay detección válida
     */
    public FieldPose getRobotPose(double turretAngleDeg, double robotHeadingDeg) {
        // Buscar primero tags de goal (más confiables por estar en pared)
        AprilTagDetection detection = findDetectionByIds(
                VisionConstants.TAG_GOAL_RED,
                VisionConstants.TAG_GOAL_BLUE
        );

        // Si no hay goal visible, usar tags de secuencia
        if (detection == null) {
            detection = findDetectionByIds(
                    VisionConstants.TAG_SEQUENCE_YYP,
                    VisionConstants.TAG_SEQUENCE_YPY,
                    VisionConstants.TAG_SEQUENCE_PYY
            );
        }

        if (detection == null || !isValidDetection(detection)) {
            return null;
        }

        return calculateFieldPose(detection, turretAngleDeg, robotHeadingDeg);
    }

    /**
     * Obtiene RBE hacia un tag específico por ID.
     * Útil para debug o casos especiales.
     */
    public RBE getRBEForTag(int tagId) {
        AprilTagDetection detection = findDetectionById(tagId);

        if (detection != null && isValidDetection(detection)) {
            return createRBE(detection);
        }
        return null;
    }

    // ===== MÉTODOS DE ESTADO =====

    /**
     * Obtiene el estado actual del subsystem.
     */
    public VisionState getState() {
        return currentState;
    }

    /**
     * Verifica si hay al menos una detección válida.
     */
    public boolean hasValidDetection() {
        return currentState == VisionState.TARGET_ACQUIRED;
    }

    /**
     * Verifica si el subsystem está deshabilitado.
     */
    public boolean isDisabled() {
        return currentState == VisionState.DISABLED;
    }

    /**
     * Obtiene la última detección válida (para debug).
     */
    public AprilTagDetection getLastValidDetection() {
        return lastValidDetection;
    }

    /**
     * Obtiene todas las detecciones actuales (para debug).
     */
    public List<AprilTagDetection> getAllDetections() {
        return aprilTagProcessor.getDetections();
    }

    // ===== PERIODIC =====

    @Override
    public void periodic() {
        // Actualizar estado basado en detecciones actuales
        if (currentState == VisionState.DISABLED) {
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

        currentState = hasValid ? VisionState.TARGET_ACQUIRED : VisionState.SEARCHING;
    }

    // ===== MÉTODOS PRIVADOS =====

    /**
     * Busca una detección por ID específico.
     */
    private AprilTagDetection findDetectionById(int tagId) {
        for (AprilTagDetection detection : aprilTagProcessor.getDetections()) {
            if (detection.id == tagId) {
                return detection;
            }
        }
        return null;
    }

    /**
     * Busca una detección que coincida con cualquiera de los IDs dados.
     */
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

    /**
     * Valida que una detección sea confiable.
     */
    private boolean isValidDetection(AprilTagDetection detection) {
        // Debe tener pose (ftcPose)
        if (detection.ftcPose == null) {
            return false;
        }

        // Verificar ambiguity
        if (detection.decisionMargin < 1.0 / VisionConstants.MAX_AMBIGUITY) {
            return false;
        }

        // Verificar rango
        double range = detection.ftcPose.range;
        if (range < VisionConstants.MIN_DETECTION_RANGE_INCHES ||
            range > VisionConstants.MAX_DETECTION_RANGE_INCHES) {
            return false;
        }

        return true;
    }

    /**
     * Crea un objeto RBE a partir de una detección.
     */
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
     * Calcula la pose del robot en cancha a partir de una detección.
     * 
     * La matemática:
     * 1. El tag tiene una pose conocida en cancha (de la librería)
     * 2. La detección nos da pose relativa cámara→tag
     * 3. Conocemos offset cámara→torreta y torreta→robot
     * 4. Invertimos para obtener robot→cancha
     */
    private FieldPose calculateFieldPose(AprilTagDetection detection,
                                         double turretAngleDeg,
                                         double robotHeadingDeg) {
        // Pose del tag en cancha (de la librería)
        if (detection.metadata == null || detection.metadata.fieldPosition == null) {
            return null;
        }

        // Heading total de la cámara en cancha
        double cameraHeadingDeg = robotHeadingDeg + turretAngleDeg 
                                  + VisionConstants.CAMERA_HEADING_OFFSET_DEG;
        double cameraHeadingRad = Math.toRadians(cameraHeadingDeg);

        // Posición del tag en cancha
        double tagX = detection.metadata.fieldPosition.get(0);
        double tagY = detection.metadata.fieldPosition.get(1);

        // Vector desde cámara hacia tag (en frame de cámara)
        double rangeToTag = detection.ftcPose.range;
        double bearingRad = Math.toRadians(detection.ftcPose.bearing);

        // Convertir a coordenadas de cancha
        // El bearing es relativo a donde mira la cámara
        double dx = rangeToTag * Math.cos(cameraHeadingRad + bearingRad);
        double dy = rangeToTag * Math.sin(cameraHeadingRad + bearingRad);

        // Posición de la cámara en cancha
        double cameraX = tagX - dx;
        double cameraY = tagY - dy;

        // Compensar offset de cámara a centro de robot
        // (simplificado - asumiendo offset principalmente forward)
        double offsetForward = VisionConstants.CAMERA_FORWARD_OFFSET_INCHES;
        double offsetLeft = VisionConstants.CAMERA_LEFT_OFFSET_INCHES;

        double robotX = cameraX - offsetForward * Math.cos(Math.toRadians(robotHeadingDeg))
                               + offsetLeft * Math.sin(Math.toRadians(robotHeadingDeg));
        double robotY = cameraY - offsetForward * Math.sin(Math.toRadians(robotHeadingDeg))
                               - offsetLeft * Math.cos(Math.toRadians(robotHeadingDeg));

        return new FieldPose(robotX, robotY, robotHeadingDeg, detection.id);
    }
}
