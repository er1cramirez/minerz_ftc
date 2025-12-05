package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.teamcode.constants.IndexerConstants;

/**
 * Subsystem for the ball indexer mechanism.
 * A 3-slot wheel controlled by a 300° servo that stores and feeds balls to the shooter.
 * 
 * Each slot has two positions:
 * - Intake: aligned with the intake mechanism for loading balls
 * - Outtake: aligned with the shooter for launching balls (60° offset from intake)
 * 
 * This design allows absolute positioning to any slot's intake or outtake position,
 * which is especially useful in autonomous for selecting which ball color to shoot first.
 */
public class ArtifactIndexer extends SubsystemBase {

    /**
     * Enum representing all possible indexer positions.
     * Each slot has both an intake and outtake position.
     */
    public enum IndexerPosition {
        SLOT_1_INTAKE(IndexerConstants.Positions.SLOT_1_INTAKE_DEGREES),
        SLOT_1_OUTTAKE(IndexerConstants.Positions.SLOT_1_OUTTAKE_DEGREES),
        SLOT_2_INTAKE(IndexerConstants.Positions.SLOT_2_INTAKE_DEGREES),
        SLOT_2_OUTTAKE(IndexerConstants.Positions.SLOT_2_OUTTAKE_DEGREES),
        SLOT_3_INTAKE(IndexerConstants.Positions.SLOT_3_INTAKE_DEGREES),
        SLOT_3_OUTTAKE(IndexerConstants.Positions.SLOT_3_OUTTAKE_DEGREES);

        private final double degrees;

        IndexerPosition(double degrees) {
            this.degrees = degrees;
        }

        public double getDegrees() {
            return degrees;
        }

        /**
         * Converts degrees to servo position (0.0 to 1.0).
         */
        public double getServoPosition() {
            return degrees / IndexerConstants.Servo.SERVO_RANGE_DEGREES;
        }

        /**
         * Gets the corresponding outtake position for an intake position.
         * @return The outtake position, or the same position if already outtake
         */
        public IndexerPosition toOuttake() {
            switch (this) {
                case SLOT_1_INTAKE: return SLOT_1_OUTTAKE;
                case SLOT_2_INTAKE: return SLOT_2_OUTTAKE;
                case SLOT_3_INTAKE: return SLOT_3_OUTTAKE;
                default: return this;
            }
        }

        /**
         * Gets the corresponding intake position for an outtake position.
         * @return The intake position, or the same position if already intake
         */
        public IndexerPosition toIntake() {
            switch (this) {
                case SLOT_1_OUTTAKE: return SLOT_1_INTAKE;
                case SLOT_2_OUTTAKE: return SLOT_2_INTAKE;
                case SLOT_3_OUTTAKE: return SLOT_3_INTAKE;
                default: return this;
            }
        }

        /**
         * Checks if this position is an intake position.
         */
        public boolean isIntake() {
            return this == SLOT_1_INTAKE || this == SLOT_2_INTAKE || this == SLOT_3_INTAKE;
        }

        /**
         * Checks if this position is an outtake position.
         */
        public boolean isOuttake() {
            return !isIntake();
        }

        /**
         * Gets the slot number (1, 2, or 3).
         */
        public int getSlotNumber() {
            switch (this) {
                case SLOT_1_INTAKE:
                case SLOT_1_OUTTAKE:
                    return 1;
                case SLOT_2_INTAKE:
                case SLOT_2_OUTTAKE:
                    return 2;
                case SLOT_3_INTAKE:
                case SLOT_3_OUTTAKE:
                    return 3;
                default:
                    return 0;
            }
        }

        /**
         * Gets the intake position for a specific slot number.
         * @param slotNumber 1, 2, or 3
         * @return The intake position for that slot
         */
        public static IndexerPosition getIntakeForSlot(int slotNumber) {
            switch (slotNumber) {
                case 1: return SLOT_1_INTAKE;
                case 2: return SLOT_2_INTAKE;
                case 3: return SLOT_3_INTAKE;
                default: return SLOT_1_INTAKE;
            }
        }

        /**
         * Gets the outtake position for a specific slot number.
         * @param slotNumber 1, 2, or 3
         * @return The outtake position for that slot
         */
        public static IndexerPosition getOuttakeForSlot(int slotNumber) {
            switch (slotNumber) {
                case 1: return SLOT_1_OUTTAKE;
                case 2: return SLOT_2_OUTTAKE;
                case 3: return SLOT_3_OUTTAKE;
                default: return SLOT_1_OUTTAKE;
            }
        }
    }

