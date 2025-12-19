package org.firstinspires.ftc.teamcode.commands.flywheel;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;

/**
 * Comando para mantener el flywheel girando a velocidad de idle.
 * Nunca termina por sí solo - ideal como default command.
 * 
 * Beneficios del idle spin:
 * - Reduce tiempo de spin-up cuando se necesita disparar
 * - Mantiene el flywheel "caliente" y estable
 * - Consume menos batería que velocidad completa
 * 
 * Uso como default command:
 * <pre>
 * flywheel.setDefaultCommand(new IdleSpinCommand(flywheel));
 * </pre>
 * 
 * Cuando se activa un SpinUpCommand, interrumpe este comando.
 * Cuando el SpinUpCommand termina, este vuelve a activarse.
 */
public class IdleSpinCommand extends CommandBase {
    
    private final FlywheelSubsystem flywheel;
    
    /**
     * Crea un comando de idle spin.
     * 
     * @param flywheel El subsystem de flywheel
     */
    public IdleSpinCommand(FlywheelSubsystem flywheel) {
        this.flywheel = flywheel;
        addRequirements(flywheel);
    }
    
    @Override
    public void initialize() {
        flywheel.idleSpin();
    }
    
    @Override
    public void execute() {
        // El control se ejecuta en periodic() del subsystem
    }
    
    @Override
    public void end(boolean interrupted) {
        // No detenemos - probablemente fue interrumpido por SpinUpCommand
        // Si queremos que se detenga al final, descomentar:
        // flywheel.stop();
    }
    
    @Override
    public boolean isFinished() {
        // Nunca termina por sí solo
        return false;
    }
}
