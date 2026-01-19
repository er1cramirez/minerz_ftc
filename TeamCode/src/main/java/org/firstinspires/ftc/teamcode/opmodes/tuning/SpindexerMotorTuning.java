package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * OpMode para probar y tunear el Spindexer con Motor DC.
 * 
 * HARDWARE REQUERIDO:
 * - Motor goBILDA 312 RPM conectado como "spindexMotor"
 * - Limit switch magnético (Hall effect) conectado como "spindexLimit"
 * 
 * FEATURES:
 * - Control PD con wrapping angular (rotación óptima)
 * - Homing inteligente: solo resetea si hay diferencia significativa
 * - Posiciones de slot para testing (0°, 120°, 240°)
 * 
 * CONTROLES:
 * ═══════════════════════════════════════════════════════
 * Gamepad 1:
 * - Left Stick X    : Control manual del motor
 * - A               : Ir a 0° (Slot 0 / Home)
 * - B               : Ir a 120° (Slot 1)
 * - X               : Ir a 240° (Slot 2)
 * - Y               : Ejecutar homing (buscar limit switch)
 * - DPAD UP/DOWN    : Ajustar target ±30°
 * - DPAD LEFT/RIGHT : Ajustar target ±5°
 * - Left Bumper     : Reset encoder en posición actual (set home)
 * - Right Bumper    : Parar motor
 * 
 * Gamepad 2 (Tuning PD):
 * - DPAD UP/DOWN    : kP ±0.01
 * - DPAD LEFT/RIGHT : kD ±0.002
 * - A               : Toggle wrapping on/off
 * - Y               : Reset PID a defaults
 * - Left Bumper     : Imprimir valores a consola
 * ═══════════════════════════════════════════════════════
 */
@TeleOp(name = "Spindexer Motor Tuning", group = "Tuning")
public class SpindexerMotorTuning extends OpMode {

    // ==================== HARDWARE CONFIG ====================
    private static final String MOTOR_NAME = "spindexMotor";
    private static final String LIMIT_SWITCH_NAME = "spindexLimit";

    // ==================== MOTOR CONSTANTS ====================
    // goBILDA 312 RPM (5203 series) - 537.7 PPR
    private static final double MOTOR_TICKS_PER_REV = 537.7;
    private static final double GEAR_RATIO = 1.0; // 1:1 direct
    private static final double TICKS_PER_SPINDEXER_REV = MOTOR_TICKS_PER_REV * GEAR_RATIO;
    private static final double TICKS_PER_DEGREE = TICKS_PER_SPINDEXER_REV / 360.0;
    private static final double DEGREES_PER_TICK = 360.0 / TICKS_PER_SPINDEXER_REV;

    // ==================== SLOT POSITIONS ====================
    private static final double SLOT_0_DEG = 0.0;
    private static final double SLOT_1_DEG = 120.0;
    private static final double SLOT_2_DEG = 240.0;

    // ==================== PD CONTROL (tunable) ====================
    private static double kP = 0.025;
    private static double kD = 0.003;
    private static final double MAX_POWER = 0.8;

    // ==================== TOLERANCES ====================
    private static final double POSITION_TOLERANCE_DEG = 3.0;
    private static final double VELOCITY_TOLERANCE_DEG_PER_SEC = 10.0;

    // ==================== HOMING CONFIG ====================
    private static final double HOMING_POWER = 0.25;
    private static final long HOMING_TIMEOUT_MS = 5000;
    /** Threshold for "smart" homing - only reset if error > this */
    private static final double SMART_HOMING_THRESHOLD_DEG = 15.0;
    
    /**
     * Offset cuando el sensor se activa viniendo desde CCW (power positivo).
     * Este es el borde "de entrada" cuando rotamos en sentido CCW.
     * Ajustar después de medir el ancho del rango activo.
     */
    private static double HOME_OFFSET_CCW_DEG = 0.0;
    
    /**
     * Offset cuando el sensor se activa viniendo desde CW (power negativo).
     * Este es el borde "de entrada" cuando rotamos en sentido CW.
     * Típicamente sería el negativo del ancho del rango activo / 2.
     */
    private static double HOME_OFFSET_CW_DEG = 0.0;

    // ==================== HARDWARE ====================
    private DcMotorEx motor;
    private TouchSensor limitSwitch;

