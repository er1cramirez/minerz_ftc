package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.drive.TeleOpDriveCommand;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

import com.seattlesolvers.solverslib.command.InstantCommand;
import org.firstinspires.ftc.teamcode.commands.ejector.EjectCycleCommand;
import org.firstinspires.ftc.teamcode.commands.sequences.SequenceAutoShootCommand;
import org.firstinspires.ftc.teamcode.commands.sequences.ThreeBallAutoShootCommand;

/**
 * OpMode for Testing Specific Shot Sequences
 * 
 * Maps buttons to specific shooting orders assuming Green is in Slot 0.
 * A: Green First (0, 1, 2)
 * B: Green Middle (1, 0, 2)
 * Y: Green Last (1, 2, 0)
 */
@Disabled
@TeleOp(name = "Teleop con secuencias", group = "Testing")
public class IntegratedSequenceTest extends CommandOpMode {
    
    // ==================== SUBSYSTEMS ====================
    private DriveSubsystem drive;
    private IntakeSubsystem intake;
    private SpindexerSubsystem spindexer;
    private EjectorSubsystem ejector;
    private ShooterSubsystem shooter;
    
    // ==================== COMMANDS ====================
    private TeleOpDriveCommand driveCommand;
    
    // ==================== GAMEPADS ====================
    private GamepadEx driverGamepad;
    private GamepadEx operatorGamepad;
    
    // ==================== FOLLOWER ====================
    private Follower follower;
    
    // ==================== ESTADO ====================
    private boolean useSensor = true;  // Por defecto intenta usar sensor
    private boolean sensorAvailable = false;
    
    // Control de botones (debouncing) - Operator
    private boolean lastDpadUp = false;
    private boolean lastDpadDown = false;
    private boolean lastDpadRight = false;
    private boolean lastDpadLeft = false;
    private boolean lastA = false;
    private boolean lastB = false;
    private boolean lastY = false;
    private boolean lastX = false;
    private boolean lastRightBumper = false;
    private boolean lastLeftBumper = false;
    private boolean lastStart = false;
    private boolean lastBack = false;
    
    // Alertas
    private boolean showRotationWarning = false;
    private String lastDetectionResult = "";
    private String currentColorReading = "";  // Lectura actual del sensor en tiempo real
    
    // ==================== INITIALIZATION ====================
    
    @Override
    public void initialize() {
        // Crear Follower para drive
        follower = DriveConstants.createFollower(hardwareMap);
        
        // Inicializar subsistemas
        drive = new DriveSubsystem(follower);
        intake = new IntakeSubsystem(hardwareMap);
        ejector = new EjectorSubsystem(hardwareMap);
        shooter = new ShooterSubsystem(hardwareMap);
        
        // Intentar inicializar spindexer con sensor
        try {
            spindexer = new SpindexerSubsystem(hardwareMap, true);
            sensorAvailable = true;
            telemetry.addLine("✅ Sensor de color detectado");
        } catch (Exception e) {
            spindexer = new SpindexerSubsystem(hardwareMap, false);
            sensorAvailable = false;
            useSensor = false;
            telemetry.addLine("⚠️ Sensor no disponible - Modo MANUAL");
        }
        
        // Registrar subsistemas
        register(drive);
        register(intake);
        register(spindexer);
        register(ejector);
        register(shooter);
        
        // Inicializar gamepads
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);
        
        // Crear comando de drive
        driveCommand = new TeleOpDriveCommand(
                drive,
                () -> -driverGamepad.getLeftY(),
                () -> -driverGamepad.getLeftX(),
                () -> -driverGamepad.getRightX()
        );
        
        // Establecer como default command
        drive.setDefaultCommand(driveCommand);
        
        // Configurar button bindings para driver
        configureDriverBindings();
        
