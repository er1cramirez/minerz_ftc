package org.firstinspires.ftc.teamcode.opmodes.teleop

import com.pedropathing.ivy.commands.Commands.instant
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextTeleop
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Flywheel

/**
 * Banco de pruebas y tuning del flywheel. Solo necesita el motor "flywheel"
 * en la configuracion del robot.
 *
 * Gamepad 1:
 *  - A: girar al objetivo   - B: detener
 *  - dpad arriba/abajo: objetivo +/- 50 RPM
 *  - dpad der/izq:      objetivo +/- 250 RPM
 *  - bumpers der/izq:   kS +/- 0.002   (paso 1 del tuning)
 *  - Y/X:               kV +/- 0.00001 (paso 2)
 *  - start/back:        kP +/- 0.0002  (paso 3)
 */
@NextTeleop("Flywheel Tuner")
class FlywheelTuner(private val robot: Robot) : NextOpMode(robot) {

    private var targetRpm = 2500.0

    override fun start() {
        val driver = CommandGamepad(gamepad1)
        val fw = robot.flywheel

        driver.a.onTrue(fw.spinAtRpm(targetRpm))
        driver.b.onTrue(fw.stop())

        driver.dpadUp.onTrue(instant { targetRpm += 50.0 })
        driver.dpadDown.onTrue(instant { targetRpm -= 50.0 })
        driver.dpadRight.onTrue(instant { targetRpm += 250.0 })
        driver.dpadLeft.onTrue(instant { targetRpm -= 250.0 })

        driver.rightBumper.onTrue(instant { fw.kS += 0.002 })
        driver.leftBumper.onTrue(instant { fw.kS -= 0.002 })
        driver.y.onTrue(instant { fw.kV += 0.00001 })
        driver.x.onTrue(instant { fw.kV -= 0.00001 })
        driver.start.onTrue(instant { fw.kP += 0.0002 })
        driver.back.onTrue(instant { fw.kP -= 0.0002 })
    }

    override fun periodic() {
        val fw = robot.flywheel
        if (fw.active) {
            fw.setTargetRpm(targetRpm)
        }

        telemetry.addData("objetivo", "%.0f RPM (%.0f t/s)".format(targetRpm, Flywheel.rpmToTicks(targetRpm)))
        telemetry.addData("actual", "%.0f RPM (%.0f t/s)".format(fw.currentRpm, fw.currentVelocity))
        telemetry.addData("error", "%.0f RPM".format(targetRpm - fw.currentRpm))
        telemetry.addData("ready", fw.ready)
        telemetry.addData("power", "%.3f".format(fw.motor.throttle))
        telemetry.addLine("kS=%.3f  kV=%.5f  kP=%.4f".format(fw.kS, fw.kV, fw.kP))
        telemetry.update()
    }
}