    // ==================== STATE ====================
    private enum State {
        IDLE,
        MANUAL,
        POSITION_CONTROL,
        HOMING
    }
    private State currentState = State.IDLE;
    private boolean isHomed = false;
    private boolean wrappingEnabled = true;

    // ==================== CONTROL VARIABLES ====================
    private double targetAngleDeg = 0.0;
    private double currentAngleDeg = 0.0;
    private double currentVelocityDegPerSec = 0.0;
    private double homeOffsetTicks = 0.0;
    private double lastPositionTicks = 0.0;
    private long lastUpdateTimeMs = 0;
    private double lastError = 0.0;
    
    /** Track last movement direction: +1 = CCW, -1 = CW, 0 = stopped */
    private int lastMovementDirection = 0;
    /** Track if limit switch was pressed in previous loop (for edge detection) */
    private boolean wasLimitPressed = false;

    // ==================== HOMING STATE ====================
    private ElapsedTime homingTimer = new ElapsedTime();
    private boolean homingComplete = false;

    // ==================== ANALYSIS ====================
    private double maxError = 0.0;
    private double settlingTime = 0.0;
    private long settlingStartTime = 0;
    private boolean wasAtTarget = false;
    private double loopTimeMs = 0.0;
    private ElapsedTime loopTimer = new ElapsedTime();

    // ==================== BUTTON DEBOUNCE (GP1) ====================
    private boolean lastA = false, lastB = false, lastX = false, lastY = false;
    private boolean lastDUp = false, lastDDown = false, lastDLeft = false, lastDRight = false;
    private boolean lastLB = false, lastRB = false;

    // ==================== BUTTON DEBOUNCE (GP2) ====================
    private boolean lastG2DUp = false, lastG2DDown = false, lastG2DLeft = false, lastG2DRight = false;
    private boolean lastG2A = false, lastG2Y = false, lastG2LB = false;

    // ==================== TUNING INCREMENTS ====================
    private static final double KP_INCREMENT = 0.005;
    private static final double KD_INCREMENT = 0.001;
    private static final double DEFAULT_KP = 0.025;
    private static final double DEFAULT_KD = 0.003;

    @Override
    public void init() {
        // Initialize motor
        motor = hardwareMap.get(DcMotorEx.class, MOTOR_NAME);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Initialize limit switch
        limitSwitch = hardwareMap.get(TouchSensor.class, LIMIT_SWITCH_NAME);

        // Initialize timers
        loopTimer = new ElapsedTime();
        homingTimer = new ElapsedTime();

        lastUpdateTimeMs = System.currentTimeMillis();

        telemetry.addLine("╔═══════════════════════════════════════╗");
        telemetry.addLine("║   SPINDEXER MOTOR TUNING              ║");
        telemetry.addLine("╠═══════════════════════════════════════╣");
        telemetry.addLine("║ GP1: A=0° B=120° X=240° Y=Home        ║");
        telemetry.addLine("║      LStickX=Manual DPAD=±30°/±5°     ║");
        telemetry.addLine("║ GP2: DPAD=kP/kD  A=Wrap  LB=Print     ║");
        telemetry.addLine("╚═══════════════════════════════════════╝");
        telemetry.update();
    }

    @Override
    public void init_loop() {
        updateSensorCache();
        
        telemetry.addData("Encoder", "%d ticks (%.1f°)", 
            motor.getCurrentPosition(), currentAngleDeg);
        telemetry.addData("Limit Switch", limitSwitch.isPressed() ? "✓ ACTIVO" : "○ Inactivo");
        telemetry.addLine();
        telemetry.addLine("Presiona START para iniciar");
        telemetry.update();
    }

    @Override
    public void start() {
        loopTimer.reset();
        
        // Check if we start on the limit switch
        if (limitSwitch.isPressed()) {
            // Already at home position
            setCurrentPositionAsHome();
            isHomed = true;
        } else {
            // Not at home, but encoder starts at 0
            // We'll consider this as "approximately homed" until user runs homing
            isHomed = false;
        }
        
        currentState = State.IDLE;
    }

    @Override
    public void loop() {
        // Measure loop time
        loopTimeMs = loopTimer.milliseconds();
        loopTimer.reset();

        // Update sensor readings
        updateSensorCache();

        // Process controls
        handleGamepad1Controls();
        handleGamepad2Tuning();

        // Execute state machine
        executeStateMachine();

        // Update analytics
        updateAnalytics();

        // Update telemetry
        updateTelemetry();
    }