    /**
     * Enum representing ball colors for tracking.
     */
    public enum BallColor {
        NONE,
        RED,
        BLUE,
        YELLOW
    }

    /**
     * Class to track the state of each slot.
     */
    public static class SlotState {
        private boolean hasBall;
        private BallColor ballColor;

        public SlotState() {
            this.hasBall = false;
            this.ballColor = BallColor.NONE;
        }

        public boolean hasBall() {
            return hasBall;
        }

        public void setBall(BallColor color) {
            this.hasBall = (color != BallColor.NONE);
            this.ballColor = color;
        }

        public void removeBall() {
            this.hasBall = false;
            this.ballColor = BallColor.NONE;
        }

        public BallColor getBallColor() {
            return ballColor;
        }
    }

    private final Servo indexerServo;
    private IndexerPosition currentPosition;
    private final SlotState[] slots;
    
    // Simple state tracking for old behavior
    private int simplePosition = 0;

    /**
     * Creates a new ArtifactIndexer.
     * @param hMap The hardware map
     * @param name The servo name in the hardware configuration
     */
    public ArtifactIndexer(final HardwareMap hMap, final String name) {
        indexerServo = hMap.get(Servo.class, name);
        currentPosition = IndexerPosition.SLOT_1_INTAKE;
        
        // Initialize slot states
        slots = new SlotState[3];
        for (int i = 0; i < 3; i++) {
            slots[i] = new SlotState();
        }
        
        // Set initial position using old method
        indexerServo.setPosition(0.0);
        simplePosition = 0;
    }

    /**
     * Sets the indexer to a specific position.
     * @param position The desired position
     */
    public void setPosition(IndexerPosition position) {
        double targetPosition = position.getServoPosition();
        indexerServo.setPosition(targetPosition);
        currentPosition = position;
    }
    
    /**
     * Sets the servo to a raw position value for testing.
     * @param rawPosition Servo position (0.0 to 1.0)
     */
    public void setRawPosition(double rawPosition) {
        indexerServo.setPosition(rawPosition);
    }

    /**
     * Gets the current position.
     * @return The current IndexerPosition
     */
    public IndexerPosition getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Moves to the next intake position (cycles through slots 1 -> 2 -> 3 -> 1).
     * Use this when loading balls sequentially.
     */
    public void advanceToNextIntake() {
        int currentSlot = currentPosition.getSlotNumber();
        int nextSlot = (currentSlot % 3) + 1;
        setPosition(IndexerPosition.getIntakeForSlot(nextSlot));
    }

    /**
     * Moves to the previous intake position (cycles through slots 1 -> 3 -> 2 -> 1).
     */
    public void reverseToPreviousIntake() {
        int currentSlot = currentPosition.getSlotNumber();
        int prevSlot = currentSlot == 1 ? 3 : currentSlot - 1;
        setPosition(IndexerPosition.getIntakeForSlot(prevSlot));
    }

    /**
     * Moves from current intake position to the corresponding outtake position.
     * Only works if currently at an intake position.
     */
    public void moveToOuttake() {
        if (currentPosition.isIntake()) {
            setPosition(currentPosition.toOuttake());
        }
    }

    /**
     * Moves from current outtake position to the corresponding intake position.
     * Only works if currently at an outtake position.
     */
    public void moveToIntake() {
        if (currentPosition.isOuttake()) {
            setPosition(currentPosition.toIntake());
        }
    }

    /**
     * Toggles between intake and outtake for the current slot.
     */
    public void toggleIntakeOuttake() {
        if (currentPosition.isIntake()) {
            setPosition(currentPosition.toOuttake());
        } else {
            setPosition(currentPosition.toIntake());
        }
    }

