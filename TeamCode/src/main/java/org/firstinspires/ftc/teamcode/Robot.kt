package org.firstinspires.ftc.teamcode

import com.pedropathing.follower.Follower
import dev.nextftc.hardware.RobotController
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.NextRobot
import org.firstinspires.ftc.teamcode.mechanisms.Demo
import org.firstinspires.ftc.teamcode.mechanisms.Feeder
import org.firstinspires.ftc.teamcode.mechanisms.Flywheel
import org.firstinspires.ftc.teamcode.mechanisms.Hood
import org.firstinspires.ftc.teamcode.mechanisms.Intake
import org.firstinspires.ftc.teamcode.mechanisms.Turret
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
    val flywheel by lazy { Flywheel("flywheel") }
    val turret by lazy { Turret("turret", "turretHall") }
    val hood by lazy { Hood("hood") }
    val intake by lazy { Intake("intake") }
    val feeder by lazy { Feeder("feeder") }

    // Solo los mechanisms listados reciben periodic() y su defaultCommand.
    override val mechanisms: Set<Mechanism> by lazy {
        setOf(demo, flywheel, turret, hood, intake, feeder)
    }

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
