package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.hardware.servos.ServoEx;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

/**
 * Subsystem para controlar el mecanismo de indexado con 3 slots
 * Cada slot puede rotar a posiciones de intake (0°, 120°, 240°)
 * y posiciones de outtake (60°, 180°, 300°)
 */
public class IndexerSubsystem {

    // ==================== ENUMS ====================

    /**
     * Estados principales del indexer para coordinación con otros subsystems
     */
    public enum IndexerState {
        IDLE,                    // En reposo, esperando comandos
        MOVING_TO_INTAKE,        // Rotando hacia posición de intake
        READY_FOR_INTAKE,        // En posición, esperando que entre pelota
        DETECTING_BALL,          // Procesando lectura del sensor
        MOVING_TO_NEXT,          // Rotando al siguiente slot
        MOVING_TO_OUTTAKE,       // Rotando hacia posición de outtake
        READY_FOR_OUTTAKE,       // En posición de outtake, listo para lanzar
        EJECTING,                // Eyectando pelota
        SEQUENCING               // Ejecutando secuencia compleja
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

    /**
     * Colores detectables
     */
    public enum BallColor {
        YELLOW,
        PURPLE,
        NONE
    }

    // ==================== CONSTANTES ====================

    // Posiciones de intake (en grados del servo)
    private static final double INTAKE_POSITION_0 = 0.0;
    private static final double INTAKE_POSITION_1 = 120.0;
    private static final double INTAKE_POSITION_2 = 240.0;

    // Posiciones de outtake (en grados del servo)
    private static final double OUTTAKE_POSITION_0 = 60.0;
    private static final double OUTTAKE_POSITION_1 = 180.0;
    private static final double OUTTAKE_POSITION_2 = 300.0;

    // Rango del servo (0 a 300 grados)
    private static final double SERVO_MIN_ANGLE = 0.0;
    private static final double SERVO_MAX_ANGLE = 300.0;

    // Umbral de distancia para detectar pelota (en cm)
    private static final double BALL_DETECTION_DISTANCE = 4.0;

    // Umbrales de color normalizados (AJUSTAR SEGÚN PRUEBAS)
    private static final double YELLOW_RED_MIN = 0.35;
    private static final double YELLOW_GREEN_MIN = 0.35;
    private static final double YELLOW_BLUE_MAX = 0.25;

    private static final double PURPLE_BLUE_MIN = 0.40;
    private static final double PURPLE_RED_MIN = 0.30;
    private static final double PURPLE_GREEN_MAX = 0.30;

    // ==================== HARDWARE ====================

    private final ServoEx indexerServo;
    private final ColorSensor colorSensor;
    private final DistanceSensor distanceSensor;
    private final boolean hasSensor; // Indica si el sensor está disponible

    // ==================== ESTADO ====================

    private IndexerState currentState;
    private SlotState[] slotStates; // Estado de cada uno de los 3 slots
    private int currentSlotIndex;   // Slot actual (0, 1, 2)
    private int targetSlotIndex;    // Slot objetivo

    // ==================== CONSTRUCTOR ====================

