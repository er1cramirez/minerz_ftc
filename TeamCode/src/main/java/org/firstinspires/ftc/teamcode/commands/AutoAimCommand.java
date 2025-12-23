package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.RBE;

/**
 * Command for automatic turret aiming using AprilTag vision.
 * 
 * <p>This command enables vision tracking on the turret subsystem and feeds
 * it bearing data from the vision subsystem each cycle. The turret handles
 * all tracking logic internally (state machine, target acquisition, etc.).</p>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * // Toggle binding (recommended)
 * operatorGamepad.getGamepadButton(GamepadKeys.Button.X)
 *     .toggleWhenPressed(new AutoAimCommand(turret, vision, Alliance.RED));
 * 
 * // Manual control interrupts auto-aim (by requiring turret subsystem)
 * operatorGamepad.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
 *     .whileHeld(new InstantCommand(() -> turret.setManualPower(-0.3), turret));
 * }</pre>
 * 
 * @see TurretSubsystem
 * @see VisionSubsystem
 */
public class AutoAimCommand extends CommandBase {
    
    private final TurretSubsystem turret;
    private final VisionSubsystem vision;
    private final Alliance alliance;
    
    /**
     * Creates a new AutoAimCommand.
     * 
     * @param turret The turret subsystem (required, will be controlled)
     * @param vision The vision subsystem (used as sensor, not required)
     * @param alliance The alliance color (determines which goal tag to track)
     */
    public AutoAimCommand(TurretSubsystem turret, VisionSubsystem vision, Alliance alliance) {
        this.turret = turret;
        this.vision = vision;
        this.alliance = alliance;
        
        // Only require turret - vision is used as a sensor
        addRequirements(turret);
    }
    
    @Override
    public void initialize() {
        turret.enableVisionTracking();
    }
    
    @Override
    public void execute() {
        // Get bearing from vision and feed to turret
        RBE goalRBE = vision.getGoalRBE(alliance);
        Double bearing = (goalRBE != null) ? goalRBE.bearing : null;
        
        turret.updateVisionTracking(bearing);
    }
    
    @Override
    public void end(boolean interrupted) {
        turret.disableVisionTracking();
    }
    
    @Override
    public boolean isFinished() {
        // Runs until interrupted (toggle off or manual override)
        return false;
    }
    
    /**
     * @return The alliance this command is tracking
     */
    public Alliance getAlliance() {
        return alliance;
    }
}
