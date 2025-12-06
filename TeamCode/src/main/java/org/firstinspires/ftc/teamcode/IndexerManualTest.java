package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.subsystems.IndexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.IndexerSubsystem.SlotState;

/**
 * OpMode para probar el IndexerSubsystem SIN sensor de color
 * Control manual completo con configuración de slots
 * 
 * CONTROLES GAMEPAD 1:
 * ==================
 * POSICIONAMIENTO:
 * - A: Mover a intake slot 0
 * - B: Mover a intake slot 1
 * - X: Mover a intake slot 2
 * - Y: Mover al siguiente slot de intake
 * - DPAD_UP: Mover a outtake del slot actual
 * - DPAD_DOWN: Ciclo de slots (0→1→2→0)
 * 
 * CONFIGURACIÓN MANUAL DE SLOTS:
 * - LEFT_BUMPER: Marcar slot actual como AMARILLO
 * - RIGHT_BUMPER: Marcar slot actual como MORADO  
 * - LEFT_TRIGGER: Marcar slot actual como VACÍO
 * - START: Limpiar todos los slots
 * - BACK: Configurar precarga (3 amarillas)
 * 
 * GAMEPAD 2 (OPCIONAL):
 * - A: Secuencia simple de lanzamiento
 * - B: Secuencia ordenada (amarillas primero)
 */
@TeleOp(name = "Indexer Manual Test", group = "Test")
public class IndexerManualTest extends LinearOpMode {
    
    private IndexerSubsystem indexer;
    private boolean lastLeftBumper = false;
    private boolean lastRightBumper = false;
    private boolean lastStart = false;
    private boolean lastBack = false;
    
    // Variables para secuencias automáticas
    private boolean runningSequence = false;
    private int[] currentSequence = null;
    private int sequenceStep = 0;
    private long lastMoveTime = 0;
    
    @Override
    public void runOpMode() {
        // Inicializar subsystem SIN sensor (false)
        indexer = new IndexerSubsystem(hardwareMap, false);
        
        telemetry.addData("Status", "Inicializado (SIN sensor)");
        telemetry.addData("", "IMPORTANTE:");
        telemetry.addData("", "Marca manualmente los colores de las pelotas");
        telemetry.addData("", "Presiona START cuando listo");
        telemetry.update();
        
        waitForStart();
        
        while (opModeIsActive()) {
            // Actualizar el subsystem
            indexer.update();
            
            // ===== CONTROLES DE POSICIONAMIENTO =====
            handlePositionControls();
            
            // ===== CONFIGURACIÓN MANUAL DE SLOTS =====
            handleSlotConfiguration();
            
            // ===== SECUENCIAS AUTOMÁTICAS (GAMEPAD 2) =====
            handleAutomaticSequences();
            
            // ===== TELEMETRÍA =====
            displayTelemetry();
            
            telemetry.update();
            sleep(20); // Pequeño delay para estabilidad
        }
    }
    
    private void handlePositionControls() {
        // A: Ir a intake del slot 0
        if (gamepad1.a) {
            indexer.moveToIntakePosition(0);
            runningSequence = false;
            sleep(50);
        }
        
        // B: Ir a intake del slot 1
        if (gamepad1.b) {
            indexer.moveToIntakePosition(1);
            runningSequence = false;
            sleep(50);
        }
        
        // X: Ir a intake del slot 2
        if (gamepad1.x) {
            indexer.moveToIntakePosition(2);
            runningSequence = false;
            sleep(50);
        }
        
        // Y: Siguiente slot de intake
        if (gamepad1.y) {
            indexer.moveToNextIntakeSlot();
            runningSequence = false;
            sleep(50);
        }
        
        // DPAD_UP: Mover a outtake del slot actual
        if (gamepad1.dpad_up) {
            indexer.moveToOuttakePosition(indexer.getCurrentSlotIndex());
            runningSequence = false;
            sleep(50);
        }
        
        // DPAD_DOWN: Ciclo de slots
        if (gamepad1.dpad_down) {
            int nextSlot = (indexer.getCurrentSlotIndex() + 1) % 3;
            indexer.moveToIntakePosition(nextSlot);
            runningSequence = false;
            sleep(50);
        }
    }
    
    private void handleSlotConfiguration() {
        int currentSlot = indexer.getCurrentSlotIndex();
        
        // LEFT_BUMPER: Marcar como AMARILLO
        if (gamepad1.left_bumper && !lastLeftBumper) {
            indexer.setSlotState(currentSlot, SlotState.YELLOW);
            telemetry.addData("✓", "Slot " + currentSlot + " = AMARILLO");
        }
        lastLeftBumper = gamepad1.left_bumper;
        
        // RIGHT_BUMPER: Marcar como MORADO
        if (gamepad1.right_bumper && !lastRightBumper) {
            indexer.setSlotState(currentSlot, SlotState.PURPLE);
            telemetry.addData("✓", "Slot " + currentSlot + " = MORADO");
        }
        lastRightBumper = gamepad1.right_bumper;
        
        // LEFT_TRIGGER: Marcar como VACÍO
        if (gamepad1.left_trigger > 0.5) {
            indexer.clearSlot(currentSlot);
            sleep(100);
        }
        
        // START: Limpiar todos
        if (gamepad1.start && !lastStart) {
            indexer.clearAllSlots();
            telemetry.addData("✓", "Todos los slots limpiados");
        }
        lastStart = gamepad1.start;
        
        // BACK: Configurar precarga de prueba (3 amarillas)
        if (gamepad1.back && !lastBack) {
            indexer.configurePreloadAll(SlotState.YELLOW);
            telemetry.addData("✓", "Precarga: 3 AMARILLAS");
        }
        lastBack = gamepad1.back;
    }
    
