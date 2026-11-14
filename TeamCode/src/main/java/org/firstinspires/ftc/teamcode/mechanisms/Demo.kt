package org.firstinspires.ftc.teamcode.mechanisms

import com.pedropathing.ivy.Command
import dev.nextftc.robot.Mechanism

/**
 * Mechanism de humo: NO toca hardware, existe para verificar que NextFTC v2
 * (descubrimiento, scheduler de Ivy, bindings) funciona en este proyecto.
 *
 * Borrar cuando exista el primer mechanism real (en la rama de cada robot).
 */
class Demo : Mechanism {

    var estado: String = "REPOSO"
        private set

    var pulsos: Int = 0
        private set

    var loops: Long = 0
        private set

    /** Comando instantaneo: corre una vez y termina. */
    fun pulso(): Command = instant { pulsos++ }

    /** Comando sostenido: activo hasta que algo lo cancele (p. ej. un toggle). */
    fun activar(): Command = Command.build()
        .setStart { estado = "ACTIVO" }
        .setEnd { _ -> estado = "REPOSO" }
        .requiring(this)

    /** periodic() corre cada loop solo si el mechanism esta listado en Robot.mechanisms. */
    override fun periodic() {
        loops++
    }
}
