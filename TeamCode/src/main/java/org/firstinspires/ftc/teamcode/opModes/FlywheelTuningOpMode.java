package org.firstinspires.ftc.teamcode.opModes;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

/**
 * Flywheel Tuning OpMode
 * 
 * Purpose: Test and tune flywheel RPM, pivot angles, and measure spin-up times
 * 
 * CONTROLS:
 * ========
 * Gamepad 1:
 * - D-pad Up:       Increase target RPM by 100
 * - D-pad Down:     Decrease target RPM by 100
 * - D-pad Right:    Increase target RPM by 500 (coarse)
 * - D-pad Left:     Decrease target RPM by 500 (coarse)
 * 
 * - Right Trigger:  Set to IDLE mode (1500 RPM)
 * - Left Trigger:   STOP flywheel (0 RPM)
 * 
 * - A:              Quick test - Measure spin-up from 0 → 2800 RPM
 * - B:              Quick test - Measure spin-up from IDLE → 2800 RPM
 * - X:              Quick test - Measure spin-up from 0 → 3200 RPM
 * - Y:              Quick test - Measure spin-up from IDLE → 3200 RPM
 * 
 * - Right Bumper:   Increase pivot angle by 5°
 * - Left Bumper:    Decrease pivot angle by 5°
 * 
 * - Back:           Reset all measurements
 * - Start:          Toggle logging mode
 * 
 * TELEMETRY:
 * =========
 * - Current RPM
 * - Target RPM
 * - Spin-up time (when in progress)
 * - Pivot angle
 * - Motor power
 * - Test results history
 * 
 * RECOMMENDED TESTING PROCEDURE:
 * =============================
 * 1. Initial Calibration:
 *    - Use D-pad to set various RPM values
 *    - Record actual RPM achieved vs target
 *    - Verify RPM is stable and accurate
 * 
 * 2. Idle Mode Testing:
 *    - Press Right Trigger (set to IDLE)
 *    - Record actual RPM and battery drain over time
 *    - Adjust IDLE_RPM constant if needed
 * 
 * 3. Spin-up Time Testing:
 *    - Press A (0 → close shot) and record time
 *    - Press B (idle → close shot) and record time
 *    - Press X (0 → far shot) and record time
 *    - Press Y (idle → far shot) and record time
 *    - Compare times to validate idle mode benefit
 * 
 * 4. Distance Tuning (requires field setup):
 *    - Position robot at known distances
 *    - Test different RPM/pivot combinations
 *    - Record which combinations successfully score
 *    - Build lookup table
 * 
 * 5. Pivot Angle Tuning:
 *    - Use bumpers to adjust pivot
 *    - Test scoring at various distances
 *    - Record optimal angles per distance
 * 
 * NOTES:
 * =====
 * - Motor must be configured as velocity control (RUN_USING_ENCODER)
 * - Ensure battery is fully charged for consistent results
 * - Record all data for building lookup tables
 * - Test in match-like conditions (with intake loaded)
 */
@TeleOp(name = "🔧 Flywheel Tuning", group = "Testing")
public class FlywheelTuningOpMode extends CommandOpMode {
    
    // Hardware
    private DcMotorEx flywheelMotor;
    private Servo pivotServo;
    
    // Gamepad
    private GamepadEx gamepad;
    
    // State tracking
    private double targetRPM = 0;
    private double idleRPM = 1500;  // Adjust this during testing
    private double closeRPM = 2800; // Adjust this during testing
    private double farRPM = 3200;   // Adjust this during testing
    
    private double pivotAngle = 45.0;  // Start position
    private double pivotMin = 20.0;    // Minimum safe angle
    private double pivotMax = 70.0;    // Maximum safe angle
    
    // Measurement
    private ElapsedTime spinUpTimer = new ElapsedTime();
    private boolean measuring = false;
    private double measurementStartRPM = 0;
    private double measurementTargetRPM = 0;
    private double lastMeasuredTime = 0;
    
    // Test results storage
    private String[] testResults = new String[10];
    private int testIndex = 0;
    
    // Logging
    private boolean loggingEnabled = false;
    private StringBuilder dataLog = new StringBuilder();
    
    // RPM tolerance for "at speed" detection
    private static final double RPM_TOLERANCE = 50;
    
