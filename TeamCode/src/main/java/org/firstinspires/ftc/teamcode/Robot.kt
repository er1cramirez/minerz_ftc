package org.firstinspires.ftc.teamcode

import com.pedropathing.follower.Follower
import dev.nextftc.hardware.RobotController
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.NextRobot
import org.firstinspires.ftc.teamcode.mechanisms.Demo
import org.firstinspires.ftc.teamcode.pedro.Constants

/**
 * La clase de robot de NextFTC v2.
 *
 * REGLA (igual que ChasisConfig): en pedro-base este archivo es una PLANTILLA con el
 * mechanism Demo y no se vuelve a modificar aqui. Cada rama de robot lo edita:
 * agrega sus mechanisms reales (shooter, turret, hood, intake...) y quita Demo.
 *
 * NextFTC exige EXACTAMENTE UNA clase que implemente NextRobot en el classpath
 * (publica, no abstracta, constructor sin argumentos). Por eso vive aqui y no hay otra.
 */
class Robot : NextRobot {

    val demo by lazy { Demo() }

    // Solo los mechanisms listados reciben periodic() y su defaultCommand.
    override val mechanisms: Set<Mechanism> by lazy { setOf(demo) }

    // Follower de Pedro, perezoso: no toca hardware hasta que un OpMode lo usa.
    private var _follower: Follower? = null
    val follower: Follower
        get() {
            if (_follower == null) {
                _follower = Constants.create(RobotController.hardwareMap)
            }
            return _follower!!
        }

    fun updateFollower() {
        follower.update()
    }
}
