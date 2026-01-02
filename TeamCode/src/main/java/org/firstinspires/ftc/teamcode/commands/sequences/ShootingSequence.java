package org.firstinspires.ftc.teamcode.commands.sequences;

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
 * Fixed 3-shot shooting sequence.
 * 
 * Architecture:
 * - First InstantCommand calls prepareShotPlan() to capture current state
 * - Each shot step reads from subsystem's stored plan
 * - Explicit 3-shot structure (no dynamic loops)
 * 
 * Designed for full spindexer (3 balls). For partial shots, use manual
 * controls.
 */
public class ShootingSequence extends SequentialCommandGroup {

    public ShootingSequence(SpindexerSubsystem spindexer, FlywheelSubsystem flywheel,
            EjectorSubsystem ejector, ShootingStrategy strategy) {

        addCommands(
                // Step 0: Prepare the shooting plan (captures current slot states)
                new InstantCommand(() -> spindexer.prepareShotPlan(strategy)),

                // ===== SHOT 1 =====
                new InstantCommand(() -> spindexer.moveToOuttakePosition(spindexer.getShotSlot(0))),
                new WaitCommand(spindexer.getShotDelay(0)),
                new WaitUntilCommand(flywheel::isReadyToShoot),
                new EjectCycleCommand(ejector),
                new InstantCommand(spindexer::clearCurrentShotAndAdvance),

                // ===== SHOT 2 =====
                new InstantCommand(() -> spindexer.moveToOuttakePosition(spindexer.getShotSlot(1))),
                new WaitCommand(spindexer.getShotDelay(1)),
                new WaitUntilCommand(flywheel::isReadyToShoot),
                new EjectCycleCommand(ejector),
                new InstantCommand(spindexer::clearCurrentShotAndAdvance),

                // ===== SHOT 3 =====
                new InstantCommand(() -> spindexer.moveToOuttakePosition(spindexer.getShotSlot(2))),
                new WaitCommand(spindexer.getShotDelay(2)),
                new WaitUntilCommand(flywheel::isReadyToShoot),
                new EjectCycleCommand(ejector),
                new InstantCommand(spindexer::clearCurrentShotAndAdvance));

        addRequirements(spindexer, ejector);
    }
}