    // Pivot servo positions (adjust based on your servo range)
    private static final double PIVOT_SERVO_MIN = 0.0;  // Servo position at pivotMin
    private static final double PIVOT_SERVO_MAX = 1.0;  // Servo position at pivotMax
    
    @Override
    public void initialize() {
        // Initialize hardware
        try {
            flywheelMotor = hardwareMap.get(DcMotorEx.class, "flywheelMotor");
            flywheelMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            flywheelMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            flywheelMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            telemetry.addLine("✓ Flywheel motor initialized");
        } catch (Exception e) {
            telemetry.addLine("✗ ERROR: Flywheel motor not found!");
            telemetry.addLine("  Check hardware name: 'flywheelMotor'");
        }
        
        try {
            pivotServo = hardwareMap.get(Servo.class, "pivotServo");
            setPivotAngle(pivotAngle);
            telemetry.addLine("✓ Pivot servo initialized");
        } catch (Exception e) {
            telemetry.addLine("✗ WARNING: Pivot servo not found!");
            telemetry.addLine("  Continuing without pivot control");
        }
        
        // Initialize gamepad
        gamepad = new GamepadEx(gamepad1);
        
        // Initialize test results
        for (int i = 0; i < testResults.length; i++) {
            testResults[i] = "";
        }
        
        telemetry.addLine();
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine("   FLYWHEEL TUNING MODE READY");
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine();
        telemetry.addLine("Press START to begin");
        telemetry.addLine("See OpMode comments for controls");
        telemetry.update();
    }
    
    @Override
    public void run() {
        super.run();
        
        // Update gamepad
        gamepad.readButtons();
        
        // ============================================
        // RPM CONTROL
        // ============================================
        
        // Fine adjustment (±100 RPM)
        if (gamepad.wasJustPressed(GamepadKeys.Button.DPAD_UP)) {
            targetRPM += 100;
            setFlywheelRPM(targetRPM);
        }
        if (gamepad.wasJustPressed(GamepadKeys.Button.DPAD_DOWN)) {
            targetRPM = Math.max(0, targetRPM - 100);
            setFlywheelRPM(targetRPM);
        }
        
        // Coarse adjustment (±500 RPM)
        if (gamepad.wasJustPressed(GamepadKeys.Button.DPAD_RIGHT)) {
            targetRPM += 500;
            setFlywheelRPM(targetRPM);
        }
        if (gamepad.wasJustPressed(GamepadKeys.Button.DPAD_LEFT)) {
            targetRPM = Math.max(0, targetRPM - 500);
            setFlywheelRPM(targetRPM);
        }
        
        // Preset modes
        if (gamepad1.right_trigger > 0.5) {
            // Idle mode
            targetRPM = idleRPM;
            setFlywheelRPM(targetRPM);
        }
        
        if (gamepad1.left_trigger > 0.5) {
            // Stop
            targetRPM = 0;
            setFlywheelRPM(0);
        }
        
        // ============================================
        // QUICK TESTS
        // ============================================
        
        if (gamepad.wasJustPressed(GamepadKeys.Button.A)) {
            // Test: 0 → close shot
            startSpinUpTest(0, closeRPM, "0→Close");
        }
        
        if (gamepad.wasJustPressed(GamepadKeys.Button.B)) {
            // Test: idle → close shot
            startSpinUpTest(idleRPM, closeRPM, "Idle→Close");
        }
        
        if (gamepad.wasJustPressed(GamepadKeys.Button.X)) {
            // Test: 0 → far shot
            startSpinUpTest(0, farRPM, "0→Far");
        }
        
        if (gamepad.wasJustPressed(GamepadKeys.Button.Y)) {
            // Test: idle → far shot
            startSpinUpTest(idleRPM, farRPM, "Idle→Far");
        }
        
        // ============================================
        // PIVOT CONTROL
        // ============================================
        
        if (gamepad.wasJustPressed(GamepadKeys.Button.RIGHT_BUMPER)) {
            pivotAngle = Math.min(pivotMax, pivotAngle + 5);
            setPivotAngle(pivotAngle);
        }
        
        if (gamepad.wasJustPressed(GamepadKeys.Button.LEFT_BUMPER)) {
            pivotAngle = Math.max(pivotMin, pivotAngle - 5);
            setPivotAngle(pivotAngle);
        }
        
        // ============================================
        // UTILITY
        // ============================================
        
        if (gamepad.wasJustPressed(GamepadKeys.Button.BACK)) {
            // Reset measurements
            for (int i = 0; i < testResults.length; i++) {
                testResults[i] = "";
            }
            testIndex = 0;
            telemetry.addLine("✓ Measurements reset");
        }
        
        if (gamepad.wasJustPressed(GamepadKeys.Button.START)) {
            // Toggle logging
            loggingEnabled = !loggingEnabled;
            if (loggingEnabled) {
                dataLog = new StringBuilder();
                dataLog.append("Time(ms),TargetRPM,ActualRPM,Power,PivotAngle\n");
            }
        }
        
        // ============================================
        // MEASUREMENT TRACKING
        // ============================================
        
        if (measuring) {
            double currentRPM = getCurrentRPM();
            if (Math.abs(currentRPM - measurementTargetRPM) < RPM_TOLERANCE) {
                // Target reached!
                lastMeasuredTime = spinUpTimer.milliseconds();
                measuring = false;
                
                // Store result
                String result = String.format("%s: %.0fms", 
                    getCurrentTestLabel(), lastMeasuredTime);
                testResults[testIndex] = result;
                testIndex = (testIndex + 1) % testResults.length;
                
                gamepad1.rumble(200); // Haptic feedback
            }
        }
        
        // ============================================
        // DATA LOGGING
        // ============================================
        
        if (loggingEnabled && flywheelMotor != null) {
            dataLog.append(String.format("%d,%.0f,%.0f,%.3f,%.1f\n",
                System.currentTimeMillis(),
                targetRPM,
                getCurrentRPM(),
                flywheelMotor.getPower(),
                pivotAngle
            ));
        }
        
        // ============================================
        // TELEMETRY
        // ============================================
        
        updateTelemetry();
    }
    
