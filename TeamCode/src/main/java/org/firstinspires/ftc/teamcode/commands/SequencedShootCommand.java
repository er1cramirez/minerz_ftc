package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * Comando que dispara pelotas siguiendo una secuencia de color específica.
 * 
 * CASOS DE USO:
 * 1. Seguir secuencia de partida (Purple-Yellow-Yellow, etc.)
 * 2. Disparar por proximidad (más rápido, menor movimiento)
 * 3. Disparar todo lo disponible
 * 
 * REQUISITOS:
 * - Shooter debe estar en velocidad correcta (IDLE, CLOSE o FAR)
 * - Spindexer debe tener al menos una pelota
 * 
 * SECUENCIA POR PELOTA:
 * 1. Mover spindexer a posición de outtake
 * 2. Esperar a que shooter esté listo (velocidad estable)
 * 3. Eject cycle (empujar pelota)
 * 4. Marcar slot como vacío
 * 5. Delay entre disparos (evitar atascos)
 * 6. Repetir para siguiente pelota
 */
public class SequencedShootCommand extends SequentialCommandGroup {
    
    /**
     * Dispara siguiendo secuencia de color específica.
     * Útil para seguir la secuencia de la partida.
     * 
     * @param spindexer Subsystem de spindexer
     * @param shooter Subsystem de shooter
     * @param ejector Subsystem de ejector
     * @param colorSequence Array de 3 colores en orden deseado
     * 
     * Ejemplo:
     * // Secuencia: Purple, Yellow, Yellow
     * SlotState[] sequence = {
     *     SlotState.PURPLE, 
     *     SlotState.YELLOW, 
     *     SlotState.YELLOW
     * };
     * schedule(new SequencedShootCommand(spindexer, shooter, ejector, sequence));
     */
    public SequencedShootCommand(
        SpindexerSubsystem spindexer,
        ShooterSubsystem shooter,
        EjectorSubsystem ejector,
        SlotState[] colorSequence
    ) {
        // Obtener orden de slots basado en secuencia de color
        int[] shootOrder = spindexer.getColorOrderedSequence(colorSequence);
        
        // Agregar comandos para cada slot en orden
        for (int i = 0; i < shootOrder.length; i++) {
            int slotIndex = shootOrder[i];
            
            // Si slot es -1, significa que no hay pelota de ese color
            if (slotIndex >= 0) {
                addCommands(
                    // Mover a posición de outtake
                    new InstantCommand(
                        () -> spindexer.moveToOuttakePosition(slotIndex),
                        spindexer
                    ),
                    
                    // Esperar a que servo llegue a posición
                    new WaitCommand(300),
                    
                    // Esperar a que shooter esté listo (opcional pero recomendado)
                    new WaitUntilShooterReadyCommand(shooter)
                        .withTimeout(2000),  // Timeout de seguridad
                    
                    // Eject cycle
                    new EjectCycleCommand(ejector),
                    
                    // Marcar slot como vacío
                    new InstantCommand(
                        () -> spindexer.clearSlot(slotIndex),
                        spindexer
                    ),
                    
                    // Delay entre disparos (evitar atascos)
                    new WaitCommand(200)
                );
            }
        }
        
        // Al final, regresar shooter a IDLE
        addCommands(
            new InstantCommand(shooter::idle, shooter)
        );
    }
    
    /**
     * Dispara por orden de proximidad (menor movimiento del servo).
     * Más rápido que seguir secuencia de color.
     * 
     * @param spindexer Subsystem de spindexer
     * @param shooter Subsystem de shooter
     * @param ejector Subsystem de ejector
     */
    public SequencedShootCommand(
        SpindexerSubsystem spindexer,
        ShooterSubsystem shooter,
        EjectorSubsystem ejector
    ) {
        // Obtener orden por proximidad
        int[] shootOrder = spindexer.getProximityOrderedSequence();
        
        // Agregar comandos para cada slot en orden
        for (int slotIndex : shootOrder) {
            if (slotIndex >= 0) {
                addCommands(
                    new InstantCommand(
                        () -> spindexer.moveToOuttakePosition(slotIndex),
                        spindexer
                    ),
                    new WaitCommand(300),
                    new WaitUntilShooterReadyCommand(shooter)
                        .withTimeout(2000),
                    new EjectCycleCommand(ejector),
                    new InstantCommand(
                        () -> spindexer.clearSlot(slotIndex),
                        spindexer
                    ),
                    new WaitCommand(200)
                );
            }
        }
        
        addCommands(
            new InstantCommand(shooter::idle, shooter)
        );
    }
}

/**
 * Comando auxiliar que espera a que el shooter alcance velocidad objetivo.
 */
class WaitUntilShooterReadyCommand extends com.seattlesolvers.solverslib.command.CommandBase {
    private final ShooterSubsystem shooter;
    
    public WaitUntilShooterReadyCommand(ShooterSubsystem shooter) {
        this.shooter = shooter;
        // NO agregar requirements - este comando solo consulta
    }
    
    @Override
    public boolean isFinished() {
        return shooter.isReady();
    }
}

/**
 * Comando auxiliar para cycle completo del ejector.
 * Implementa la lógica que removimos del EjectorSubsystem.
 */
class EjectCycleCommand extends com.seattlesolvers.solverslib.command.CommandBase {
    private final EjectorSubsystem ejector;
    private final com.qualcomm.robotcore.util.ElapsedTime timer;
    
    public EjectCycleCommand(EjectorSubsystem ejector) {
        this.ejector = ejector;
        this.timer = new com.qualcomm.robotcore.util.ElapsedTime();
        addRequirements(ejector);
    }
    
    @Override
    public void initialize() {
        ejector.eject();
        timer.reset();
    }
    
    @Override
    public boolean isFinished() {
        // Usar las constantes del EjectorConstants
        return timer.milliseconds() >= org.firstinspires.ftc.teamcode.constants.EjectorConstants.Timing.FULL_CYCLE_TIME_MS;
    }
    
    @Override
    public void end(boolean interrupted) {
        ejector.stow();
    }
}