        // Mensaje de inicio
        telemetry.addLine();
        telemetry.addLine("════════════════════════════");
        telemetry.addLine("  🤖 INTEGRATED SEQUENCE TEST");
        telemetry.addLine("════════════════════════════");
        telemetry.addLine();
        telemetry.addLine("GAMEPAD 2: Sequences");
        telemetry.addLine("A: Green First (0-1-2)");
        telemetry.addLine("B: Green Middle (1-0-2)");
        telemetry.addLine("Y: Green Last (1-2-0)");
        telemetry.addLine();
        telemetry.addLine("Press START when ready!");
        telemetry.update();
    }
    
    // ==================== BUTTON BINDINGS ====================
    
    private void configureDriverBindings() {
        // Left Bumper: Slow Mode
        driverGamepad.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
                .whenPressed(new InstantCommand(() -> {
                    driveCommand.toggleSlowMode();
                }));
        
        // Right Bumper: Turbo Mode
        driverGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
                .whenPressed(new InstantCommand(() -> {
                    driveCommand.toggleTurboMode();
                }));
        
        // Back: Toggle Robot/Field Centric
        driverGamepad.getGamepadButton(GamepadKeys.Button.BACK)
                .whenPressed(new InstantCommand(() -> {
                    driveCommand.toggleCentricMode();
                }));
    }
    
    // ==================== MAIN LOOP ====================
    
    @Override
    public void run() {
        super.run();  // Ejecuta el scheduler
        
        // Controles del operator (mecanismos)
        handleIntakeControls();
        handleSpindexerControls();
        handleSequenceControls(); // Replaces handleEjectorControls
        handleShooterControls();
        
        // Actualizar lectura del sensor (si está disponible)
        updateSensorReading();
        
        // Verificar condiciones de seguridad
        checkSafety();
        
        // Actualizar telemetría
        updateTelemetry();
    }
    
    // ==================== INTAKE CONTROLS ====================
    
    private void handleIntakeControls() {
        float rightTrigger = gamepad2.right_trigger;
        float leftTrigger = gamepad2.left_trigger;
        
        if (rightTrigger > 0.1) {
            // SAFETY: Only intake if at intake position
            if (spindexer.isAtIntake()) {
                intake.intake();
            } else {
                intake.stop();
                if (!lastDpadRight) { // Avoid spamming telemetry
                     // Optional: Add telemetry alert here if needed
                }
            }
        } else if (leftTrigger > 0.1) {
            intake.outtake();
        } else {
            intake.stop();
        }
    }
    
    // ==================== SPINDEXER CONTROLS ====================
    
    private void handleSpindexerControls() {
        // === MOVIMIENTO ===
        
        // DPAD UP: Posición INTAKE
        if (gamepad2.dpad_up && !lastDpadUp) {
            spindexer.moveToIntakePosition(spindexer.getCurrentSlotIndex());
        }
        lastDpadUp = gamepad2.dpad_up;
        
        // DPAD DOWN: Posición OUTTAKE
        if (gamepad2.dpad_down && !lastDpadDown) {
            spindexer.moveToOuttakePosition(spindexer.getCurrentSlotIndex());
        }
        lastDpadDown = gamepad2.dpad_down;
        
        // === REMOVED MANUAL SLOT SELECTION (A/B) to free up buttons for Sequences ===
        // Use DPAD Right/Left for manual slot control if needed, or rely on Auto
        
        // DPAD RIGHT: Siguiente slot
        if (gamepad2.dpad_right && !lastDpadRight) {
             int nextSlot = (spindexer.getCurrentSlotIndex() + 1) % 3;
             spindexer.moveToIntakePosition(nextSlot);
        }
        lastDpadRight = gamepad2.dpad_right;

        // DPAD LEFT: Anterior slot / Empty search
        if (gamepad2.dpad_left && !lastDpadLeft) {
             // Simple cycle backwards for manual adjusting
             int prevSlot = (spindexer.getCurrentSlotIndex() + 2) % 3;
             spindexer.moveToIntakePosition(prevSlot);
        }
        lastDpadLeft = gamepad2.dpad_left;
        
        // === DETECCIÓN Y ETIQUETADO ===
        
        // X: Auto-detectar (si sensor disponible) o marcar YELLOW
        if (gamepad2.x && !lastX) {
            if (useSensor && sensorAvailable) {
                // Modo AUTO: detectar con sensor
                try {
                    SpindexerSubsystem.BallColor detected = spindexer.autoDetectAndLabel();
        // ... (rest of logic)
                    lastDetectionResult = "Detected: " + detected.name();
                    
                    // Feedback al driver
                    if (detected == SpindexerSubsystem.BallColor.YELLOW) {
                        gamepad2.rumble(100);
                    } else if (detected == SpindexerSubsystem.BallColor.PURPLE) {
                        gamepad2.rumble(200);
                    } else if (detected == SpindexerSubsystem.BallColor.NONE) {
                        gamepad2.rumble(500);
                    }
                } catch (Exception e) {
                    lastDetectionResult = "Error: " + e.getMessage();
                    // Si falla, marcar como YELLOW manual
                    spindexer.setSlotState(spindexer.getCurrentSlotIndex(), SlotState.YELLOW);
                }
            } else {
                // Modo MANUAL: marcar como YELLOW
                spindexer.setSlotState(spindexer.getCurrentSlotIndex(), SlotState.YELLOW);
                lastDetectionResult = "Manual: YELLOW";
            }
        }
        lastX = gamepad2.x;
        
        // START: Toggle modo AUTO/MANUAL
        if (gamepad2.start && !lastStart) {
            if (sensorAvailable) {
                useSensor = !useSensor;
                gamepad2.rumble(300);
            }
        }
        lastStart = gamepad2.start;
    }
    
    // ==================== SEQUENCE CONTROLS ====================
    
    private void handleSequenceControls() {
        // SAFETY: Only shoot if at OUTTAKE position or just allow the sequence to handle moves
        // SequenceAutoShootCommand handles movement, so we just need to trigger it.

        // A: Green First (0, 1, 2)
        if (gamepad2.a && !lastA) {
            schedule(new SequenceAutoShootCommand(ejector, spindexer, shooter, 0, 1, 2));
            gamepad2.rumble(200);
        }
        lastA = gamepad2.a;

        // B: Green Middle (1, 0, 2)
        if (gamepad2.b && !lastB) {
            schedule(new SequenceAutoShootCommand(ejector, spindexer, shooter, 1, 0, 2));
            gamepad2.rumble(200);
        }
        lastB = gamepad2.b;

        // Y: Green Last (1, 2, 0)
        // Note: Slot 0 is Green. P-P-G means 1->2->0.
        if (gamepad2.y && !lastY) {
            schedule(new SequenceAutoShootCommand(ejector, spindexer, shooter, 1, 2, 0));
            gamepad2.rumble(200);
        }
        lastY = gamepad2.y;
    }

    // ==================== SHOOTER CONTROLS ====================

    private void handleShooterControls() {
        // DPAD LEFT was conflicting with Spindexer Move in duplicate.
        // Let's use Shoulders or other buttons? Or just keep Dpad directions non-overlapping in logic.
        // In handleSpindexerControls I used DPAD LEFT for prev slot.
        // In handleShooterControls it was IDLE.
        // Conflict! Let's remove IDLE from Dpad Left and rely on Stop (Right).
        
        if (gamepad2.dpad_up) {
            shooter.spinUpFar(); // High Goal
        } else if (gamepad2.dpad_down) { // Low/Mid or Close
            shooter.spinUpClose(); 
        } else if (gamepad2.dpad_right) { // Stop
            shooter.stop();
        }
        // Removed Dpad Left Idle to allow Spindexer control
    }
    
    // ==================== SENSOR READING ====================
    
    private void updateSensorReading() {
        if (!sensorAvailable) {
            currentColorReading = "";
            return;
        }
        
        // Solo leer si está en posición de intake y el slot actual está vacío o es UNKNOWN
        if (!spindexer.isAtIntake()) {
            currentColorReading = "";
            return;
        }
        
        SlotState currentState = spindexer.getCurrentSlotState();
        if (currentState != SlotState.EMPTY && currentState != SlotState.UNKNOWN) {
            currentColorReading = "";
            return;
        }
        
        try {
            // Verificar si hay pelota detectada
            if (!spindexer.isBallDetected()) {
                currentColorReading = "No ball detected";
                return;
            }
            
            // Leer una muestra rápida del color
            int[] argb = spindexer.getColorSensor().getARGB();
            int red = argb[1];
            int green = argb[2];
            int blue = argb[3];
            int total = red + green + blue;
            
            if (total == 0) {
                currentColorReading = "No color data";
                return;
            }
            
            double redPercent = (red * 100.0) / total;
            double greenPercent = (green * 100.0) / total;
            double bluePercent = (blue * 100.0) / total;
            
            // Determinar color basado en umbrales
            boolean isYellow = redPercent >= 35 && greenPercent >= 35 && bluePercent <= 25;
            boolean isPurple = redPercent >= 30 && bluePercent >= 30 && greenPercent <= 25;
            
            if (isYellow && !isPurple) {
                currentColorReading = String.format("🟡 YELLOW (R:%.0f%% G:%.0f%% B:%.0f%%)", 
                                                   redPercent, greenPercent, bluePercent);
            } else if (isPurple && !isYellow) {
                currentColorReading = String.format("🟣 PURPLE (R:%.0f%% G:%.0f%% B:%.0f%%)", 
                                                   redPercent, greenPercent, bluePercent);
            } else {
                currentColorReading = String.format("❓ UNKNOWN (R:%.0f%% G:%.0f%% B:%.0f%%)", 
                                                   redPercent, greenPercent, bluePercent);
            }
            
        } catch (Exception e) {
            currentColorReading = "Sensor error";
        }
    }
    
    // ==================== SAFETY CHECKS ====================
    
    private void checkSafety() {
        showRotationWarning = false; 
    }
    
    // ==================== TELEMETRY ====================
    
    private void updateTelemetry() {
        telemetry.clear();
        
        // ===== HEADER =====
        telemetry.addLine("════════════════════════════════════");
        telemetry.addLine("       🤖 INTEGRATED SEQUENCE TEST");
        telemetry.addLine("════════════════════════════════════");
        telemetry.addLine();
        
        // ===== ADVERTENCIAS =====
        if (showRotationWarning) {
            telemetry.addLine("⚠️⚠️⚠️ WARNING ⚠️⚠️⚠️");
            telemetry.addLine("ROTATING WHILE INTAKING!");
            telemetry.addLine("STOP INTAKE FIRST!");
            telemetry.addLine();
        }
        
        // ===== DRIVE STATUS =====
        telemetry.addLine("┌─ DRIVE ─────────────────────┐");
        String speedEmoji = getSpeedModeEmoji();
        telemetry.addData("│ Speed", speedEmoji + " " + driveCommand.getSpeedMode());
        String centricMode = driveCommand.isRobotCentric() ? "🤖 Robot" : "🌍 Field";
        telemetry.addData("│ Control", centricMode);
        telemetry.addData("│ Position", String.format("(%.1f, %.1f, %.0f°)",
                drive.getPose().getX(),
                drive.getPose().getY(),
                Math.toDegrees(drive.getHeading())));
        telemetry.addLine("└──────────────────────────────┘");
        telemetry.addLine();
        
        // ===== INTAKE STATUS =====
        telemetry.addLine("┌─ INTAKE ─────────────────────┐");
        telemetry.addData("│ State", getIntakeEmoji() + " " + intake.getState().name());
        telemetry.addData("│ Current", "%.2f A", intake.getCurrentValue());
        telemetry.addData("│ Controls", "RT: %.2f | LT: %.2f", 
                         gamepad2.right_trigger, gamepad2.left_trigger);
        telemetry.addLine("└──────────────────────────────┘");
        telemetry.addLine();
        
        // ===== SPINDEXER STATUS =====
        telemetry.addLine("┌─ SPINDEXER ──────────────────┐");
        telemetry.addData("│ Mode", useSensor ? "🔍 AUTO" : "✋ MANUAL");
        telemetry.addData("│ State", spindexer.getStateName());
        telemetry.addData("│ Current Slot", "%d - %s %s", 
                         spindexer.getCurrentSlotIndex(),
                         spindexer.getSlotEmoji(spindexer.getCurrentSlotIndex()),
                         spindexer.getCurrentSlotState().name());
        telemetry.addLine("│");
        telemetry.addLine("│ SLOTS:");
        for (int i = 0; i < 3; i++) {
            String current = (i == spindexer.getCurrentSlotIndex()) ? "│ → " : "│   ";
            String emoji = spindexer.getSlotEmoji(i);
            String state = spindexer.getSlotState(i).name();
            telemetry.addLine(String.format("%sSlot %d: %s %-7s", current, i, emoji, state));
        }
        telemetry.addLine("│");
        telemetry.addData("│ Filled", "%d/3 slots", spindexer.getFilledSlotCount());
        telemetry.addLine("│");
        
        // Mostrar lectura actual del sensor (si está disponible)
        if (sensorAvailable && !currentColorReading.isEmpty()) {
            telemetry.addData("│ Sensor Reads", currentColorReading);
            if (!useSensor) {
                telemetry.addLine("│ (Press X or RB to confirm)");
            }
        }
        
        if (!lastDetectionResult.isEmpty()) {
            telemetry.addData("│ Last Action", lastDetectionResult);
        }
        
        telemetry.addLine("└──────────────────────────────┘");
        telemetry.addLine();
        
        // ===== EJECTOR STATUS =====
        telemetry.addLine("┌─ EJECTOR ────────────────────┐");
        telemetry.addData("│ State", getEjectorEmoji() + " " + ejector.getState().name());
        telemetry.addData("│ Is Stowed", ejector.isStowed() ? "✅ Yes" : "❌ No");
        telemetry.addLine("└──────────────────────────────┘");
        telemetry.addLine();

        // ===== SHOOTER STATUS =====
        telemetry.addLine("┌─ SHOOTER ────────────────────┐");
        telemetry.addData("│ State", shooter.getState().name());
        telemetry.addData("│ RPM", "%.0f / %.0f", shooter.getCurrentRpm(), shooter.getTargetRpm());
        if (shooter.isReady()) {
             telemetry.addData("│ Status", "✅ READY TO FIRE");
        } else if (shooter.isSpinningUp()) {
             telemetry.addData("│ Status", "⏳ SPINNING UP...");
        } else {
             telemetry.addData("│ Status", "⏹️ STOPPED/IDLE");
        }
        telemetry.addLine("└──────────────────────────────┘");
        telemetry.addLine();
        
        // ===== WORKFLOW HINTS =====
        telemetry.addLine("┌─ SEQUENCES ──────────────────┐");
        telemetry.addLine("│ A: Green First (0-1-2)");
        telemetry.addLine("│ B: Green Middle (1-0-2)");
        telemetry.addLine("│ Y: Green Last (1-2-0)");
        telemetry.addLine("└──────────────────────────────┘");
        
        telemetry.update();
    }
    
    // ==================== HELPERS ====================
    
    private String getSpeedModeEmoji() {
        switch (driveCommand.getSpeedMode()) {
            case SLOW:
                return "🐢";
            case TURBO:
                return "🚀";
            case NORMAL:
            default:
                return "▶️";
        }
    }
    
    private String getIntakeEmoji() {
        switch (intake.getState()) {
            case INTAKING:
                return "⬇️";
            case OUTTAKING:
                return "⬆️";
            case IDLE:
            default:
                return "⏸️";
        }
    }
    
    private String getEjectorEmoji() {
        switch (ejector.getState()) {
            case EJECTING:
                return "🚀";
            case STOWED:
            default:
                return "📦";
        }
    }
}
