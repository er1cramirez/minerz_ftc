package org.firstinspires.ftc.teamcode.commands.intake;

import com.seattlesolvers.solverslib.command.ConditionalCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * Comando secuencial que ejecuta UNA secuencia completa de intake con detección.
 * 
 * SECUENCIA:
 * 1. CheckSlotCommand      → Verifica si ya hay pelota, si hay la etiqueta
 * 2. ConditionalCommand    → Según estado del slot:
 *    ├─ EMPTY → Secuencia intake:
 *    │    a. intake.intake()
 *    │    b. DetectBallCommand (detecta y etiqueta)
 *    │    c. intake.stop()
 *    └─ NO EMPTY → nada (ya etiquetado por CheckSlot)
 * 3. Mover al siguiente slot vacío
 * 4. Esperar 500ms para que el servo llegue
 */
public class IntakeWithColorDetectionCommand extends SequentialCommandGroup {

    private static final long SPINDEXER_MOVE_TIME_MS = 500;

    public IntakeWithColorDetectionCommand(IntakeSubsystem intake, SpindexerSubsystem spindexer) {
        addCommands(
            // 1. Verificar si ya hay pelota
            new CheckSlotCommand(spindexer),
            
            // Pequeña espera para que CheckSlot termine de etiquetar si había pelota
            new WaitCommand(50),

            // 2. Decidir flujo según estado del slot
            new ConditionalCommand(
                // Si está vacío → secuencia con intake
                new SequentialCommandGroup(
                    new InstantCommand(intake::intake, intake),
                    new DetectBallCommand(spindexer),
                    new InstantCommand(intake::stop, intake)
                ),
                // Si no está vacío → no hacer nada
                new InstantCommand(() -> {}),
                // Condición: ¿está vacío?
                () -> spindexer.getCurrentSlotState() == SlotState.EMPTY
            ),

            // 3. Mover al siguiente slot vacío
            new InstantCommand(() -> {
                int nextEmpty = spindexer.getNextEmptySlot();
                if (nextEmpty != -1) {
                    spindexer.moveToIntakePosition(nextEmpty);
                }
            }),

            // 4. Esperar a que el servo llegue
            new WaitCommand(SPINDEXER_MOVE_TIME_MS)
        );

        addRequirements(intake, spindexer);
    }
}