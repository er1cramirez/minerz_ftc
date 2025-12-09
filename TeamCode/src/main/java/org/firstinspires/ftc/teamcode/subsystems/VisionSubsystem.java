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
}