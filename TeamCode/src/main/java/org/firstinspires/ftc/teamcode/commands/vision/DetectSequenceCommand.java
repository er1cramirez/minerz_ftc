// commands/vision/DetectSequenceCommand.java
package org.firstinspires.ftc.teamcode.commands.vision;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.util.RobotState;

/**
 * Detecta el ID de secuencia (21-23) al inicio del autónomo.
 * Termina cuando se detecta o cuando expira el timeout.
 */
public class DetectSequenceCommand extends CommandBase {
    
    private final VisionSubsystem vision;
    private final double timeoutSeconds;
    private ElapsedTime timer;
    
    /**
     * Crea el comando con timeout por defecto de 3 segundos.
     */
    public DetectSequenceCommand(VisionSubsystem vision) {
        this(vision, 3.0);
    }
    
    /**
     * Crea el comando con timeout personalizado.
     * @param vision VisionSubsystem
     * @param timeoutSeconds Tiempo máximo de espera
     */
    public DetectSequenceCommand(VisionSubsystem vision, double timeoutSeconds) {
        this.vision = vision;
        this.timeoutSeconds = timeoutSeconds;
        addRequirements(vision);
    }
    
    @Override
    public void initialize() {
        timer = new ElapsedTime();
        vision.startSequenceDetection();
    }
    
    @Override
    public void execute() {
        // VisionSubsystem.periodic() hace el trabajo
        // Aquí solo verificamos si ya se detectó
        if (vision.hasDetectedSequence()) {
            Integer sequenceId = vision.getDetectedSequenceId();
            RobotState.getInstance().setGameSequenceId(sequenceId);
        }
    }
    
    @Override
    public boolean isFinished() {
        // Terminar si se detectó o si expiró el timeout
        return vision.hasDetectedSequence() || timer.seconds() > timeoutSeconds;
    }
    
    @Override
    public void end(boolean interrupted) {
        vision.stopTracking();
        
        if (!vision.hasDetectedSequence()) {
            // TODO: Manejar caso de no detección
            // Podría usar un default o reportar error
        }
    }
}