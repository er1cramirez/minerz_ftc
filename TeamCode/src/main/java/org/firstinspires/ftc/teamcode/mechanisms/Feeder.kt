package org.firstinspires.ftc.teamcode.mechanisms

import com.pedropathing.ivy.Command
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism

/**
 * Feeder: trabaja en conjunto con el intake y controla el paso de pelotas
 * al shooter. Detenido en BRAKE, la propia rueda/banda bloquea el paso;
 * no hay compuerta aparte.
 *
 * Coordinacion esperada (se arma en el opmode, no aqui):
 *  - Recolectar: intake.run() en paralelo con feeder.collect()
 *  - Disparar:   feeder.feed() SOLO cuando flywheel.ready
 */
class Feeder(motorName: String = "feeder") : Mechanism {

    companion object {
        /** Velocidad al alimentar el shooter. */
        const val FEED_POWER = 1.0

        /** Velocidad al acomodar pelotas durante la recoleccion. TODO ajustar. */
        const val COLLECT_POWER = 0.4
    }

    val motor = NextMotor(motorName)

    init {
        // BRAKE: detenido, bloquea el paso de pelotas al shooter.
        motor.zeroPowerBehavior = NextMotor.ZeroPowerBehavior.BRAKE
    }

    /** Alimenta el shooter a potencia plena. Activo hasta cancelarse; termina bloqueando. */
    fun feed(): Command = Command.build()
        .setStart { motor.throttle = FEED_POWER }
        .setEnd { _ -> motor.throttle = 0.0 }
        .requiring(this)

    /** Acompana al intake durante la recoleccion, sin empujar al shooter. */
    fun collect(): Command = Command.build()
        .setStart { motor.throttle = COLLECT_POWER }
        .setEnd { _ -> motor.throttle = 0.0 }
        .requiring(this)

    fun reverse(): Command = Command.build()
        .setStart { motor.throttle = -COLLECT_POWER }
        .setEnd { _ -> motor.throttle = 0.0 }
        .requiring(this)

    /** Detiene y bloquea (BRAKE) el paso al shooter. */
    fun block(): Command = instant { motor.throttle = 0.0 }
}
