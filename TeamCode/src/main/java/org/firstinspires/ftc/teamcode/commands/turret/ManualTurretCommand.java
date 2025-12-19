package org.firstinspires.ftc.teamcode.commands.turret;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;

import java.util.function.DoubleSupplier;

/**
 * Comando para control manual de la torreta usando un joystick.
 * Nunca termina por sí solo - diseñado para uso como default command
 * o mientras se mantiene presionado un botón.
 * 
 * Uso como default command:
 * <pre>
 * turret.setDefaultCommand(
 *     new ManualTurretCommand(turret, () -> -gamepad2.left_stick_x)
 * );
 * </pre>
 * 
 * Uso con botón:
 * <pre>
 * gamepad.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
 *     .whileHeld(new ManualTurretCommand(turret, () -> -gamepad.left_stick_x));
 * </pre>
 */
public class ManualTurretCommand extends CommandBase {
    
    private final TurretSubsystem turret;
    private final DoubleSupplier powerSupplier;
    private final double deadzone;
    
    private static final double DEFAULT_DEADZONE = 0.1;
    
    /**
     * Crea un comando de control manual con deadzone por defecto.
     * 
     * @param turret El subsystem de torreta
     * @param powerSupplier Proveedor de potencia (típicamente un joystick)
     */
    public ManualTurretCommand(TurretSubsystem turret, DoubleSupplier powerSupplier) {
        this(turret, powerSupplier, DEFAULT_DEADZONE);
    }
    
    /**
     * Crea un comando de control manual con deadzone personalizado.
     * 
     * @param turret El subsystem de torreta
     * @param powerSupplier Proveedor de potencia (típicamente un joystick)
     * @param deadzone Zona muerta del joystick (0.0 - 1.0)
     */
    public ManualTurretCommand(TurretSubsystem turret, DoubleSupplier powerSupplier, double deadzone) {
        this.turret = turret;
        this.powerSupplier = powerSupplier;
        this.deadzone = deadzone;
        
        addRequirements(turret);
    }
    
    @Override
    public void initialize() {
        // Nada especial en initialize
    }
    
    @Override
    public void execute() {
        double input = powerSupplier.getAsDouble();
        
        // Aplicar deadzone
        if (Math.abs(input) < deadzone) {
            input = 0;
        }
        
        turret.setManualPower(input);
    }
    
    @Override
    public void end(boolean interrupted) {
        turret.stop();
    }
    
    @Override
    public boolean isFinished() {
        // Nunca termina por sí solo
        return false;
    }
}
