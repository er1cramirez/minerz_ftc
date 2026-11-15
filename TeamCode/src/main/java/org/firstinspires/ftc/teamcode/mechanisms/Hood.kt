package org.firstinspires.ftc.teamcode.mechanisms

import com.pedropathing.ivy.Command
import dev.nextftc.hardware.actuators.NextServo
import dev.nextftc.robot.Mechanism

/**
 * Hood ajustable del lanzador (servo de posicion).
 * Posiciones en el rango del servo [0, 1]; limites fisicos como constantes.
 */
class Hood(servoName: String = "hood") : Mechanism {

    companion object {
        /** TODO medir los limites fisicos reales del hood en unidades de servo. */
        const val POS_MIN = 0.0
        const val POS_MAX = 1.0
    }

    val servo = NextServo(servoName)

    var targetPosition = POS_MIN
        private set

    fun toPos(pos: Double): Command = instant {
        targetPosition = pos.coerceIn(POS_MIN, POS_MAX)
        servo.position = targetPosition
    }

    fun adjust(delta: Double) {
        targetPosition = (targetPosition + delta).coerceIn(POS_MIN, POS_MAX)
        servo.position = targetPosition
    }
}
