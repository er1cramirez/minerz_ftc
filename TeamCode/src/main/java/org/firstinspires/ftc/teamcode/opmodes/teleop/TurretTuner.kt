package org.firstinspires.ftc.teamcode.opmodes.teleop

import com.pedropathing.ivy.commands.Commands.instant
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextTeleop
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot

/**
 * Banco de la torreta. Necesita: motor "turret" + hall "turretHall".
 *
 * Gamepad 1:
 *  - A: iniciar homing (gira lento hasta el hall y re-zerea)
 *  - B: stop
 *  - dpad izq/der: objetivo -/+ 5 grados (una vez homed)
 *  - bumpers: kP -/+ 0.0005
 *  - X/Y: kS -/+ 0.002
 *
 * Primer uso: verifiquen TICKS_PER_DEG moviendo a +90 y midiendo con
 * transportador; si no coincide, corrijan la constante antes de tunear kP.
 */
@NextTeleop("Turret Tuner")
class TurretTuner(private val robot: Robot) : NextOpMode(robot) {

    override fun start() {
        val driver = CommandGamepad(gamepad1)
        val t = robot.turret

        driver.a.onTrue(t.home())
        driver.b.onTrue(t.stop())

        driver.dpadRight.onTrue(instant { t.adjustAngle(+5.0) })
        driver.dpadLeft.onTrue(instant { t.adjustAngle(-5.0) })

        driver.rightBumper.onTrue(instant { t.kP += 0.0005 })
        driver.leftBumper.onTrue(instant { t.kP -= 0.0005 })
        driver.y.onTrue(instant { t.kS += 0.002 })
        driver.x.onTrue(instant { t.kS -= 0.002 })
    }

    override fun periodic() {
        val t = robot.turret
        telemetry.addData("estado", t.state)
        telemetry.addData("hall", t.hallTriggered)
        telemetry.addData("angulo", "%.1f°".format(t.currentAngleDeg))
        telemetry.addLine("kP=%.4f  kS=%.3f  kD=%.4f".format(t.kP, t.kS, t.kD))
        telemetry.update()
    }
}
