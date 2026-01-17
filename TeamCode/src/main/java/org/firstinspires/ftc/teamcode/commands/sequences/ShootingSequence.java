package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.ConditionalCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.command.WaitUntilCommand;

import org.firstinspires.ftc.teamcode.commands.ejector.EjectCycleCommand;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.ShootingStrategy;

/**
 * Adaptive shooting sequence that handles 1-3 balls.
 * 
 * Architecture:
 * - First InstantCommand calls prepareShotPlan() to capture current state
 * - Each shot is wrapped in ConditionalCommand to skip if no valid ball
 * - Handles partial loads gracefully without crashes
 * 
 * Works with any number of balls (1, 2, or 3).
 */
public class ShootingSequence extends SequentialCommandGroup {

    public ShootingSequence(SpindexerSubsystem spindexer, FlywheelSubsystem flywheel,
            EjectorSubsystem ejector, ShootingStrategy strategy) {

        addCommands(
                // Step 0: Prepare the shooting plan (captures current slot states)
                new InstantCommand(() -> spindexer.prepareShotPlan(strategy)),

                // ===== SHOT 1 (index 0) =====
                new ConditionalCommand(
                        new SequentialCommandGroup(
                                new InstantCommand(() -> spindexer.moveToOuttakePosition(spindexer.getShotSlot(0))),
                                new WaitCommand(spindexer.getShotDelay(0)),
                                new WaitUntilCommand(flywheel::isReadyToShoot),
                                new EjectCycleCommand(ejector),
                                new InstantCommand(spindexer::clearCurrentShotAndAdvance)
                        ),
                        new InstantCommand(), // Do nothing if no valid shot
                        () -> spindexer.isValidShotIndex(0)
                ),

                // ===== SHOT 2 (index 1) =====
                new ConditionalCommand(
                        new SequentialCommandGroup(
                                new InstantCommand(() -> spindexer.moveToOuttakePosition(spindexer.getShotSlot(1))),
                                new WaitCommand(spindexer.getShotDelay(1)),
                                new WaitUntilCommand(flywheel::isReadyToShoot),
                                new EjectCycleCommand(ejector),
                                new InstantCommand(spindexer::clearCurrentShotAndAdvance)
                        ),
                        new InstantCommand(), // Do nothing if no valid shot
                        () -> spindexer.isValidShotIndex(1)
                ),

                // ===== SHOT 3 (index 2) =====
                new ConditionalCommand(
                        new SequentialCommandGroup(
                                new InstantCommand(() -> spindexer.moveToOuttakePosition(spindexer.getShotSlot(2))),
                                new WaitCommand(spindexer.getShotDelay(2)),
                                new WaitUntilCommand(flywheel::isReadyToShoot),
                                new EjectCycleCommand(ejector),
                                new InstantCommand(spindexer::clearCurrentShotAndAdvance)
                        ),
                        new InstantCommand(), // Do nothing if no valid shot
                        () -> spindexer.isValidShotIndex(2)
                )
        );

        addRequirements(spindexer, ejector);
    }
}
