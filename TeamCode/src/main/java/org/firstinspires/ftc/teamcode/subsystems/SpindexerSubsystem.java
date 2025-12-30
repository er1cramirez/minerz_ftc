package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;

/**
 * Subsystem controlling the Spindexer (3-slot rotary indexer).
 * 
 * RESPONSIBILITIES:
 * - Basic servo movement (Intake/Outtake positions for each slot)
 * - State management (Slot colors, current target slot)
 * - Raw sensor access
 * 
 * NOTE: Advanced logic like "nearest path" or "auto-indexing" belongs in Commands.
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

    // ==================== STATE ====================

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

    // ==================== BASIC MOVEMENT ====================
    
    /**
     * Moves the specified slot to the intake position.
     * @param slotIndex 0-2
     */
    public void moveToIntakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        // Reset direction to standard for predictable absolute positioning
        // Unless user logic confirms 'wrapping' works via direction filp
        indexerServo.setDirection(Servo.Direction.FORWARD); 
        indexerServo.setPosition(getIntakePosition(slotIndex));
        
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.AT_INTAKE;
    }

    /**
     * Moves the specified slot to the outtake position.
     * @param slotIndex 0-2
     */
    public void moveToOuttakePosition(int slotIndex) {
        validateSlotIndex(slotIndex);
        indexerServo.setDirection(Servo.Direction.FORWARD);
        indexerServo.setPosition(getOuttakePosition(slotIndex));
        
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.AT_OUTTAKE;
    }
    
    /**
     * Directly sets servo position.
     * Useful for tuning or custom moves.
     */
    public void setServoPosition(double position) {
        indexerServo.setPosition(position);
    }
    


    // ==================== STATE ACCESSORS ====================

    public SpindexerState getState() {
        return currentState;
    }

    public int getCurrentSlotIndex() {
        return currentSlotIndex;
    }

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

    /**
     * Clears the state of a specific slot (sets to EMPTY).
     * @param slotIndex The index of the slot to clear.
     */
    public void clearSlot(int slotIndex) {
        setSlotState(slotIndex, SlotState.EMPTY);
    }
    
    public void clearAllSlots() {
        for(int i=0; i<3; i++) slotStates[i] = SlotState.EMPTY;
    }
    
    public boolean isFull() {
        for (SlotState state : slotStates) {
            if (state == SlotState.EMPTY) return false;
        }
        return true;
    }

    public int getFilledSlotCount() {
        int count = 0;
        for (SlotState s : slotStates) if (s != SlotState.EMPTY) count++;
        return count;
    }
    
    public int getNextEmptySlot() {
        for (int i = 0; i < 3; i++) {
            int idx = (currentSlotIndex + i) % 3;
            if (slotStates[idx] == SlotState.EMPTY) return idx;
        }
        return -1; // Full
    }

    /**
     * Finds the first slot containing a GREEN ball.
     * @return Slot index (0-2) or -1 if no Green ball found.
     */
    public int getGreenSlotIndex() {
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] == SlotState.GREEN) return i;
        }
        return -1;
    }
    
    /**
     * Sets the initial state of all slots. Useful for autonomous pre-load.
     */
    public void setInitialState(SlotState s0, SlotState s1, SlotState s2) {
        slotStates[0] = s0;
        slotStates[1] = s1;
        slotStates[2] = s2;
    }
    
    // ==================== SENSOR ACCESS ====================

    public double getDistance() {
        return colorSensor.distance(DistanceUnit.CM);
    }
    
    public int getRed() { return colorSensor.red(); }
    public int getGreen() { return colorSensor.green(); }
    public int getBlue() { return colorSensor.blue(); }
    
    public float getHue() {
        float[] hsv = new float[3];
        android.graphics.Color.RGBToHSV(getRed(), getGreen(), getBlue(), hsv);
        return hsv[0];
    }

    public float getSaturation() {
        float[] hsv = new float[3];
        android.graphics.Color.RGBToHSV(getRed(), getGreen(), getBlue(), hsv);
        return hsv[1];
    }

    public float getValue() {
        float[] hsv = new float[3];
        android.graphics.Color.RGBToHSV(getRed(), getGreen(), getBlue(), hsv);
        return hsv[2];
    }
    
    // ==================== TELEMETRY HELPERS ====================

    public String getSlotEmoji(int slotIndex) {
        validateSlotIndex(slotIndex);
        switch (slotStates[slotIndex]) {
            case GREEN: return "🟢";
            case PURPLE: return "🟣";
            case UNKNOWN: return "❓";
            default: return "⚫";
        }
    }
    
    /**
     * Returns a compact status string for telemetry.
     * Format: [🟢][🟣][⚫] S0 ▸AT_INTAKE
     */
    public String getCompactStatus() {
        StringBuilder sb = new StringBuilder("📥 ");
        for (int i = 0; i < 3; i++) {
            if (i == currentSlotIndex) {
                sb.append("[").append(getSlotEmoji(i)).append("]");
            } else {
                sb.append(" ").append(getSlotEmoji(i)).append(" ");
            }
        }
        sb.append(" S").append(currentSlotIndex);
        sb.append(" ▸").append(currentState.name().replace("AT_", ""));
        return sb.toString();
    }

    // ==================== PRIVATE HELPERS ====================

    private double getIntakePosition(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_INTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_INTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_INTAKE_POSITION;
            default: return 0;
        }
    }

    private double getOuttakePosition(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_OUTTAKE_POSITION;
            case 1: return SpindexerConstants.SLOT_1_OUTTAKE_POSITION;
            case 2: return SpindexerConstants.SLOT_2_OUTTAKE_POSITION;
            default: return 0;
        }
    }

    private void validateSlotIndex(int slotIndex) {
        if (slotIndex < 0 || slotIndex > 2) {
            throw new IllegalArgumentException("Slot index must be 0, 1, or 2");
        }
    }
}