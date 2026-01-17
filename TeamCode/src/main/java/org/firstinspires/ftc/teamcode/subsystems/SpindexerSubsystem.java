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
 * NOTE: Advanced logic like "nearest path" or "auto-indexing" belongs in
 * Commands.
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

    public enum ShootingStrategy {
        GREEN_FIRST,
        GREEN_MIDDLE,
        GREEN_LAST,
        FASTEST // Optimized path 1 -> 0 -> 2
    }

    // ==================== HARDWARE ====================

    private final Servo indexerServo;
    private final SensorRevColorV3 colorSensor;

    // ==================== STATE ====================

    private SpindexerState currentState;
    private final SlotState[] slotStates;
    private int currentSlotIndex;

    // ==================== SHOOTING PLAN STATE ====================

    private int[] shootingPlan = new int[3]; // Slot indices in order
    private long[] transitionDelays = new long[3]; // Delay before each shot
    private int currentShotIndex = 0;
    private int totalShots = 0;

    // ==================== CONSTRUCTOR ====================

    public SpindexerSubsystem(HardwareMap hardwareMap) {
        indexerServo = hardwareMap.get(Servo.class, SpindexerConstants.SERVO_NAME);
        colorSensor = new SensorRevColorV3(hardwareMap, "colorSensor", DistanceUnit.CM);

        currentState = SpindexerState.IDLE;
        slotStates = new SlotState[] { SlotState.EMPTY, SlotState.EMPTY, SlotState.EMPTY };
        currentSlotIndex = 0;
    }

    // ==================== BASIC MOVEMENT ====================

    /**
     * Moves the specified slot to the intake position.
     * Silently ignores invalid slot indices to prevent crashes.
     * 
     * @param slotIndex 0-2
     */
    public void moveToIntakePosition(int slotIndex) {
        if (!isValidSlotIndex(slotIndex)) {
            return; // Silently ignore invalid indices
        }
        // Reset direction to standard for predictable absolute positioning
        // Unless user logic confirms 'wrapping' works via direction filp
        indexerServo.setDirection(Servo.Direction.FORWARD);
        indexerServo.setPosition(getIntakePosition(slotIndex));

        currentSlotIndex = slotIndex;
        currentState = SpindexerState.AT_INTAKE;
    }

    /**
     * Moves the specified slot to the outtake position.
     * Silently ignores invalid slot indices to prevent crashes.
     * 
     * @param slotIndex 0-2
     */
    public void moveToOuttakePosition(int slotIndex) {
        if (!isValidSlotIndex(slotIndex)) {
            return; // Silently ignore invalid indices
        }
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
        if (!isValidSlotIndex(slotIndex)) {
            return SlotState.EMPTY; // Safe default for invalid indices
        }
        return slotStates[slotIndex];
    }

    public SlotState getCurrentSlotState() {
        return slotStates[currentSlotIndex];
    }

    public void setSlotState(int slotIndex, SlotState state) {
        if (!isValidSlotIndex(slotIndex)) {
            return; // Silently ignore invalid indices
        }
        slotStates[slotIndex] = state;
    }

    /**
     * Clears the state of a specific slot (sets to EMPTY).
     * 
     * @param slotIndex The index of the slot to clear.
     */
    public void clearSlot(int slotIndex) {
        setSlotState(slotIndex, SlotState.EMPTY);
    }

    public void clearAllSlots() {
        for (int i = 0; i < 3; i++)
            slotStates[i] = SlotState.EMPTY;
    }

    public boolean isFull() {
        for (SlotState state : slotStates) {
            if (state == SlotState.EMPTY)
                return false;
        }
        return true;
    }

    public int getFilledSlotCount() {
        int count = 0;
        for (SlotState s : slotStates)
            if (s != SlotState.EMPTY)
                count++;
        return count;
    }

    public int getNextEmptySlot() {
        for (int i = 0; i < 3; i++) {
            int idx = (currentSlotIndex + i) % 3;
            if (slotStates[idx] == SlotState.EMPTY)
                return idx;
        }
        return -1; // Full
    }

    /**
     * Finds the first slot containing a GREEN ball.
     * 
     * @return Slot index (0-2) or -1 if no Green ball found.
     */
    public int getGreenSlotIndex() {
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] == SlotState.GREEN)
                return i;
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

    // ==================== SHOOTING PLAN METHODS ====================

    /**
     * Prepares the shooting plan based on current slot states and strategy.
     * Call this at the START of ShootingSequence to capture current state.
     * 
     * @param strategy The shooting strategy to use
     */
    public void prepareShotPlan(ShootingStrategy strategy) {
        java.util.List<Integer> order = getShootingOrder(strategy);
        totalShots = order.size();
        currentShotIndex = 0;

        int prevSlot = currentSlotIndex;
        for (int i = 0; i < totalShots; i++) {
            shootingPlan[i] = order.get(i);
            transitionDelays[i] = calculateTransitionTime(prevSlot, shootingPlan[i]);
            prevSlot = shootingPlan[i];
        }
    }

    /**
     * Gets the slot index for a specific shot number (0, 1, or 2).
     * @return Slot index (0-2) or -1 if shot number is invalid
     */
    public int getShotSlot(int shotNumber) {
        if (shotNumber < 0 || shotNumber >= totalShots)
            return -1;
        return shootingPlan[shotNumber];
    }

    /**
     * Checks if the specified shot number is valid in the current plan.
     * Use this to conditionally execute shots in ShootingSequence.
     * 
     * @param shotNumber The shot number to check (0, 1, or 2)
     * @return true if the shot is valid and should be executed
     */
    public boolean isValidShotIndex(int shotNumber) {
        return shotNumber >= 0 && shotNumber < totalShots;
    }

    /**
     * Gets the transition delay for a specific shot number.
     */
    public long getShotDelay(int shotNumber) {
        if (shotNumber < 0 || shotNumber >= totalShots)
            return 100;
        return transitionDelays[shotNumber];
    }

    /**
     * Gets the total number of shots in the current plan.
     */
    public int getTotalShots() {
        return totalShots;
    }

    /**
     * Clears the slot and advances to the next shot.
     * Call after each successful shot.
     */
    public void clearCurrentShotAndAdvance() {
        if (currentShotIndex < totalShots) {
            clearSlot(shootingPlan[currentShotIndex]);
            currentShotIndex++;
        }
    }

    // ==================== SHOOTING ORDER ====================

    /**
     * Determines the shooting order based on current slot states and strategy.
     * This is the subsystem's responsibility as it knows its own state best.
     * 
     * @param strategy The shooting strategy to use
     * @return List of slot indices in shooting order (empty if no balls)
     */
    public java.util.List<Integer> getShootingOrder(ShootingStrategy strategy) {
        java.util.List<Integer> slots = new java.util.ArrayList<>();

        // Populate valid slots (non-empty)
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] != SlotState.EMPTY) {
                slots.add(i);
            }
        }

        if (slots.isEmpty()) {
            return slots;
        }

        // Sort based on strategy
        switch (strategy) {
            case FASTEST:
                // Hardcoded preference: 1 -> 0 -> 2
                slots.sort(java.util.Comparator.comparingInt(this::getFastestPriority));
                break;

            case GREEN_FIRST:
                slots.sort((a, b) -> {
                    boolean aGreen = slotStates[a] == SlotState.GREEN;
                    boolean bGreen = slotStates[b] == SlotState.GREEN;
                    return Boolean.compare(!aGreen, !bGreen); // Green comes first
                });
                break;

            case GREEN_LAST:
                slots.sort((a, b) -> {
                    boolean aGreen = slotStates[a] == SlotState.GREEN;
                    boolean bGreen = slotStates[b] == SlotState.GREEN;
                    return Boolean.compare(aGreen, bGreen); // Green comes last
                });
                break;

            case GREEN_MIDDLE:
                java.util.List<Integer> green = new java.util.ArrayList<>();
                java.util.List<Integer> others = new java.util.ArrayList<>();
                for (int s : slots) {
                    if (slotStates[s] == SlotState.GREEN) {
                        green.add(s);
                    } else {
                        others.add(s);
                    }
                }

                slots.clear();
                if (!others.isEmpty()) {
                    slots.add(others.remove(0));
                }
                slots.addAll(green);
                slots.addAll(others);
                break;
        }

        return slots;
    }

    /**
     * Calculates the time needed for servo to transition from one slot to another.
     * Returns appropriate wait time based on travel distance.
     * 
     * @param fromSlot Starting slot index
     * @param toSlot   Target slot index
     * @return Wait time in milliseconds
     */
    public long calculateTransitionTime(int fromSlot, int toSlot) {
        if (fromSlot == toSlot) {
            return 100; // Same slot adjustment
        }

        // Calculate distance based on outtake positions
        double posFrom = getOuttakePosition(fromSlot);
        double posTo = getOuttakePosition(toSlot);
        double distance = Math.abs(posTo - posFrom);

        // Long travel threshold (>105 degrees)
        final double LONG_TRAVEL_THRESHOLD = 0.35;
        final long SHORT_WAIT_MS = 350;
        final long LONG_WAIT_MS = 950;

        return (distance > LONG_TRAVEL_THRESHOLD) ? LONG_WAIT_MS : SHORT_WAIT_MS;
    }

    /**
     * Returns priority for FASTEST strategy (1 -> 0 -> 2).
     * Lower value = higher priority.
     */
    private int getFastestPriority(int slotIndex) {
        if (slotIndex == 1)
            return 0;
        if (slotIndex == 0)
            return 1;
        if (slotIndex == 2)
            return 2;
        return 3;
    }

    // ==================== SENSOR ACCESS ====================

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
            case GREEN:
                return "🟢";
            case PURPLE:
                return "🟣";
            case UNKNOWN:
                return "❓";
            default:
                return "⚫";
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
            case 0:
                return SpindexerConstants.SLOT_0_INTAKE_POSITION;
            case 1:
                return SpindexerConstants.SLOT_1_INTAKE_POSITION;
            case 2:
                return SpindexerConstants.SLOT_2_INTAKE_POSITION;
            default:
                return 0;
        }
    }

    private double getOuttakePosition(int slotIndex) {
        switch (slotIndex) {
            case 0:
                return SpindexerConstants.SLOT_0_OUTTAKE_POSITION;
            case 1:
                return SpindexerConstants.SLOT_1_OUTTAKE_POSITION;
            case 2:
                return SpindexerConstants.SLOT_2_OUTTAKE_POSITION;
            default:
                return 0;
        }
    }

    /**
     * Checks if a slot index is valid (0, 1, or 2).
     * @param slotIndex The index to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidSlotIndex(int slotIndex) {
        return slotIndex >= 0 && slotIndex <= 2;
    }

    public double getServoPos () {
        return indexerServo.getPosition();
    }
}