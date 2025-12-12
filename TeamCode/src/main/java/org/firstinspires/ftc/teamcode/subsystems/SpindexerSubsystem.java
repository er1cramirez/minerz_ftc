package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;

/**
 * Subsystem que controla el spindexer (indexador rotativo de 3 slots).
 *
 * RESPONSABILIDADES:
 * - Rotar entre posiciones de intake (donde entra pelota) y outtake (donde se lanza)
 * - Detectar presencia de pelota mediante sensor de distancia
 * - Identificar color de pelota (amarillo/púrpura)
 * - Mantener estado de cada slot (vacío/color)
 * - Proveer información para generar secuencias de lanzamiento
 *
 * IMPORTANTE:
 * - El subsystem NO ejecuta secuencias automáticas (eso es responsabilidad de Commands)
 * - El subsystem NO maneja delays o timers (eso es responsabilidad de Commands)
 * - Métodos de detección son INSTANTÁNEOS (una lectura), no bloqueantes
 *
 * DISEÑO DEL MECANISMO:
 * - 3 slots distribuidos cada 120° (0°, 120°, 240°)
 * - Posición de intake: donde el slot recibe pelotas del intake
 * - Posición de outtake: donde el slot presenta pelota al shooter (desfase 60°)
 * - Sensor fijo en posición de intake para detectar presencia y color
 */
public class SpindexerSubsystem extends SubsystemBase {

    // ==================== ENUMS ====================

    /**
     * Estados del spindexer.
     * SIMPLIFICADO: Solo refleja posición física, no procesos.
     */
    public enum SpindexerState {
        IDLE,        // En reposo, posición arbitraria
        AT_INTAKE,   // En posición de intake (slot bajo sensor)
        AT_OUTTAKE   // En posición de outtake (slot frente a shooter)
    }

    /**
     * Estado de cada slot individual.
     */
    public enum SlotState {
        EMPTY,      // Slot vacío
        YELLOW,     // Pelota amarilla
        PURPLE,     // Pelota morada
        UNKNOWN     // Hay algo pero color no identificado
    }

    /**
     * Resultado de lectura de color.
     */
    public enum BallColor {
        YELLOW,     // Pelota amarilla detectada
        PURPLE,     // Pelota púrpura/morada detectada
        NONE,       // Sin pelota (distancia > umbral)
        UNKNOWN     // Hay algo pero color no identificado claramente
    }

    // ==================== HARDWARE ====================

    private final Servo indexerServo;
    private final SensorRevColorV3 colorSensor;
    private final boolean useSensor;

    // ==================== ESTADO ====================

    private SpindexerState currentState;
    private SlotState[] slotStates;      // Estado de cada uno de los 3 slots
    private int currentSlotIndex;        // Slot actualmente en posición (0, 1, 2)

    // ==================== CONSTRUCTORES ====================