    /**
     * Constructor principal - sin sensor de color
     * Usa este constructor cuando no tengas el sensor instalado
     */
    public IndexerSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, false);
    }

    /**
     * Constructor completo - con opción de sensor
     * @param hardwareMap El hardware map del robot
     * @param useSensor true si el sensor de color está instalado y configurado
     */
    public IndexerSubsystem(HardwareMap hardwareMap, boolean useSensor) {
        // Inicializar servo con ServoEx para control avanzado
        indexerServo = new ServoEx(
                hardwareMap,
                "indexerServo",
                SERVO_MIN_ANGLE,
                SERVO_MAX_ANGLE
        );

        // Inicializar sensores solo si están disponibles
        this.hasSensor = useSensor;
        if (hasSensor) {
            try {
                colorSensor = hardwareMap.get(ColorSensor.class, "colorSensor");
                distanceSensor = hardwareMap.get(DistanceSensor.class, "colorSensor");
            } catch (Exception e) {
                throw new RuntimeException("Color sensor not found in hardware map. " +
                        "Use IndexerSubsystem(hardwareMap) constructor if sensor is not installed.");
            }
        } else {
            colorSensor = null;
            distanceSensor = null;
        }

        // Inicializar estados
        currentState = IndexerState.IDLE;
        slotStates = new SlotState[3];
        for (int i = 0; i < 3; i++) {
            slotStates[i] = SlotState.EMPTY;
        }
        currentSlotIndex = 1; // Empezar en slot 1
        targetSlotIndex = 1;

        // IMPORTANTE: Posicionar en OUTTAKE para precarga del autónomo
        // Esto permite que empieces con pelotas ya cargadas
        moveToOuttakePosition(1);
    }

    // ==================== MÉTODOS DE POSICIONAMIENTO ====================

    /**
     * Mueve el indexer a la posición de intake del slot especificado
     */
    public void moveToIntakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        double angle = getIntakeAngle(slotIndex);
        indexerServo.set(angle);
        currentSlotIndex = slotIndex;
        currentState = IndexerState.MOVING_TO_INTAKE;
    }

    /**
     * Mueve el indexer a la posición de outtake del slot especificado
     */
    public void moveToOuttakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        double angle = getOuttakeAngle(slotIndex);
        indexerServo.set(angle);
        currentSlotIndex = slotIndex;
        currentState = IndexerState.MOVING_TO_OUTTAKE;
    }

    /**
     * Rota al siguiente slot en posición de intake
     */
    public void moveToNextIntakeSlot() {
        currentSlotIndex = (currentSlotIndex + 1) % 3;
        moveToIntakePosition(currentSlotIndex);
    }

    // ==================== MÉTODOS DE DETECCIÓN ====================

    /**
     * Detecta si hay una pelota presente en el slot actual
     * @return true si hay pelota, false si no (o si no hay sensor)
     */
    public boolean isBallPresent() {
        if (!hasSensor || distanceSensor == null) {
            return false; // Sin sensor, no podemos detectar automáticamente
        }
        double distance = distanceSensor.getDistance(DistanceUnit.CM);
        return distance <= BALL_DETECTION_DISTANCE;
    }

    /**
     * Detecta el color de la pelota en el slot actual
     * @return BallColor detectado, o NONE si no hay sensor o no hay pelota
     */
    public BallColor detectBallColor() {
        if (!hasSensor || colorSensor == null) {
            return BallColor.NONE; // Sin sensor, no podemos detectar color
        }

        if (!isBallPresent()) {
            return BallColor.NONE;
        }

        int red = colorSensor.red();
        int green = colorSensor.green();
        int blue = colorSensor.blue();

        double total = red + green + blue;
        if (total == 0) {
            return BallColor.NONE;
        }

        double redNorm = red / total;
        double greenNorm = green / total;
        double blueNorm = blue / total;

        // Detectar amarillo
        if (redNorm >= YELLOW_RED_MIN &&
                greenNorm >= YELLOW_GREEN_MIN &&
                blueNorm <= YELLOW_BLUE_MAX) {
            return BallColor.YELLOW;
        }

        // Detectar morado
        if (blueNorm >= PURPLE_BLUE_MIN &&
                redNorm >= PURPLE_RED_MIN &&
                greenNorm <= PURPLE_GREEN_MAX) {
            return BallColor.PURPLE;
        }

        return BallColor.NONE;
    }

    /**
     * Actualiza el estado del slot actual basado en el sensor
     * Si no hay sensor, este método no hace nada (usa setSlotState manual)
     */
    public void updateCurrentSlotState() {
        if (!hasSensor) {
            return; // Sin sensor, actualizar manualmente con setSlotState()
        }

        BallColor color = detectBallColor();

        switch (color) {
            case YELLOW:
                slotStates[currentSlotIndex] = SlotState.YELLOW;
                break;
            case PURPLE:
                slotStates[currentSlotIndex] = SlotState.PURPLE;
                break;
            case NONE:
                slotStates[currentSlotIndex] = SlotState.EMPTY;
                break;
        }
    }

    /**
     * Verifica si el subsystem tiene sensor de color instalado
     */
    public boolean hasSensor() {
        return hasSensor;
    }

    // ==================== MÉTODOS DE ESTADO ====================

    /**
     * Actualiza el estado del subsystem
     * Llamar en el loop principal
     */
    public void update() {
        // Verificar si el servo llegó a su posición
        // ServoEx no tiene método para verificar si llegó, pero podemos usar tiempo

        switch (currentState) {
            case MOVING_TO_INTAKE:
                // Verificar si llegó (simplificado por ahora)
                currentState = IndexerState.READY_FOR_INTAKE;
                break;

            case MOVING_TO_OUTTAKE:
                currentState = IndexerState.READY_FOR_OUTTAKE;
                break;

            case DETECTING_BALL:
                updateCurrentSlotState();
                currentState = IndexerState.IDLE;
                break;
        }
    }

    /**
     * Inicia detección del color de pelota en slot actual
     */
    public void startBallDetection() {
        currentState = IndexerState.DETECTING_BALL;
    }

    // ==================== GETTERS ====================

    public IndexerState getCurrentState() {
        return currentState;
    }

    public SlotState getSlotState(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slotStates[slotIndex];
    }

    public SlotState getCurrentSlotState() {
        return slotStates[currentSlotIndex];
    }

    public int getCurrentSlotIndex() {
        return currentSlotIndex;
    }

    public boolean isInIntakePosition() {
        return currentState == IndexerState.READY_FOR_INTAKE;
    }

    public boolean isInOuttakePosition() {
        return currentState == IndexerState.READY_FOR_OUTTAKE;
    }

    public boolean isIdle() {
        return currentState == IndexerState.IDLE;
    }

    /**
     * Verifica si todos los slots están llenos
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
     * Cuenta cuántos slots tienen pelotas
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
     * Encuentra el primer slot vacío
     * @return índice del slot vacío, o -1 si todos están llenos
     */
    public int getFirstEmptySlot() {
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] == SlotState.EMPTY) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Encuentra el primer slot con el color especificado
     * @return índice del slot, o -1 si no se encuentra
     */
    public int getFirstSlotWithColor(SlotState color) {
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] == color) {
                return i;
            }
        }
        return -1;
    }

    // ==================== SETTERS MANUALES ====================

    /**
     * Establece manualmente el estado de un slot
     * (útil para debugging o si el sensor falla)
     */
    public void setSlotState(int slotIndex, SlotState state) {
        validateSlotIndex(slotIndex);
        slotStates[slotIndex] = state;
    }

    /**
     * Limpia el estado de un slot (lo marca como vacío)
     */
    public void clearSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        slotStates[slotIndex] = SlotState.EMPTY;
    }

    /**
     * Limpia todos los slots
     */
    public void clearAllSlots() {
        for (int i = 0; i < 3; i++) {
            slotStates[i] = SlotState.EMPTY;
        }
    }

    // ==================== MÉTODOS PARA AUTÓNOMO ====================

    /**
     * Configura la precarga para el inicio del autónomo
     * Útil para establecer el estado inicial de los slots con pelotas precargadas
     *
     * @param slot0 Estado del slot 0 (EMPTY, YELLOW, o PURPLE)
     * @param slot1 Estado del slot 1 (EMPTY, YELLOW, o PURPLE)
     * @param slot2 Estado del slot 2 (EMPTY, YELLOW, o PURPLE)
     *
     * Ejemplo:
     * // Si precargaste 2 amarillas y 1 morada
     * indexer.configurePreload(SlotState.YELLOW, SlotState.YELLOW, SlotState.PURPLE);
     */
    public void configurePreload(SlotState slot0, SlotState slot1, SlotState slot2) {
        slotStates[0] = slot0;
        slotStates[1] = slot1;
        slotStates[2] = slot2;
    }

    /**
     * Configura todos los slots con el mismo color (útil para precarga uniforme)
     */
    public void configurePreloadAll(SlotState color) {
        for (int i = 0; i < 3; i++) {
            slotStates[i] = color;
        }
    }

    /**
     * Genera una secuencia de lanzamiento ordenada por color
     * @param preferredColor Color a lanzar primero (YELLOW o PURPLE)
     * @return Array con índices de slots en orden de lanzamiento, -1 indica slot vacío
     *
     * Ejemplo:
     * // Si tienes: slot0=YELLOW, slot1=PURPLE, slot2=YELLOW
     * // Y prefieres YELLOW primero:
     * int[] order = getColorOrderedSequence(SlotState.YELLOW);
     * // Resultado: [0, 2, 1] - lanza slots 0 y 2 (amarillas) antes que 1 (morada)
     */
    public int[] getColorOrderedSequence(SlotState preferredColor) {
        int[] sequence = new int[3];
        int index = 0;

        // Primero, agregar slots con el color preferido
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] == preferredColor) {
                sequence[index++] = i;
            }
        }

        // Luego, agregar los demás slots que no están vacíos
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] != SlotState.EMPTY && slotStates[i] != preferredColor) {
                sequence[index++] = i;
            }
        }

        // Rellenar el resto con -1
        for (int i = index; i < 3; i++) {
            sequence[i] = -1;
        }

        return sequence;
    }

    /**
     * Genera una secuencia de lanzamiento simple (en orden: 0, 1, 2)
     * Solo incluye slots que no están vacíos
     * @return Array con índices de slots en orden, -1 indica que no hay más
     */
    public int[] getSimpleSequence() {
        int[] sequence = new int[3];
        int index = 0;

        for (int i = 0; i < 3; i++) {
            if (slotStates[i] != SlotState.EMPTY) {
                sequence[index++] = i;
            }
        }

        // Rellenar el resto con -1
        for (int i = index; i < 3; i++) {
            sequence[i] = -1;
        }

        return sequence;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private double getIntakeAngle(int slotIndex) {
        switch (slotIndex) {
            case 0: return INTAKE_POSITION_0;
            case 1: return INTAKE_POSITION_1;
            case 2: return INTAKE_POSITION_2;
            default: throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
    }

    private double getOuttakeAngle(int slotIndex) {
        switch (slotIndex) {
            case 0: return OUTTAKE_POSITION_0;
            case 1: return OUTTAKE_POSITION_1;
            case 2: return OUTTAKE_POSITION_2;
            default: throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 2) {
            throw new IllegalArgumentException("Slot index must be 0, 1, or 2. Got: " + slotIndex);
        }
    }

    // ==================== MÉTODOS DE TELEMETRÍA ====================

    /**
     * Obtiene una cadena con información del estado para telemetría
     */
    public String getTelemetryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Estado: ").append(currentState).append("\n");
        sb.append("Slot Actual: ").append(currentSlotIndex).append("\n");
        sb.append("Slots: [");
        for (int i = 0; i < 3; i++) {
            sb.append(slotStates[i]);
            if (i < 2) sb.append(", ");
        }
        sb.append("]\n");
        sb.append("Pelotas: ").append(getFilledSlotCount()).append("/3\n");
        sb.append("Sensor: ").append(hasSensor ? "Instalado" : "NO instalado");
        return sb.toString();
    }
}