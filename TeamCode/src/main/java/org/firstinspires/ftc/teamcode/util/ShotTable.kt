package org.firstinspires.ftc.teamcode.util

/**
 * Tabla empirica de tiro: distancia (in) -> (RPM del flywheel, posicion del hood).
 * Interpolacion lineal entre puntos, con sujecion en los extremos.
 *
 * Se llena tirando de verdad (P4 del roadmap). La geometria 3D de la cell
 * (altura, inclinacion de la abertura) queda codificada implicitamente aqui.
 */
class ShotTable {

    data class Shot(val rpm: Double, val hoodPos: Double)

    private data class Entry(val distance: Double, val rpm: Double, val hoodPos: Double)

    private val entries = mutableListOf<Entry>()

    /** Agrega un punto medido. Mantiene la tabla ordenada por distancia. */
    fun add(distance: Double, rpm: Double, hoodPos: Double) {
        entries += Entry(distance, rpm, hoodPos)
        entries.sortBy { it.distance }
    }

    val isEmpty: Boolean get() = entries.isEmpty()

    /** Distancias entre las que la tabla es valida (fuera de esto, sujeta al extremo). */
    val minDistance: Double get() = entries.first().distance
    val maxDistance: Double get() = entries.last().distance

    fun get(distance: Double): Shot {
        check(entries.isNotEmpty()) { "ShotTable vacia" }
        if (distance <= entries.first().distance) return entries.first().toShot()
        if (distance >= entries.last().distance) return entries.last().toShot()

        val hi = entries.indexOfFirst { it.distance >= distance }
        val a = entries[hi - 1]
        val b = entries[hi]
        val t = (distance - a.distance) / (b.distance - a.distance)
        return Shot(
            rpm = a.rpm + t * (b.rpm - a.rpm),
            hoodPos = a.hoodPos + t * (b.hoodPos - a.hoodPos),
        )
    }

    private fun Entry.toShot() = Shot(rpm, hoodPos)

    companion object {
        /**
         * Tabla por defecto. TODO reemplazar con datos reales del banco/cancha.
         * Valores placeholder solo para que el codigo corra.
         */
        fun default() = ShotTable().apply {
            add(40.0, 2200.0, 0.20)
            add(80.0, 2800.0, 0.45)
            add(120.0, 3400.0, 0.65)
        }
    }
}