    /**
     * Advances to the next position in sequence for testing.
     * Sequence: SLOT_1_INTAKE -> SLOT_2_INTAKE -> SLOT_3_INTAKE -> 
     *           SLOT_1_OUTTAKE -> SLOT_2_OUTTAKE -> SLOT_3_OUTTAKE -> (repeat)
     * 
     * Useful for manual testing of all 6 positions.
     */
    public void advanceToNextPosition() {
        switch (currentPosition) {
            case SLOT_1_INTAKE:
                setPosition(IndexerPosition.SLOT_2_INTAKE);
                break;
            case SLOT_2_INTAKE:
                setPosition(IndexerPosition.SLOT_3_INTAKE);
                break;
            case SLOT_3_INTAKE:
                setPosition(IndexerPosition.SLOT_1_OUTTAKE);
                break;
            case SLOT_1_OUTTAKE:
                setPosition(IndexerPosition.SLOT_2_OUTTAKE);
                break;
            case SLOT_2_OUTTAKE:
                setPosition(IndexerPosition.SLOT_3_OUTTAKE);
                break;
            case SLOT_3_OUTTAKE:
                setPosition(IndexerPosition.SLOT_1_INTAKE);
                break;
        }
    }

    /**
     * Reverses to the previous position in sequence for testing.
     * Opposite of advanceToNextPosition().
     */
    public void reverseToPreviousPosition() {
        switch (currentPosition) {
            case SLOT_1_INTAKE:
                setPosition(IndexerPosition.SLOT_3_OUTTAKE);
                break;
            case SLOT_2_INTAKE:
                setPosition(IndexerPosition.SLOT_1_INTAKE);
                break;
            case SLOT_3_INTAKE:
                setPosition(IndexerPosition.SLOT_2_INTAKE);
                break;
            case SLOT_1_OUTTAKE:
                setPosition(IndexerPosition.SLOT_3_INTAKE);
                break;
            case SLOT_2_OUTTAKE:
                setPosition(IndexerPosition.SLOT_1_OUTTAKE);
                break;
            case SLOT_3_OUTTAKE:
                setPosition(IndexerPosition.SLOT_2_OUTTAKE);
                break;
        }
    }

    /**
     * Sets the intake position for a specific slot.
     * @param slotNumber 1, 2, or 3
     */
    public void goToSlotIntake(int slotNumber) {
        setPosition(IndexerPosition.getIntakeForSlot(slotNumber));
    }

    /**
     * Sets the outtake position for a specific slot.
     * @param slotNumber 1, 2, or 3
     */
    public void goToSlotOuttake(int slotNumber) {
        setPosition(IndexerPosition.getOuttakeForSlot(slotNumber));
    }

    // ==================== Slot State Management ====================

    /**
     * Records that a ball was placed in the current slot.
     * @param color The color of the ball
     */
    public void loadBall(BallColor color) {
        int slotIndex = currentPosition.getSlotNumber() - 1;
        slots[slotIndex].setBall(color);
    }

    /**
     * Records that a ball was ejected from the current slot.
     */
    public void ejectBall() {
        int slotIndex = currentPosition.getSlotNumber() - 1;
        slots[slotIndex].removeBall();
    }

    /**
     * Gets the state of a specific slot.
     * @param slotNumber 1, 2, or 3
     * @return The SlotState for that slot
     */
    public SlotState getSlotState(int slotNumber) {
        return slots[slotNumber - 1];
    }

    /**
     * Checks if a specific slot has a ball.
     * @param slotNumber 1, 2, or 3
     * @return true if the slot has a ball
     */
    public boolean slotHasBall(int slotNumber) {
        return slots[slotNumber - 1].hasBall();
    }

    /**
     * Gets the color of the ball in a specific slot.
     * @param slotNumber 1, 2, or 3
     * @return The ball color, or NONE if empty
     */
    public BallColor getSlotBallColor(int slotNumber) {
        return slots[slotNumber - 1].getBallColor();
    }

