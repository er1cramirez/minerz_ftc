package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.follower.Follower;
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

import org.firstinspires.ftc.teamcode.commands.ejector.EjectCycleCommand;
//import org.firstinspires.ftc.teamcode.commands.sequences.SequenceAutoShootCommand;
//import org.firstinspires.ftc.teamcode.commands.sequences.ThreeBallAutoShootCommand;

/**
 * OpMode de Testing Integrado - Drive + Intake + Spindexer
 * 
 * Combina el control completo del robot: movimiento, recolección e indexado.
 * El sensor de color está habilitado pero NO es obligatorio para el funcionamiento.
 * 
 * ═══════════════════════════════════════════════════════════════════
 * GAMEPAD 1 - DRIVER (MOVIMIENTO)
 * ═══════════════════════════════════════════════════════════════════
 * - Left Stick: Forward/Strafe
 * - Right Stick X: Rotation
 * - Left Bumper: Slow Mode
 * - Right Bumper: Turbo Mode
 * - Back: Toggle Robot/Field Centric
 * 
 * ═══════════════════════════════════════════════════════════════════
 * GAMEPAD 2 - OPERATOR (MECANISMOS)
 * ═══════════════════════════════════════════════════════════════════
 * 
 * INTAKE:
 * - Right Trigger: Intake (recoger)
 * - Left Trigger: Outtake (expulsar)
 * 
 * SPINDEXER - MOVIMIENTO:
 * - DPAD UP: Posición INTAKE (slot actual)
 * - DPAD DOWN: Posición OUTTAKE (slot actual)
 * - DPAD RIGHT: Siguiente slot
 * - DPAD LEFT: Slot vacío más cercano
 * - A: Slot 0  |  B: Slot 1  |  Y: Slot 2
 * 
 * SPINDEXER - DETECCIÓN Y ETIQUETADO:
 * - X: AUTO=Auto-detectar y etiquetar | MANUAL=Marcar YELLOW
 * - Right Bumper: Marcar slot como PURPLE
 * - Left Bumper: Limpiar slot (EMPTY)
 * - Start: Toggle entre modo AUTO y MANUAL
 * 
 * NOTA: En modo MANUAL, el sensor sigue mostrando el color detectado
 *       en la telemetría, pero requiere confirmación manual para etiquetar.
 * 
 * EJECTOR:
 * - Y: Ejecutar eyección (Ciclo completo: EJECT → STOW)
 * 
 * SHOOTER:
 * - DPAD UP: Velocidad FAR
 * - DPAD DOWN: Velocidad CLOSE
 * - DPAD LEFT: Velocidad IDLE
 * - DPAD RIGHT: STOP
 * ═══════════════════════════════════════════════════════════════════
 * WORKFLOW RECOMENDADO:
 * ═══════════════════════════════════════════════════════════════════
 * 
 * MODO AUTO (con sensor):
 * 1. Mover spindexer a slot vacío (DPAD LEFT)
 * 2. Posicionar en INTAKE (DPAD UP)
 * 3. Activar intake (RT)
 * 4. Cuando entre pelota, presionar X para auto-detectar
 * 5. Repetir con siguiente slot (DPAD RIGHT)
 * 
 * MODO MANUAL (etiquetado manual, lectura de sensor si disponible):
 * 1. Mover spindexer a slot vacío (DPAD LEFT)
 * 2. Posicionar en INTAKE (DPAD UP)
 * 3. Activar intake (RT)
 * 4. Ver color detectado en telemetría (si sensor disponible)
 * 5. Etiquetar manualmente: X=YELLOW, RB=PURPLE
 * 6. Repetir con siguiente slot (DPAD RIGHT)
 * 
 * LANZAR:
 * 1. Mover a slot lleno (A/B/Y o DPAD RIGHT)
 * 2. Posicionar en OUTTAKE (DPAD DOWN)
 * 3. Activar outtake (LT)
 * 4. Limpiar slot después de lanzar (LB)
 */
@TeleOp(name = "🤖 Integrated Drive Test", group = "Testing")
public class IntegratedDriveTest extends CommandOpMode {
    
    // ==================== SUBSYSTEMS ====================
    private DriveSubsystem drive;
    private IntakeSubsystem intake;
    private SpindexerSubsystem spindexer;
    private EjectorSubsystem ejector;
//    private ShooterSubsystem shooter;
    
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
//        shooter = new ShooterSubsystem(hardwareMap);
        
        // Intentar inicializar spindexer con sensor
        try {
            spindexer = new SpindexerSubsystem(hardwareMap);
            sensorAvailable = true;
            telemetry.addLine("✅ Sensor de color detectado");
        } catch (Exception e) {
            spindexer = new SpindexerSubsystem(hardwareMap);
            sensorAvailable = false;
            useSensor = false;
            telemetry.addLine("⚠️ Sensor no disponible - Modo MANUAL");
        }
        
        // Registrar subsistemas
        register(drive);
        register(intake);
        register(spindexer);
        register(ejector);
//        register(shooter);
        
