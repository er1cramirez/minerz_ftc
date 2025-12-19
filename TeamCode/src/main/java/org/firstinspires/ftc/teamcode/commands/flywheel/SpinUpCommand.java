package org.firstinspires.ftc.teamcode.commands.flywheel;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;

/**
 * Comando para acelerar el flywheel a una velocidad específica.
 * Termina cuando el flywheel alcanza la velocidad y está estable.
 * 
 * Uso:
 * <pre>
 * // Ir a velocidad específica
 * new SpinUpCommand(flywheel, 3200);
 * 
 * // Con timeout
 * new SpinUpCommand(flywheel, 3600).withTimeout(2000);
 * 
 * // Esperar solo a que llegue (sin verificar estabilidad)
 * new SpinUpCommand(flywheel, 3000, false);
 * </pre>
 */
public class SpinUpCommand extends CommandBase {
    
    private final FlywheelSubsystem flywheel;
    private final double targetRPM;
    private final boolean waitForStable;
    
    /**
     * Crea un comando para acelerar el flywheel.
     * Espera a que esté estable antes de terminar.
     * 
     * @param flywheel El subsystem de flywheel
     * @param targetRPM Velocidad objetivo en RPM
     */
    public SpinUpCommand(FlywheelSubsystem flywheel, double targetRPM) {
        this(flywheel, targetRPM, true);
    }
    
    /**
     * Crea un comando para acelerar el flywheel.
     * 
     * @param flywheel El subsystem de flywheel
     * @param targetRPM Velocidad objetivo en RPM
     * @param waitForStable Si true, espera estabilidad; si false, termina al llegar a velocidad
     */
    public SpinUpCommand(FlywheelSubsystem flywheel, double targetRPM, boolean waitForStable) {
        this.flywheel = flywheel;
        this.targetRPM = targetRPM;
        this.waitForStable = waitForStable;
        
        addRequirements(flywheel);
    }
    
    @Override
    public void initialize() {
        flywheel.setTargetRPM(targetRPM);
    }
    
    @Override
    public void execute() {
        // El control se ejecuta en periodic() del subsystem
    }
    
    @Override
    public void end(boolean interrupted) {
        // NO detenemos el flywheel al terminar - mantiene la velocidad
        // Esto permite encadenar con comandos de disparo
        
        if (interrupted) {
            // Si fue interrumpido, podríamos querer detenerlo
            // Pero generalmente queremos mantenerlo girando
            // flywheel.stop();  // Descomentar si se desea detener al interrumpir
        }
    }
    
    @Override
    public boolean isFinished() {
        if (waitForStable) {
            return flywheel.isReadyToShoot();
        } else {
            return flywheel.isAtSpeed();
        }
    }
}
