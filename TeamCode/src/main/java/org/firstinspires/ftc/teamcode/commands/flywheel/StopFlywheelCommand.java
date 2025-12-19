package org.firstinspires.ftc.teamcode.commands.flywheel;

import com.seattlesolvers.solverslib.command.InstantCommand;

import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;

/**
 * Comando instantáneo para detener el flywheel.
 * Se ejecuta y termina inmediatamente.
 * 
 * Uso:
 * <pre>
 * // Binding a botón
 * gamepad.getGamepadButton(GamepadKeys.Button.B)
 *     .whenPressed(new StopFlywheelCommand(flywheel));
 * 
 * // En secuencia
 * new SequentialCommandGroup(
 *     new ShootCommand(...),
 *     new WaitCommand(500),
 *     new StopFlywheelCommand(flywheel)
 * );
 * </pre>
 */
public class StopFlywheelCommand extends InstantCommand {
    
    /**
     * Crea un comando para detener el flywheel.
     * 
     * @param flywheel El subsystem de flywheel
     */
    public StopFlywheelCommand(FlywheelSubsystem flywheel) {
        super(flywheel::stop, flywheel);
    }
}
