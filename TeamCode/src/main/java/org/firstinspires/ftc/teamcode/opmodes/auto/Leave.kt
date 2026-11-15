package org.firstinspires.ftc.teamcode.opmodes.auto

import com.pedropathing.ivy.pedro.PedroCommands.follow
import com.pedropathing.ivy.pedro.PedroCommands.hold
import com.pedropathing.math.Pose
import com.pedropathing.paths.AtomicPath
import com.pedropathing.paths.curves.Line
import dev.nextftc.robot.opmode.NextAutonomous
import dev.nextftc.robot.opmode.NextOpMode
import org.firstinspires.ftc.teamcode.Robot

/**
 * Auto minimo "Leave": sale de la zona de arranque y mantiene la pose.
 * Mientras maneja, activa el mechanism Demo (prueba de composicion
 * drive + mechanism); al terminar el path, Demo vuelve a REPOSO y
 * se dispara un pulso.
 *
 * POSES PLACEHOLDER (pulgadas, heading en radianes) - cambiar por las reales.
 * Requiere el tuning de Pedro de la rama del chasis.
 */
object LeavePaths {
    // TODO poses reales
    val start = Pose(56.0, 8.0, Math.toRadians(90.0))
    val end = Pose(56.0, 40.0, Math.toRadians(90.0))

    /** Linea recta manteniendo el heading inicial. */
    val leave = AtomicPath(Line(start, end)).constant(start.heading())
}

@NextAutonomous("Leave")
class Leave(private val robot: Robot) : NextOpMode(robot) {

    override fun start() {
        robot.follower.setPose(LeavePaths.start)

        follow(robot.follower, LeavePaths.leave)   // sigue el path...
            .raceWith(robot.demo.activar())        // ...con Demo ACTIVO mientras tanto
            .then(robot.demo.pulso())              // pulso al llegar
            .then(hold(robot.follower))            // y mantiene la pose final
            .schedule()
    }

    override fun periodic() {
        robot.updateFollower()

        val pose = robot.follower.pose()
        telemetry.addData("demo", "${robot.demo.estado} pulsos=${robot.demo.pulsos}")
        telemetry.addData("pose", "x=%.1f y=%.1f h=%.0f".format(pose.x(), pose.y(), Math.toDegrees(pose.heading())))
        telemetry.update()
    }
}
