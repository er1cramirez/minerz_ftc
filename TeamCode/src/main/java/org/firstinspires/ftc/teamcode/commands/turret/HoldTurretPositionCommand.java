package org.firstinspires.ftc.teamcode.commands.turret;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;

/**
 * Comando para mantener la posición actual de la torreta.
 * Útil como default command para que la torreta no se mueva 
 * cuando no hay otro comando activo.
 * 
 * Este comando:
 * - En initialize(), captura la posición actual como target
 * - Mantiene esa posición usando control PID
 * - Nunca termina por sí solo
 * 
 * Uso:
 * <pre>
 * turret.setDefaultCommand(new HoldTurretPositionCommand(turret));
 * </pre>
 */
public class HoldTurretPositionCommand extends CommandBase {
    
    private final TurretSubsystem turret;
    
    /**
     * Crea un comando para mantener la posición de la torreta.
     * 
     * @param turret El subsystem de torreta
     */
    public HoldTurretPositionCommand(TurretSubsystem turret) {
        this.turret = turret;
        addRequirements(turret);
    }
    
    @Override
    public void initialize() {
        // Capturar la posición actual como el nuevo target
        turret.setTargetPosition(turret.getCurrentAngleDeg());
    }
    
    @Override
    public void execute() {
        // El control PID se ejecuta en periodic() del subsystem
    }
    
    @Override
    public void end(boolean interrupted) {
        // No hacer nada especial - otro comando tomará el control
    }
    
    @Override
    public boolean isFinished() {
        // Nunca termina por sí solo
        return false;
    }
}
