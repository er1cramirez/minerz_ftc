// commands/vision/TrackTargetCommand.java
package org.firstinspires.ftc.teamcode.commands.vision;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.util.RobotState;

/**
 * Activa el tracking continuo del AprilTag target.
 * NO termina por sí mismo - es un comando continuo para TeleOp.
 * 
 * Uso típico:
 * - Bindearlo a un botón con whileHeld()
 * - Se detiene automáticamente cuando se suelta el botón
 */
public class TrackTargetCommand extends CommandBase {
    
    private final VisionSubsystem vision;
    
    public TrackTargetCommand(VisionSubsystem vision) {
        this.vision = vision;
        addRequirements(vision);
    }
    
    @Override
    public void initialize() {
        vision.startTargetTracking();
    }
    
    @Override
    public void execute() {
        // VisionSubsystem.periodic() actualiza las detecciones
        // Actualizar RobotState para que otros subsystems puedan consultar
        if (vision.hasValidTarget()) {
            RobotState.getInstance().updateVisionTarget(vision.getLastValidTarget());
        } else {
            RobotState.getInstance().updateVisionTarget(org.firstinspires.ftc.teamcode.util.VisionTarget.invalid());
        }
    }
    
    @Override
    public boolean isFinished() {
        // Nunca termina por sí mismo - es un comando continuo
        return false;
    }
    
    @Override
    public void end(boolean interrupted) {
        vision.stopTracking();
    }
}