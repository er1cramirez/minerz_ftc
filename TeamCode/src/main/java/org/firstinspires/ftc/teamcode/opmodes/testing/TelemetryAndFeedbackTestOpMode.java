package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.telemetry.TelemetryHelper;
import org.firstinspires.ftc.teamcode.util.UserGamepadFeedback;

/**
 * OpMode de prueba integral para Telemetría y Feedback del Gamepad.
 * 
 * PROPÓSITO:
 * - Probar los tres formatos de display (CLASSIC, MONOSPACE, HTML)
 * - Probar todos los efectos de rumble y LED
 * - Simular datos dinámicos para ver cómo se ve la telemetría en uso real
 * - Probar autoClear vs retained items
 * 
 * CONTROLES GAMEPAD 1 (Test de Efectos):
 * A = ERROR_GENERIC + LED_ERROR
 * B = WARNING_FULL + LED_WARNING
 * X = SUCCESS_INTAKE + LED_SUCCESS
 * Y = READY_TO_SHOOT + LED_READY
 * LB = TARGET_ACQUIRED + LED_LOCKED
 * RB = TARGET_LOST
 * DPAD_UP = LED_TARGETING (pulsante azul)
 * DPAD_DOWN = LED_RAINBOW
 * DPAD_LEFT = TICK (pulso corto)
 * DPAD_RIGHT = LOW_BATTERY
 * 
 * CONTROLES GAMEPAD 2 (Control de Telemetría):
 * A = Ciclar DisplayFormat (CLASSIC → MONOSPACE → HTML)
 * B = Toggle autoClear
 * X = Simular cambio de datos (manual)
 * Y = Clear all telemetry
 * DPAD_UP/DOWN = Cambiar RPM simulado
 * DPAD_LEFT/RIGHT = Cambiar slot actual
 * LT/RT = Cambiar ángulo turret
 */
@TeleOp(name = "Test: Telemetry & Feedback", group = "Tuning")
public class TelemetryAndFeedbackTestOpMode extends LinearOpMode {

    // Telemetry helper
    private TelemetryHelper helper;
    
    // Simulated data
    private double simulatedRPM = 0;
    private double targetRPM = 2500;
    private int currentSlot = 0;
    private String[] slotStates = {"⚫", "⚫", "⚫"}; // Empty slots
    private double turretAngle = 0;
    private String turretState = "DISABLED";
    private String driveMode = "NORMAL";
    private double heading = 0;
    private boolean isFieldCentric = true;
    private double voltage = 12.8;
    
    // Timing
    private ElapsedTime runtime = new ElapsedTime();
    private ElapsedTime loopTimer = new ElapsedTime();
    private long lastLoopMs = 0;
    
    // State
    private boolean autoClear = true;
    private String lastEffect = "None";
    private boolean autoSimulate = true;
    
    // Button debounce
    private boolean[] gp1Pressed = new boolean[16];
    private boolean[] gp2Pressed = new boolean[16];

    @Override
    public void runOpMode() {
        // Initialize helper
        helper = new TelemetryHelper(telemetry);
        helper.setDisplayMode(TelemetryHelper.DisplayMode.CLASSIC);
        
        // Init message
        telemetry.addLine("=== TELEMETRY & FEEDBACK TEST ===");
        telemetry.addLine("Press START to begin");
        telemetry.addLine("");
        telemetry.addLine("GP1: Test gamepad effects");
        telemetry.addLine("GP2: Control telemetry settings");
        telemetry.update();
        
        waitForStart();
        runtime.reset();
        loopTimer.reset();
        
        while (opModeIsActive()) {
            // Calculate loop time
            lastLoopMs = (long) loopTimer.milliseconds();
            loopTimer.reset();
            
            // Handle inputs
            handleGamepad1Effects();
            handleGamepad2Telemetry();
            
            // Auto-simulate data changes
            if (autoSimulate) {
                simulateDataChanges();
            }
            
            // Update telemetry display
            updateTelemetryDisplay();
        }
    }
    
