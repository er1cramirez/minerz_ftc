package org.firstinspires.ftc.teamcode.commands.vision;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;

import java.util.function.Consumer;

/**
 * Comando para detectar la secuencia del obelisco al inicio del autónomo.
 * 
 * Espera hasta detectar uno de los tags de secuencia (21, 22, 23) y
 * guarda el resultado. Termina cuando detecta o cuando alcanza timeout.
 * 
 * Uso:
 * <pre>
 * String[] sequenceHolder = new String[1];
 * new DetectSequenceCommand(vision, seq -> sequenceHolder[0] = seq)
 * </pre>
 */
public class DetectSequenceCommand extends CommandBase {

    private final VisionSubsystem vision;
    private final Consumer<String> onSequenceDetected;
    private final long timeoutMs;

    private ElapsedTime timer;
    private String detectedSequence;

    /**
     * Crea el comando con timeout default.
     * 
     * @param vision Subsystem de visión
     * @param onSequenceDetected Callback que recibe la secuencia detectada
     */
    public DetectSequenceCommand(VisionSubsystem vision, Consumer<String> onSequenceDetected) {
        this(vision, onSequenceDetected, VisionConstants.DETECTION_TIMEOUT_MS);
    }

    /**
     * Crea el comando con timeout custom.
     * 
     * @param vision Subsystem de visión
     * @param onSequenceDetected Callback que recibe la secuencia detectada
     * @param timeoutMs Timeout en milisegundos
     */
    public DetectSequenceCommand(VisionSubsystem vision,
                                  Consumer<String> onSequenceDetected,
                                  long timeoutMs) {
        this.vision = vision;
        this.onSequenceDetected = onSequenceDetected;
        this.timeoutMs = timeoutMs;

        addRequirements(vision);
    }

    @Override
    public void initialize() {
        timer = new ElapsedTime();
        detectedSequence = null;
        vision.enable();
    }

    @Override
    public void execute() {
        // Intentar detectar secuencia
        if (detectedSequence == null) {
            detectedSequence = vision.getSequence();
        }
    }

    @Override
    public void end(boolean interrupted) {
        // Llamar callback con resultado (puede ser null si timeout)
        if (onSequenceDetected != null) {
            onSequenceDetected.accept(detectedSequence);
        }
    }

    @Override
    public boolean isFinished() {
        // Terminar si detectamos secuencia o timeout
        return detectedSequence != null || timer.milliseconds() >= timeoutMs;
    }

    /**
     * Obtiene la secuencia detectada (para uso después del comando).
     */
    public String getDetectedSequence() {
        return detectedSequence;
    }

    /**
     * Verifica si la detección fue exitosa.
     */
    public boolean wasSuccessful() {
        return detectedSequence != null;
    }
}
