package org.firstinspires.ftc.teamcode.commands.flywheel;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;

/**
 * Comando para acelerar el flywheel a la velocidad de la zona lejana.
 * Termina cuando el flywheel está listo para disparar.
 * 
 * Uso:
 * <pre>
 * // En secuencia de disparo
 * new SequentialCommandGroup(
 *     new SpinUpFarZoneCommand(flywheel),
 *     new ShootCommand(...)
 * );
 * 
 * // Con timeout de seguridad
 * new SpinUpFarZoneCommand(flywheel).withTimeout(2500);
 * </pre>
 */
public class SpinUpFarZoneCommand extends CommandBase {
    
    private final FlywheelSubsystem flywheel;
    
    /**
     * Crea un comando para acelerar a velocidad de zona lejana.
     * 
     * @param flywheel El subsystem de flywheel
     */
    public SpinUpFarZoneCommand(FlywheelSubsystem flywheel) {
        this.flywheel = flywheel;
        addRequirements(flywheel);
    }
    
    @Override
    public void initialize() {
        flywheel.spinUpFarZone();
    }
    
    @Override
    public void execute() {
        // Control en periodic()
    }
    
    @Override
    public void end(boolean interrupted) {
        // Mantener girando para el disparo
    }
    
    @Override
    public boolean isFinished() {
        return flywheel.isReadyToShoot();
    }
}
