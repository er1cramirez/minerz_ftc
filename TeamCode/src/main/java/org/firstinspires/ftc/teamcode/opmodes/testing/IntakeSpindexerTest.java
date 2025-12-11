package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;

/**
 * OpMode de testing para Intake + Spindexer integrados.
 * Simula el flujo completo de recolección e indexado.
 * 
 * CONTROLES - INTAKE:
 * - Right Trigger: Intake (recoger piezas)
 * - Left Trigger: Outtake (expulsar piezas)
 * 
 * CONTROLES - SPINDEXER:
 * - DPAD UP: Mover a posición de INTAKE (slot actual)
 * - DPAD DOWN: Mover a posición de OUTTAKE (slot actual)
 * - DPAD RIGHT: Siguiente slot
 * - DPAD LEFT: Slot vacío más cercano
 * 
 * - A: Slot 0  |  B: Slot 1  |  Y: Slot 2
 * 
 * CONTROLES - ETIQUETADO:
 * - X: Marcar slot actual como YELLOW
 * - RIGHT BUMPER: Marcar slot actual como PURPLE
 * - LEFT BUMPER: Limpiar slot actual (EMPTY)
 * 
 * CONTROLES - EJECTOR:
 * - Back/Select: Ejecutar eyección (EJECT → STOW automático)
 * 
 * WORKFLOW SUGERIDO:
 * 1. Mover spindexer a posición de intake (DPAD UP o DPAD LEFT para slot vacío)
 * 2. Activar intake con RT
 * 3. Cuando entre la pelota, etiquetar con X (yellow) o RB (purple)
 * 4. Mover a siguiente slot con DPAD RIGHT
 * 5. Repetir
 * 6. Para lanzar: DPAD DOWN (outtake position) + LT (expulsar)
 * 
 * TELEMETRÍA:
 * - Estado de ambos subsistemas
 * - Corriente del intake
 * - Slots del spindexer
 * - Alertas de seguridad
 */