    private void handleAutomaticSequences() {
        // Si ya hay una secuencia corriendo, ejecutarla
        if (runningSequence && currentSequence != null) {
            executeSequenceStep();
            return;
        }
        
        // GAMEPAD 2 - A: Secuencia simple
        if (gamepad2.a) {
            currentSequence = indexer.getSimpleSequence();
            sequenceStep = 0;
            runningSequence = true;
            lastMoveTime = System.currentTimeMillis();
            telemetry.addData("✓", "Iniciando secuencia simple");
            sleep(200);
        }
        
        // GAMEPAD 2 - B: Secuencia ordenada (amarillas primero)
        if (gamepad2.b) {
            currentSequence = indexer.getColorOrderedSequence(SlotState.YELLOW);
            sequenceStep = 0;
            runningSequence = true;
            lastMoveTime = System.currentTimeMillis();
            telemetry.addData("✓", "Iniciando secuencia: AMARILLAS primero");
            sleep(200);
        }
        
        // GAMEPAD 2 - X: Secuencia ordenada (moradas primero)
        if (gamepad2.x) {
            currentSequence = indexer.getColorOrderedSequence(SlotState.PURPLE);
            sequenceStep = 0;
            runningSequence = true;
            lastMoveTime = System.currentTimeMillis();
            telemetry.addData("✓", "Iniciando secuencia: MORADAS primero");
            sleep(200);
        }
        
        // GAMEPAD 2 - Y: Detener secuencia
        if (gamepad2.y) {
            runningSequence = false;
            currentSequence = null;
            telemetry.addData("✓", "Secuencia detenida");
            sleep(200);
        }
    }
    
    private void executeSequenceStep() {
        // Verificar si la secuencia terminó
        if (sequenceStep >= 3 || currentSequence[sequenceStep] == -1) {
            runningSequence = false;
            telemetry.addData("✓", "Secuencia completada");
            return;
        }
        
        // Esperar que el servo llegue a posición (500ms por movimiento)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveTime < 500) {
            return; // Aún moviéndose
        }
        
        // Mover al siguiente slot en outtake
        int slotIndex = currentSequence[sequenceStep];
        indexer.moveToOuttakePosition(slotIndex);
        
        // Aquí normalmente esperarías a que el launcher dispare
        // Por ahora, simular un delay de lanzamiento
        sleep(1000); // Simular tiempo de lanzamiento
        
        // Limpiar el slot después de lanzar
        indexer.clearSlot(slotIndex);
        
        // Siguiente paso
        sequenceStep++;
        lastMoveTime = System.currentTimeMillis();
    }
    
    private void displayTelemetry() {
        telemetry.addData("=== ESTADO INDEXER ===", "");
        telemetry.addData("Estado", indexer.getCurrentState());
        telemetry.addData("Slot Actual", indexer.getCurrentSlotIndex());
        telemetry.addData("Posición", indexer.isInIntakePosition() ? "INTAKE" : 
                         indexer.isInOuttakePosition() ? "OUTTAKE" : "Moviendo...");
        
        telemetry.addData("", "");
        telemetry.addData("=== CONTENIDO SLOTS ===", "");
        for (int i = 0; i < 3; i++) {
            String indicator = (i == indexer.getCurrentSlotIndex()) ? " ◄" : "";
            telemetry.addData("Slot " + i, "%s%s", indexer.getSlotState(i), indicator);
        }
        
        telemetry.addData("", "");
        telemetry.addData("Pelotas Totales", "%d/3", indexer.getFilledSlotCount());
        telemetry.addData("Todos Llenos", indexer.areAllSlotsFull() ? "SÍ" : "NO");
        
        if (runningSequence && currentSequence != null) {
            telemetry.addData("", "");
            telemetry.addData("=== SECUENCIA ACTIVA ===", "");
            telemetry.addData("Paso", "%d/3", sequenceStep + 1);
            telemetry.addData("Siguiente Slot", 
                currentSequence[sequenceStep] >= 0 ? currentSequence[sequenceStep] : "FIN");
        }
        
        telemetry.addData("", "");
        telemetry.addData("=== CONTROLES GP1 ===", "");
        telemetry.addData("A/B/X", "Intake slots 0/1/2");
        telemetry.addData("Y", "Siguiente slot");
        telemetry.addData("DPAD UP", "Outtake actual");
        telemetry.addData("DPAD DOWN", "Ciclo slots");
        telemetry.addData("LB/RB", "Amarillo/Morado");
        telemetry.addData("LT", "Vaciar slot");
        telemetry.addData("START/BACK", "Limpiar/Precarga");
        
        telemetry.addData("", "");
        telemetry.addData("=== CONTROLES GP2 ===", "");
        telemetry.addData("A", "Secuencia simple");
        telemetry.addData("B", "Amarillas primero");
        telemetry.addData("X", "Moradas primero");
        telemetry.addData("Y", "Detener secuencia");
    }
}