        // Inicializar gamepads
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);
        
        // Crear comando de drive
        driveCommand = new TeleOpDriveCommand(
                drive,
                () -> driverGamepad.getLeftY(),
                () -> driverGamepad.getLeftX(),
                () -> driverGamepad.getRightX()
        );
        
        // Establecer como default command
        drive.setDefaultCommand(driveCommand);
        
        // Configurar button bindings para driver
        configureDriverBindings();
        
        // Mensaje de inicio
        telemetry.addLine();
        telemetry.addLine("════════════════════════════");
        telemetry.addLine("  🤖 INTEGRATED DRIVE TEST");
        telemetry.addLine("════════════════════════════");
        telemetry.addLine();
        telemetry.addLine("GAMEPAD 1: Drive Controls");
        telemetry.addLine("GAMEPAD 2: Mechanisms");
        telemetry.addLine();
        telemetry.addData("Mode", sensorAvailable ? "AUTO (sensor)" : "MANUAL");
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
        handleEjectorControls();
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

        
        
        // === SELECCIÓN DE SLOT (SECUENCIAL) ===
        
        // A: Siguiente posición INTAKE (Ciclo 0 -> 1 -> 2)
        if (gamepad2.a && !lastA) {
            int nextSlot = (spindexer.getCurrentSlotIndex() + 1) % 3;
            spindexer.moveToIntakePosition(nextSlot);
        }
        lastA = gamepad2.a;
        
        // B: Siguiente posición OUTTAKE (Ciclo 0 -> 1 -> 2)
        if (gamepad2.b && !lastB) {
            int nextSlot = (spindexer.getCurrentSlotIndex() + 1) % 3;
            spindexer.moveToOuttakePosition(nextSlot);
        }
        lastB = gamepad2.b;
        
        // === DETECCIÓN Y ETIQUETADO ===
        
//        // X: Auto-detectar (si sensor disponible) o marcar YELLOW
//        if (gamepad2.x && !lastX) {
//            if (useSensor && sensorAvailable) {
//                // Modo AUTO: detectar con sensor
//                try {
//                    SpindexerSubsystem.BallColor detected = spindexer.autoDetectAndLabel();
//        // ... (rest of logic)
//                    lastDetectionResult = "Detected: " + detected.name();
//
//                    // Feedback al driver
//                    if (detected == SpindexerSubsystem.BallColor.YELLOW) {
//                        gamepad2.rumble(100);
//                    } else if (detected == SpindexerSubsystem.BallColor.PURPLE) {
//                        gamepad2.rumble(200);
//                    } else if (detected == SpindexerSubsystem.BallColor.NONE) {
//                        gamepad2.rumble(500);
//                    }
//                } catch (Exception e) {
//                    lastDetectionResult = "Error: " + e.getMessage();
//                    // Si falla, marcar como YELLOW manual
//                    spindexer.setSlotState(spindexer.getCurrentSlotIndex(), SlotState.YELLOW);
//                }
//            } else {
//                // Modo MANUAL: marcar como YELLOW
//                spindexer.setSlotState(spindexer.getCurrentSlotIndex(), SlotState.YELLOW);
//                lastDetectionResult = "Manual: YELLOW";
//            }
//        }
//        lastX = gamepad2.x;
        
        // REMOVED RIGHT BUMPER PURPLE (Moved to RB=AutoShoot) - use another button if needed or stick to Auto
        // Re-assigning Manual Purple to LEFT_STICK_BUTTON for now? Or just rely on Auto.
        
        // REMOVED CONFLICTING DPAD_LEFT LOGIC FOR SPINDEXER
        // It was conflicting with Shooter Idle
        
        // START: Toggle modo AUTO/MANUAL
        if (gamepad2.start && !lastStart) {
            if (sensorAvailable) {
                useSensor = !useSensor;
                gamepad2.rumble(300);
            }
        }
        lastStart = gamepad2.start;
    }
    
    // ==================== EJECTOR CONTROLS ====================
    
    private void handleEjectorControls() {
        // SAFETY: Only eject if at OUTTAKE position
        if (!spindexer.isAtOuttake()) {
            return;
        }

        // Y: Ejecutar comando de eyección SINGLE
        if (gamepad2.y && !lastY) {
            // SAFETY BYPASSED: Allow shooting even if not at full speed
            schedule(new EjectCycleCommand(ejector));
            
            // if (shooter.isReady()) {
            //    schedule(new EjectCycleCommand(ejector));
            // } else {
            //    gamepad2.rumble(200); // Feedback if not ready
            // }
        }
        lastY = gamepad2.y;

//        // RIGHT BUMPER: Auto Shoot 3 (Optimized)
//        if (gamepad2.right_bumper && !lastRightBumper) {
//             schedule(new ThreeBallAutoShootCommand(ejector, spindexer, shooter));
//        }
//        lastRightBumper = gamepad2.right_bumper;

//        // LEFT BUMPER: Custom Sequence Test [0, 2, 1]
//        if (gamepad2.left_bumper && !lastLeftBumper) {
//             // Example sequence: Slot 0 -> Slot 2 -> Slot 1
//             schedule(new SequenceAutoShootCommand(ejector, spindexer, shooter, 0, 2, 1));
//        }
        lastLeftBumper = gamepad2.left_bumper;
    }

    // ==================== SHOOTER CONTROLS ====================

    private void handleShooterControls() {
//        if (gamepad2.dpad_up) {
//            shooter.spinUpFar();
//        } else if (gamepad2.dpad_down) {
//            shooter.spinUpClose();
//        } else if (gamepad2.dpad_left) {
//            shooter.idle();
//        } else if (gamepad2.dpad_right) {
//            shooter.stop();
//        }
    }
    
    // ==================== SENSOR READING ====================
    
    /**
     * Actualiza la lectura del sensor en tiempo real (sin etiquetar).
     * Esto permite al operador ver qué color detecta el sensor antes de confirmar.
     */
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
//            if (!spindexer.isBallDetected()) {
//                currentColorReading = "No ball detected";
//                return;
//            }
            
            // Leer una muestra rápida del color
