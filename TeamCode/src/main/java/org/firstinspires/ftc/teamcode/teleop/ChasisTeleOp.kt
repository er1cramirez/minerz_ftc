package org.firstinspires.ftc.teamcode.teleop

import com.pedropathing.follower.Follower
import com.pedropathing.follower.ManualDrive
import com.pedropathing.math.Pose
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.pedro.Constants

/**
 * TeleOp base del chasis (robot-centric), compartido por ambos robots.
 *
 * - Modos de velocidad: RB sube, LB baja (30% / 60% / 100%).
 * - Al soltar los sticks, Pedro mantiene la pose (driveOrHold); util en el
 *   chasis SPARK, que esta en COAST por hardware.
 * - BACK/SHARE re-zerea la pose (para pruebas).
 *
 * Requiere el tuning de Pedro hecho en la rama del chasis (ChasisConfig).
 * Tambien sirve como smoke test de Kotlin + Pedro 3.0.2 + Constants.create().
 */
@TeleOp(name = "Chasis TeleOp", group = "chasis")
class ChasisTeleOp : OpMode() {

    private lateinit var follower: Follower

    // Modos de velocidad (multiplicador sobre la potencia)
    private val speedModes = doubleArrayOf(0.30, 0.60, 1.00)
    private var modeIndex = speedModes.lastIndex // arranca al 100%

    // Flancos de subida para los toggles
    private var lastRb = false
    private var lastLb = false
    private var lastBack = false

    override fun init() {
        follower = Constants.create(hardwareMap)
        follower.setPose(Pose(0.0, 0.0, 0.0))
        telemetry.addLine("Follower creado. Listo.")
        telemetry.update()
    }

    override fun loop() {
        follower.update()

        // --- Modos de velocidad con bumpers ---
        val rb = gamepad1.right_bumper
        val lb = gamepad1.left_bumper
        if (rb && !lastRb && modeIndex < speedModes.lastIndex) modeIndex++
        if (lb && !lastLb && modeIndex > 0) modeIndex--
        lastRb = rb
        lastLb = lb
        val mult = speedModes[modeIndex]

        // --- Re-zero de pose (solo pruebas) ---
        val back = gamepad1.back
        if (back && !lastBack) follower.setPose(Pose(0.0, 0.0, 0.0))
        lastBack = back

        // --- Drive robot-centric ---
        // Convencion Pedro: forward +, lateral + a la izquierda, heading + CCW.
        val forward = -gamepad1.left_stick_y.toDouble() * mult
        val lateral = -gamepad1.left_stick_x.toDouble() * mult
        val turn = -gamepad1.right_stick_x.toDouble() * mult

        // Maneja, y al soltar sticks mantiene la pose actual.
        // El umbral de entrada se escala con el modo para que el "feel" no cambie.
        ManualDrive.driveOrHold(follower, forward, lateral, turn, 0.10 * mult, 0.2)

        // --- Telemetria ---
        val pose = follower.pose()
        telemetry.addData("modo velocidad", "%.0f%%".format(mult * 100))
        telemetry.addData("estado", if (follower.holding()) "HOLD" else "MANUAL")
        telemetry.addData("x", "%.2f".format(pose.x()))
        telemetry.addData("y", "%.2f".format(pose.y()))
        telemetry.addData("heading", "%.1f deg".format(Math.toDegrees(pose.heading())))
        telemetry.update()
    }
}
