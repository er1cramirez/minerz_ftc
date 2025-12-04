package org.firstinspires.ftc.teamcode.commands;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.ShooterConstants;
import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;

/**
 * Command to shoot a single ball.
 * 
 * Sequence:
 * 1. Verify shooter is ready (at velocity)
 * 2. Move indexer to current slot's outtake position
 * 3. Trigger ejector to push ball
 * 4. Wait for ejector cycle
 * 5. Return indexer to intake (optional)
 * 
 * Prerequisites:
 * - Shooter must be at target velocity (use SpinUpCommand first)
 * - Indexer should be at an intake position with a ball
 */
public class ShootBallCommand extends CommandBase {

    private final ShooterSubsystem shooter;
    private final ArtifactIndexer indexer;
    private final EjectorSubsystem ejector;
    private final ElapsedTime timer;
    private final boolean returnToIntake;
    
    private enum Phase {
        CHECK_READY,
        MOVE_TO_OUTTAKE,
        WAIT_INDEXER,
        EJECT,
        WAIT_EJECT,
        RETURN_TO_INTAKE,
        DONE
    }
    
    private Phase currentPhase;

    /**
     * Creates a shoot ball command.
     * @param shooter The shooter subsystem
     * @param indexer The indexer subsystem
     * @param ejector The ejector subsystem
     */
    public ShootBallCommand(ShooterSubsystem shooter, ArtifactIndexer indexer, 
                            EjectorSubsystem ejector) {
        this(shooter, indexer, ejector, true);
    }

    /**
     * Creates a shoot ball command with option to stay at outtake.
     * @param shooter The shooter subsystem
     * @param indexer The indexer subsystem
     * @param ejector The ejector subsystem
     * @param returnToIntake Whether to return indexer to intake after shooting
     */
    public ShootBallCommand(ShooterSubsystem shooter, ArtifactIndexer indexer, 
                            EjectorSubsystem ejector, boolean returnToIntake) {
        this.shooter = shooter;
        this.indexer = indexer;
        this.ejector = ejector;
        this.returnToIntake = returnToIntake;
        this.timer = new ElapsedTime();
        
        addRequirements(indexer, ejector);
        // Note: We don't require shooter since we just read its state
    }

    @Override
    public void initialize() {
        currentPhase = Phase.CHECK_READY;
        timer.reset();
    }

    @Override
    public void execute() {
        switch (currentPhase) {
            case CHECK_READY:
                if (shooter.isReady() || shooter.atTargetVelocity()) {
                    currentPhase = Phase.MOVE_TO_OUTTAKE;
                    timer.reset();
                }
                // If not ready, we wait (could add timeout)
                break;
                
            case MOVE_TO_OUTTAKE:
                indexer.moveToOuttake();
                currentPhase = Phase.WAIT_INDEXER;
                timer.reset();
                break;
                
            case WAIT_INDEXER:
                // Wait for servo to reach position
                if (timer.milliseconds() >= 150) {  // Indexer servo travel time
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
                    // Mark ball as ejected
                    indexer.ejectBall();
                    
                    if (returnToIntake) {
                        currentPhase = Phase.RETURN_TO_INTAKE;
                        timer.reset();
                    } else {
                        currentPhase = Phase.DONE;
                    }
                }
                break;
                
            case RETURN_TO_INTAKE:
                indexer.moveToIntake();
                currentPhase = Phase.DONE;
                break;
                
            case DONE:
                break;
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            ejector.cancelAndStow();
        }
    }

    @Override
    public boolean isFinished() {
        return currentPhase == Phase.DONE;
    }
}
