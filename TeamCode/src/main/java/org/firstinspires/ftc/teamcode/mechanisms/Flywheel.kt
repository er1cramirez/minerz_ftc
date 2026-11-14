package org.firstinspires.ftc.teamcode.mechanisms

import com.pedropathing.ivy.Command
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.units.radiansPerSecond
import kotlin.math.abs

/**
 * Flywheel del lanzador: goBILDA 5203 6000 RPM, 1:1, encoder de 28 ticks/rev,
 * una sola rueda con masa agregada.
 *
 * Control (mismo patron que el Shooter del ejemplo ftc13100/BioBuzz26-27):
 *  - Error grande -> bang-bang (potencia 1.0 o 0.0): spin-up/recuperacion al maximo.
 *  - Cerca del objetivo -> FF (kS + kV*v) + P integrados de NextMotor
 *    (velocityConstants + setVelocitySetpoint).
 *
 * UNIDADES: velocidades en ticks/s (anglePerCount default de NextMotor).
 * Para este motor: ticks/s = RPM * 28 / 60. Velocidad libre ~2800 ticks/s.
 * Usa rpmToTicks()/ticksToRpm() en telemetria y objetivos.
 *
 * Tuning (banco, con FlywheelTuner):
 *  1. kS: potencia minima que apenas mantiene la rueda girando.
 *  2. kV: (potencia_crucero - kS) / velocidad_medida, a ~60% de potencia.
 *  3. kP: subir hasta que el error en estado estable sea aceptable sin oscilar.
 *  4. BANG_BANG_BAND: mas angosto = recuperacion mas agresiva entre tiros.
 */
class Flywheel(motorName: String = "flywheel") : Mechanism {

    val motor = NextMotor(motorName)

    companion object {
        const val TICKS_PER_REV = 28.0
        fun rpmToTicks(rpm: Double) = rpm * TICKS_PER_REV / 60.0
        fun ticksToRpm(ticks: Double) = ticks * 60.0 / TICKS_PER_REV

        /** Techo de seguridad: ~4700 RPM de los 6000 teoricos. */
        val MAX_SPEED = rpmToTicks(4700.0)
    }

    // Constantes de control (punto de partida: shooter del 13100, mismo motor).
    @JvmField var kP = 0.001
    @JvmField var kS = 0.060
    @JvmField var kV = 0.00037

    /** Fuera de esta banda (ticks/s) se usa bang-bang; dentro, FF + P. */
    @JvmField var BANG_BANG_BAND = 100.0

    /** Tolerancia (ticks/s) para declarar el flywheel listo para disparar. */
    @JvmField var READY_BAND = 21.0

    var targetVelocity = 0.0   // ticks/s
        private set
    var active = false
        private set

    /** true cuando la velocidad esta dentro de READY_BAND del objetivo. */
    val ready: Boolean
        get() = active && abs(targetVelocity - currentVelocity) < READY_BAND

    val currentVelocity: Double
        get() = motor.encoderVelocity.magnitude

    val currentRpm: Double
        get() = ticksToRpm(currentVelocity)

    init {
        // FLOAT: nunca frenar activamente una rueda con inercia.
        motor.zeroPowerBehavior = NextMotor.ZeroPowerBehavior.FLOAT
    }

    override fun periodic() {
        motor.velocityConstants.apply {
            kP = this@Flywheel.kP
            kS = this@Flywheel.kS
            kV = this@Flywheel.kV
        }

        if (active) {
            val error = targetVelocity - currentVelocity
            when {
                error > BANG_BANG_BAND -> motor.throttle = 1.0
                error < -BANG_BANG_BAND -> motor.throttle = 0.0
                else -> motor.setVelocitySetpoint(targetVelocity.radiansPerSecond)
            }
        } else {
            motor.throttle = 0.0
        }
    }

    /** Gira al RPM dado y se queda ahi hasta stop() (o hasta que un toggle lo cancele). */
    fun spinAtRpm(rpm: Double): Command = Command.build()
        .setStart {
            targetVelocity = rpmToTicks(rpm).coerceIn(0.0, MAX_SPEED)
            active = true
        }
        .requiring(this)

    fun stop(): Command = instant {
        active = false
        targetVelocity = 0.0
    }

    /** Ajuste en caliente del objetivo (p. ej. offset del operador), en RPM. */
    fun adjustRpm(deltaRpm: Double) {
        targetVelocity = (targetVelocity + rpmToTicks(deltaRpm)).coerceIn(0.0, MAX_SPEED)
    }

    /** Cambia el objetivo sin pasar por el scheduler (para tuners / tracking continuo). */
    fun setTargetRpm(rpm: Double) {
        targetVelocity = rpmToTicks(rpm).coerceIn(0.0, MAX_SPEED)
    }
}
