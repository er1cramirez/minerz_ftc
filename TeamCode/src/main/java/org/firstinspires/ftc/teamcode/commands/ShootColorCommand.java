package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer;
import org.firstinspires.ftc.teamcode.subsystems.ArtifactIndexer.BallColor;

/**
 * Command to shoot a ball of a specific color.
 * Finds the slot containing the specified color and moves to outtake position.
 * Useful in autonomous to prioritize which color ball to shoot first.
 */
public class ShootColorCommand extends CommandBase {

    private final ArtifactIndexer indexer;
    private final BallColor targetColor;
    private boolean colorFound;

    /**
     * Creates a command to shoot a specific colored ball.
     * @param indexer The indexer subsystem
     * @param color The color of ball to shoot
     */
    public ShootColorCommand(ArtifactIndexer indexer, BallColor color) {
        this.indexer = indexer;
        this.targetColor = color;
        addRequirements(indexer);
    }

    @Override
    public void initialize() {
        colorFound = indexer.goToShootColor(targetColor);
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    /**
     * Returns true if the command successfully found a ball of the target color.
     */
    public boolean wasColorFound() {
        return colorFound;
    }
}
