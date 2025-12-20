package org.firstinspires.ftc.teamcode.commands.vision.nuevo;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.vision.VisionSubsystem.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.vision.VisionSubsystem.RBE;

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
 *
 * IMPORTANTE: El bearing del AprilTag indica:
 *   - Bearing positivo = target está a la IZQUIERDA de la cámara
 *   - Bearing negativo = target está a la DERECHA de la cámara
 *
 * La torreta del TurretSubsystem:
 *   - Ángulos positivos = rotación antihoraria (izquierda visto desde arriba)
 *   - Ángulos negativos = rotación horaria (derecha)
 *
 * Por lo tanto: bearing positivo → ajuste positivo → torreta gira a la izquierda ✓
 */
public class AimAtGoalCommand extends CommandBase {

    private final VisionSubsystem vision;
    private final TurretSubsystem turret;
    private final Alliance alliance;
    private final double bearingToleranceDeg;

    private RBE lastRBE;
    private boolean isLocked;
    private boolean wasLocked;  // Para evitar llamadas repetidas cuando ya está locked
    private int framesWithoutDetection;

    // Constantes de control
    private static final double DEFAULT_BEARING_TOLERANCE_DEG = 2.0;
    private static final double AIM_GAIN = 0.8;  // Aumentado para respuesta más rápida
    private static final int MAX_FRAMES_WITHOUT_DETECTION = 10;  // ~330ms a 30fps

    /**
     * Crea el comando de auto-aim con tolerancia default.
     */
    public AimAtGoalCommand(VisionSubsystem vision,
                            TurretSubsystem turret,
                            Alliance alliance) {
        this(vision, turret, alliance, DEFAULT_BEARING_TOLERANCE_DEG);
    }

    /**
     * Crea el comando de auto-aim con tolerancia custom.
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
        wasLocked = false;
        framesWithoutDetection = 0;
        vision.enable();
    }

    @Override
    public void execute() {
        // Obtener RBE actual al goal
        RBE rbe = vision.getGoalRBE(alliance);

        if (rbe != null) {
            lastRBE = rbe;
            framesWithoutDetection = 0;

            // Calcular error de bearing
            double bearingError = rbe.bearing;

            if (Math.abs(bearingError) <= bearingToleranceDeg) {
                // Dentro de tolerancia - mantener posición
                if (!wasLocked) {
                    // Primera vez que entramos en locked - fijar posición actual
                    turret.setTrackingTarget(turret.getCurrentAngleDeg());
                    wasLocked = true;
                }
                // Si ya estábamos locked, no hacer nada (mantiene el target anterior)
                isLocked = true;
            } else {
                // Fuera de tolerancia - ajustar
                // Usar adjustAngle que internamente usa setTrackingTarget (no resetea PID)
                double adjustment = bearingError * AIM_GAIN;
                turret.adjustAngle(adjustment);
                isLocked = false;
                wasLocked = false;
            }
        } else {
            // Sin detección
            framesWithoutDetection++;
            isLocked = false;

            // Si perdemos detección por poco tiempo, mantener última posición
            // Si perdemos por mucho tiempo, podríamos iniciar búsqueda
            if (framesWithoutDetection > MAX_FRAMES_WITHOUT_DETECTION) {
                // Opcionalmente: iniciar búsqueda o quedarse quieto
                // Por ahora solo mantenemos posición
                wasLocked = false;
            }
        }
    }

    @Override
    public void end(boolean interrupted) {
        // Mantener última posición al terminar
        turret.setTrackingTarget(turret.getCurrentAngleDeg());
    }

    @Override
    public boolean isFinished() {
        // Este comando nunca termina por sí solo
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
     * Obtiene el último RBE medido.
     */
    public RBE getLastRBE() {
        return lastRBE;
    }

    /**
     * Obtiene el rango al goal.
     * @return Rango en pulgadas, o -1 si no hay detección
     */
    public double getRangeToGoal() {
        return lastRBE != null ? lastRBE.range : -1;
    }

    /**
     * Verifica si el goal es visible actualmente.
     */
    public boolean canSeeGoal() {
        return framesWithoutDetection == 0 && lastRBE != null;
    }

    /**
     * Obtiene el ID del tag que está buscando.
     */
    public int getTargetTagId() {
        return alliance.goalTagId;
    }

    /**
     * Obtiene la alianza objetivo.
     */
    public Alliance getTargetAlliance() {
        return alliance;
    }
}