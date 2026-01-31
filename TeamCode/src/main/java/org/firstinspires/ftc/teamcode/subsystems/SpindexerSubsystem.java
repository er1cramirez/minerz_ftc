package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.SensorRevColorV3;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;

/**
 * Subsystem controlling the Spindexer (3-slot rotary indexer) with DC Motor.
 * 
 * HARDWARE:
 * - DC Motor with encoder (goBILDA 312 RPM)
 * - Hall effect limit switch for homing
 * - Color sensor for ball detection
 * 
 * FEATURES:
 * - PD position control with angular wrapping (shortest path)
 * - Smart homing with directional offsets
 * - Position feedback (isAtPosition) for command synchronization
 */
public class SpindexerSubsystem extends SubsystemBase {

    // ==================== ENUMS ====================

    public enum SpindexerState {
        IDLE,
        AT_INTAKE,
        AT_OUTTAKE,
        MOVING,
        HOMING
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
        FASTEST
    }

    // ==================== HARDWARE ====================

    private final DcMotorEx motor;
    private final TouchSensor limitSwitch;
    private final SensorRevColorV3 colorSensor;

    // ==================== STATE ====================

    private SpindexerState currentState = SpindexerState.IDLE;
    private final SlotState[] slotStates = {SlotState.EMPTY, SlotState.EMPTY, SlotState.EMPTY};
    private int currentSlotIndex = 0;
    private boolean isHomed = false;

    // ==================== POSITION CONTROL ====================

    private double targetAngleDeg = 0.0;
    private double currentAngleDeg = 0.0;
    private double currentVelocityDegPerSec = 0.0;
    private double homeOffsetTicks = 0.0;
    private double lastPositionTicks = 0.0;
    private long lastUpdateTimeMs = 0;
    private double lastError = 0.0;
    private int lastMovementDirection = 0;  // +1=CCW, -1=CW
    private boolean wasLimitPressed = false;

    // ==================== HOMING ====================

    private final ElapsedTime homingTimer = new ElapsedTime();

    // ==================== SHOOTING PLAN ====================

    private int[] shootingPlan = new int[3];
    private int currentShotIndex = 0;
    private int totalShots = 0;

    // ==================== CONSTRUCTOR ====================

    public SpindexerSubsystem(HardwareMap hardwareMap) {
        motor = hardwareMap.get(DcMotorEx.class, SpindexerConstants.MOTOR_NAME);
        limitSwitch = hardwareMap.get(TouchSensor.class, SpindexerConstants.LIMIT_SWITCH_NAME);
        colorSensor = new SensorRevColorV3(hardwareMap, SpindexerConstants.COLOR_SENSOR_NAME, DistanceUnit.CM);

        // Configure motor
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Check if starting at home
        if (limitSwitch.isPressed()) {
            setCurrentPositionAsHome();
            isHomed = true;
        }

        lastUpdateTimeMs = System.currentTimeMillis();
    }

    // ==================== PERIODIC (must be called each loop) ====================

    @Override
    public void periodic() {
        updateSensorCache();
        
        if (currentState == SpindexerState.HOMING) {
            executeHoming();
        } else if (currentState == SpindexerState.MOVING) {
            executePositionControl();
            checkPassiveHoming();
            
            // Update state when position reached
            if (isAtPositionAndStable()) {
                currentState = determineStateFromAngle(currentAngleDeg);
            }
        }
    }

    private void updateSensorCache() {
        long now = System.currentTimeMillis();
        double deltaTimeSec = (now - lastUpdateTimeMs) / 1000.0;

        double currentTicks = motor.getCurrentPosition();
        currentAngleDeg = ticksToDegrees(currentTicks - homeOffsetTicks);

        if (deltaTimeSec > 0.001) {
            double deltaTicks = currentTicks - lastPositionTicks;
            currentVelocityDegPerSec = ticksToDegrees(deltaTicks) / deltaTimeSec;
        }

        lastPositionTicks = currentTicks;
        lastUpdateTimeMs = now;
    }

