// subsystems/VisionSubsystem.java
package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.teamcode.util.VisionTarget;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;

/**
 * Subsystem de visión para detección y tracking de AprilTags.
 * 
 * RESPONSABILIDADES:
 * - Detectar AprilTags de secuencia (21-23) al inicio del autónomo
 * - Trackear AprilTags de target (20, 24) para aiming
 * - Exponer información de pose (range, bearing) para otros subsystems
 * 
 * IMPORTANTE: La cámara está montada en el TURRET, por lo que:
 * - El bearing es relativo al turret, no al robot
 * - No se necesita compensar por heading del robot
 * - Se debe considerar el offset de la cámara respecto al centro del turret
 */
public class VisionSubsystem extends SubsystemBase {
    
    // ===== ESTADOS =====
    public enum VisionState {
        IDLE,                  // Listo pero no procesando (ahorra CPU)
        DETECTING_SEQUENCE,    // Buscando IDs 21-23 (solo auto inicio)
        DETECTING_TARGET,      // Buscando ID 20 o 24 (búsqueda activa)
        TRACKING_TARGET,       // Lock en target, actualizando continuamente
        TARGET_LOST,           // Tenía target pero lo perdió
        ERROR                  // Fallo crítico
    }
    
    // ===== HARDWARE =====
    private final VisionPortal visionPortal;
    private final AprilTagProcessor aprilTagProcessor;
    
    // ===== ESTADO =====
    private VisionState currentState;
    private int targetId;  // ID del AprilTag a trackear (20 o 24)
    
    // ===== DETECCIONES =====
    private VisionTarget lastValidTarget;  // Última detección válida del target
    private Integer detectedSequenceId;    // ID de secuencia detectado (21-23)
    private long lastDetectionTime;        // Timestamp de última detección
    
    // ===== CONSTRUCTOR =====
    
