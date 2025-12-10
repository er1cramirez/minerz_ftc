package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;

import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * OpMode de testing para SpindexerSubsystem.
 * 
 * CONTROLES:
 * - DPAD UP: Mover slot actual a posición de INTAKE
 * - DPAD DOWN: Mover slot actual a posición de OUTTAKE
 * - DPAD RIGHT: Siguiente slot (rotación circular)
 * - DPAD LEFT: Ir al slot vacío más cercano
 * 
 * - A: Seleccionar Slot 0
 * - B: Seleccionar Slot 1
 * - Y: Seleccionar Slot 2
 * 
 * - X: Marcar slot actual como YELLOW
 * - RIGHT BUMPER: Marcar slot actual como PURPLE
 * - LEFT BUMPER: Marcar slot actual como EMPTY
 * - BACK: Marcar slot actual como UNKNOWN
 * 
 * TELEMETRÍA:
 * - Estado del spindexer
 * - Slot actual y su contenido
 * - Estado visual de los 3 slots
 */
@TeleOp(name = "Spindexer Manual Test", group = "Testing")
public class IndexerManualTest extends CommandOpMode {
    
    private SpindexerSubsystem spindexer;
    
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
    
    @Override
    public void initialize() {
        // Crear SpindexerSubsystem sin sensor (manual)
        spindexer = new SpindexerSubsystem(hardwareMap, false);
        
        // Registrar subsystem
        register(spindexer);
        
        telemetry.addLine("Spindexer Manual Test");
        telemetry.addLine("MOVEMENT:");
        telemetry.addLine("  DPAD UP: Intake position");
        telemetry.addLine("  DPAD DOWN: Outtake position");
        telemetry.addLine("  DPAD RIGHT: Next slot");
        telemetry.addLine("  DPAD LEFT: Closest empty");
        telemetry.addLine();
        telemetry.addLine("SLOT SELECTION:");
        telemetry.addLine("  A: Slot 0  B: Slot 1  Y: Slot 2");
        telemetry.addLine();
        telemetry.addLine("LABELING:");
        telemetry.addLine("  X: Yellow  RB: Purple");
        telemetry.addLine("  LB: Empty  BACK: Unknown");
        telemetry.update();
    }
    
    @Override
    public void run() {
        super.run();  // Ejecuta el scheduler
        
        // Controles manuales
        handleControls();
        
        // Actualizar telemetría
        updateTelemetry();
    }
    
    private void handleControls() {
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
        
        // BACK: Marcar como UNKNOWN
        if (gamepad1.back && !lastBack) {
            spindexer.setSlotState(spindexer.getCurrentSlotIndex(), SlotState.UNKNOWN);
        }
        lastBack = gamepad1.back;
    }
    
    private void updateTelemetry() {
        telemetry.clear();
        
        // Encabezado
        telemetry.addLine("=== SPINDEXER TEST ===");
        telemetry.addLine();
        
        // Estado
        telemetry.addData("State", spindexer.getStateName());
        telemetry.addData("Current Slot", spindexer.getCurrentSlotIndex());
        telemetry.addData("Current Content", spindexer.getCurrentSlotState().name());
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
        telemetry.addData("Filled Slots", "%d/3", spindexer.getFilledSlotCount());
        telemetry.addData("All Full", spindexer.areAllSlotsFull() ? "YES" : "NO");
        telemetry.addData("Has Empty", spindexer.hasEmptySlot() ? "YES" : "NO");
        telemetry.addLine();
        
        // Posiciones
        telemetry.addData("In Intake Pos", spindexer.isInIntakePosition() ? "✓" : "✗");
        telemetry.addData("In Outtake Pos", spindexer.isInOuttakePosition() ? "✓" : "✗");
        
        telemetry.update();
    }
}