    // ==================== POSITION CONTROL ====================

    private void executePositionControl() {
        double error = calculateWrappedError(currentAngleDeg, targetAngleDeg);
        double derivative = lastError != 0 ? (error - lastError) : 0;
        lastError = error;

        double output = (SpindexerConstants.kP * error) + (SpindexerConstants.kD * derivative);
        output = clamp(output, -SpindexerConstants.MAX_POWER, SpindexerConstants.MAX_POWER);

        motor.setPower(output);
        
        // Track direction for homing
        if (Math.abs(output) > 0.05) {
            lastMovementDirection = output > 0 ? 1 : -1;
        }
    }

    /**
     * Calculates error with wrapping for shortest path rotation.
     */
    private double calculateWrappedError(double current, double target) {
        double error = target - current;
        // Normalize to [-180, 180] for shortest path
        while (error > 180) error -= 360;
        while (error < -180) error += 360;
        return error;
    }

    // ==================== MOVEMENT METHODS ====================

    /**
     * Moves the specified slot to the intake position.
     */
    public void moveToIntakePosition(int slotIndex) {
        if (!isValidSlotIndex(slotIndex)) return;
        
        setTargetAngle(getIntakeAngle(slotIndex));
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.MOVING;
    }

    /**
     * Moves the specified slot to the outtake position.
     */
    public void moveToOuttakePosition(int slotIndex) {
        if (!isValidSlotIndex(slotIndex)) return;
        
        setTargetAngle(getOuttakeAngle(slotIndex));
        currentSlotIndex = slotIndex;
        currentState = SpindexerState.MOVING;
    }

    /**
     * Sets absolute target angle.
     * Note: Callers (moveToIntakePosition, moveToOuttakePosition) set MOVING state.
     */
    public void setTargetAngle(double degrees) {
        targetAngleDeg = degrees;
        lastError = 0;
        // State is set by caller - don't override here to avoid state confusion
    }

    // ==================== POSITION FEEDBACK ====================

    /**
     * @return true if at target position within tolerance
     */
    public boolean isAtPosition() {
        return Math.abs(calculateWrappedError(currentAngleDeg, targetAngleDeg)) 
               <= SpindexerConstants.POSITION_TOLERANCE_DEG;
    }

    /**
     * @return true if at position AND velocity is low (stable)
     */
    public boolean isAtPositionAndStable() {
        return isAtPosition() && 
               Math.abs(currentVelocityDegPerSec) <= SpindexerConstants.VELOCITY_TOLERANCE_DEG_PER_SEC;
    }

    public double getCurrentAngle() { return currentAngleDeg; }
    public double getTargetAngle() { return targetAngleDeg; }
    public double getPositionError() { return calculateWrappedError(currentAngleDeg, targetAngleDeg); }

    // ==================== HOMING ====================

    /**
     * Initiates homing sequence - rotates CCW until limit switch triggered.
     */
    public void startHoming() {
        currentState = SpindexerState.HOMING;
        homingTimer.reset();
    }

    private void executeHoming() {
        if (limitSwitch.isPressed()) {
            motor.setPower(0);
            applyHomeWithOffset(SpindexerConstants.HOME_OFFSET_CCW_DEG);
            isHomed = true;
            currentState = SpindexerState.IDLE;
            targetAngleDeg = 0.0;
        } else if (homingTimer.milliseconds() > SpindexerConstants.HOMING_TIMEOUT_MS) {
            motor.setPower(0);
            currentState = SpindexerState.IDLE;
        } else {
            motor.setPower(SpindexerConstants.HOMING_POWER);
            lastMovementDirection = 1;
        }
    }

