package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.command.WaitUntilCommand;
import org.firstinspires.ftc.teamcode.commands.ejector.EjectCycleCommand;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Executes a specific sequence of shots based on provided slot indices.
 * Useful for shooting specific balls (colors) in a specific order.
 * 
 * Usage: new SequenceAutoShootCommand(ejector, spindexer, 0, 2, 1);
 */
public class SequenceAutoShootCommand extends SequentialCommandGroup {

    public SequenceAutoShootCommand(EjectorSubsystem ejector, SpindexerSubsystem spindexer, ShooterSubsystem shooter, int... slotSequence) {
        
        int currentPos = spindexer.getCurrentSlotIndex();

        for (int targetSlot : slotSequence) {
            // Calculate delay based on distance to target
            long delay = isLongTransition(currentPos, targetSlot) ? 1000 : 600;

            // 1. Move to target slot
            // Note: capturing 'targetSlot' in lambda is fine as it's effectively final per iteration context in simpler compilers, 
            // but for safety in loops with some Java versions, we use the value directly.
            int finalTargetSlot = targetSlot;
            addCommands(
                new InstantCommand(() -> spindexer.moveToOuttakePosition(finalTargetSlot)),
                new WaitCommand(delay)
            );

            // 2. Shoot (Check readiness first)
            addCommands(
                new WaitUntilCommand(shooter::isReady),
                new EjectCycleCommand(ejector),
                new WaitCommand(250)
            );

            // Update currentPos for next iteration's delay calculation
            currentPos = targetSlot;
        }
    }

    private boolean isLongTransition(int from, int to) {
        // Transition between 0 and 2 is physically long (wrap around via 1) on a non-continuous servo
        return Math.abs(from - to) == 2;
    }
}
