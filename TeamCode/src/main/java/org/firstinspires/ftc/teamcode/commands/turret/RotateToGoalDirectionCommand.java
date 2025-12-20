package org.firstinspires.ftc.teamcode.commands.turret;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;

/**
 * Comando para rotar la torreta hacia la dirección conocida del goal.
 * 
 * Este comando usa posiciones pre-calculadas basadas en:
 * - La posición inicial conocida del robot en el autónomo
 * - La posición fija de los goals en cancha
 * 
 * NO usa visión - simplemente rota a un ángulo conocido.
 * Después de este comando, la cámara debería poder ver el AprilTag del goal.
 */
public class RotateToGoalDirectionCommand extends CommandBase {

    private final TurretSubsystem turret;
    private final Alliance alliance;

    // Ángulos pre-calculados hacia cada goal desde posición inicial
    // TODO: Ajustar según la posición inicial de tu robot
    private static final double ANGLE_TO_RED_GOAL_DEG = 45.0;   // Ejemplo
    private static final double ANGLE_TO_BLUE_GOAL_DEG = -45.0; // Ejemplo

    public RotateToGoalDirectionCommand(TurretSubsystem turret, Alliance alliance) {
        this.turret = turret;
        this.alliance = alliance;
        addRequirements(turret);
    }

    @Override
    public void initialize() {
        double targetAngle = (alliance == Alliance.RED) 
            ? ANGLE_TO_RED_GOAL_DEG 
            : ANGLE_TO_BLUE_GOAL_DEG;
        
        turret.rotateTo(targetAngle);
    }

    @Override
    public boolean isFinished() {
        return turret.isAtTarget();
    }

    @Override
    public void end(boolean interrupted) {
        if (!interrupted) {
            turret.hold();
        }
    }
}