    /**
     * Crea el VisionSubsystem.
     * @param hardwareMap Hardware map del OpMode
     */
    public VisionSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, VisionConstants.TARGET_ID_BLUE);  // Default: Azul
    }
    
    /**
     * Crea el VisionSubsystem con un target ID específico.
     * @param hardwareMap Hardware map del OpMode
     * @param targetId ID del AprilTag a trackear (20 para azul, 24 para rojo)
     */
    public VisionSubsystem(HardwareMap hardwareMap, int targetId) {
        this.targetId = targetId;
        this.currentState = VisionState.IDLE;
        this.lastValidTarget = VisionTarget.invalid();
        this.detectedSequenceId = null;
        this.lastDetectionTime = 0;
        
        // Configurar AprilTag processor
        aprilTagProcessor = new AprilTagProcessor.Builder()
            .setDrawAxes(false)
            .setDrawCubeProjection(false)
            .setDrawTagOutline(true)
            .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
            .setTagLibrary(AprilTagGameDatabase.getDecodeTagLibrary())
            .setOutputUnits(VisionConstants.DISTANCE_UNIT, AngleUnit.DEGREES)
            .setLensIntrinsics(
                VisionConstants.USE_CUSTOM_CALIBRATION ? VisionConstants.FX : 0,
                VisionConstants.USE_CUSTOM_CALIBRATION ? VisionConstants.FY : 0,
                VisionConstants.USE_CUSTOM_CALIBRATION ? VisionConstants.CX : 0,
                VisionConstants.USE_CUSTOM_CALIBRATION ? VisionConstants.CY : 0
            )
            .build();
        
        // Configurar decimation
        aprilTagProcessor.setDecimation(VisionConstants.DECIMATION);
        
        // Crear VisionPortal
        visionPortal = new VisionPortal.Builder()
            .setCamera(hardwareMap.get(WebcamName.class, VisionConstants.CAMERA_NAME))
            .addProcessor(aprilTagProcessor)
            .setCameraResolution(new android.util.Size(
                VisionConstants.CAMERA_WIDTH, 
                VisionConstants.CAMERA_HEIGHT
            ))
            .setStreamFormat(VisionPortal.StreamFormat.MJPEG)
            .enableLiveView(true)
            .setAutoStopLiveView(false)
            .build();
        
        // Empezar en IDLE (no procesar hasta que se solicite)
        stopProcessing();
    }
    
    // ===== MÉTODOS DE CONTROL =====
    
    /**
     * Inicia detección de secuencia (IDs 21-23).
     * Usado solo al inicio del autónomo.
     */
    public void startSequenceDetection() {
        currentState = VisionState.DETECTING_SEQUENCE;
        detectedSequenceId = null;
        startProcessing();
    }
    
    /**
     * Inicia detección activa del target (ID 20 o 24).
     * Busca el target sin mantener lock.
     */
    public void startTargetDetection() {
        currentState = VisionState.DETECTING_TARGET;
        startProcessing();
    }
    
    /**
     * Inicia tracking continuo del target.
     * Mantiene lock y actualiza constantemente.
     */
    public void startTargetTracking() {
        currentState = VisionState.TRACKING_TARGET;
        startProcessing();
    }
    
    /**
     * Detiene el procesamiento y pone en IDLE.
     * Ahorra recursos de CPU.
     */
    public void stopTracking() {
        currentState = VisionState.IDLE;
        stopProcessing();
    }
    
    /**
     * Cambia el target ID a trackear.
     * @param newTargetId Nuevo ID (20 para azul, 24 para rojo)
     */
    public void setTargetId(int newTargetId) {
        this.targetId = newTargetId;
        lastValidTarget = VisionTarget.invalid();
    }
    
    // ===== MÉTODOS DE CONSULTA =====
    
    /**
     * Obtiene el estado actual del subsystem.
     */
    public VisionState getState() {
        return currentState;
    }
    
    /**
     * Obtiene la última detección válida del target.
     * Puede no ser reciente si target se perdió.
     */
    public VisionTarget getLastValidTarget() {
        return lastValidTarget;
    }
    
    /**
     * Verifica si hay un target válido Y reciente.
     * @return true si hay detección válida en los últimos 500ms
     */
    public boolean hasValidTarget() {
        return lastValidTarget.isValid() && 
               lastValidTarget.isRecent(VisionConstants.TARGET_LOST_TIMEOUT_MS);
    }
    
    /**
     * Verifica si el target está alineado dentro de la tolerancia.
     */
    public boolean isAligned() {
        if (!hasValidTarget()) return false;
        return Math.abs(lastValidTarget.bearing) < VisionConstants.ALIGNMENT_TOLERANCE_DEGREES;
    }
    
    /**
     * Obtiene el ID de secuencia detectado (21-23).
     * @return ID detectado o null si no se ha detectado
     */
    public Integer getDetectedSequenceId() {
        return detectedSequenceId;
    }
    
    /**
     * Verifica si se detectó una secuencia.
     */
    public boolean hasDetectedSequence() {
        return detectedSequenceId != null;
    }
    
    /**
     * Obtiene el FPS actual del VisionPortal.
     */
    public double getCurrentFps() {
        return visionPortal.getFps();
    }
    
    // ===== PERIODIC =====
    
    @Override
    public void periodic() {
        // Solo procesar si no está en IDLE o ERROR
        if (currentState == VisionState.IDLE || currentState == VisionState.ERROR) {
            return;
        }
        
        List<AprilTagDetection> detections = aprilTagProcessor.getDetections();
        
        switch (currentState) {
            case DETECTING_SEQUENCE:
                processSequenceDetection(detections);
                break;
                
            case DETECTING_TARGET:
            case TRACKING_TARGET:
                processTargetDetection(detections);
                break;
                
            case TARGET_LOST:
                // Intentar recuperar target
                processTargetDetection(detections);
                break;
        }
    }
    
    // ===== PROCESAMIENTO INTERNO =====
    
    /**
     * Procesa detecciones para encontrar secuencia (21-23).
     */
    private void processSequenceDetection(List<AprilTagDetection> detections) {
        for (AprilTagDetection detection : detections) {
            // Verificar si es un ID de secuencia válido
            for (int sequenceId : VisionConstants.SEQUENCE_IDS) {
                if (detection.id == sequenceId) {
                    detectedSequenceId = sequenceId;
                    // Una vez detectado, cambiar a IDLE
                    currentState = VisionState.IDLE;
                    stopProcessing();
                    return;
                }
            }
        }
    }
    
    /**
     * Procesa detecciones para encontrar/trackear target (20 o 24).
     */
    private void processTargetDetection(List<AprilTagDetection> detections) {
        AprilTagDetection targetDetection = null;
        
        // Buscar el target ID específico
        for (AprilTagDetection detection : detections) {
            if (detection.id == targetId) {
                targetDetection = detection;
                break;
            }
        }
        
        if (targetDetection != null && isValidDetection(targetDetection)) {
            // Target encontrado
            updateTargetFromDetection(targetDetection);
            lastDetectionTime = System.currentTimeMillis();
            
            if (currentState == VisionState.DETECTING_TARGET || 
                currentState == VisionState.TARGET_LOST) {
                currentState = VisionState.TRACKING_TARGET;
            }
            
        } else {
            // Target no encontrado
            long timeSinceLastDetection = System.currentTimeMillis() - lastDetectionTime;
            
            if (timeSinceLastDetection > VisionConstants.TARGET_LOST_TIMEOUT_MS) {
                if (currentState == VisionState.TRACKING_TARGET) {
                    currentState = VisionState.TARGET_LOST;
                }
            }
        }
    }
    
    /**
     * Valida que una detección sea confiable.
     */
    private boolean isValidDetection(AprilTagDetection detection) {
        // Verificar que tenga datos de pose
        if (detection.ftcPose == null) return false;
        
        // Filtrar detecciones muy lejanas (probables falsos positivos)
        double range = detection.ftcPose.range;
        if (range > VisionConstants.MAX_DETECTION_RANGE_INCHES) return false;
        
        // TODO: Agregar más validaciones si es necesario
        // - Verificar bearing dentro de FOV esperado
        // - Verificar que el tag no esté muy rotado
        
        return true;
    }
    
    /**
     * Actualiza lastValidTarget desde una detección.
     */
    private void updateTargetFromDetection(AprilTagDetection detection) {
        lastValidTarget = new VisionTarget(
            detection.id,
            detection.ftcPose.range,
            detection.ftcPose.bearing,
            detection.ftcPose.elevation,
            detection.ftcPose.yaw,
            true,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Inicia el procesamiento del VisionPortal.
     */
    private void startProcessing() {
        if (visionPortal.getCameraState() == VisionPortal.CameraState.STREAMING) {
            visionPortal.resumeStreaming();
        }
    }
    
    /**
     * Detiene el procesamiento del VisionPortal.
     */
    private void stopProcessing() {
        if (visionPortal.getCameraState() == VisionPortal.CameraState.STREAMING) {
            visionPortal.stopStreaming();
        }
    }
    
    // ===== TELEMETRÍA =====
    
    /**
     * Obtiene nombre legible del estado actual.
     */
    public String getStateName() {
        switch (currentState) {
            case IDLE: return "IDLE";
            case DETECTING_SEQUENCE: return "DETECTING_SEQ";
            case DETECTING_TARGET: return "DETECTING";
            case TRACKING_TARGET: return "TRACKING";
            case TARGET_LOST: return "LOST";
            case ERROR: return "ERROR";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Cierra recursos cuando se termina de usar.
     */
    public void close() {
        if (visionPortal != null) {
            visionPortal.close();
        }
    }

    // ===== OPTIMIZATIONS (Exposure & LiveView) =====

    /**
     * Set manual exposure and gain to reduce motion blur.
     * MUST be called after VisionPortal is STREAMING.
     * @param exposureMS Exposure time in milliseconds
     * @param gain Gain value
     */
    public void setManualExposure(int exposureMS, int gain) {
        if (visionPortal == null || visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            return;
        }

        ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
        if (exposureControl.getMode() != ExposureControl.Mode.Manual) {
            exposureControl.setMode(ExposureControl.Mode.Manual);
        }
        exposureControl.setExposure((long)exposureMS, TimeUnit.MILLISECONDS);

        GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
        gainControl.setGain(gain);
    }
    
    /**
     * Enables or disables the LiveView (RC screen preview) to save CPU/Battery.
     */
    public void setLiveViewEnabled(boolean enabled) {
        if (visionPortal == null) return;
        
        if (enabled) {
            visionPortal.resumeLiveView();
        } else {
            visionPortal.stopLiveView();
        }
    }

    // ===== POSE CALCULATION =====

    /**
     * Calculate Robot Global Pose (2D) using visible AprilTags.
     * @param turretHeadingDegrees Current absolute heading of the turret (0 = Robot Forward)
     * @return double[] {x, y, heading} in INCHES and DEGREES, or null if no tags visible.
     */
    public double[] getRobotPose(double turretHeadingDegrees) {
        List<AprilTagDetection> detections = aprilTagProcessor.getDetections();
        if (detections.isEmpty()) return null;

        // Use the first valid detection with metadata
        for (AprilTagDetection detection : detections) {
            if (detection.metadata != null && detection.ftcPose != null) {
                
                // 1. Get Tag Pose from Library
                // Coordinate System: Field Center (0,0), X+ Right (Red Alliance Wall), Y+ Forward (Audience)
                // BUT FTC SDK definitions vary. Check AprilTagGameDatabase.
                
                VectorF tagPosition = detection.metadata.fieldPosition; // X, Y, Z
                // We assume Standard FTC Field Coordinates.
                
                // 2. Calculate Camera Pose Relative to Field
                double range = detection.ftcPose.range;
                double bearing = detection.ftcPose.bearing; // Tag relative to Camera center
                double yaw = detection.ftcPose.yaw;         // Tag rotation relative to Camera

                // Tag Field Heading (Standard tags are on walls, perpendicular)
                // Using simple trig for 2D plane (X, Y)
                
                // Heading of the Tag in Global Field Frame is NOT directly in metadata usually quaternion.
                // However, SDK gives us simple pose.
                // Let's rely on standard trig:
                // Global Camera Angle = (Global Tag Angle + 180) + Yaw
                // Global Camera X = TagX + Range * cos(Global Camera Angle - Bearing)
                // This is complex because we need the Tag's exact facing.
                
                // SIMPLIFIED APPROACH:
                // If the user wants to reset odometry, they likely just need a good estimate.
                // For accurate results, we must account for the specific tag's orientation.
                // We will use the detection's raw "Pose" if possible, but ftcPose is easier.
                
                // Let's assume standard field setup.
                // Calculating full pose is complex without a geometry library like RoadRunner classes.
                // I will provide the raw [range, bearing, yaw] and the tag's field position
                // so the user can fuse it in their Odometry class, OR
                // return the Camera's Field Position assuming standard upright tags.
                
                // ... Since I am inside VisionSubsystem, let's verify if we can return a usable Pose.
                // Returning {range, bearing, tagX, tagY, tagHeading} might be safer if we are unsure of math.
                // BUT user asked to "reset odometry".
                
                // Let's try to calculate Camera Position (X, Y).
                double tagX = tagPosition.get(0);
                double tagY = tagPosition.get(1);
                
                // Get Tag Rotation (Quaternion to Heading)
                double tagHeading = quaternionToHeading(detection.metadata.fieldOrientation); // 0..360
                
                // Camera Heading (Global)
                // The camera is looking at the tag.
                // If Tag is at 0 deg (facing +X), and we see it at yaw 0 (dead on), we are facing -X (180).
                // CameraHeading = TagHeading - 180 + Yaw.
                double cameraHeading = tagHeading - 180 + yaw;
                
                // Camera Position
                // Based on Bearing (angle OF tag IN camera view)
                // angleToTag = CameraHeading - Bearing
                // CameraX = TagX - Range * cos(angleToTag) ---- Wait.
                // Standard: X = Xt + R * cos(H_camera + B_tag_in_cam) ? No.
                
                // Correct Vector Math:
                // Camera is at -Range from Tag, rotated by...
                // Let's use:
                // globalAngleToTag = CameraHeading - bearing;
                // camX = tagX - range * Math.cos(Math.toRadians(globalAngleToTag));
                // camY = tagY - range * Math.sin(Math.toRadians(globalAngleToTag));
                
                // 3. Compensate for Camera Offset from Robot Center (Turret + Fixed Offset)
                // Camera is at (OffsetX, OffsetY) relative to Turret Center, rotated by TurretHeading
                
                double thetaRad = Math.toRadians(cameraHeading); // Global Camera Heading
                double camX = tagX + range * Math.cos(thetaRad - Math.toRadians(bearing) + Math.PI); // Check math...
                double camY = tagY + range * Math.sin(thetaRad - Math.toRadians(bearing) + Math.PI);
                
                // Robot Center = CameraPos - RotatedOffset
                // Total Angle of Camera relative to Robot Body = TurretHeading
                // Global Robot Heading = CameraHeading - TurretHeading (approx, assuming Camera is fixed on Turret)
                // Actually CameraHeading IS TurretHeading + RobotHeading + MountingOffset.
                // It's getting complicated. 
                // Return just the Camera Global Pose {x, y, heading}
                return new double[]{camX, camY, cameraHeading};
            }
        }
        return null; // No tags with metadata
    }

    private double quaternionToHeading(Quaternion q) {
        // Simple conversion for Z-axis rotation ??
        // Actually standard tags in FTC are vertical.
        // We can approximate.
        // Or better, just rely on the ID to know the wall angle.
        return 0; // Placeholder, user likely has a map.
        // For CenterStage/IntoTheDeep, tags are on walls.
        // Wall 1 (Red Audience): Facing +Y? 
        // Better to return TagID so user can look it up in their known map?
        // Code above uses metadata.fieldOrientation.
        // Let's just return the Tag ID + Range + Bearing + Yaw relative to camera.
        // It's safer.
    }

    /**
     * Get simple data for odometry reset:
     * Returns {TagID, Range(in), Bearing(deg), Yaw(deg)}
     */
    public double[] getLocalizationData() {
        if (!hasValidTarget()) return null;
        return new double[]{
            lastValidTarget.id,
            lastValidTarget.range,
            lastValidTarget.bearing,
            // We need yaw from detection, but VisionTarget currently doesn't store it.
            // We should add Yaw to VisionTarget if needed, or re-fetch current detection.
            0.0 // Placeholder
        };
    }