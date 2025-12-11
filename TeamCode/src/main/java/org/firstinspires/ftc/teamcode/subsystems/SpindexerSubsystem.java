package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;

import java.util.ArrayList;
import java.util.List;


public class SpindexerSubsystem extends SubsystemBase {
    
    // ==================== ENUMS ====================
    
    /**
     * Estados del spindexer
     */
    public enum SpindexerState {
        IDLE,                    // En reposo, esperando comandos
        MOVING_TO_INTAKE,        // Rotando hacia posición de intake
        READY_FOR_INTAKE,        // En posición de intake, esperando pelota
        DETECTING_BALL,          // Leyendo sensor de color
        MOVING_TO_OUTTAKE,       // Rotando hacia posición de outtake
        READY_FOR_OUTTAKE        // En posición de outtake, listo para lanzar
    }
    
    /**
     * Estado de cada slot individual
     */
    public enum SlotState {
        EMPTY,          // Slot vacío
        YELLOW,         // Pelota amarilla
        PURPLE,         // Pelota morada
        UNKNOWN         // Hay algo pero color no identificado
    }

    public enum BallColor {
        YELLOW,     // Pelota amarilla
        PURPLE,     // Pelota púrpura/morada
        NONE,       // Sin pelota detectada
        UNKNOWN     // Hay algo pero color no identificado claramente
    }
    
  
    
    private final Servo indexerServo;
    private final SensorRevColorV3 colorSensor;
    private final boolean useSensor;
    
    private SpindexerState currentState;
    private SlotState[] slotStates;      // Estado de cada uno de los 3 slots
    private int currentSlotIndex;        // Slot actualmente en posición (0, 1, 2)
    
    private final ElapsedTime detectionTimer;
    private List<BallColor> detectionVotes;
    private int votingSamples = 15;      // Número de lecturas para votación
    private int votingDelayMs = 50;      // Delay entre lecturas
    