    /**
     * Checks for passive homing opportunity during normal movement.
     */
    private void checkPassiveHoming() {
        boolean isPressed = limitSwitch.isPressed();
        
        if (isPressed && !wasLimitPressed && lastMovementDirection != 0) {
            double offsetDeg = lastMovementDirection > 0 ? 
                SpindexerConstants.HOME_OFFSET_CCW_DEG : 
                SpindexerConstants.HOME_OFFSET_CW_DEG;
            
            double assumedError = Math.abs(wrapAngle(currentAngleDeg) - offsetDeg);
            
            if (assumedError > SpindexerConstants.SMART_HOMING_THRESHOLD_DEG || !isHomed) {
                applyHomeWithOffset(offsetDeg);
            }
        }
        
        wasLimitPressed = isPressed;
    }

    private void applyHomeWithOffset(double offsetDeg) {
        homeOffsetTicks = motor.getCurrentPosition() - degreesToTicks(offsetDeg);
        currentAngleDeg = offsetDeg;
        lastError = 0;
        isHomed = true;
    }

    public void setCurrentPositionAsHome() {
        homeOffsetTicks = motor.getCurrentPosition();
        currentAngleDeg = 0;
        targetAngleDeg = 0;
        lastError = 0;
    }

    public boolean isHomed() { return isHomed; }

    // ==================== STATE ACCESSORS ====================

    public SpindexerState getState() { return currentState; }
    public int getCurrentSlotIndex() { return currentSlotIndex; }

    public SlotState getSlotState(int slotIndex) {
        return isValidSlotIndex(slotIndex) ? slotStates[slotIndex] : SlotState.EMPTY;
    }

    public SlotState getCurrentSlotState() { return slotStates[currentSlotIndex]; }

    public void setSlotState(int slotIndex, SlotState state) {
        if (isValidSlotIndex(slotIndex)) slotStates[slotIndex] = state;
    }

    public void clearSlot(int slotIndex) { setSlotState(slotIndex, SlotState.EMPTY); }

    public void clearAllSlots() {
        for (int i = 0; i < 3; i++) slotStates[i] = SlotState.EMPTY;
    }

    public boolean isFull() {
        for (SlotState s : slotStates) if (s == SlotState.EMPTY) return false;
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
        return -1;
    }

    public int getGreenSlotIndex() {
        for (int i = 0; i < 3; i++) if (slotStates[i] == SlotState.GREEN) return i;
        return -1;
    }

    public void setInitialState(SlotState s0, SlotState s1, SlotState s2) {
        slotStates[0] = s0;
        slotStates[1] = s1;
        slotStates[2] = s2;
    }

    // ==================== SHOOTING PLAN ====================

    public void prepareShotPlan(ShootingStrategy strategy) {
        java.util.List<Integer> order = getShootingOrder(strategy);
        totalShots = order.size();
        currentShotIndex = 0;
        for (int i = 0; i < totalShots; i++) {
            shootingPlan[i] = order.get(i);
        }
    }

    public int getShotSlot(int shotNumber) {
        return (shotNumber >= 0 && shotNumber < totalShots) ? shootingPlan[shotNumber] : -1;
    }

    public boolean isValidShotIndex(int shotNumber) {
        return shotNumber >= 0 && shotNumber < totalShots;
    }

    public int getTotalShots() { return totalShots; }

    public void clearCurrentShotAndAdvance() {
        if (currentShotIndex < totalShots) {
            clearSlot(shootingPlan[currentShotIndex]);
            currentShotIndex++;
        }
    }

    public java.util.List<Integer> getShootingOrder(ShootingStrategy strategy) {
        java.util.List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            if (slotStates[i] != SlotState.EMPTY) slots.add(i);
        }
        if (slots.isEmpty()) return slots;

