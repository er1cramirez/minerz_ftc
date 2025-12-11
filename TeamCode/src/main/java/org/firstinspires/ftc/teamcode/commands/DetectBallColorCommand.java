package org.firstinspires.ftc.teamcode.commands.spindexer;

import com.qualcomm.robotcore.util.ElapsedTime;
import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.BallColor;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comando que detecta el color de una pelota usando votación.
 * 
 * FLUJO:
 * 1. Verifica que haya pelota bajo el sensor
 * 2. Toma múltiples muestras de color con delay entre cada una
 * 3. Usa votación para determinar el color más consistente
 * 4. Actualiza el slot correspondiente con el color detectado
 * 
 * IMPORTANTE:
 * - Este comando es ASÍNCRONO (no bloquea el scheduler)
 * - Usa execute() + delay entre ciclos para tomar muestras
 * - Termina cuando completa todas las muestras o timeout
 * 
 * REQUISITOS:
 * - El spindexer debe estar en posición AT_INTAKE
 * - Debe haber pelota bajo el sensor
 */
public class DetectBallColorCommand extends CommandBase {
    
    private final SpindexerSubsystem spindexer;
    private final int targetSlot;
    private final int votingSamples;
    private final int votingDelayMs;
    
    private final ElapsedTime sampleTimer;
    private final ElapsedTime timeoutTimer;
    private final List<BallColor> votes;
    
    private int samplesCollected;
    
    /**
     * Crea un comando de detección con parámetros por defecto.
     * 
     * @param spindexer El subsystem de spindexer
     * @param targetSlot Slot donde guardar el resultado (normalmente el slot actual)
     */
    public DetectBallColorCommand(SpindexerSubsystem spindexer, int targetSlot) {
        this(
            spindexer, 
            targetSlot,
            SpindexerConstants.DEFAULT_VOTING_SAMPLES,
            SpindexerConstants.DEFAULT_VOTING_DELAY_MS
        );
    }
    
    /**
     * Crea un comando de detección con parámetros personalizados.
     * 
     * @param spindexer El subsystem de spindexer
     * @param targetSlot Slot donde guardar el resultado
     * @param votingSamples Número de muestras para votación (recomendado: 15-25)
     * @param votingDelayMs Delay entre muestras en ms (recomendado: 30-100)
     */
    public DetectBallColorCommand(
        SpindexerSubsystem spindexer, 
        int targetSlot,
        int votingSamples,
        int votingDelayMs
    ) {
        this.spindexer = spindexer;
        this.targetSlot = targetSlot;
        this.votingSamples = votingSamples;
        this.votingDelayMs = votingDelayMs;
        
        this.sampleTimer = new ElapsedTime();
        this.timeoutTimer = new ElapsedTime();
        this.votes = new ArrayList<>();
        
        addRequirements(spindexer);
    }
    
    @Override
    public void initialize() {
        samplesCollected = 0;
        votes.clear();
        sampleTimer.reset();
        timeoutTimer.reset();
    }
    
    @Override
    public void execute() {
        // Solo tomar muestra si ha pasado suficiente tiempo
        if (sampleTimer.milliseconds() >= votingDelayMs) {
            // Leer color actual
            BallColor sample = spindexer.readCurrentColor();
            votes.add(sample);
            samplesCollected++;
            
            // Reset timer para próxima muestra
            sampleTimer.reset();
        }
    }
    
    @Override
    public boolean isFinished() {
        // Terminar si:
        // 1. Completamos todas las muestras
        if (samplesCollected >= votingSamples) {
            return true;
        }
        
        // 2. Timeout de seguridad
        if (timeoutTimer.milliseconds() >= SpindexerConstants.DETECTION_TIMEOUT_MS) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            // Si fue interrumpido, marcar como UNKNOWN
            spindexer.setSlotState(targetSlot, SlotState.UNKNOWN);
        } else {
            // Procesar votación y actualizar slot
            BallColor detectedColor = tallyVotes();
            SlotState slotState = colorToSlotState(detectedColor);
            spindexer.setSlotState(targetSlot, slotState);
        }
    }
    
    /**
     * Cuenta los votos y retorna el color más frecuente.
     */
    private BallColor tallyVotes() {
        if (votes.isEmpty()) {
            return BallColor.UNKNOWN;
        }
        
        // Contar votos
        Map<BallColor, Integer> counts = new HashMap<>();
        for (BallColor color : votes) {
            counts.put(color, counts.getOrDefault(color, 0) + 1);
        }
        
        // Encontrar el color con más votos
        BallColor winner = BallColor.UNKNOWN;
        int maxVotes = 0;
        
        for (Map.Entry<BallColor, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
            }
        }
        
        // Filtrar NONE si hay suficientes votos de color real
        if (winner == BallColor.NONE && maxVotes < votingSamples * 0.6) {
            // Si NONE no tiene mayoría clara (>60%), buscar el color más votado
            int colorVotes = Math.max(
                counts.getOrDefault(BallColor.YELLOW, 0),
                counts.getOrDefault(BallColor.PURPLE, 0)
            );
            
            if (colorVotes > 0) {
                winner = counts.getOrDefault(BallColor.YELLOW, 0) > 
                        counts.getOrDefault(BallColor.PURPLE, 0) 
                        ? BallColor.YELLOW : BallColor.PURPLE;
            }
        }
        
        return winner;
    }
    
    /**
     * Convierte BallColor a SlotState.
     */
    private SlotState colorToSlotState(BallColor color) {
        switch (color) {
            case YELLOW: return SlotState.YELLOW;
            case PURPLE: return SlotState.PURPLE;
            case NONE: return SlotState.EMPTY;
            case UNKNOWN:
            default: return SlotState.UNKNOWN;
        }
    }
    
    /**
     * Obtiene el color detectado después de que el comando termine.
     * Solo válido después de end().
     */
    public BallColor getDetectedColor() {
        return tallyVotes();
    }
    
    /**
     * Obtiene el número de muestras recolectadas hasta el momento.
     */
    public int getSamplesCollected() {
        return samplesCollected;
    }
}
