package org.firstinspires.ftc.teamcode.commands.flywheel;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;

/**
 * Comando que espera hasta que el flywheel esté listo para disparar.
 * NO cambia la velocidad del flywheel, solo espera.
 * 
 * Útil en secuencias donde el flywheel ya está girando
 * pero queremos asegurar que esté estable antes de disparar.
 * 
 * Uso:
 * <pre>
 * // En secuencia de disparo
 * new SequentialCommandGroup(
 *     // El flywheel ya está en idle spin por default command
 *     new InstantCommand(() -> flywheel.setTargetRPM(3200)),
 *     new WaitForFlywheelReadyCommand(flywheel),
 *     new ShootCommand(...)
 * );
 * </pre>
 * 
 * NOTA: Este comando NO requiere el subsystem porque no lo controla,
 * solo lo observa. Esto permite que otro comando mantenga el control.
 */
public class WaitForFlywheelReadyCommand extends CommandBase {
    
    private final FlywheelSubsystem flywheel;
    
    /**
     * Crea un comando que espera a que el flywheel esté listo.
     * 
     * @param flywheel El subsystem de flywheel (solo para observar)
     */
    public WaitForFlywheelReadyCommand(FlywheelSubsystem flywheel) {
        this.flywheel = flywheel;
        // NO agregamos requirements - solo observamos
    }
    
    @Override
    public void initialize() {
        // Nada que inicializar
    }
    
    @Override
    public void execute() {
        // Solo esperamos
    }
    
    @Override
    public void end(boolean interrupted) {
        // Nada que limpiar
    }
    
    @Override
    public boolean isFinished() {
        return flywheel.isReadyToShoot();
    }
}
