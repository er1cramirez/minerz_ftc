package org.firstinspires.ftc.teamcode.opmodes.teleop

import com.pedropathing.follower.ManualDrive
import com.pedropathing.ivy.commands.Commands.instant
import com.pedropathing.math.Pose
import dev.nextftc.robot.opmode.NextOpMode
import dev.nextftc.robot.opmode.NextTeleop
import dev.nextftc.robot.triggers.CommandGamepad
import org.firstinspires.ftc.teamcode.Robot
import org.firstinspires.ftc.teamcode.mechanisms.Flywheel

/**
 * TeleOp principal (2 drivers).
 *
 * GAMEPAD 1 - chasis (robot-centric con hold de pose, igual que Chasis TeleOp):
 *  - sticks: manejar          - bumpers: modo de velocidad (30/60/100%)
 *  - back: re-zero de pose
 *
 * GAMEPAD 2 - mecanismos:
 *  - A (toggle): flywheel a SHOOT_RPM        - dpad arriba/abajo: RPM +/- 100
 *  - RT (manten): alimentar al shooter       (ver READY en telemetria)
 *  - LT (manten): recolectar (intake+feeder)
 *  - B: homing de torreta                    - dpad izq/der: torreta -/+ 2 grados
 *  - X/Y: hood -/+ 0.01
 *
 * TODO (cuando exista tabla de tiro y cancha): reemplazar RPM/hood manuales
 * por GoalTracker + ShotTable (apuntado y parametros automaticos por pose).
 */
@NextTeleop("Main TeleOp")
class MainTeleOp(private val robot: Robot) : NextOpMode(robot) {

    companion object {
        const val SHOOT_RPM = 3000.0
    }

    private val speedModes = doubleArrayOf(0.30, 0.60, 1.00)
    private var modeIndex = speedModes.lastIndex
    private var lastRb = false
    private var lastLb = false
    private var lastBack = false

    override fun start() {
        robot.follower.setPose(Pose(0.0, 0.0, 0.0))

        val operator = CommandGamepad(gamepad2)

        // Flywheel
        operator.a.toggleOnTrue(robot.flywheel.spinAtRpm(SHOOT_RPM))
        operator.dpadUp.onTrue(instant { robot.flywheel.adjustRpm(+100.0) })
        operator.dpadDown.onTrue(instant { robot.flywheel.adjustRpm(-100.0) })

        // Alimentar: sostener RT. El driver dispara cuando READY (telemetria);
        // al soltar, el feeder bloquea de nuevo.
        operator.rightTrigger.isOver(0.5)
            .onTrue(robot.feeder.feed())
            .onFalse(robot.feeder.block())

        // Recolectar: sostener LT (intake + feeder acompanando, bloqueado al shooter)
        operator.leftTrigger.isOver(0.5)
            .onTrue(robot.intake.run().with(robot.feeder.collect()))
            .onFalse(robot.intake.stop().with(robot.feeder.block()))

        // Torreta y hood
        operator.b.onTrue(robot.turret.home())
        operator.dpadRight.onTrue(instant { robot.turret.adjustAngle(+2.0) })
        operator.dpadLeft.onTrue(instant { robot.turret.adjustAngle(-2.0) })
        operator.y.onTrue(instant { robot.hood.adjust(+0.01) })
        operator.x.onTrue(instant { robot.hood.adjust(-0.01) })
    }

    override fun periodic() {
        robot.updateFollower()

        // --- Chasis (gamepad 1), mismo esquema que Chasis TeleOp ---
        val rb = gamepad1.right_bumper
        val lb = gamepad1.left_bumper
        if (rb && !lastRb && modeIndex < speedModes.lastIndex) modeIndex++
        if (lb && !lastLb && modeIndex > 0) modeIndex--
        lastRb = rb; lastLb = lb
        val mult = speedModes[modeIndex]

        val back = gamepad1.back
        if (back && !lastBack) robot.follower.setPose(Pose(0.0, 0.0, 0.0))
        lastBack = back

        val forward = -gamepad1.left_stick_y.toDouble() * mult
        val lateral = -gamepad1.left_stick_x.toDouble() * mult
        val turn = -gamepad1.right_stick_x.toDouble() * mult
        ManualDrive.driveOrHold(robot.follower, forward, lateral, turn, 0.10 * mult, 0.2)

        // --- Telemetria ---
        val fw = robot.flywheel
        telemetry.addData("READY", if (fw.ready) ">>> SI <<<" else "no")
        telemetry.addData("flywheel", "%.0f / %.0f RPM".format(fw.currentRpm, Flywheel.ticksToRpm(fw.targetVelocity)))
        telemetry.addData("torreta", "%s  %.1f°".format(robot.turret.state, robot.turret.currentAngleDeg))
        telemetry.addData("hood", "%.3f".format(robot.hood.targetPosition))
        telemetry.addData("velocidad chasis", "%.0f%%".format(mult * 100))
        telemetry.update()
    }
}
