package org.firstinspires.ftc.teamcode.commands.intake;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.command.CommandScheduler;

import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

/**
 * Comando rápido que verifica si hay pelota en el slot actual.
 * 
 * FLUJO:
 * - Lee distancia UNA vez
 * - Si hay pelota → ejecuta DetectBallCommand para etiquetar
 * - Termina inmediatamente
 * 
 * El comando padre puede luego consultar spindexer.getCurrentSlotState()
 * para decidir si necesita activar intake o no.
 */
public class CheckSlotCommand extends CommandBase {

    private static final double DISTANCE_BALL_PRESENT = 3.0;

    private final SpindexerSubsystem spindexer;
    private boolean checked = false;

    public CheckSlotCommand(SpindexerSubsystem spindexer) {
        this.spindexer = spindexer;
    }

    @Override
    public void initialize() {
        checked = false;
    }

    @Override
    public void execute() {
        if (checked) return;

        double distance = spindexer.getDistance();
        
        if (distance < DISTANCE_BALL_PRESENT) {
            // Hay pelota, ejecutar detección para etiquetar
            CommandScheduler.getInstance().schedule(new DetectBallCommand(spindexer));
        }
        
        checked = true;
    }

    @Override
    public boolean isFinished() {
        return checked;
    }
}
