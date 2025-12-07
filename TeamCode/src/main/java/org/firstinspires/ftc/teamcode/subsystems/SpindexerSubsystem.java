package org.firstinspires.ftc.teamcode.subsystems;


import com.seattlesolvers.solverslib.command.SubsystemBase;

public class SpindexerSubsystem extends SubsystemBase {
    
    public enum SpindexerState {
        IDLE,                    // En reposo, esperando comandos
        MOVING_TO_INTAKE,        // Rotando hacia posición de intake
        READY_FOR_INTAKE,        // En posición, esperando que entre pelota
        DETECTING_BALL,          // Procesando lectura del sensor
        MOVING_TO_NEXT,          // Rotando al siguiente slot
        MOVING_TO_OUTTAKE,       // Rotando hacia posición de outtake
        READY_FOR_OUTTAKE       // En posición de outtake, listo para lanzar
    }

    public enum SlotState {
        EMPTY,          // Slot vacío
        YELLOW,         // Pelota amarilla
        PURPLE,         // Pelota morada
        UNKNOWN         // Hay algo pero color no identificado
    }
    
    private final ServoEx indexerServo;
    private final ColorSensor colorSensor;
    private final DistanceSensor distanceSensor;
    private final boolean useSensor; // Indica si el sensor está disponible


    private SpindexerState currentState;
    private SlotState[] slotStates; // Estado de cada uno de los 3 slots
    private int currentSlotIndex;   // Slot actual (0, 1, 2)
    private int targetSlotIndex;    // Slot objetivo

    public SpindexerSubsystem(HardwareMap hardwareMap) {
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
            SERVO_NAME
        );
        
        // Inicializar sensores solo si están disponibles
        this.useSensor = useSensor;
        if (useSensor) {
            try {
                colorSensor = hardwareMap.get(ColorSensor.class, "colorSensor");
                distanceSensor = hardwareMap.get(DistanceSensor.class, "colorSensor");
            } catch (Exception e) {
                throw new RuntimeException("Color sensor not found in hardware map. " +
                    "Use SpindexerSubsystem(hardwareMap) constructor if sensor is not installed.");
            }
        } else {
            colorSensor = null;
            distanceSensor = null;
        }
        
        // Inicializar estados
        currentState = SpindexerState.IDLE;
        slotStates = new SlotState[3];
      
        // moveToOuttakePosition(1);
        // Considerar que en auto se desea iniciar en posición de outtake ya que se empieza con una pelota cargada
        // Para teleop, iniciar en posición de intake, almenos que en el auto se haya dejado en outtake
    }

    public void moveToIntakePosition(int slotIndex) {
        double angle = getIntakeAngle(slotIndex);
        indexerServo.set(angle);
        currentSlotIndex = slotIndex;
        currentState = IndexerState.MOVING_TO_INTAKE;
    }

    public void moveToOuttakePosition(int slotIndex) {
        double angle = getOuttakeAngle(slotIndex);
        indexerServo.set(angle);
        currentSlotIndex = slotIndex;
        currentState = IndexerState.MOVING_TO_OUTTAKE;
    }

    public void moveToNextIntakeSlot() {
        currentSlotIndex = (currentSlotIndex + 1) % 3;
        moveToIntakePosition(currentSlotIndex);
    }

    public void moveClosestEmptySlot() {
        for (int i = 0; i < 3; i++) {
            int slotToCheck = (currentSlotIndex + i) % 3;
            if (slotStates[slotToCheck] == SlotState.EMPTY) {
                moveToIntakePosition(slotToCheck);
                return;
            }
        }
        // Si no hay slots vacíos, permanecer en el actual
    }

    public boolean isBallDetected() {
        if (!useSensor) {
            throw new IllegalStateException("Color sensor not available.");
        }
        double distance = distanceSensor.getDistance(DistanceUnit.CM);
        return distance < BALL_DETECTION_DISTANCE;
        //
    }
    
    public BallColor detectBallColor() {
        if (!useSensor) {
            throw new IllegalStateException("Color sensor not available.");
        }
        
    }

    public autoIndexIntakedBall() {
        if (!useSensor) {
            throw new IllegalStateException("Color sensor not available.");
        }
    }

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
    

    /**
     * Establece manualmente el estado de un slot
     * (útil si no se usa el sensor de color y se quiere indexar manualmente)
     * Usar algun boton para cada color
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
     * Genera una secuencia de lanzamiento ordenada por color
     * @param colorSeq Array con la secuencia de colores deseada (ej. [PURPLE, YELLOW, YELLOW])
     * @return Array con índices de slots en orden de lanzamiento, -1 indica slot vacío
     * 
     * Ejemplo:
     * // Si tienes: slot0=YELLOW, slot1=PURPLE, slot2=YELLOW
     * // Y la secuencia de colores es: [PURPLE, YELLOW, YELLOW]
     * // Resultado: [1, 0, 2] - lanza slot 1 (morada) antes que 0 y 2 (amarillas)
     */
    public int[] getColorOrderedSequence(SlotState[] colorSeq) {
        int[] sequence = new int[3];
        return sequence;
    }
    
    /**
     * Genera una secuencia de lanzamiento simple dependiendo de la posición actual(calcular más cercano primero en sentido de giro del servo)
     * @return Array con índices de slots en orden de lanzamiento, -1 indica slot vacío
     * Ejemplo:
     * // Si tienes: slot0=YELLOW, slot1=EMPTY, slot2=PURPLE
     * // Y estás en slot0:
     * 
     * int[] order = getSimpleSequence();
     * // Resultado: [0, 2, -1] - lanza slot 0 (amarilla) antes que 2 (morada), slot1 está vacío
     */
    public int[] getSimpleSequence() {
        int[] sequence = new int[3];
        return sequence;
    }
    
    private double getIntakeAngle(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_INTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_INTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_INTAKE_POSITION;
            default: throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
    }
    
    private double getOuttakeAngle(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_OUTTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_OUTTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_OUTTAKE_POSITION;
            default: throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }
    }
    
    

    /**
     * Obtiene una cadena con información del estado para telemetría
     */
    public String getTelemetryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Estado Spindexer: ").append(currentState).append("\n");
        sb.append("Slot Actual: ").append(currentSlotIndex).append("\n");
        sb.append("Slots: [");
        for (int i = 0; i < 3; i++) {
            sb.append(slotStates[i]);
            if (i < 2) sb.append(", ");
        }
        sb.append("]\n");
        sb.append("Pelotas: ").append(getFilledSlotCount()).append("/3\n");
        return sb.toString();
    }
}