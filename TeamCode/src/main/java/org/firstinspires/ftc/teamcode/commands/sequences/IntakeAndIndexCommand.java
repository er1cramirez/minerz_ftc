package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.Command;
import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.command.CommandGroupBase;
import com.seattlesolvers.solverslib.command.ConditionalCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.ParallelCommandGroup;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.commands.spindexer.CheckSlotCommand;
import org.firstinspires.ftc.teamcode.commands.spindexer.DetectBallCommand;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;
import org.firstinspires.ftc.teamcode.util.UserGamepadFeedback;

/**
 * Command that executes a complete intake sequence with labeling.
 * 
 * SEQUENCE:
 * 1. CheckSlotCommand → Verifies if there is a ball, if there is the label
 * 2. ConditionalCommand → According to slot state:
 * ├─ EMPTY → Intake sequence:
 * │ a. intake.intake()
 * │ b. DetectBallCommand (detects and labels)
 * │ c. intake.stop()
 * └─ NO EMPTY → nothing (already labeled by CheckSlot)
 * 3. Move to the next empty slot
 * 4. Wait 500ms for the servo to reach
 */
public class IntakeAndIndexCommand extends SequentialCommandGroup {

        private static final long SPINDEXER_MOVE_TIME_MS = 750;

        public IntakeAndIndexCommand(IntakeSubsystem intake, SpindexerSubsystem spindexer) {
                addCommands(
                                // 1. CheckSlotCommand → Verifies if there is a ball, if there is the label
                                new CheckSlotCommand(spindexer),
                                // 2. ConditionalCommand: Handle 3 states (EMPTY, UNKNOWN, KNOWN)
                                new ConditionalCommand(
                                                // Case A: Slot is EMPTY → Run Intake Sequence
                                                new SequentialCommandGroup(
                                                                new InstantCommand(intake::intake, intake),
                                                                new DetectBallCommand(spindexer),
                                                                new InstantCommand(intake::stop, intake)),
                                                // Case B: Slot is NOT EMPTY (Could be UNKNOWN or ALREADY KNOWN)
                                                new ConditionalCommand(
                                                                // Case B.1: State is UNKNOWN (Ball found by CheckSlot)
                                                                // → Run Detection only
                                                                new DetectBallCommand(spindexer),
                                                                // Case B.2: State is KNOWN (Green/Purple) → Do Nothing
                                                                new InstantCommand(() -> {
                                                                }),
                                                                // Condition for B
                                                                () -> spindexer.getCurrentSlotState() == SlotState.UNKNOWN),
                                                // Condition for A: Is it empty?
                                                () -> spindexer.getCurrentSlotState() == SlotState.EMPTY),

                                // 3. Move to the next empty slot
                                new InstantCommand(() -> {
                                        int nextEmpty = spindexer.getNextEmptySlot();
                                        if (nextEmpty != -1) {
                                                spindexer.moveToIntakePosition(nextEmpty);
                                        }
                                }),

                                // 4. Wait for the servo to reach
                                new WaitCommand(SPINDEXER_MOVE_TIME_MS));

                addRequirements(intake, spindexer);
        }
}