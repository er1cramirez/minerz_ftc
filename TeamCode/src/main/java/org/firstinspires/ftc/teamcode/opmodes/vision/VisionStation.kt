package org.firstinspires.ftc.teamcode.opmodes.vision

import android.util.Size
import com.pedropathing.follower.Follower
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.teamcode.pedro.Constants
import org.firstinspires.ftc.teamcode.util.Field
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor

/**
 * Estacion de visión (OpMode plano del SDK: NO necesita chasis ni tuning).
 *
 * Qué hace:
 *  1. Detecta AprilTags con VisionPortal y muestra por tag: id, range,
 *     bearing, yaw y decision margin (calidad).
 *  2. Si el SDK puede resolver la pose del robot (tag en la librería +
 *     calibración de la cámara), la convierte de coordenadas FTC a PEDRO.
 *  3. Si hay un chasis configurado, crea el Follower y muestra la pose del
 *     Pinpoint junto a la de visión: el residual (error) entre ambas es el
 *     dato que alimenta las covarianzas del filtro de enero.
 *
 * En banco (solo cámara + hub) corre igual: simplemente reporta
 * "sin chasis" y trabaja solo con los tags.
 *
 * NOTA OV9281: el SDK no trae calibración para esta cámara; sin calibrar,
 * range/pose salen sesgados. El primer trabajo de esta estación es calibrar
 * (P3-13 del roadmap) y validar aquí que el range coincida con una cinta métrica.
 */
@TeleOp(name = "Vision Station", group = "vision")
class VisionStation : OpMode() {

    companion object {
        const val WEBCAM_NAME = "Webcam 1"
    }

    private lateinit var aprilTag: AprilTagProcessor
    private lateinit var portal: VisionPortal
    private var follower: Follower? = null

    override fun init() {
        aprilTag = AprilTagProcessor.Builder()
            .setDrawTagOutline(true)
            .setDrawAxes(true)
            .build()

        portal = VisionPortal.Builder()
            .setCamera(hardwareMap.get(WebcamName::class.java, WEBCAM_NAME))
            .setCameraResolution(Size(640, 480))
            .addProcessor(aprilTag)
            .build()

        // Chasis opcional: en banco no existe y no pasa nada.
        follower = runCatching { Constants.create(hardwareMap) }.getOrNull()
        telemetry.addLine(if (follower != null) "Chasis detectado: comparando vs Pinpoint" else "Sin chasis: solo vision")
        telemetry.update()
    }

    override fun loop() {
        follower?.update()

        val detections = aprilTag.detections
        telemetry.addData("tags visibles", detections.size)
        telemetry.addData("FPS camara", "%.0f".format(portal.fps))

        for (d in detections) {
            if (d.metadata != null && d.ftcPose != null) {
                telemetry.addLine(
                    "ID %d  range=%.1f in  bearing=%.1f°  yaw=%.1f°  margin=%.0f".format(
                        d.id, d.ftcPose.range, d.ftcPose.bearing, d.ftcPose.yaw, d.decisionMargin
                    )
                )
                // Pose del robot resuelta por el SDK (requiere setCameraPose en el
                // builder y tag en la libreria). Convertida a coordenadas Pedro:
                val rp = d.robotPose
                if (rp != null) {
                    val pedro = Field.ftcToPedro(
                        rp.position.toUnit(DistanceUnit.INCH).x,
                        rp.position.toUnit(DistanceUnit.INCH).y,
                        rp.orientation.getYaw(AngleUnit.RADIANS),
                    )
                    telemetry.addLine(
                        "  visionPose (Pedro): x=%.1f y=%.1f h=%.0f°".format(
                            pedro.x(), pedro.y(), Math.toDegrees(pedro.heading())
                        )
                    )
                }
            } else {
                telemetry.addLine("ID %d (sin metadata - no esta en la libreria)".format(d.id))
            }
        }

        follower?.let {
            val p = it.pose()
            telemetry.addLine(
                "pinpointPose:       x=%.1f y=%.1f h=%.0f°".format(
                    p.x(), p.y(), Math.toDegrees(p.heading())
                )
            )
        }

        telemetry.update()
    }

    override fun stop() {
        portal.close()
    }
}