        switch (strategy) {
            case FASTEST:
                slots.sort(java.util.Comparator.comparingInt(this::getFastestPriority));
                break;
            case GREEN_FIRST:
                slots.sort((a, b) -> Boolean.compare(
                    slotStates[a] != SlotState.GREEN, 
                    slotStates[b] != SlotState.GREEN));
                break;
            case GREEN_LAST:
                slots.sort((a, b) -> Boolean.compare(
                    slotStates[a] == SlotState.GREEN, 
                    slotStates[b] == SlotState.GREEN));
                break;
            case GREEN_MIDDLE:
                java.util.List<Integer> green = new java.util.ArrayList<>();
                java.util.List<Integer> others = new java.util.ArrayList<>();
                for (int s : slots) {
                    if (slotStates[s] == SlotState.GREEN) green.add(s);
                    else others.add(s);
                }
                slots.clear();
                if (!others.isEmpty()) slots.add(others.remove(0));
                slots.addAll(green);
                slots.addAll(others);
                break;
        }
        return slots;
    }

    private int getFastestPriority(int slotIndex) {
        if (slotIndex == 1) return 0;
        if (slotIndex == 0) return 1;
        return 2;
    }

    // ==================== SENSOR ACCESS ====================

    public double getDistance() { return colorSensor.distance(DistanceUnit.CM); }
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

    // ==================== TELEMETRY ====================

    public String getSlotEmoji(int slotIndex) {
        switch (slotStates[slotIndex]) {
            case GREEN: return "🟢";
            case PURPLE: return "🟣";
            case UNKNOWN: return "❓";
            default: return "⚫";
        }
    }

    public String getCompactStatus() {
        StringBuilder sb = new StringBuilder("📥 ");
        for (int i = 0; i < 3; i++) {
            sb.append(i == currentSlotIndex ? "[" : " ")
              .append(getSlotEmoji(i))
              .append(i == currentSlotIndex ? "]" : " ");
        }
        sb.append(" S").append(currentSlotIndex);
        sb.append(" ▸").append(currentState.name().replace("AT_", ""));
        sb.append(isAtPosition() ? " ✓" : "");
        sb.append(isHomed ? "" : " !HOME");
        return sb.toString();
    }

    // ==================== UTILITIES ====================

    private double getIntakeAngle(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_INTAKE_ANGLE;
            case 1: return SpindexerConstants.SLOT_1_INTAKE_ANGLE;
            case 2: return SpindexerConstants.SLOT_2_INTAKE_ANGLE;
            default: return 0;
        }
    }

    private double getOuttakeAngle(int slotIndex) {
        switch (slotIndex) {
            case 0: return SpindexerConstants.SLOT_0_OUTTAKE_ANGLE;
            case 1: return SpindexerConstants.SLOT_1_OUTTAKE_ANGLE;
            case 2: return SpindexerConstants.SLOT_2_OUTTAKE_ANGLE;
            default: return 0;
        }
    }

    private SpindexerState determineStateFromAngle(double angle) {
        // Determine if at intake or outtake based on angle
        // Use calculateWrappedError for shortest-distance comparison [-180, 180]
        for (int i = 0; i < 3; i++) {
            if (Math.abs(calculateWrappedError(angle, getIntakeAngle(i))) <= SpindexerConstants.POSITION_TOLERANCE_DEG) {
                return SpindexerState.AT_INTAKE;
            }
            if (Math.abs(calculateWrappedError(angle, getOuttakeAngle(i))) <= SpindexerConstants.POSITION_TOLERANCE_DEG) {
                return SpindexerState.AT_OUTTAKE;
            }
        }
        return SpindexerState.IDLE;
    }

    private double wrapAngle(double angle) {
        angle = angle % 360;
        if (angle < 0) angle += 360;
        return angle;
    }

    private double ticksToDegrees(double ticks) {
        return ticks * SpindexerConstants.DEGREES_PER_TICK;
    }

    private double degreesToTicks(double degrees) {
        return degrees * SpindexerConstants.TICKS_PER_DEGREE;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isValidSlotIndex(int slotIndex) {
        return slotIndex >= 0 && slotIndex <= 2;
    }
}