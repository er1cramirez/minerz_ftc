package org.firstinspires.ftc.teamcode.commands.turret;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;

/**
 * Comando para mover la torreta a una posición específica.
 * Termina cuando la torreta alcanza la posición y está estable.
 * 
 * Uso:
 * <pre>
 * // Ir a 45 grados
 * new TurretToPositionCommand(turret, 45.0);
 * 
 * // Con timeout
 * new TurretToPositionCommand(turret, 45.0).withTimeout(2000);
 * </pre>
 */
public class TurretToPositionCommand extends CommandBase {
    
    private final TurretSubsystem turret;
    private final double targetAngleDeg;
    private final boolean waitForStable;
    
    /**
     * Crea un comando para mover la torreta a una posición.
     * Espera a que esté estable (baja velocidad) antes de terminar.
     * 
     * @param turret El subsystem de torreta
     * @param targetAngleDeg Ángulo objetivo en grados (0 = frente del robot)
     */
    public TurretToPositionCommand(TurretSubsystem turret, double targetAngleDeg) {
        this(turret, targetAngleDeg, true);
    }
    
    /**
     * Crea un comando para mover la torreta a una posición.
     * 
     * @param turret El subsystem de torreta
     * @param targetAngleDeg Ángulo objetivo en grados (0 = frente del robot)
     * @param waitForStable Si es true, espera a que esté estable; si es false, 
     *                      termina cuando llega a la posición aunque tenga velocidad
     */
    public TurretToPositionCommand(TurretSubsystem turret, double targetAngleDeg, boolean waitForStable) {
        this.turret = turret;
        this.targetAngleDeg = targetAngleDeg;
        this.waitForStable = waitForStable;
        
        addRequirements(turret);
    }
    
    @Override
    public void initialize() {
        turret.setTargetPosition(targetAngleDeg);
    }
    
    @Override
    public void execute() {
        // El control PID se ejecuta en periodic() del subsystem
    }
    
    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            turret.stop();
        }
        // Si no fue interrumpido, mantiene la posición (el subsystem sigue en POSITION)
    }
    
    @Override
    public boolean isFinished() {
        if (waitForStable) {
            return turret.isAtTargetAndStable();
        } else {
            return turret.isAtTarget();
        }
    }
}
