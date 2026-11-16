package org.firstinspires.ftc.teamcode.opmodes.teleop

import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextTeleop
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot

/**
 * Banco de intake + feeder. Necesita: motores "intake" y "feeder".
 *
 * Gamepad 1 (todos toggle: presionar enciende, presionar de nuevo apaga):
 *  - A: recolectar (intake + feeder a COLLECT_POWER, bloqueado hacia el shooter)
 *  - Y: alimentar (feeder a FEED_POWER - lo que hara al disparar)
 *  - B: reversa de ambos (destrabar)
 *  - X: bloquear feeder (stop en BRAKE)
 *
 * Con esto validan el sentido de giro de ambos motores (si uno va al reves,
 * inviertanlo en la config del RC o con Direction en el mechanism) y ajustan
 * COLLECT_POWER para que las pelotas se acomoden sin empujarse al shooter.
 */
@NextTeleop("Intake-Feeder Tester")
class IntakeFeederTester(private val robot: Robot) : NextOpMode(robot) {

    override fun start() {
        val driver = CommandGamepad(gamepad1)

        driver.a.toggleOnTrue(robot.intake.run().with(robot.feeder.collect()))
        driver.y.toggleOnTrue(robot.feeder.feed())
        driver.b.toggleOnTrue(robot.intake.reverse().with(robot.feeder.reverse()))
        driver.x.onTrue(robot.feeder.block())
    }

    override fun periodic() {
        telemetry.addData("intake", "%.2f".format(robot.intake.motor.throttle))
        telemetry.addData("feeder", "%.2f".format(robot.feeder.motor.throttle))
        telemetry.update()
    }
}
