package org.firstinspires.ftc.teamcode.mechanisms

import com.pedropathing.ivy.Command
import com.qualcomm.robotcore.hardware.DigitalChannel
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.units.radians

/**
 * Torreta con motor DC + encoder relativo + sensor hall (REV magnetic limit
 * switch) para homing.
 *
 * Como el encoder es relativo, la torreta arranca UNHOMED: no acepta objetivos
 * hasta hacer homing (girar lento hasta el hall y re-zerear ahi).
 *
 * Unidades internas en ticks; la API publica en grados (0 = posicion del hall).
 */
class Turret(
    motorName: String = "turret",
    private val hallName: String = "turretHall",
) : Mechanism {

    companion object {
        /** TODO medir: ticks del encoder por grado de torreta (ticks/rev motor * relacion / 360). */
        const val TICKS_PER_DEG = 10.0

        /** TODO definir cuando exista el rango fisico. Grados respecto al hall. */
        const val MIN_ANGLE_DEG = -90.0
        const val MAX_ANGLE_DEG = 90.0

        /** Potencia de busqueda del hall durante homing. */
        const val HOMING_POWER = 0.15
    }

    val motor = NextMotor(motorName)

    private val hall: DigitalChannel by lazy {
        RobotController.hardwareMap.get(DigitalChannel::class.java, hallName)
            .apply { mode = DigitalChannel.Mode.INPUT }
    }

    /** REV magnetic limit switch es activo-bajo: state == false cuando detecta el iman. */
    val hallTriggered: Boolean
        get() = !hall.state

    enum class State { UNHOMED, HOMING, READY }

    var state = State.UNHOMED
        private set
    val homed: Boolean get() = state == State.READY

    private var homeOffsetTicks = 0.0
    private var targetDeg = 0.0

    // Constantes de posicion (tunear en banco; ver positionConstants de NextMotor).
    @JvmField var kP = 0.0
    @JvmField var kD = 0.0
    @JvmField var kS = 0.0

    val currentAngleDeg: Double
        get() = (motor.encoderPosition.magnitude - homeOffsetTicks) / TICKS_PER_DEG

    init {
        motor.zeroPowerBehavior = NextMotor.ZeroPowerBehavior.BRAKE
    }

    override fun periodic() {
        when (state) {
            State.UNHOMED -> motor.throttle = 0.0

            State.HOMING -> {
                if (hallTriggered) {
                    homeOffsetTicks = motor.encoderPosition.magnitude
                    targetDeg = 0.0
                    state = State.READY
                } else {
                    motor.throttle = HOMING_POWER
                }
            }

            State.READY -> {
                motor.positionConstants.apply {
                    kP = this@Turret.kP
                    kD = this@Turret.kD
                    kS = this@Turret.kS
                }
                val targetTicks = homeOffsetTicks + targetDeg * TICKS_PER_DEG
                motor.setPositionSetpoint(targetTicks.radians)
            }
        }
    }

    /** Inicia el homing; al encontrar el hall pasa solo a READY. */
    fun home(): Command = instant { if (state == State.UNHOMED) state = State.HOMING }

    /** Objetivo en grados (0 = hall). Ignorado hasta que la torreta este homed. */
    fun toAngle(deg: Double): Command = instant {
        if (homed) targetDeg = deg.coerceIn(MIN_ANGLE_DEG, MAX_ANGLE_DEG)
    }

    fun adjustAngle(deltaDeg: Double) {
        if (homed) targetDeg = (targetDeg + deltaDeg).coerceIn(MIN_ANGLE_DEG, MAX_ANGLE_DEG)
    }

    fun stop(): Command = instant {
        if (state == State.HOMING) state = State.UNHOMED
        motor.throttle = 0.0
    }
}
