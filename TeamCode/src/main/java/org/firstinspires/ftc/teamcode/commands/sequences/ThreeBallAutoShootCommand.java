package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.WaitUntilCommand;
import org.firstinspires.ftc.teamcode.commands.ejector.EjectCycleCommand;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Automates shooting 3 balls with delays.
 * Assumes the shooter is already up to speed and the spindexer is in position (or handled externally).
 * This command focuses purely on the ejector timing.
 */
public class ThreeBallAutoShootCommand extends SequentialCommandGroup {

    public ThreeBallAutoShootCommand(EjectorSubsystem ejector, SpindexerSubsystem spindexer, ShooterSubsystem shooter) {
        int first = spindexer.getCurrentSlotIndex();
        int second, third;

        // Optimization: If starting at slot 2, go backwards (2->1->0) to practice short moves.
        // Otherwise go forwards (0->1->2 or 1->2->0).
        // Standard moves (0-1, 1-2) are short. Wrap moves (0-2) are long.
        if (first == 2) {
            second = 1;
            third = 0;
        } else {
            second = (first + 1) % 3;
            third = (first + 2) % 3;
        }

        long delay1 = isLongTransition(first, second) ? 1000 : 600;
        long delay2 = isLongTransition(second, third) ? 1000 : 600;

        addCommands(
            // Shot 1
            new WaitUntilCommand(shooter::isReady),
            new EjectCycleCommand(ejector),
            new WaitCommand(250), // Recovery time

            // Move to Second Slot
            new InstantCommand(() -> spindexer.moveToOuttakePosition(second)),
            new WaitCommand(delay1),

            // Shot 2
            new WaitUntilCommand(shooter::isReady),
            new EjectCycleCommand(ejector),
            new WaitCommand(250), // Recovery time

            // Move to Third Slot
            new InstantCommand(() -> spindexer.moveToOuttakePosition(third)),
            new WaitCommand(delay2),

            // Shot 3
            new WaitUntilCommand(shooter::isReady),
            new EjectCycleCommand(ejector)
        );
    }

    private boolean isLongTransition(int from, int to) {
        // Transition between 0 and 2 is physically long (wrap around)
        return Math.abs(from - to) == 2;
    }
}
