package org.firstinspires.ftc.teamcode.opmodes.teleop

import com.pedropathing.ivy.commands.Commands.instant
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextTeleop
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot

/**
 * Banco del hood. Necesita: servo "hood".
 *
 * Gamepad 1:
 *  - dpad arriba/abajo: +/- 0.01
 *  - dpad der/izq:      +/- 0.001 (fino)
 *
 * Objetivo: encontrar el rango fisico real y fijar POS_MIN / POS_MAX en Hood.kt.
 * OJO: los limites en codigo arrancan en [0, 1]; muevan despacio cerca de los
 * topes mecanicos para no forzar el servo.
 */
@NextTeleop("Hood Tuner")
class HoodTuner(private val robot: Robot) : NextOpMode(robot) {

    override fun start() {
        val driver = CommandGamepad(gamepad1)
        val h = robot.hood

        driver.dpadUp.onTrue(instant { h.adjust(+0.01) })
        driver.dpadDown.onTrue(instant { h.adjust(-0.01) })
        driver.dpadRight.onTrue(instant { h.adjust(+0.001) })
        driver.dpadLeft.onTrue(instant { h.adjust(-0.001) })
    }

    override fun periodic() {
        telemetry.addData("posicion", "%.3f".format(robot.hood.targetPosition))
        telemetry.addLine("Anoten el minimo y maximo fisicos -> Hood.POS_MIN/POS_MAX")
        telemetry.update()
    }
}
