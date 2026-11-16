package org.firstinspires.ftc.teamcode.util

import com.pedropathing.math.Pose

/**
 * Geometria del campo BIOBUZZ en COORDENADAS PEDRO
 * (origen en la esquina inferior-izquierda, x a la derecha, y arriba,
 * pulgadas, heading antihorario en radianes).
 *
 * TODO llenar con el manual del juego / field drawings (P1-5 del roadmap):
 *  - posicion (x, y) del centro de la abertura de cada cell
 *  - altura de la abertura y la inclinacion de su normal
 *  - offset del punto virtual de apuntado (a lo largo de la normal, hacia adentro)
 *
 * Conversion de coordenadas FTC -> Pedro (pedropathing.com/docs/pathing/reference/coordinates):
 *   pedroX = ftcY + 72 ; pedroY = 72 - ftcX ; pedroHeading = ftcHeading - PI/2
 */
object Field {

    const val FIELD_SIZE = 144.0 // in

    /** Convierte una pose en coordenadas FTC (origen al centro) a coordenadas Pedro. */
    fun ftcToPedro(ftcX: Double, ftcY: Double, ftcHeadingRad: Double) =
        Pose(ftcY + 72.0, 72.0 - ftcX, ftcHeadingRad - Math.PI / 2.0)

    // ---- Goals (PLACEHOLDERS - medir/verificar en el manual) ----

    /** Centro (x, y) de la abertura del hive aliado, lado azul. TODO */
    val BLUE_GOAL = GoalTarget(x = 16.0, y = 128.0, openingHeight = 38.0, aimDepthOffset = 4.0)

    /** Centro (x, y) de la abertura del hive aliado, lado rojo. TODO */
    val RED_GOAL = GoalTarget(x = 128.0, y = 128.0, openingHeight = 38.0, aimDepthOffset = 4.0)
}

/**
 * Un objetivo de tiro: posicion horizontal de la abertura, su altura y el
 * offset del punto virtual de apuntado (pulgadas hacia adentro de la cell,
 * a lo largo de la normal de la abertura, para que tiros fuera de eje entren).
 */
data class GoalTarget(
    val x: Double,
    val y: Double,
    val openingHeight: Double,
    val aimDepthOffset: Double,
)
