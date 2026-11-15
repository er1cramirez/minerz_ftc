package org.firstinspires.ftc.teamcode.mechanisms

import com.pedropathing.ivy.Command
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism

/**
 * Intake simple: un motor, sin sensado de pelotas (por ahora).
 * Los comandos run/reverse quedan activos hasta que algo los cancele
 * (p. ej. un toggle del gamepad); al cancelarse detienen el motor.
 */
class Intake(motorName: String = "intake") : Mechanism {

    companion object {
        const val POWER = 1.0
    }

    val motor = NextMotor(motorName)

    init {
        motor.zeroPowerBehavior = NextMotor.ZeroPowerBehavior.FLOAT
    }

    fun run(): Command = Command.build()
        .setStart { motor.throttle = POWER }
        .setEnd { _ -> motor.throttle = 0.0 }
        .requiring(this)

    fun reverse(): Command = Command.build()
        .setStart { motor.throttle = -POWER }
        .setEnd { _ -> motor.throttle = 0.0 }
        .requiring(this)

    fun stop(): Command = instant { motor.throttle = 0.0 }
}