//            int[] argb = spindexer.getColorSensor().getARGB();
//            int red = argb[1];
//            int green = argb[2];
//            int blue = argb[3];
//            int total = red + green + blue;
            
//            if (total == 0) {
//                currentColorReading = "No color data";
//                return;
//            }
            
//            double redPercent = (red * 100.0) / total;
//            double greenPercent = (green * 100.0) / total;
//            double bluePercent = (blue * 100.0) / total;
            
            // Determinar color basado en umbrales
//            boolean isYellow = redPercent >= 35 && greenPercent >= 35 && bluePercent <= 25;
//            boolean isPurple = redPercent >= 30 && bluePercent >= 30 && greenPercent <= 25;
//
//            if (isYellow && !isPurple) {
//                currentColorReading = String.format("🟡 YELLOW (R:%.0f%% G:%.0f%% B:%.0f%%)",
//                                                   redPercent, greenPercent, bluePercent);
//            } else if (isPurple && !isYellow) {
//                currentColorReading = String.format("🟣 PURPLE (R:%.0f%% G:%.0f%% B:%.0f%%)",
//                                                   redPercent, greenPercent, bluePercent);
//            } else {
//                currentColorReading = String.format("❓ UNKNOWN (R:%.0f%% G:%.0f%% B:%.0f%%)",
//                                                   redPercent, greenPercent, bluePercent);
//            }
            
        } catch (Exception e) {
            currentColorReading = "Sensor error";
        }
    }
    
    // ==================== SAFETY CHECKS ====================
    
    private void checkSafety() {
        // No rotar spindexer mientras intake está activo
        // (Lógica deshabilitada temporalmente porque el subsistema ya no reporta estados de movimiento)
        showRotationWarning = false; 
    }
    
    // ==================== TELEMETRY ====================
    
    private void updateTelemetry() {
        telemetry.clear();
        
        // ===== HEADER =====
        telemetry.addLine("════════════════════════════════════");
        telemetry.addLine("       🤖 INTEGRATED ROBOT TEST");
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
        telemetry.addData("│ Control", "Y button to eject");
        telemetry.addLine("└──────────────────────────────┘");
        telemetry.addLine();

        // ===== SHOOTER STATUS =====
        telemetry.addLine("┌─ SHOOTER ────────────────────┐");
//        telemetry.addData("│ State", shooter.getState().name());
//        telemetry.addData("│ RPM", "%.0f / %.0f", shooter.getCurrentRpm(), shooter.getTargetRpm());
//        if (shooter.isReady()) {
//             telemetry.addData("│ Status", "✅ READY TO FIRE");
//        } else if (shooter.isSpinningUp()) {
//             telemetry.addData("│ Status", "⏳ SPINNING UP...");
//        } else {
//             telemetry.addData("│ Status", "⏹️ STOPPED/IDLE");
//        }
        telemetry.addLine("└──────────────────────────────┘");
        telemetry.addLine();
        
        // ===== WORKFLOW HINTS =====
        telemetry.addLine("┌─ NEXT STEPS ─────────────────┐");
        if (spindexer.isFull()) {
            telemetry.addLine("│ ✅ ALL SLOTS FULL");
            telemetry.addLine("│ → DPAD DOWN + LT to launch");
        } else if (spindexer.isAtIntake()) {
            if (spindexer.getCurrentSlotState() == SlotState.EMPTY) {
                telemetry.addLine("│ → RT to intake ball");
                telemetry.addLine("│ → X to detect/label");
                telemetry.addLine("│ → DPAD RIGHT for next");
            } else {
                telemetry.addLine("│ → Slot already filled");
                telemetry.addLine("│ → DPAD RIGHT for next");
            }
        } else {
            telemetry.addLine("│ → DPAD UP for intake pos");
            telemetry.addLine("│ → DPAD LEFT for empty slot");
        }
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