    /**
     * Start a spin-up measurement test
     */
    private void startSpinUpTest(double fromRPM, double toRPM, String label) {
        if (flywheelMotor == null) return;
        
        // Set to starting RPM first
        setFlywheelRPM(fromRPM);
        targetRPM = fromRPM;
        
        // Wait a moment for motor to settle (you might need to extend this)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Start measurement
        measuring = true;
        measurementStartRPM = fromRPM;
        measurementTargetRPM = toRPM;
        spinUpTimer.reset();
        
        // Spin up to target
        setFlywheelRPM(toRPM);
        targetRPM = toRPM;
        
        telemetry.addLine("▶ Test started: " + label);
        telemetry.update();
    }
    
    /**
     * Set flywheel RPM using velocity control
     */
    private void setFlywheelRPM(double rpm) {
        if (flywheelMotor == null) return;
        
        // Convert RPM to ticks per second
        // Assumes 28 ticks per revolution for goBILDA motor encoder
        // Adjust TICKS_PER_REV if using different motor
        final double TICKS_PER_REV = 28.0;
        double ticksPerSecond = (rpm / 60.0) * TICKS_PER_REV;
        
        if (rpm == 0) {
            flywheelMotor.setPower(0);
        } else {
            flywheelMotor.setVelocity(ticksPerSecond);
        }
    }
    
    /**
     * Get current flywheel RPM
     */
    private double getCurrentRPM() {
        if (flywheelMotor == null) return 0;
        
        final double TICKS_PER_REV = 28.0;
        double ticksPerSecond = flywheelMotor.getVelocity();
        return (ticksPerSecond / TICKS_PER_REV) * 60.0;
    }
    
    /**
     * Set pivot servo angle
     */
    private void setPivotAngle(double angle) {
        if (pivotServo == null) return;
        
        // Map angle to servo position (0.0 to 1.0)
        double normalized = (angle - pivotMin) / (pivotMax - pivotMin);
        double servoPos = PIVOT_SERVO_MIN + normalized * (PIVOT_SERVO_MAX - PIVOT_SERVO_MIN);
        servoPos = Math.max(0.0, Math.min(1.0, servoPos));
        
        pivotServo.setPosition(servoPos);
    }
    
