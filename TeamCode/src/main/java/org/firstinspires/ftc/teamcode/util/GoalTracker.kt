package org.firstinspires.ftc.teamcode.util

import com.pedropathing.math.Pose
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Capa 1 del apuntado: calcula el angulo de torreta y la distancia al goal
 * a partir de la pose del robot (Pinpoint/follower). Funciona 360 grados,
 * en movimiento y sin linea de vista.
 *
 * La capa 2 (correccion por vision cuando hay tag visible) se suma despues
 * como un offset sobre el resultado de esta clase.
 *
 * Convencion: angulo de torreta en grados, 0 = frente del robot,
 * positivo antihorario (mismo sentido que el heading de Pedro).
 */
class GoalTracker(private val goal: GoalTarget) {

    /**
     * Offset (x, y) de la torreta respecto al centro del robot, en pulgadas,
     * en marco-robot. TODO medir del CAD si la torreta no esta al centro.
     */
    var turretOffsetX = 0.0
    var turretOffsetY = 0.0

    /** Angulo que debe tomar la torreta (en marco-robot, grados) para apuntar al goal. */
    fun turretAngleDeg(robotPose: Pose): Double {
        val (tx, ty) = turretFieldPosition(robotPose)
        val fieldAngle = atan2(goal.y - ty, goal.x - tx)          // marco-campo
        val robotRelative = fieldAngle - robotPose.heading()       // marco-robot
        return Math.toDegrees(normalize(robotRelative))
    }

    /** Distancia horizontal torreta -> goal (entrada de la ShotTable). */
    fun distance(robotPose: Pose): Double {
        val (tx, ty) = turretFieldPosition(robotPose)
        return hypot(goal.x - tx, goal.y - ty)
    }

    private fun turretFieldPosition(robotPose: Pose): Pair<Double, Double> {
        val h = robotPose.heading()
        val tx = robotPose.x() + turretOffsetX * Math.cos(h) - turretOffsetY * Math.sin(h)
        val ty = robotPose.y() + turretOffsetX * Math.sin(h) + turretOffsetY * Math.cos(h)
        return tx to ty
    }

    /** Normaliza a (-PI, PI]. */
    private fun normalize(rad: Double): Double {
        var a = rad
        while (a > Math.PI) a -= 2.0 * Math.PI
        while (a <= -Math.PI) a += 2.0 * Math.PI
        return a
    }
}