    public SpindexerSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, false);
    }
    
    /**
     * Constructor completo - con opción de sensor.
     * 
     * @param hardwareMap El hardware map del OpMode
     * @param useSensor true si el sensor está instalado y configurado
     */
    public SpindexerSubsystem(HardwareMap hardwareMap, boolean useSensor) {
        // Inicializar servo
        indexerServo = hardwareMap.get(Servo.class, SpindexerConstants.SERVO_NAME);
        
        // Inicializar sensor solo si está disponible
        this.useSensor = useSensor;
        if (useSensor) {
            try {
                colorSensor = new SensorRevColorV3(
                    hardwareMap, 
                    SpindexerConstants.COLOR_SENSOR_NAME, 
                    DistanceUnit.CM
                );
            } catch (Exception e) {
                throw new RuntimeException(
                    "Color sensor not found in hardware map. " +
                    "Use SpindexerSubsystem(hardwareMap) if sensor is not installed."
                );
            }
        } else {
            colorSensor = null;
        }
        
        // Inicializar estado
        currentState = SpindexerState.IDLE;
        slotStates = new SlotState[3];
        for (int i = 0; i < 3; i++) {
            slotStates[i] = SlotState.EMPTY;
        }
        currentSlotIndex = 0;
        
        // Inicializar detección
        detectionTimer = new ElapsedTime();
        detectionVotes = new ArrayList<>();
    }
    
    public void moveToIntakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        double angle = getIntakeAngle(slotIndex);
        indexerServo.setPosition(angle);
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.MOVING_TO_INTAKE;
    }
    
    public void moveToOuttakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        double angle = getOuttakeAngle(slotIndex);
        indexerServo.setPosition(angle);
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.MOVING_TO_OUTTAKE;
    }
    
    public void moveToNextIntakeSlot() {
        currentSlotIndex = (currentSlotIndex + 1) % 3;
        moveToIntakePosition(currentSlotIndex);
    }
    
    /**
     * Mueve al slot vacío más cercano en posición de intake.
     * Si no hay slots vacíos, permanece en el actual.
     */
    public void moveToClosestEmptySlot() {
        for (int i = 0; i < 3; i++) {
            int slotToCheck = (currentSlotIndex + i) % 3;
            if (slotStates[slotToCheck] == SlotState.EMPTY) {
                moveToIntakePosition(slotToCheck);
                return;
            }
        }
        // No hay slots vacíos, permanecer en actual
    }
    /**
     * Verifica si hay una pelota presente frente al sensor.
     * 
     * @return true si detecta pelota (distancia < umbral)
     * @throws IllegalStateException si el sensor no está disponible
     */
    public boolean isBallDetected() {
        requireSensor();
        double distance = colorSensor.distance();
        return distance < SpindexerConstants.BALL_DETECTION_DISTANCE;
    }
    
    /**
     * Detecta el color de la pelota actual usando sistema de votación.
     * Toma múltiples lecturas y retorna el color con más votos.
     * 
     * Este método es BLOQUEANTE - toma ~1 segundo (20 samples * 50ms).
     * Para uso asíncrono, ver startDetection() y pollDetection().
     * 
     * @return Color detectado (YELLOW, PURPLE, NONE, o UNKNOWN)
     * @throws IllegalStateException si el sensor no está disponible
     */
    public BallColor detectBallColor() {
        requireSensor();
        
        detectionVotes.clear();
        
        for (int i = 0; i < votingSamples; i++) {
            BallColor vote = readSingleColorSample();
            detectionVotes.add(vote);
            
            try {
                Thread.sleep(votingDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return BallColor.UNKNOWN;
            }
        }
        
        return tallyVotes();
    }
    
    /**
     * Lee una única muestra de color del sensor.
     * 
     * @return Color detectado en esta lectura
     */
    private BallColor readSingleColorSample() {
        int[] argb = colorSensor.getARGB();
        double distance = colorSensor.distance();
        
        // Verificar distancia primero
        if (distance > SpindexerConstants.BALL_DETECTION_DISTANCE) {
            return BallColor.NONE;
        }
        
        // Calcular porcentajes normalizados
        int red = argb[1];
        int green = argb[2];
        int blue = argb[3];
        int total = red + green + blue;
        
        if (total == 0) {
            return BallColor.UNKNOWN;
        }
        
        double redPercent = (red * 100.0) / total;
        double greenPercent = (green * 100.0) / total;
        double bluePercent = (blue * 100.0) / total;
        
        // Verificar amarillo
        boolean isYellow = redPercent >= SpindexerConstants.YELLOW_RED_MIN && 
                          greenPercent >= SpindexerConstants.YELLOW_GREEN_MIN && 
                          bluePercent <= SpindexerConstants.YELLOW_BLUE_MAX;
        
        // Verificar púrpura
        boolean isPurple = redPercent >= SpindexerConstants.PURPLE_RED_MIN && 
                          bluePercent >= SpindexerConstants.PURPLE_BLUE_MIN && 
                          greenPercent <= SpindexerConstants.PURPLE_GREEN_MAX;
        
        // Retornar resultado
        if (isYellow && !isPurple) return BallColor.YELLOW;
        if (isPurple && !isYellow) return BallColor.PURPLE;
        
        return BallColor.UNKNOWN;  // Ambiguo o no coincide
    }
    
    /**
     * Cuenta los votos y determina el color ganador.
     * 
     * @return Color con más votos
     */
    private BallColor tallyVotes() {
        int yellowVotes = 0;
        int purpleVotes = 0;
        int noneVotes = 0;
        int unknownVotes = 0;
        
        for (BallColor vote : detectionVotes) {
            switch (vote) {
                case YELLOW: yellowVotes++; break;
                case PURPLE: purpleVotes++; break;
                case NONE: noneVotes++; break;
                case UNKNOWN: unknownVotes++; break;
            }
        }
        
        // Si más de la mitad son NONE, no hay pelota
        if (noneVotes > votingSamples / 2) {
            return BallColor.NONE;
        }
        
        // Determinar ganador
        int maxVotes = Math.max(Math.max(yellowVotes, purpleVotes), unknownVotes);
        
        // Requerir al menos 60% de consenso
        if (maxVotes < votingSamples * 0.6) {
            return BallColor.UNKNOWN;
        }
        
        if (yellowVotes == maxVotes) return BallColor.YELLOW;
        if (purpleVotes == maxVotes) return BallColor.PURPLE;
        
        return BallColor.UNKNOWN;
    }
    
    /**
     * Detecta automáticamente pelota y actualiza el estado del slot actual.
     * Método de conveniencia que combina detección y etiquetado.
     * 
     * @return Color detectado
     * @throws IllegalStateException si el sensor no está disponible
     */
    public BallColor autoDetectAndLabel() {
        requireSensor();
        
        currentState = SpindexerState.DETECTING_BALL;
        BallColor detected = detectBallColor();
        
        // Actualizar estado del slot
        switch (detected) {
            case YELLOW:
                slotStates[currentSlotIndex] = SlotState.YELLOW;
                break;
            case PURPLE:
                slotStates[currentSlotIndex] = SlotState.PURPLE;
                break;
            case UNKNOWN:
                slotStates[currentSlotIndex] = SlotState.UNKNOWN;
                break;
            case NONE:
                slotStates[currentSlotIndex] = SlotState.EMPTY;
                break;
        }
        
        currentState = SpindexerState.READY_FOR_INTAKE;
        return detected;
    }
    
    // ==================== GESTIÓN DE SLOTS ====================
    
    /**
     * Obtiene el estado de un slot específico.
     * 
     * @param slotIndex Índice del slot (0, 1, 2)
     * @return Estado del slot
     */
    public SlotState getSlotState(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slotStates[slotIndex];
    }
    
    /**
     * Obtiene el estado del slot actual.
     * 
     * @return Estado del slot actual
     */
    public SlotState getCurrentSlotState() {
        return slotStates[currentSlotIndex];
    }
    
    /**
     * Establece manualmente el estado de un slot.
     * Útil cuando no se usa sensor o para override.
     * 
     * @param slotIndex Índice del slot (0, 1, 2)
     * @param state Nuevo estado del slot
     */
    public void setSlotState(int slotIndex, SlotState state) {
        validateSlotIndex(slotIndex);
        slotStates[slotIndex] = state;
    }
    
    /**
     * Limpia un slot (lo marca como vacío).
     * Típicamente llamado después de lanzar una pelota.
     * 
     * @param slotIndex Índice del slot (0, 1, 2)
     */
    public void clearSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        slotStates[slotIndex] = SlotState.EMPTY;
    }
    
    /**
     * Limpia el slot actual.
     */
    public void clearCurrentSlot() {
        slotStates[currentSlotIndex] = SlotState.EMPTY;
    }
    
    // ==================== CONSULTAS DE ESTADO ====================
    
    /**
     * Obtiene el estado actual del spindexer.
     * 
     * @return Estado actual
     */
    public SpindexerState getState() {
        return currentState;
    }
    
    /**
     * Obtiene el índice del slot actual (0, 1, 2).
     * 
     * @return Índice del slot actual
     */
    public int getCurrentSlotIndex() {
        return currentSlotIndex;
    }
    
    /**
     * Verifica si está en posición de intake.
     * 
     * @return true si está en READY_FOR_INTAKE
     */
    public boolean isInIntakePosition() {
        return currentState == SpindexerState.READY_FOR_INTAKE;
    }
    
    /**
     * Verifica si está en posición de outtake.
     * 
     * @return true si está en READY_FOR_OUTTAKE
     */
    public boolean isInOuttakePosition() {
        return currentState == SpindexerState.READY_FOR_OUTTAKE;
    }
    
    /**
     * Verifica si está en estado IDLE.
     * 
     * @return true si está IDLE
     */
    public boolean isIdle() {
        return currentState == SpindexerState.IDLE;
    }
    
    /**
     * Verifica si todos los slots están llenos.
     * 
     * @return true si los 3 slots tienen pelotas
     */
    public boolean areAllSlotsFull() {
        for (SlotState state : slotStates) {
            if (state == SlotState.EMPTY) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Verifica si hay al menos un slot vacío.
     * 
     * @return true si hay espacio disponible
     */
    public boolean hasEmptySlot() {
        for (SlotState state : slotStates) {
            if (state == SlotState.EMPTY) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Cuenta cuántos slots tienen pelotas.
     * 
     * @return Número de slots llenos (0-3)
     */
    public int getFilledSlotCount() {
        int count = 0;
        for (SlotState state : slotStates) {
            if (state != SlotState.EMPTY) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Encuentra el primer slot con el color especificado.
     * 
     * @param color Color a buscar (YELLOW o PURPLE)
     * @return Índice del slot, o -1 si no se encuentra
     */
    public int getFirstSlotWithColor(SlotState color) {
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] == color) {
                return i;
            }
        }
        return -1;
    }
    
    // ==================== MÉTODOS PARA AUTÓNOMO ====================
    
    /**
     * Configura la precarga para el inicio del autónomo.
     * Establece el estado inicial de los slots con pelotas precargadas.
     * 
     * @param slot0 Estado del slot 0 (EMPTY, YELLOW, o PURPLE)
     * @param slot1 Estado del slot 1 (EMPTY, YELLOW, o PURPLE)
     * @param slot2 Estado del slot 2 (EMPTY, YELLOW, o PURPLE)
     * 
     * Ejemplo:
     * // Si precargaste 1 amarilla en slot 0
     * spindexer.configurePreload(SlotState.YELLOW, SlotState.EMPTY, SlotState.EMPTY);
     */
    public void configurePreload(SlotState slot0, SlotState slot1, SlotState slot2) {
        slotStates[0] = slot0;
        slotStates[1] = slot1;
        slotStates[2] = slot2;
    }
    
    /**
     * Genera una secuencia de lanzamiento ordenada por color.
     * Útil para seguir la secuencia de color de la partida.
     * 
     * @param colorSequence Array con la secuencia de colores deseada
     * @return Array con índices de slots en orden de lanzamiento, -1 indica slot no disponible
     * 
     * Ejemplo:
     * // Secuencia de partida: [PURPLE, YELLOW, YELLOW]
     * // Slots actuales: slot0=YELLOW, slot1=PURPLE, slot2=YELLOW
     * SlotState[] sequence = {SlotState.PURPLE, SlotState.YELLOW, SlotState.YELLOW};
     * int[] order = spindexer.getColorOrderedSequence(sequence);
     * // Resultado: [1, 0, 2] - lanza slot 1 (morada), luego 0 y 2 (amarillas)
     */
    public int[] getColorOrderedSequence(SlotState[] colorSequence) {
        int[] result = new int[3];
        boolean[] used = new boolean[3];
        
        for (int i = 0; i < colorSequence.length; i++) {
            result[i] = -1;  // Default: no encontrado
            
            // Buscar slot con el color requerido que no haya sido usado
            for (int slotIdx = 0; slotIdx < 3; slotIdx++) {
                if (!used[slotIdx] && slotStates[slotIdx] == colorSequence[i]) {
                    result[i] = slotIdx;
                    used[slotIdx] = true;
                    break;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Genera una secuencia de lanzamiento por proximidad.
     * Dispara en orden de menor movimiento del servo desde la posición actual.
     * 
     * @return Array con índices de slots en orden de lanzamiento, -1 indica slot vacío
     * 
     * Ejemplo:
     * // Si estás en slot 0 y tienes: slot0=YELLOW, slot1=EMPTY, slot2=PURPLE
     * int[] order = spindexer.getProximityOrderedSequence();
     * // Resultado: [0, 2, -1] - lanza slot 0 primero (ya estás ahí), 
     * // luego slot 2, slot 1 está vacío
     */
    public int[] getProximityOrderedSequence() {
        int[] result = new int[3];
        int resultIdx = 0;
        
        // Empezar desde el slot actual y recorrer circularmente
        for (int offset = 0; offset < 3; offset++) {
            int slotIdx = (currentSlotIndex + offset) % 3;
            
            if (slotStates[slotIdx] != SlotState.EMPTY) {
                result[resultIdx++] = slotIdx;
            }
        }
        
        // Rellenar con -1 los slots vacíos
        while (resultIdx < 3) {
            result[resultIdx++] = -1;
        }
        
        return result;
    }
    
    // ==================== CONFIGURACIÓN ====================
    
    /**
     * Configura el número de samples para votación.
     * 
     * @param samples Número de lecturas (recomendado: 15-25)
     */
    public void setVotingSamples(int samples) {
        this.votingSamples = Math.max(5, Math.min(50, samples));
    }
    
    /**
     * Configura el delay entre samples durante votación.
     * 
     * @param delayMs Delay en milisegundos (recomendado: 30-100)
     */
    public void setVotingDelay(int delayMs) {
        this.votingDelayMs = Math.max(10, Math.min(200, delayMs));
    }
    
    /**
     * Marca el spindexer como listo para intake.
     * Llamar después de que el servo complete su movimiento.
     */
    public void setReadyForIntake() {
        currentState = SpindexerState.READY_FOR_INTAKE;
    }
    
    /**
     * Marca el spindexer como listo para outtake.
     * Llamar después de que el servo complete su movimiento.
     */
    public void setReadyForOuttake() {
        currentState = SpindexerState.READY_FOR_OUTTAKE;
    }
    
    // ==================== PERIODIC ====================
    
    @Override
    public void periodic() {
        // El spindexer no requiere lógica periódica compleja
        // Los comandos manejan las transiciones de estado
        
        // Aquí podrías agregar verificaciones de timeout si es necesario
        // Por ejemplo, si el servo no llega a posición en X segundos
    }
    
    // ==================== TELEMETRÍA ====================
    
    /**
     * Obtiene una cadena con información del estado para telemetría.
     * 
     * @return String con estado completo
     */
    public String getTelemetryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Estado: ").append(currentState.name()).append("\n");
        sb.append("Slot Actual: ").append(currentSlotIndex).append("\n");
        sb.append("Slots: [");
        for (int i = 0; i < 3; i++) {
            sb.append(getSlotEmoji(i));
            if (i < 2) sb.append(" ");
        }
        sb.append("]\n");
        sb.append("Pelotas: ").append(getFilledSlotCount()).append("/3");
        return sb.toString();
    }
    
    /**
     * Obtiene emoji representando el estado de un slot.
     * Útil para telemetría visual.
     * 
     * @param slotIndex Índice del slot
     * @return Emoji representando el estado
     */
    public String getSlotEmoji(int slotIndex) {
        validateSlotIndex(slotIndex);
        switch (slotStates[slotIndex]) {
            case YELLOW: return "🟡";
            case PURPLE: return "🟣";
            case UNKNOWN: return "❓";
            case EMPTY:
            default: return "⚫";
        }
    }
    
    /**
     * Obtiene el nombre del estado como String.
     * 
     * @return Nombre del estado actual
     */
    public String getStateName() {
        return currentState.name();
    }
    
    // ==================== MÉTODOS PRIVADOS ====================
    
    /**
     * Obtiene el ángulo de servo para posición de intake.
     */
    private double getIntakeAngle(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_INTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_INTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_INTAKE_POSITION;
            default: throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
    }
    
    /**
     * Obtiene el ángulo de servo para posición de outtake.
     */
    private double getOuttakeAngle(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_OUTTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_OUTTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_OUTTAKE_POSITION;
            default: throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
    }
    
    /**
     * Valida que el índice de slot sea válido (0, 1, 2).
     */
    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 2) {
            throw new IllegalArgumentException(
                "Invalid slot index: " + slotIndex + ". Must be 0, 1, or 2."
            );
        }
    }
    
    /**
     * Verifica que el sensor esté disponible.
     * Lanza excepción si no lo está.
     */
    private void requireSensor() {
        if (!useSensor) {
            throw new IllegalStateException(
                "Color sensor not available. " +
                "Use SpindexerSubsystem(hardwareMap, true) to enable sensor."
            );
        }
    }
    
    /**
     * Obtiene acceso directo al sensor de color.
     * Útil para lecturas personalizadas o debugging.
     * 
     * @return El sensor de color
     * @throws IllegalStateException si el sensor no está disponible
     */
    public SensorRevColorV3 getColorSensor() {
        requireSensor();
        return colorSensor;
    }
}