    /**
     * Get label for current test
     */
    private String getCurrentTestLabel() {
        if (measurementStartRPM == 0 && measurementTargetRPM == closeRPM) return "0→Close";
        if (measurementStartRPM == idleRPM && measurementTargetRPM == closeRPM) return "Idle→Close";
        if (measurementStartRPM == 0 && measurementTargetRPM == farRPM) return "0→Far";
        if (measurementStartRPM == idleRPM && measurementTargetRPM == farRPM) return "Idle→Far";
        return "Custom";
    }
    
    /**
     * Update telemetry display
     */
    private void updateTelemetry() {
        telemetry.clear();
        
        // Header
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine("    🔧 FLYWHEEL TUNING MODE");
        telemetry.addLine("═══════════════════════════════════");
        telemetry.addLine();
        
        // Current state
        if (flywheelMotor != null) {
            double currentRPM = getCurrentRPM();
            telemetry.addLine("╔═══ CURRENT STATE ═══╗");
            telemetry.addData("│ Target RPM", "%.0f", targetRPM);
            telemetry.addData("│ Actual RPM", "%.0f", currentRPM);
            telemetry.addData("│ Error", "%.0f", Math.abs(currentRPM - targetRPM));
            telemetry.addData("│ Motor Power", "%.2f", flywheelMotor.getPower());
            
            // Status indicator
            String status;
            if (targetRPM == 0) {
                status = "⏹ STOPPED";
            } else if (Math.abs(currentRPM - targetRPM) < RPM_TOLERANCE) {
                status = "✓ AT SPEED";
            } else {
                status = "⏫ SPINNING UP";
            }
            telemetry.addData("│ Status", status);
            telemetry.addLine("╚═════════════════════╝");
        } else {
            telemetry.addLine("✗ Flywheel motor not initialized");
        }
        
        telemetry.addLine();
        
        // Pivot state
        if (pivotServo != null) {
            telemetry.addLine("╔═══ PIVOT ANGLE ═══╗");
            telemetry.addData("│ Angle", "%.1f°", pivotAngle);
            telemetry.addData("│ Range", "%.0f° - %.0f°", pivotMin, pivotMax);
            telemetry.addLine("╚═══════════════════╝");
        }
        
        telemetry.addLine();
        
        // Measurement in progress
        if (measuring) {
            telemetry.addLine("╔═══ MEASURING ═══╗");
            telemetry.addData("│ Test", "%s", getCurrentTestLabel());
            telemetry.addData("│ Start RPM", "%.0f", measurementStartRPM);
            telemetry.addData("│ Target RPM", "%.0f", measurementTargetRPM);
            telemetry.addData("│ Time", "%.0f ms", spinUpTimer.milliseconds());
            telemetry.addLine("╚═════════════════╝");
            telemetry.addLine();
        }
        
        // Test results
        telemetry.addLine("╔═══ TEST RESULTS ═══╗");
        boolean hasResults = false;
        for (int i = 0; i < testResults.length; i++) {
            if (!testResults[i].isEmpty()) {
                telemetry.addLine("│ " + testResults[i]);
                hasResults = true;
            }
        }
        if (!hasResults) {
            telemetry.addLine("│ (No tests run yet)");
        }
        telemetry.addLine("╚════════════════════╝");
        
        telemetry.addLine();
        
        // Quick reference
        telemetry.addLine("╔═══ QUICK REFERENCE ═══╗");
        telemetry.addLine("│ A: 0→Close  B: Idle→Close");
        telemetry.addLine("│ X: 0→Far    Y: Idle→Far");
        telemetry.addLine("│ D-pad: Adjust RPM");
        telemetry.addLine("│ Bumpers: Adjust pivot");
        telemetry.addLine("│ RT: Idle  LT: Stop");
        telemetry.addLine("╚═══════════════════════╝");
        
        // Logging indicator
        if (loggingEnabled) {
            telemetry.addLine();
            telemetry.addLine("📊 Data logging ENABLED");
            telemetry.addLine("   (Press START to stop)");
        }
        
        telemetry.update();
    }
    
    @Override
    public void reset() {
        // Stop flywheel on reset
        if (flywheelMotor != null) {
            flywheelMotor.setPower(0);
        }
        
        // Save log if enabled
        if (loggingEnabled && dataLog.length() > 0) {
            telemetry.addLine("Data log captured:");
            telemetry.addLine(dataLog.toString());
            telemetry.addLine();
            telemetry.addLine("Copy this data for analysis");
        }
        
        super.reset();
    }
}