package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;

/**
 * Subsystem que controla el spindexer (indexador rotativo de 3 slots)
 * con sensor de color integrado para detección de pelotas.
 * 
 * RESPONSABILIDADES:
 * - Controlar rotación del servo (posiciones intake/outtake)
 * - Mantener estado de cada slot (vacío/color)
 * - Proveer lecturas instantáneas del sensor de color
 * 
 * IMPORTANTE:
 * - Los métodos del sensor son lecturas INSTANTÁNEAS (no bloqueantes)
 * - La lógica de confirmación de color va en los Commands
 */
public class SpindexerSubsystem extends SubsystemBase {

    // ==================== ENUMS ====================

    public enum SpindexerState {
        IDLE,
        AT_INTAKE,
        AT_OUTTAKE
    }

    public enum SlotState {
        EMPTY,
        GREEN,
        PURPLE,
        UNKNOWN
    }

    // ==================== HARDWARE ====================

    private final Servo indexerServo;
    private final SensorRevColorV3 colorSensor;

    // ==================== ESTADO ====================

    private SpindexerState currentState;
    private final SlotState[] slotStates;
    private int currentSlotIndex;

    // ==================== CONSTRUCTOR ====================


    public SpindexerSubsystem(HardwareMap hardwareMap) {
        indexerServo = hardwareMap.get(Servo.class, SpindexerConstants.SERVO_NAME);
        colorSensor = new SensorRevColorV3(hardwareMap, "colorSensor", DistanceUnit.CM);

        currentState = SpindexerState.IDLE;
        slotStates = new SlotState[]{SlotState.EMPTY, SlotState.EMPTY, SlotState.EMPTY};
        currentSlotIndex = 0;
    }

    // ==================== MOVIMIENTO ====================

    public void moveToIntakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        indexerServo.setPosition(getIntakePosition(slotIndex));
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.AT_INTAKE;
    }

    public void moveToOuttakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        indexerServo.setPosition(getOuttakePosition(slotIndex));
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.AT_OUTTAKE;
    }

    // ==================== ESTADO DEL SPINDEXER ====================

    public SpindexerState getState() {
        return currentState;
    }

    public boolean isAtIntake() {
        return currentState == SpindexerState.AT_INTAKE;
    }

    public boolean isAtOuttake() {
        return currentState == SpindexerState.AT_OUTTAKE;
    }

    public int getCurrentSlotIndex() {
        return currentSlotIndex;
    }

    // ==================== ESTADO DE SLOTS ====================

    public SlotState getSlotState(int slotIndex) {
        validateSlotIndex(slotIndex);
        return slotStates[slotIndex];
    }

    public SlotState getCurrentSlotState() {
        return slotStates[currentSlotIndex];
    }

    public void setSlotState(int slotIndex, SlotState state) {
        validateSlotIndex(slotIndex);
        slotStates[slotIndex] = state;
    }

    public void clearSlot(int slotIndex) {
        setSlotState(slotIndex, SlotState.EMPTY);
    }

    public void clearAllSlots() {
        for (int i = 0; i < 3; i++) {
            slotStates[i] = SlotState.EMPTY;
        }
    }

    public boolean isFull() {
        for (SlotState state : slotStates) {
            if (state == SlotState.EMPTY) return false;
        }
        return true;
    }

    public int getFilledSlotCount() {
        int count = 0;
        for (SlotState state : slotStates) {
            if (state != SlotState.EMPTY) count++;
        }
        return count;
    }

    /**
     * Busca el siguiente slot vacío desde el slot actual (circular).
     * @return índice del slot vacío, o -1 si todos están llenos
     */
    public int getNextEmptySlot() {
        for (int i = 0; i < 3; i++) {
            int idx = (currentSlotIndex + i) % 3;
            if (slotStates[idx] == SlotState.EMPTY) {
                return idx;
            }
        }
        return -1;
    }

    // ==================== SENSOR DE COLOR (Lecturas Instantáneas) ====================

    public double getDistance() {
        return colorSensor.distance(DistanceUnit.CM);
    }

    public int getRed() {
        return colorSensor.red();
    }

    public int getGreen() {
        return colorSensor.green();
    }

    public int getBlue() {
        return colorSensor.blue();
    }

    /**
     * Obtiene valores HSV de la lectura actual.
     * @return array [hue (0-360), saturation (0-1), value (0-1)]
     */
    public float[] getHSV() {
        float[] hsv = new float[3];
        colorSensor.RGBtoHSV(getRed(), getGreen(), getBlue(), hsv);
        return hsv;
    }

    public float getHue() {
        return getHSV()[0];
    }

    public float getSaturation() {
        return getHSV()[1];
    }

    public float getValue() {
        return getHSV()[2];
    }

    // ==================== TELEMETRÍA ====================

    public String getSlotEmoji(int slotIndex) {
        validateSlotIndex(slotIndex);
        switch (slotStates[slotIndex]) {
            case GREEN: return "🟢";
            case PURPLE: return "🟣";
            case UNKNOWN: return "❓";
            default: return "⚫";
        }
    }

    // ==================== HELPERS PRIVADOS ====================

    private double getIntakePosition(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_INTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_INTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_INTAKE_POSITION;
            default: throw new IllegalArgumentException("Invalid slot: " + slotIndex);
        }
    }

    private double getOuttakePosition(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_OUTTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_OUTTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_OUTTAKE_POSITION;
            default: throw new IllegalArgumentException("Invalid slot: " + slotIndex);
        }
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 2) {
            throw new IllegalArgumentException("Slot index must be 0, 1, or 2");
        }
    }
}