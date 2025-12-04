package org.firstinspires.ftc.teamcode.commands;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.ShooterConstants;
import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Command to shoot all balls in the indexer in sequence (burst mode).
 * 
 * Sequence:
 * 1. Ensure shooter is at velocity
 * 2. For each ball: shoot and advance to next slot
 * 3. Return shooter to idle when done
 * 
 * This is useful for autonomous routines where you want to
 * dump all collected balls quickly.
 */
public class ShootAllCommand extends CommandBase {

    private final ShooterSubsystem shooter;
    private final ArtifactIndexer indexer;
    private final EjectorSubsystem ejector;
    private final ElapsedTime timer;
    private final boolean returnToIdle;
    
    private int ballsToShoot;
    private int ballsShot;
    
    private enum Phase {
        SPIN_UP,
        MOVE_TO_OUTTAKE,
        WAIT_INDEXER,
        EJECT,
        WAIT_EJECT,
        ADVANCE_SLOT,
        WAIT_BETWEEN_SHOTS,
        FINISH,
        DONE
    }
    
    private Phase currentPhase;

    /**
     * Creates a shoot-all command.
     * @param shooter The shooter subsystem
     * @param indexer The indexer subsystem
     * @param ejector The ejector subsystem
     */
    public ShootAllCommand(ShooterSubsystem shooter, ArtifactIndexer indexer, 
                           EjectorSubsystem ejector) {
        this(shooter, indexer, ejector, true);
    }

    /**
     * Creates a shoot-all command with idle control.
     * @param shooter The shooter subsystem
     * @param indexer The indexer subsystem
     * @param ejector The ejector subsystem
     * @param returnToIdle Whether to return shooter to idle after shooting
     */
    public ShootAllCommand(ShooterSubsystem shooter, ArtifactIndexer indexer, 
                           EjectorSubsystem ejector, boolean returnToIdle) {
        this.shooter = shooter;
        this.indexer = indexer;
        this.ejector = ejector;
        this.returnToIdle = returnToIdle;
        this.timer = new ElapsedTime();
        
        addRequirements(shooter, indexer, ejector);
    }

    @Override
    public void initialize() {
        ballsToShoot = indexer.getBallCount();
        ballsShot = 0;
        
        if (ballsToShoot == 0) {
            currentPhase = Phase.DONE;
            return;
        }
        
        // Start spin-up if not already at velocity
        if (!shooter.isReady()) {
            shooter.spinUp();
            currentPhase = Phase.SPIN_UP;
        } else {
            currentPhase = Phase.MOVE_TO_OUTTAKE;
        }
        
        timer.reset();
    }

    @Override
    public void execute() {
        switch (currentPhase) {
            case SPIN_UP:
                if (shooter.isReady()) {
                    currentPhase = Phase.MOVE_TO_OUTTAKE;
                    timer.reset();
                }
                break;
                
            case MOVE_TO_OUTTAKE:
                // Find next slot with a ball and move to its outtake
                if (findAndMoveToNextBall()) {
                    currentPhase = Phase.WAIT_INDEXER;
                    timer.reset();
                } else {
                    // No more balls
                    currentPhase = Phase.FINISH;
                }
                break;
                
            case WAIT_INDEXER:
                if (timer.milliseconds() >= 150) {
                    currentPhase = Phase.EJECT;
                }
                break;
                
            case EJECT:
                shooter.setShootingState();
                ejector.ejectAndStow();
                currentPhase = Phase.WAIT_EJECT;
                timer.reset();
                break;
                
            case WAIT_EJECT:
                if (!ejector.isCycleInProgress()) {
                    indexer.ejectBall();
                    ballsShot++;
                    
                    if (ballsShot >= ballsToShoot) {
                        currentPhase = Phase.FINISH;
                    } else {
                        currentPhase = Phase.WAIT_BETWEEN_SHOTS;
                        timer.reset();
                    }
                }
                break;
                
            case WAIT_BETWEEN_SHOTS:
                if (timer.milliseconds() >= ShooterConstants.Timing.BURST_INTERVAL_MS) {
                    currentPhase = Phase.MOVE_TO_OUTTAKE;
                }
                break;
                
            case FINISH:
                if (returnToIdle) {
                    shooter.returnToIdle();
                }
                currentPhase = Phase.DONE;
                break;
                
            case DONE:
                break;
        }
    }

    /**
     * Finds the next slot with a ball and moves to its outtake position.
     * @return true if a ball was found, false if no balls remain
     */
    private boolean findAndMoveToNextBall() {
        // Check current slot first
        int currentSlot = indexer.getCurrentSlot();
        if (indexer.slotHasBall(currentSlot)) {
            indexer.moveToOuttake();
            return true;
        }
        
        // Check other slots
        for (int i = 1; i <= 3; i++) {
            if (indexer.slotHasBall(i)) {
                indexer.goToSlotOuttake(i);
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            ejector.cancelAndStow();
            if (returnToIdle) {
                shooter.returnToIdle();
            }
        }
    }

    @Override
    public boolean isFinished() {
        return currentPhase == Phase.DONE;
    }

    /**
     * Gets the number of balls shot so far.
     */
    public int getBallsShot() {
        return ballsShot;
    }
}