    /**
     * Handle Gamepad 1 - Effect testing buttons
     */
    private void handleGamepad1Effects() {
        // A - Error
        if (gamepad1.a && !gp1Pressed[0]) {
            UserGamepadFeedback.playError(gamepad1);
            lastEffect = "ERROR_GENERIC + LED_ERROR";
        }
        gp1Pressed[0] = gamepad1.a;
        
        // B - Warning
        if (gamepad1.b && !gp1Pressed[1]) {
            UserGamepadFeedback.playWarning(gamepad1);
            lastEffect = "WARNING_FULL + LED_WARNING";
        }
        gp1Pressed[1] = gamepad1.b;
        
        // X - Success
        if (gamepad1.x && !gp1Pressed[2]) {
            UserGamepadFeedback.playSuccess(gamepad1);
            lastEffect = "SUCCESS_INTAKE + LED_SUCCESS";
        }
        gp1Pressed[2] = gamepad1.x;
        
        // Y - Ready to shoot
        if (gamepad1.y && !gp1Pressed[3]) {
            UserGamepadFeedback.playReady(gamepad1);
            lastEffect = "READY_TO_SHOOT + LED_READY";
        }
        gp1Pressed[3] = gamepad1.y;
        
        // LB - Target acquired
        if (gamepad1.left_bumper && !gp1Pressed[4]) {
            UserGamepadFeedback.playTargetAcquired(gamepad1);
            lastEffect = "TARGET_ACQUIRED + LED_LOCKED";
        }
        gp1Pressed[4] = gamepad1.left_bumper;
        
        // RB - Target lost
        if (gamepad1.right_bumper && !gp1Pressed[5]) {
            UserGamepadFeedback.playTargetLost(gamepad1);
            lastEffect = "TARGET_LOST (rumble only)";
        }
        gp1Pressed[5] = gamepad1.right_bumper;
        
        // DPAD UP - LED Targeting
        if (gamepad1.dpad_up && !gp1Pressed[6]) {
            gamepad1.runLedEffect(UserGamepadFeedback.LED_TARGETING);
            lastEffect = "LED_TARGETING (blue pulse)";
        }
        gp1Pressed[6] = gamepad1.dpad_up;
        
        // DPAD DOWN - LED Rainbow
        if (gamepad1.dpad_down && !gp1Pressed[7]) {
            gamepad1.runLedEffect(UserGamepadFeedback.LED_RAINBOW);
            lastEffect = "LED_RAINBOW";
        }
        gp1Pressed[7] = gamepad1.dpad_down;
        
        // DPAD LEFT - Tick
        if (gamepad1.dpad_left && !gp1Pressed[8]) {
            gamepad1.runRumbleEffect(UserGamepadFeedback.TICK);
            lastEffect = "TICK (short pulse)";
        }
        gp1Pressed[8] = gamepad1.dpad_left;
        
        // DPAD RIGHT - Low battery
        if (gamepad1.dpad_right && !gp1Pressed[9]) {
            UserGamepadFeedback.playLowBattery(gamepad1);
            lastEffect = "LOW_BATTERY + LED_WARNING";
        }
        gp1Pressed[9] = gamepad1.dpad_right;
    }
    
    /**
     * Handle Gamepad 2 - Telemetry control
     */
    private void handleGamepad2Telemetry() {
        // A - Cycle display format
        if (gamepad2.a && !gp2Pressed[0]) {
            TelemetryHelper.DisplayMode newMode = helper.cycleDisplayMode();
            lastEffect = "Display: " + newMode.name();
        }
        gp2Pressed[0] = gamepad2.a;
        
        // B - Toggle autoClear
        if (gamepad2.b && !gp2Pressed[1]) {
            autoClear = !autoClear;
            helper.setAutoClear(autoClear);
            lastEffect = "AutoClear: " + (autoClear ? "ON" : "OFF");
        }
        gp2Pressed[1] = gamepad2.b;
        
        // X - Toggle auto simulate
        if (gamepad2.x && !gp2Pressed[2]) {
            autoSimulate = !autoSimulate;
            lastEffect = "AutoSimulate: " + (autoSimulate ? "ON" : "OFF");
        }
        gp2Pressed[2] = gamepad2.x;
        
        // Y - Clear all
        if (gamepad2.y && !gp2Pressed[3]) {
            telemetry.clearAll();
            lastEffect = "Cleared all telemetry";
        }
        gp2Pressed[3] = gamepad2.y;
        
        // DPAD UP/DOWN - Change RPM
        if (gamepad2.dpad_up && !gp2Pressed[4]) {
            targetRPM = Math.min(3000, targetRPM + 250);
            lastEffect = "Target RPM: " + targetRPM;
        }
        gp2Pressed[4] = gamepad2.dpad_up;
        
        if (gamepad2.dpad_down && !gp2Pressed[5]) {
            targetRPM = Math.max(0, targetRPM - 250);
            lastEffect = "Target RPM: " + targetRPM;
        }
        gp2Pressed[5] = gamepad2.dpad_down;
        
        // DPAD LEFT/RIGHT - Change slot
        if (gamepad2.dpad_left && !gp2Pressed[6]) {
            currentSlot = (currentSlot + 2) % 3;
            lastEffect = "Slot: " + currentSlot;
        }
        gp2Pressed[6] = gamepad2.dpad_left;
        
        if (gamepad2.dpad_right && !gp2Pressed[7]) {
            currentSlot = (currentSlot + 1) % 3;
            lastEffect = "Slot: " + currentSlot;
        }
        gp2Pressed[7] = gamepad2.dpad_right;
        
        // LB/RB - Cycle slot state
        if (gamepad2.left_bumper && !gp2Pressed[8]) {
            cycleSlotState(currentSlot);
            lastEffect = "Slot " + currentSlot + ": " + slotStates[currentSlot];
        }
        gp2Pressed[8] = gamepad2.left_bumper;
        
        // Triggers - Turret angle
        if (gamepad2.left_trigger > 0.5) {
            turretAngle -= 2;
        }
        if (gamepad2.right_trigger > 0.5) {
            turretAngle += 2;
        }
        turretAngle = Math.max(-180, Math.min(180, turretAngle));
    }
    
