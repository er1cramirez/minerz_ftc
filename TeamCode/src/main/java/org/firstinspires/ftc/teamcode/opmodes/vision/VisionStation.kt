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
import org.firstinspires.ftc.vision.apriltag.AprilTagClusterDetection
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.firstinspires.ftc.vision.apriltag.AprilTagSingleDetection

/**
 * Estacion de visión (OpMode plano del SDK: NO necesita chasis ni tuning).
 *
 * Qué hace:
 *  1. Detecta AprilTags con VisionPortal y muestra por detección: range,
 *     bearing y yaw. SDK 12 entrega dos tipos de detección: tag individual
 *     (AprilTagSingleDetection, con id) y cluster de tags
 *     (AprilTagClusterDetection, con el % de tags del cluster encontrados).
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
            when (d) {
                // Tag individual (SDK 12): id y metadata viven en la subclase.
                is AprilTagSingleDetection -> {
                    if (d.metadata != null) {
                        telemetry.addLine("ID %d (%s)".format(d.id, d.metadata.name))
                        addPoseLines(d)
                    } else {
                        telemetry.addLine(
                            "ID %d (sin metadata, no esta en la libreria)  centro=(%.0f, %.0f) px"
                                .format(d.id, d.center.x, d.center.y)
                        )
                    }
                }
                // Cluster de tags (p. ej. los tags de una cell): una sola pose
                // estimada con varios tags. Base natural del "multitag".
                is AprilTagClusterDetection -> {
                    telemetry.addLine(
                        "Cluster %s  (%d%% de sus tags encontrados)"
                            .format(d.metadata.name, d.percentClusterFound)
                    )
                    addPoseLines(d)
                }
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

    /** Range/bearing/yaw y, si el SDK la resolvio, la pose del robot en coordenadas Pedro. */
    private fun addPoseLines(d: AprilTagDetection) {
        val p = d.ftcPose ?: return
        telemetry.addLine(
            "  range=%.1f in  bearing=%.1f°  yaw=%.1f°".format(p.range, p.bearing, p.yaw)
        )
        // Requiere setCameraPose en el builder y el tag/cluster en la libreria.
        val rp = d.robotPose ?: return
        val pos = rp.position.toUnit(DistanceUnit.INCH)
        val pedro = Field.ftcToPedro(pos.x, pos.y, rp.orientation.getYaw(AngleUnit.RADIANS))
        telemetry.addLine(
            "  visionPose (Pedro): x=%.1f y=%.1f h=%.0f°".format(
                pedro.x(), pedro.y(), Math.toDegrees(pedro.heading())
            )
        )
    }

    override fun stop() {
        portal.close()
    }
}