@TeleOp(name = "Intake + Spindexer Test", group = "Testing")
public class
IntakeSpindexerTest extends CommandOpMode {
    
    private IntakeSubsystem intake;
    private SpindexerSubsystem spindexer;
    private EjectorSubsystem ejector;
    
    // Control de botones (debouncing)
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
    private boolean lastBack = false;
    
    // Alertas de seguridad
    private boolean showRotationWarning = false;
    
    @Override
    public void initialize() {
        // Crear subsistemas
        intake = new IntakeSubsystem(hardwareMap);
        spindexer = new SpindexerSubsystem(hardwareMap, false);  // Sin sensor
        ejector = new EjectorSubsystem();
        
        // Registrar subsistemas
        register(intake);
        register(spindexer);
        register(ejector);
        
        telemetry.addLine("Intake + Spindexer Test");
        telemetry.addLine();
        telemetry.addLine("INTAKE:");
        telemetry.addLine("  RT: Intake  |  LT: Outtake");
        telemetry.addLine();
        telemetry.addLine("SPINDEXER MOVEMENT:");
        telemetry.addLine("  DPAD UP/DOWN: Intake/Outtake pos");
        telemetry.addLine("  DPAD RIGHT: Next slot");
        telemetry.addLine("  DPAD LEFT: Closest empty");
        telemetry.addLine("  A/B/Y: Select slot 0/1/2");
        telemetry.addLine();
        telemetry.addLine("LABELING:");
        telemetry.addLine("  X: Yellow  |  RB: Purple  |  LB: Empty");
        telemetry.addLine();
        telemetry.addLine("EJECTOR:");
        telemetry.addLine("  Back: Eject (auto return)");
        telemetry.update();
    }
    
    @Override
    public void run() {
        super.run();  // Ejecuta el scheduler
        
        // Controles manuales
        handleIntakeControls();
        handleSpindexerControls();
        handleEjectorControls();
        
        // Verificar condiciones de seguridad
        checkSafety();
        
        // Actualizar telemetría
        updateTelemetry();
    }
    
    private void handleIntakeControls() {
        float rightTrigger = gamepad1.right_trigger;
        float leftTrigger = gamepad1.left_trigger;
        
        // Prioridad: Right trigger > Left trigger > Stop
        if (rightTrigger > 0.1) {
            intake.intake();
        } else if (leftTrigger > 0.1) {
            intake.outtake();
        } else {
            intake.stop();
        }
    }
    
    private void handleSpindexerControls() {
        // === MOVIMIENTO ===
        
        // DPAD UP: Mover a posición de intake
        if (gamepad1.dpad_up && !lastDpadUp) {
            spindexer.moveToIntakePosition(spindexer.getCurrentSlotIndex());
        }
        lastDpadUp = gamepad1.dpad_up;
        
        // DPAD DOWN: Mover a posición de outtake
        if (gamepad1.dpad_down && !lastDpadDown) {
            spindexer.moveToOuttakePosition(spindexer.getCurrentSlotIndex());
        }
        lastDpadDown = gamepad1.dpad_down;
        
        // DPAD RIGHT: Siguiente slot
        if (gamepad1.dpad_right && !lastDpadRight) {
            spindexer.moveToNextIntakeSlot();
        }
        lastDpadRight = gamepad1.dpad_right;
        
        // DPAD LEFT: Slot vacío más cercano
        if (gamepad1.dpad_left && !lastDpadLeft) {
            spindexer.moveToClosestEmptySlot();
        }
        lastDpadLeft = gamepad1.dpad_left;
        
        // === SELECCIÓN DE SLOT ===
        
        // A: Slot 0
        if (gamepad1.a && !lastA) {
            spindexer.moveToIntakePosition(0);
        }
        lastA = gamepad1.a;
        
        // B: Slot 1
        if (gamepad1.b && !lastB) {
            spindexer.moveToIntakePosition(1);
        }
        lastB = gamepad1.b;
        
        // Y: Slot 2
        if (gamepad1.y && !lastY) {
            spindexer.moveToIntakePosition(2);
        }
        lastY = gamepad1.y;
        
        // === ETIQUETADO ===
        
        // X: Marcar como YELLOW
        if (gamepad1.x && !lastX) {
            spindexer.setSlotState(spindexer.getCurrentSlotIndex(), SlotState.YELLOW);
        }
        lastX = gamepad1.x;
        
        // RIGHT BUMPER: Marcar como PURPLE
        if (gamepad1.right_bumper && !lastRightBumper) {
            spindexer.setSlotState(spindexer.getCurrentSlotIndex(), SlotState.PURPLE);
        }
        lastRightBumper = gamepad1.right_bumper;
        
        // LEFT BUMPER: Marcar como EMPTY
        if (gamepad1.left_bumper && !lastLeftBumper) {
            spindexer.clearCurrentSlot();
        }
        lastLeftBumper = gamepad1.left_bumper;
    }
    
    private void handleEjectorControls() {
        // BACK/SELECT: Ejecutar comando completo (EJECT → STOW)
        if (gamepad1.back && !lastBack) {
            ejector.eject();
            // Inmediatamente pasar a stow
            ejector.stow();
        }
        lastBack = gamepad1.back;
    }
    
    private void checkSafety() {
        // ADVERTENCIA: No rotar spindexer mientras intake está activo
        // Esto podría dañar mecanismos o atascar pelotas
        showRotationWarning = intake.isActive() && 
                              (spindexer.getState() == SpindexerSubsystem.SpindexerState.MOVING_TO_INTAKE ||
                               spindexer.getState() == SpindexerSubsystem.SpindexerState.MOVING_TO_OUTTAKE);
    }
    
    private void updateTelemetry() {
        telemetry.clear();
        
        // Encabezado
        telemetry.addLine("=== INTAKE + SPINDEXER ===");
        telemetry.addLine();
        
        // ADVERTENCIA DE SEGURIDAD
        if (showRotationWarning) {
            telemetry.addLine("⚠️ WARNING: ROTATING WHILE INTAKING!");
            telemetry.addLine("⚠️ STOP INTAKE BEFORE ROTATING!");
            telemetry.addLine();
        }
        
        // === INTAKE ===
        telemetry.addLine("--- INTAKE ---");
        telemetry.addData("State", intake.getState().name());
        telemetry.addData("Motor Current", "%.2f A", intake.getCurrentValue());
        telemetry.addData("Triggers", "RT:%.2f LT:%.2f", gamepad1.right_trigger, gamepad1.left_trigger);
        telemetry.addLine();
        
        // === SPINDEXER ===
        telemetry.addLine("--- SPINDEXER ---");
        telemetry.addData("State", spindexer.getStateName());
        telemetry.addData("Current Slot", "%d (%s)", 
                         spindexer.getCurrentSlotIndex(),
                         spindexer.getCurrentSlotState().name());
        telemetry.addLine();
        
        // Visualización de slots
        telemetry.addLine("SLOTS:");
        for (int i = 0; i < 3; i++) {
            String current = (i == spindexer.getCurrentSlotIndex()) ? "→ " : "  ";
            String emoji = spindexer.getSlotEmoji(i);
            String state = spindexer.getSlotState(i).name();
            telemetry.addLine(String.format("%sSlot %d: %s %s", current, i, emoji, state));
        }
        telemetry.addLine();
        
        // Estadísticas
        telemetry.addData("Filled", "%d/3", spindexer.getFilledSlotCount());
        telemetry.addData("Position", "%s | %s",
                         spindexer.isInIntakePosition() ? "✓ Intake" : "✗ Intake",
                         spindexer.isInOuttakePosition() ? "✓ Outtake" : "✗ Outtake");
        telemetry.addLine();
        
        // === EJECTOR ===
        telemetry.addLine("--- EJECTOR ---");
        telemetry.addData("State", ejector.getState().name());
        telemetry.addData("Is Stowed", ejector.isStowed() ? "✓ Stowed" : "✗ Not Stowed");
        telemetry.addData("Control", "Back button to eject");
        
        // === WORKFLOW SUGERIDO ===
        telemetry.addLine();
        telemetry.addLine("--- WORKFLOW ---");
        if (spindexer.areAllSlotsFull()) {
            telemetry.addLine("✓ ALL SLOTS FULL");
            telemetry.addLine("→ Use DPAD DOWN + LT to launch");
        } else if (spindexer.isInIntakePosition()) {
            telemetry.addLine("→ Use RT to intake ball");
            telemetry.addLine("→ Label with X or RB");
            telemetry.addLine("→ Use DPAD RIGHT for next slot");
        } else {
            telemetry.addLine("→ Use DPAD UP or DPAD LEFT");
            telemetry.addLine("→ Position for intake first");
        }
        
        telemetry.update();
    }
}