    /**
     * Simulate automatic data changes
     */
    private void simulateDataChanges() {
        double elapsed = runtime.seconds();
        
        // Simulate RPM ramping
        if (targetRPM > 0) {
            double diff = targetRPM - simulatedRPM;
            simulatedRPM += diff * 0.05; // Smooth approach
        } else {
            simulatedRPM *= 0.95; // Decay
        }
        
        // Simulate heading oscillation
        heading = 45 * Math.sin(elapsed * 0.5);
        
        // Simulate turret tracking
        if (autoSimulate) {
            turretAngle = 30 * Math.sin(elapsed * 0.3);
            
            // Cycle through turret states
            int stateIndex = ((int)(elapsed / 3)) % 5;
            switch (stateIndex) {
                case 0: turretState = "DISABLED"; break;
                case 1: turretState = "SEARCHING"; break;
                case 2: turretState = "ACQUIRING"; break;
                case 3: turretState = "LOCKED"; break;
                case 4: turretState = "HOLDING"; break;
            }
        }
        
        // Simulate voltage decay
        voltage = 12.8 - (elapsed / 600) * 1.5; // Slow decay over 10 min
        voltage = Math.max(11.0, voltage);
    }
    
    /**
     * Cycle slot state: ⚫ → 🟢 → 🟣 → ⚫
     */
    private void cycleSlotState(int slot) {
        switch (slotStates[slot]) {
            case "⚫": slotStates[slot] = "🟢"; break;
            case "🟢": slotStates[slot] = "🟣"; break;
            case "🟣": slotStates[slot] = "⚫"; break;
            default: slotStates[slot] = "⚫";
        }
    }
    
    /**
     * Build and display the telemetry
     */
    private void updateTelemetryDisplay() {
        helper.begin();
        
        // Header
        helper.addHeader("MINERZ FTC");
        helper.addSpace();
        
        // ====== DRIVE SECTION ======
        helper.addSectionHeader("DRIVE", "#00BFFF");
        helper.addDriveLine(driveMode, heading, isFieldCentric);
        helper.addSpace();
        
        // ====== INDEXER SECTION ======
        helper.addSectionHeader("INDEXER", "#FFD700");
        // Spindexer (manual build to show slots with big text in HTML)
        StringBuilder spindexLine = new StringBuilder("📥 ");
        for (int i = 0; i < 3; i++) {
            if (i == currentSlot) {
                spindexLine.append("[").append(slotStates[i]).append("]");
            } else {
                spindexLine.append(" ").append(slotStates[i]).append(" ");
            }
        }
        spindexLine.append(" S").append(currentSlot).append(" ▸INTAKE");
        helper.addBigLine(spindexLine.toString());
        helper.addSpace();
        
        // ====== SHOOTER SECTION ======
        helper.addSectionHeader("SHOOTER", "#FF6347");
        // Turret
        boolean isLocked = turretState.equals("LOCKED");
        helper.addTurretLine(turretAngle, turretState, isLocked);
        
        // Flywheel
        boolean isReady = simulatedRPM >= targetRPM * 0.95 && targetRPM > 0;
        helper.addFlywheelLine(simulatedRPM, targetRPM, isReady);
        helper.addSpace();
        
        // ====== SYSTEM SECTION ======
        helper.addSectionHeader("SYSTEM", "#808080");
        String matchTime = TelemetryHelper.formatTime(runtime.seconds());
        helper.addSystemLine(voltage);
        
        // Config indicator
        helper.addLine("Format: " + helper.getDisplayMode().name() + 
                      " | AutoClear: " + (autoClear ? "ON" : "OFF") +
                      " | Sim: " + (autoSimulate ? "ON" : "OFF"));
        
        helper.addSeparator();
        
        // Last effect
        helper.addBoldLine("Last: " + lastEffect);
        
        // Compact help (smaller for more space)
        helper.addLine("GP1:Effects | GP2:A=Fmt B=Auto X=Sim");
        
        helper.end();
    }
}
