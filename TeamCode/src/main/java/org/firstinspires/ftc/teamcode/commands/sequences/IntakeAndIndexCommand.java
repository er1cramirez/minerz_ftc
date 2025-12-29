package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.ConditionalCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.commands.spindexer.CheckSlotCommand;
import org.firstinspires.ftc.teamcode.commands.spindexer.DetectBallCommand;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * Command that executes a complete intake sequence with labeling.
 * 
 * SEQUENCE:
 * 1. CheckSlotCommand      → Verifies if there is a ball, if there is the label
 * 2. ConditionalCommand    → According to slot state:
 *    ├─ EMPTY → Intake sequence:
 *    │    a. intake.intake()
 *    │    b. DetectBallCommand (detects and labels)
 *    │    c. intake.stop()
 *    └─ NO EMPTY → nothing (already labeled by CheckSlot)
 * 3. Move to the next empty slot
 * 4. Wait 500ms for the servo to reach
 */
public class IntakeAndIndexCommand extends SequentialCommandGroup {

    private static final long SPINDEXER_MOVE_TIME_MS = 500;

    public IntakeAndIndexCommand(IntakeSubsystem intake, SpindexerSubsystem spindexer) {
        addCommands(
            // 1. CheckSlotCommand      → Verifies if there is a ball, if there is the label
            new CheckSlotCommand(spindexer),
            // Small wait for CheckSlot to finish labeling if there was a ball
            new WaitCommand(50),
            // 2. ConditionalCommand    → According to slot state:
            new ConditionalCommand(
                // If empty → intake sequence
                new SequentialCommandGroup(
                    new InstantCommand(intake::intake, intake),
                    new DetectBallCommand(spindexer),//Maybe add timeout or handle timeout whe calling IntakeAndIndexCommand
                    new InstantCommand(intake::stop, intake)
                ),
                // If not empty → do nothing
                new InstantCommand(() -> {}),
                // Condition: is empty?
                () -> spindexer.getCurrentSlotState() == SlotState.EMPTY
            ),

            // 3. Move to the next empty slot
            new InstantCommand(() -> {
                int nextEmpty = spindexer.getNextEmptySlot();
                if (nextEmpty != -1) {
                    spindexer.moveToIntakePosition(nextEmpty);
                }
            }),

            // 4. Wait for the servo to reach
            new WaitCommand(SPINDEXER_MOVE_TIME_MS)
        );

        addRequirements(intake, spindexer);
    }
}