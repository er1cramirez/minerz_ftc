package org.firstinspires.ftc.teamcode.commands.vision;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.RBE;

/**
 * Comando para auto-aiming de la torreta hacia el goal.
 * 
 * Este comando:
 * 1. Lee RBE hacia el goal desde visión
 * 2. Usa el bearing para ajustar la torreta
 * 3. Continúa ajustando hasta que el bearing está dentro de tolerancia
 * 
 * Este comando NUNCA termina por sí solo (debe ser cancelado o usado con timeout).
 * Esto permite mantener el aim activo mientras se prepara el disparo.
 */
public class AimAtGoalCommand extends CommandBase {

    private final VisionSubsystem vision;
    private final TurretSubsystem turret;
    private final Alliance alliance;
    private final double bearingToleranceDeg;

    private RBE lastRBE;
    private boolean isLocked;

    // Constantes de control
    private static final double DEFAULT_BEARING_TOLERANCE_DEG = 2.0;
    private static final double AIM_GAIN = 0.5;  // Proporcional para ajuste de torreta

    /**
     * Crea el comando de auto-aim con tolerancia default.
     * 
     * @param vision Subsystem de visión
     * @param turret Subsystem de torreta
     * @param alliance Alianza (determina qué goal buscar)
     */
    public AimAtGoalCommand(VisionSubsystem vision,
                            TurretSubsystem turret,
                            Alliance alliance) {
        this(vision, turret, alliance, DEFAULT_BEARING_TOLERANCE_DEG);
    }

    /**
     * Crea el comando de auto-aim con tolerancia custom.
     * 
     * @param vision Subsystem de visión
     * @param turret Subsystem de torreta
     * @param alliance Alianza (determina qué goal buscar)
     * @param bearingToleranceDeg Tolerancia de bearing en grados
     */
    public AimAtGoalCommand(VisionSubsystem vision,
                            TurretSubsystem turret,
                            Alliance alliance,
                            double bearingToleranceDeg) {
        this.vision = vision;
        this.turret = turret;
        this.alliance = alliance;
        this.bearingToleranceDeg = bearingToleranceDeg;

        addRequirements(vision, turret);
    }

    @Override
    public void initialize() {
        lastRBE = null;
        isLocked = false;
        vision.enable();
    }

    @Override
    public void execute() {
        // Obtener RBE actual al goal
        RBE rbe = vision.getGoalRBE(alliance);

        if (rbe != null) {
            lastRBE = rbe;

            // Calcular ajuste de torreta basado en bearing
            double bearingError = rbe.bearing;

            if (Math.abs(bearingError) <= bearingToleranceDeg) {
                // Dentro de tolerancia - mantener posición (hold)
                turret.hold();
                isLocked = true;
            } else {
                // Fuera de tolerancia - ajustar
                // Bearing positivo = target está a la izquierda = rotar torreta a la izquierda
                double adjustment = bearingError * AIM_GAIN;
                turret.adjustAngle(adjustment);
                isLocked = false;
            }
        } else {
            // Sin detección - mantener última posición o buscar
            isLocked = false;
            // Opcionalmente: turret.search() para buscar el goal
        }
    }

    @Override
    public void end(boolean interrupted) {
        turret.hold();  // Mantener última posición al terminar
    }

    @Override
    public boolean isFinished() {
        // Este comando nunca termina por sí solo
        // Debe usarse con .withTimeout() o ser interrumpido
        return false;
    }

    // ===== MÉTODOS DE CONSULTA =====

    /**
     * Verifica si la torreta está alineada al goal.
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Obtiene el último RBE medido (para cálculos de velocidad del shooter).
     */
    public RBE getLastRBE() {
        return lastRBE;
    }

    /**
     * Obtiene el rango al goal (para cálculos de velocidad).
     * @return Rango en pulgadas, o -1 si no hay detección
     */
    public double getRangeToGoal() {
        return lastRBE != null ? lastRBE.range : -1;
    }

    /**
     * Verifica si el goal es visible.
     */
    public boolean canSeeGoal() {
        return lastRBE != null;
    }
}
