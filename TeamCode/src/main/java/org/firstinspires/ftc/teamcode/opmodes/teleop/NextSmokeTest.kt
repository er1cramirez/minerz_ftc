package org.firstinspires.ftc.teamcode.opmodes.teleop

import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextTeleop
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot

/**
 * Smoke test de NextFTC v2. NO necesita hardware ni tuning: si este OpMode
 * aparece en el Driver Station y responde a los botones, el stack
 * (Sloth + Ivy + NextFTC v2 + Kotlin) esta vivo.
 *
 * Controles (gamepad 1):
 *  - A: pulso (comando instantaneo, incrementa contador)
 *  - B: toggle ACTIVO/REPOSO (comando sostenido)
 *
 * Borrar junto con mechanisms/Demo.kt cuando haya mechanisms reales.
 */
@NextTeleop("Next Smoke Test")
class NextSmokeTest(private val robot: Robot) : NextOpMode(robot) {

    override fun start() {
        val driver = CommandGamepad(gamepad1)
        driver.a.onTrue(robot.demo.pulso())
        driver.b.toggleOnTrue(robot.demo.activar())
    }

    override fun periodic() {
        telemetry.addData("estado", robot.demo.estado)
        telemetry.addData("pulsos (A)", robot.demo.pulsos)
        telemetry.addData("loops de periodic", robot.demo.loops)
        telemetry.update()
    }
}