    // ==================== SENSOR CACHE ====================

    private void updateSensorCache() {
        long now = System.currentTimeMillis();
        double deltaTimeSec = (now - lastUpdateTimeMs) / 1000.0;

        double currentTicks = motor.getCurrentPosition();
        currentAngleDeg = ticksToDegrees(currentTicks - homeOffsetTicks);

        if (deltaTimeSec > 0.001) { // Avoid division by zero
            double deltaTicks = currentTicks - lastPositionTicks;
            currentVelocityDegPerSec = ticksToDegrees(deltaTicks) / deltaTimeSec;
        }

        lastPositionTicks = currentTicks;
        lastUpdateTimeMs = now;
    }

    // ==================== STATE MACHINE ====================

    private void executeStateMachine() {
        // Always check for passive homing opportunity
        if (currentState != State.HOMING) {
            checkPassiveHoming();
        }
        
        switch (currentState) {
            case POSITION_CONTROL:
                executePositionControl();
                // Track direction based on motor output
                double power = motor.getPower();
                if (Math.abs(power) > 0.05) {
                    lastMovementDirection = (power > 0) ? 1 : -1;
                }
                break;
            case HOMING:
                executeHoming();
                break;
            case MANUAL:
            case IDLE:
            default:
                // Motor controlled directly by gamepad or stopped
                break;
        }
    }

    private void executePositionControl() {
        double error = calculateError(currentAngleDeg, targetAngleDeg);
        double derivative = (error - lastError) * (loopTimeMs > 0 ? 1000.0 / loopTimeMs : 0);
        lastError = error;

        double output = (kP * error) + (kD * derivative);
        output = clamp(output, -MAX_POWER, MAX_POWER);

        motor.setPower(output);
    }

    private void executeHoming() {
        if (limitSwitch.isPressed()) {
            // Found home! We're approaching from CCW (positive power)
            motor.setPower(0);
            
            // Apply CCW offset since we're coming from that direction
            applyHomeWithOffset(HOME_OFFSET_CCW_DEG, "CCW");
            
            isHomed = true;
            homingComplete = true;
            currentState = State.IDLE;
            targetAngleDeg = 0.0;
        } else if (homingTimer.milliseconds() > HOMING_TIMEOUT_MS) {
            // Timeout
            motor.setPower(0);
            currentState = State.IDLE;
            homingComplete = false;
        } else {
            // Keep searching in CCW direction (positive power)
            motor.setPower(HOMING_POWER);
            lastMovementDirection = 1; // CCW
        }
    }
    
    /**
     * Checks for passive homing during normal operation.
     * If the limit switch is triggered while moving, we can use that
     * to verify/correct our position without explicit homing.
     */
    private void checkPassiveHoming() {
        boolean limitPressed = limitSwitch.isPressed();
        
        // Detect rising edge (transition from not-pressed to pressed)
        if (limitPressed && !wasLimitPressed && lastMovementDirection != 0) {
            // We just entered the magnet zone
            double offset = (lastMovementDirection > 0) ? HOME_OFFSET_CCW_DEG : HOME_OFFSET_CW_DEG;
            String direction = (lastMovementDirection > 0) ? "CCW" : "CW";
            
            double assumedError = Math.abs(currentAngleDeg - offset);
            if (assumedError > SMART_HOMING_THRESHOLD_DEG) {
                applyHomeWithOffset(offset, direction + " (passive)");
            }
        }
        
        wasLimitPressed = limitPressed;
    }
    
    /**
     * Applies home offset and resets encoder reference.
     */
    private void applyHomeWithOffset(double offsetDeg, String source) {
        double assumedError = Math.abs(currentAngleDeg);
        
        if (assumedError > SMART_HOMING_THRESHOLD_DEG || !isHomed) {
            homeOffsetTicks = motor.getCurrentPosition() - degreesToTicks(offsetDeg);
            currentAngleDeg = offsetDeg;
            lastError = 0;
        }
        
        isHomed = true;
    }

    // ==================== CONTROL HELPERS ====================

    /**
     * Calculates error with optional wrapping for shortest path.
     */
    private double calculateError(double current, double target) {
        double error = target - current;
        
        if (wrappingEnabled) {
            // Normalize error to [-180, 180] for shortest path
            while (error > 180) error -= 360;
            while (error < -180) error += 360;
        }
        
        return error;
    }