    /**
     * Finds the first slot containing a ball of the specified color.
     * @param color The color to search for
     * @return The slot number (1-3), or -1 if not found
     */
    public int findSlotWithColor(BallColor color) {
        for (int i = 0; i < 3; i++) {
            if (slots[i].getBallColor() == color) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Finds the next empty slot starting from the current position.
     * @return The slot number (1-3), or -1 if all slots are full
     */
    public int findNextEmptySlot() {
        int startSlot = currentPosition.getSlotNumber();
        for (int i = 0; i < 3; i++) {
            int checkSlot = ((startSlot - 1 + i) % 3) + 1;
            if (!slots[checkSlot - 1].hasBall()) {
                return checkSlot;
            }
        }
        return -1;
    }

    /**
     * Counts the total number of balls currently stored.
     * @return Number of balls (0-3)
     */
    public int getBallCount() {
        int count = 0;
        for (SlotState slot : slots) {
            if (slot.hasBall()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if the indexer is full (all 3 slots have balls).
     * @return true if full
     */
    public boolean isFull() {
        return getBallCount() == 3;
    }

    /**
     * Checks if the indexer is empty (no balls).
     * @return true if empty
     */
    public boolean isEmpty() {
        return getBallCount() == 0;
    }

    /**
     * Clears all slot states (useful when resetting).
     */
    public void clearAllSlots() {
        for (SlotState slot : slots) {
            slot.removeBall();
        }
    }

    // ==================== Autonomous Helpers ====================

    /**
     * Moves to the outtake position for a slot containing a specific color.
     * Useful in autonomous to shoot a specific colored ball first.
     * @param color The ball color to shoot
     * @return true if a ball of that color was found, false otherwise
     */
    public boolean goToShootColor(BallColor color) {
        int slot = findSlotWithColor(color);
        if (slot != -1) {
            goToSlotOuttake(slot);
            return true;
        }
        return false;
    }

    /**
     * Moves to the intake position for the next empty slot.
     * Useful for loading balls sequentially.
     * @return true if an empty slot was found, false if all slots are full
     */
    public boolean goToNextEmptyIntake() {
        int slot = findNextEmptySlot();
        if (slot != -1) {
            goToSlotIntake(slot);
            return true;
        }
        return false;
    }

    // ==================== Legacy Methods (for backwards compatibility) ====================

    /**
     * OLD SIMPLE VERSION - Advances to next slot intake position.
     * Uses raw servo positions like the original code.
     */
    public void turnTrigger() {
        if (simplePosition == 0) {
            indexerServo.setPosition(0.0 / 300.0);  // 0°
            simplePosition = 1;
        } else if (simplePosition == 1) {
            indexerServo.setPosition(120.0 / 300.0);  // 120°
            simplePosition = 2;
        } else {
            indexerServo.setPosition(240.0 / 300.0);  // 240°
            simplePosition = 2;  // Stay at 2
        }
    }

    /**
     * OLD SIMPLE VERSION - Toggles to outtake position.
     * Uses raw servo positions like the original code.
     */
    public void feed() {
        if (simplePosition == 2) {
            indexerServo.setPosition(300.0 / 300.0);  // 300°
            simplePosition = 3;
        } else if (simplePosition == 3) {
            indexerServo.setPosition(180.0 / 300.0);  // 180°
            simplePosition = 4;
        } else {
            indexerServo.setPosition(60.0 / 300.0);  // 60°
            simplePosition = 0;
        }
    }

    /**
     * Returns to initial position.
     * @deprecated Use {@link #setPosition(IndexerPosition)} with SLOT_1_INTAKE instead
     */
    @Deprecated
    public void stow() {
        setPosition(IndexerPosition.SLOT_1_INTAKE);
    }

    /**
     * Gets the current slot number.
     * @return Current slot (1, 2, or 3)
     */
    public int getCurrentSlot() {
        return currentPosition.getSlotNumber();
    }

    /**
     * Checks if currently at an intake position.
     * @return true if at intake position
     */
    public boolean isAtIntake() {
        return currentPosition.isIntake();
    }

    /**
     * Checks if currently at an outtake position.
     * @return true if at outtake position
     */
    public boolean isAtOuttake() {
        return currentPosition.isOuttake();
    }

    /**
     * Gets the current position in degrees.
     * @return Current position in degrees (0-300)
     */
    public double getCurrentDegrees() {
        return currentPosition.getDegrees();
    }

    /**
     * Gets the current servo position value.
     * @return Servo position (0.0 to 1.0)
     */
    public double getServoPosition() {
        return indexerServo.getPosition();
    }

    /**
     * Gets the simple position state for debugging.
     */
    public int getSimplePosition() {
        return simplePosition;
    }

    /**
     * Gets telemetry string for debugging.
     * @return Formatted debug string
     */
    public String getDebugInfo() {
        return String.format("%s | Slot %d | %.1f° | Servo: %.3f | Simple: %d",
                currentPosition.name(),
                getCurrentSlot(),
                getCurrentDegrees(),
                getServoPosition(),
                simplePosition);
    }
}