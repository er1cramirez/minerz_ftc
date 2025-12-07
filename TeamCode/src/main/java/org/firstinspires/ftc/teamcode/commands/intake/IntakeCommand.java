package org.firstinspires.ftc.teamcode.commands.intake;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
// Cambiar este comando por una secuencia, ya que se debe verificar si indexer ya esta en una posicion de intake
public class IntakeCommand extends CommandBase {

    private final IntakeSubsystem intake;
    private final SpindexerSubsystem spindexer;

    public IntakeCommand(IntakeSubsystem intake, SpindexerSubsystem spindexer) {
        this.intake = intake;
        this.spindexer = spindexer;
        addRequirements(intake);
    }

    @Override
    public void initialize() {
        // Solo activar si spindexer tiene espacio
//        if (spindexer.hasEmptySlot()) {
//            spindexer.goToClosestEmptySlot();
//            intake.intake();
        }
    }

    @Override
    public void end(boolean interrupted) {
//        intake.stop();
    }

    @Override
    public boolean isFinished() {
        // Terminar cuando spindexer detecte pelota
//        return spindexer.isBallDetected();
    }
}