    private void setCurrentPositionAsHome() {
        homeOffsetTicks = motor.getCurrentPosition();
        currentAngleDeg = HOME_OFFSET_DEG;
        targetAngleDeg = HOME_OFFSET_DEG;
        lastError = 0;
    }

    private void goToPosition(double angleDeg) {
        targetAngleDeg = angleDeg;
        currentState = State.POSITION_CONTROL;
        lastError = 0;
        resetAnalytics();
    }

    private void startHoming() {
        currentState = State.HOMING;
        homingTimer.reset();
        homingComplete = false;
    }

    // ==================== GAMEPAD HANDLERS ====================

    private void handleGamepad1Controls() {
        // Manual control
        double manualInput = -gamepad1.left_stick_x;
        if (Math.abs(manualInput) > 0.1) {
            currentState = State.MANUAL;
            motor.setPower(manualInput * 0.5);
            // Track direction for passive homing
            lastMovementDirection = (manualInput > 0) ? 1 : -1;
        } else if (currentState == State.MANUAL) {
            motor.setPower(0);
            currentState = State.IDLE;
            lastMovementDirection = 0;
        }

        // A -> Slot 0 (0°)
        if (gamepad1.a && !lastA) {
            goToPosition(SLOT_0_DEG);
        }
        lastA = gamepad1.a;

        // B -> Slot 1 (120°)
        if (gamepad1.b && !lastB) {
            goToPosition(SLOT_1_DEG);
        }
        lastB = gamepad1.b;

        // X -> Slot 2 (240°)
        if (gamepad1.x && !lastX) {
            goToPosition(SLOT_2_DEG);
        }
        lastX = gamepad1.x;

        // Y -> Homing
        if (gamepad1.y && !lastY) {
            startHoming();
        }
        lastY = gamepad1.y;

        // DPAD UP/DOWN -> ±30°
        if (gamepad1.dpad_up && !lastDUp) {
            goToPosition(targetAngleDeg + 30);
        }
        lastDUp = gamepad1.dpad_up;

        if (gamepad1.dpad_down && !lastDDown) {
            goToPosition(targetAngleDeg - 30);
        }
        lastDDown = gamepad1.dpad_down;

        // DPAD LEFT/RIGHT -> ±5°
        if (gamepad1.dpad_left && !lastDLeft) {
            goToPosition(targetAngleDeg + 5);
        }
        lastDLeft = gamepad1.dpad_left;

        if (gamepad1.dpad_right && !lastDRight) {
            goToPosition(targetAngleDeg - 5);
        }
        lastDRight = gamepad1.dpad_right;

        // Left Bumper -> Set current as home
        if (gamepad1.left_bumper && !lastLB) {
            setCurrentPositionAsHome();
            isHomed = true;
        }
        lastLB = gamepad1.left_bumper;

        // Right Bumper -> Stop
        if (gamepad1.right_bumper && !lastRB) {
            motor.setPower(0);
            currentState = State.IDLE;
        }
        lastRB = gamepad1.right_bumper;
    }

    private void handleGamepad2Tuning() {
        // DPAD UP -> kP++
        if (gamepad2.dpad_up && !lastG2DUp) {
            kP += KP_INCREMENT;
            resetAnalytics();
        }
        lastG2DUp = gamepad2.dpad_up;

        // DPAD DOWN -> kP--
        if (gamepad2.dpad_down && !lastG2DDown) {
            kP = Math.max(0, kP - KP_INCREMENT);
            resetAnalytics();
        }
        lastG2DDown = gamepad2.dpad_down;

        // DPAD RIGHT -> kD++
        if (gamepad2.dpad_right && !lastG2DRight) {
            kD += KD_INCREMENT;
            resetAnalytics();
        }
        lastG2DRight = gamepad2.dpad_right;

        // DPAD LEFT -> kD--
        if (gamepad2.dpad_left && !lastG2DLeft) {
            kD = Math.max(0, kD - KD_INCREMENT);
            resetAnalytics();
        }
        lastG2DLeft = gamepad2.dpad_left;

        // A -> Toggle wrapping
        if (gamepad2.a && !lastG2A) {
            wrappingEnabled = !wrappingEnabled;
        }
        lastG2A = gamepad2.a;

        // Y -> Reset PID
        if (gamepad2.y && !lastG2Y) {
            kP = DEFAULT_KP;
            kD = DEFAULT_KD;
        }
        lastG2Y = gamepad2.y;

        // Left Bumper -> Print values
        if (gamepad2.left_bumper && !lastG2LB) {
            System.out.println("===== SPINDEXER PD VALUES =====");
            System.out.println("public static double kP = " + kP + ";");
            System.out.println("public static double kD = " + kD + ";");
            System.out.println("Wrapping: " + wrappingEnabled);
            System.out.println("================================");
        }
        lastG2LB = gamepad2.left_bumper;
    }

