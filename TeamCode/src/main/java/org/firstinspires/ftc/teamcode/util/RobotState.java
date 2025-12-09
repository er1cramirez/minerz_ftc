// util/RobotState.java
package org.firstinspires.ftc.teamcode.util;

/**
 * Estado global del robot compartido entre subsystems.
 * Singleton thread-safe.
 */
public class RobotState {
    
    private static RobotState instance;
    
    // ===== VISION =====
    private VisionTarget currentVisionTarget;
    private Integer gameSequenceId;  // ID detectado al inicio (21-23)
    
    // ===== MODO =====
    public enum RobotMode {
        DISABLED,
        AUTONOMOUS,
        TELEOP
    }
    private RobotMode currentMode;
    
    // ===== ALIANZA =====
    public enum Alliance {
        RED,
        BLUE
    }
    private Alliance alliance;
    
    // ===== CONSTRUCTOR PRIVADO =====
    private RobotState() {
        currentVisionTarget = VisionTarget.invalid();
        gameSequenceId = null;
        currentMode = RobotMode.DISABLED;
        alliance = Alliance.BLUE;  // Default
    }
    
    // ===== SINGLETON =====
    public static synchronized RobotState getInstance() {
        if (instance == null) {
            instance = new RobotState();
        }
        return instance;
    }
    
    /**
     * Resetea la instancia (útil entre OpModes).
     */
    public static synchronized void reset() {
        instance = new RobotState();
    }
    
    // ===== VISION =====
    
    /**
     * Actualiza el target de visión actual.
     * Llamado por VisionSubsystem.periodic().
     */
    public synchronized void updateVisionTarget(VisionTarget target) {
        this.currentVisionTarget = target;
    }
    
    /**
     * Obtiene el target de visión actual.
     */
    public synchronized VisionTarget getVisionTarget() {
        return currentVisionTarget;
    }
    
    /**
     * Establece el ID de secuencia del juego (21-23).
     * Solo se llama UNA VEZ al inicio del primer autónomo.
     */
    public synchronized void setGameSequenceId(int sequenceId) {
        if (gameSequenceId == null) {  // Solo permitir set una vez
            this.gameSequenceId = sequenceId;
        }
    }
    
    /**
     * Obtiene el ID de secuencia del juego.
     */
    public synchronized Integer getGameSequenceId() {
        return gameSequenceId;
    }
    
    // ===== MODO Y ALIANZA =====
    
    public synchronized void setMode(RobotMode mode) {
        this.currentMode = mode;
    }
    
    public synchronized RobotMode getMode() {
        return currentMode;
    }
    
    public synchronized void setAlliance(Alliance alliance) {
        this.alliance = alliance;
    }
    
    public synchronized Alliance getAlliance() {
        return alliance;
    }
    
    public synchronized boolean isRed() {
        return alliance == Alliance.RED;
    }
    
    public synchronized boolean isBlue() {
        return alliance == Alliance.BLUE;
    }
}