    /**
     * Constructor sin sensor (solo control de posición).
     * Útil para testing o si el sensor no está disponible.
     */
    public SpindexerSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, false);
    }

    /**
     * Constructor completo con opción de sensor.
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
    }

    // ==================== MÉTODOS DE ACCIÓN ====================

    /**
     * Mueve el spindexer a la posición de INTAKE del slot especificado.
     * El slot queda bajo el sensor de color para detección.
     *
     * @param slotIndex Índice del slot (0, 1, 2)
     */
    public void moveToIntakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        double position = getIntakePosition(slotIndex);
        indexerServo.setPosition(position);
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.AT_INTAKE;
    }

    /**
     * Mueve el spindexer a la posición de OUTTAKE del slot especificado.
     * El slot queda frente al shooter para lanzar.
     *
     * @param slotIndex Índice del slot (0, 1, 2)
     */
    public void moveToOuttakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        double position = getOuttakePosition(slotIndex);
        indexerServo.setPosition(position);
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.AT_OUTTAKE;
    }

    /**
     * Detiene el spindexer y marca como IDLE.
     * Útil al final de secuencias.
     */
    public void idle() {
        currentState = SpindexerState.IDLE;
    }

    // ==================== MÉTODOS DE CONSULTA - ESTADO ====================

    /**
     * Obtiene el estado actual del spindexer.
     */
    public SpindexerState getState() {
        return currentState;
    }

    /**
     * Verifica si está en posición de intake.
     */
    public boolean isAtIntake() {
        return currentState == SpindexerState.AT_INTAKE;
    }

    /**
     * Verifica si está en posición de outtake.
     */
    public boolean isAtOuttake() {
        return currentState == SpindexerState.AT_OUTTAKE;
    }

    /**
     * Verifica si está en idle.
     */
    public boolean isIdle() {
        return currentState == SpindexerState.IDLE;
    }

    /**
     * Obtiene el índice del slot actualmente en posición.
     */
    public int getCurrentSlotIndex() {
        return currentSlotIndex;
    }

    /**
     * Obtiene la posición actual del servo (0.0 - 1.0).
     */
    public double getServoPosition() {
        return indexerServo.getPosition();
    }

    /**
     * Obtiene el estado del slot actual.
     */
    public SlotState getCurrentSlotState() {
        return getSlotState(currentSlotIndex);
    }

    // ==================== MÉTODOS DE CONSULTA - SLOTS ====================

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
     * Verifica si un slot específico está vacío.
     */
    public boolean isSlotEmpty(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slotStates[slotIndex] == SlotState.EMPTY;
    }

    /**
     * Verifica si hay al menos un slot vacío.
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
     * Verifica si todos los slots están llenos.
     */
    public boolean isFull() {
        return !hasEmptySlot();
    }

    /**
     * Obtiene el número de slots ocupados.
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
     * Obtiene el número de slots vacíos.
     */
    public int getEmptySlotCount() {
        return 3 - getFilledSlotCount();
    }

    /**
     * Verifica si el spindexer está completamente vacío.
     */
    public boolean isEmpty() {
        return getFilledSlotCount() == 0;
    }

    /**
     * Obtiene el estado de todos los slots como array.
     * Útil para telemetría o debugging.
     *
     * @return Array de 3 elementos con el estado de cada slot
     */
    public SlotState[] getAllSlotStates() {
        return slotStates.clone();
    }

    // ==================== MÉTODOS DE MODIFICACIÓN - SLOTS ====================

    /**
     * Establece el estado de un slot específico.
     *
     * @param slotIndex Índice del slot (0, 1, 2)
     * @param state Nuevo estado del slot
     */
    public void setSlotState(int slotIndex, SlotState state) {
        validateSlotIndex(slotIndex);
        slotStates[slotIndex] = state;
    }

    /**
     * Marca un slot como vacío.
     * Útil después de lanzar una pelota.
     */
    public void clearSlot(int slotIndex) {
        validateSlotIndex(slotIndex);
        slotStates[slotIndex] = SlotState.EMPTY;
    }

    /**
     * Marca el slot actual como vacío.
     */
    public void clearCurrentSlot() {
        slotStates[currentSlotIndex] = SlotState.EMPTY;
    }

    /**
     * Marca todos los slots como vacíos.
     * Útil al inicio de autonomous o para reset.
     */
    public void clearAllSlots() {
        for (int i = 0; i < 3; i++) {
            slotStates[i] = SlotState.EMPTY;
        }
    }

    /**
     * Establece el estado de todos los slots de una vez.
     *
     * @param slot0 Estado del slot 0
     * @param slot1 Estado del slot 1
     * @param slot2 Estado del slot 2
     */
    public void setAllSlots(SlotState slot0, SlotState slot1, SlotState slot2) {
        slotStates[0] = slot0;
        slotStates[1] = slot1;
        slotStates[2] = slot2;
    }

    // ==================== MÉTODOS DE DETECCIÓN Y LÓGICA ====================

    /**
     * Detecta el color de la pelota actual y actualiza el estado del slot automáticamente.
     * 
     * @return El color detectado
     * @throws IllegalStateException si no está en posición de intake
     */
    public BallColor autoDetectAndLabel() {
        if (!isAtIntake()) {
            throw new IllegalStateException("Spindexer must be at intake position to detect.");
        }
        
        BallColor color = readCurrentColor();
        SlotState newState;
        
        switch (color) {
            case YELLOW:
                newState = SlotState.YELLOW;
                break;
            case PURPLE:
                newState = SlotState.PURPLE;
                break;
            case NONE:
                newState = SlotState.EMPTY;
                break;
            default:
                newState = SlotState.UNKNOWN;
                break;
        }
        
        setSlotState(currentSlotIndex, newState);
        return color;
    }

    // ==================== MÉTODOS DE DETECCIÓN - SENSOR ====================

    /**
     * Verifica si hay una pelota presente bajo el sensor.
     * Usa medición de distancia instantánea.
     *
     * @return true si detecta pelota (distancia < umbral)
     * @throws IllegalStateException si el sensor no está disponible
     */
    public boolean isBallDetected() {
        ensureSensorAvailable();
        double distance = colorSensor.distance();
        return distance < SpindexerConstants.BALL_DETECTION_DISTANCE;
    }

    /**
     * Lee el color actual bajo el sensor (una sola muestra).
     * Para detección confiable, llamar múltiples veces desde un comando.
     *
     * @return Color detectado en esta lectura instantánea
     * @throws IllegalStateException si el sensor no está disponible
     */
    public BallColor readCurrentColor() {
        ensureSensorAvailable();
        return readSingleColorSample();
    }

    /**
     * Obtiene los valores raw ARGB del sensor.
     * Útil para calibración o debugging.
     *
     * @return Array [Alpha, Red, Green, Blue]
     * @throws IllegalStateException si el sensor no está disponible
     */
    public int[] getRawColorValues() {
        ensureSensorAvailable();
        return colorSensor.getARGB();
    }

    /**
     * Obtiene la distancia actual del sensor en cm.
     * Útil para calibración o debugging.
     *
     * @return Distancia en centímetros
     * @throws IllegalStateException si el sensor no está disponible
     */
    public double getDistance() {
        ensureSensorAvailable();
        return colorSensor.distance();
    }

    /**
     * Verifica si el sensor está disponible.
     *
     * @return true si el sensor fue inicializado
     */
    public boolean hasSensor() {
        return useSensor;
    }

    // ==================== MÉTODOS DE SECUENCIA ====================

    /**
     * Genera una secuencia de lanzamiento ordenada por color.
     * Útil para seguir la secuencia de color de la partida.
     *
     * @param colorSequence Array con la secuencia de colores deseada (3 elementos)
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
        if (colorSequence.length != 3) {
            throw new IllegalArgumentException("Color sequence must have exactly 3 elements");
        }

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

    /**
     * Obtiene el índice del primer slot vacío encontrado.
     * Busca circularmente desde el slot actual.
     *
     * @return Índice del primer slot vacío, o -1 si todos están llenos
     */
    public int getFirstEmptySlotIndex() {
        for (int i = 0; i < 3; i++) {
            int slotIdx = (currentSlotIndex + i) % 3;
            if (slotStates[slotIdx] == SlotState.EMPTY) {
                return slotIdx;
            }
        }
        return -1;  // Todos llenos
    }

    /**
     * Obtiene el índice del próximo slot (circularmente).
     * No verifica si está vacío.
     *
     * @return Índice del siguiente slot
     */
    public int getNextSlotIndex() {
        return (currentSlotIndex + 1) % 3;
    }

    // ==================== PERIODIC ====================

    @Override
    public void periodic() {
        // El spindexer no requiere lógica periódica.
        // Los comandos manejan las transiciones y verificaciones.
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
     * Lee una única muestra de color del sensor.
     * Método interno usado por readCurrentColor().
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

        // Ambos o ninguno = desconocido
        return BallColor.UNKNOWN;
    }

    /**
     * Obtiene la posición del servo para intake de un slot.
     */
    private double getIntakePosition(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_INTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_INTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_INTAKE_POSITION;
            default: throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
    }

    /**
     * Obtiene la posición del servo para outtake de un slot.
     */
    private double getOuttakePosition(int slotIndex) {
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
    private void ensureSensorAvailable() {
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
        ensureSensorAvailable();
        return colorSensor;
    }
}