    // ==================== ANALYTICS ====================

    private void updateAnalytics() {
        double currentError = Math.abs(calculateError(currentAngleDeg, targetAngleDeg));

        if (currentError > maxError) {
            maxError = currentError;
        }

        boolean atTarget = isAtPosition();
        if (!wasAtTarget && atTarget) {
            if (settlingStartTime > 0) {
                settlingTime = (System.currentTimeMillis() - settlingStartTime) / 1000.0;
            }
        } else if (wasAtTarget && !atTarget) {
            settlingStartTime = System.currentTimeMillis();
        }
        wasAtTarget = atTarget;
    }

    private void resetAnalytics() {
        maxError = Math.abs(calculateError(currentAngleDeg, targetAngleDeg));
        settlingStartTime = System.currentTimeMillis();
        settlingTime = 0.0;
        wasAtTarget = false;
    }

    private boolean isAtPosition() {
        return Math.abs(calculateError(currentAngleDeg, targetAngleDeg)) <= POSITION_TOLERANCE_DEG;
    }

    private boolean isAtPositionAndStable() {
        return isAtPosition() && Math.abs(currentVelocityDegPerSec) <= VELOCITY_TOLERANCE_DEG_PER_SEC;
    }

    // ==================== TELEMETRY ====================

    private void updateTelemetry() {
        // STATE
        telemetry.addLine("═══════ ESTADO ═══════");
        telemetry.addData("Estado", currentState.name());
        String dirSymbol = lastMovementDirection > 0 ? "↺CCW" : (lastMovementDirection < 0 ? "↻CW" : "⏹");
        telemetry.addData("Homed", (isHomed ? "✓" : "✗") + " | Dir: " + dirSymbol);
        telemetry.addData("Limit Switch", limitSwitch.isPressed() ? "✓ ACTIVO" : "○");

        // POSITION
        telemetry.addLine();
        telemetry.addLine("═══════ POSICIÓN ═══════");
        telemetry.addData("Actual", "%.2f°", currentAngleDeg);
        telemetry.addData("Target", "%.2f°", targetAngleDeg);
        telemetry.addData("Error", "%.2f°", calculateError(currentAngleDeg, targetAngleDeg));
        telemetry.addData("En Posición", isAtPositionAndStable() ? "✓" : "✗");

        // MOTOR
        telemetry.addLine();
        telemetry.addLine("═══════ MOTOR ═══════");
        telemetry.addData("Power", "%.3f", motor.getPower());
        telemetry.addData("Velocidad", "%.1f °/s", currentVelocityDegPerSec);
        telemetry.addData("Encoder Raw", "%.0f", (double) motor.getCurrentPosition());

        // PD TUNING
        telemetry.addLine();
        telemetry.addLine("═══════ PD (GP2) ═══════");
        telemetry.addData("kP (↑↓)", "%.4f", kP);
        telemetry.addData("kD (←→)", "%.4f", kD);
        telemetry.addData("Wrapping (A)", wrappingEnabled ? "ON" : "OFF");
        
        // HOMING OFFSETS
        telemetry.addLine();
        telemetry.addLine("═══════ HOMING OFFSETS ═══════");
        telemetry.addData("CCW Offset", "%.1f°", HOME_OFFSET_CCW_DEG);
        telemetry.addData("CW Offset", "%.1f°", HOME_OFFSET_CW_DEG);

        // ANALYSIS
        telemetry.addLine();
        telemetry.addLine("═══════ ANÁLISIS ═══════");
        telemetry.addData("Max Error", "%.2f°", maxError);
        telemetry.addData("Settling Time", "%.2f s", settlingTime);
        telemetry.addData("Loop", "%.1f ms", loopTimeMs);

        telemetry.update();
    }

    // ==================== UTILITIES ====================

    private double ticksToDegrees(double ticks) {
        return ticks * DEGREES_PER_TICK;
    }

    private double degreesToTicks(double degrees) {
        return degrees * TICKS_PER_DEGREE;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void stop() {
        motor.setPower(0);
    }
}
