package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.chasis.SparkMiniMecanum;

/**
 * Valores y hardware del chasis.
 *
 * Chasis SPARK: motores goBILDA conectados con REV SPARKmini (puertos de servo).
 * Interruptor de cada SPARKmini en BRAKE. manualBrakeMode no aplica.
 *
 * Los tuners generan el código: pega SOLO el contenido de "c -> { ... }" en el bloque que corresponde.
 */
public class ChasisConfig {

    // Resultado del Mecanum Tuner
    public static MecanumConfig drivetrainConfig = new MecanumConfig(c -> {
        c.frontLeftName.set("left_front");
        c.frontRightName.set("right_front");
        c.backLeftName.set("left_back");
        c.backRightName.set("right_back");
        c.frontLeftDirection.set(DcMotorSimple.Direction.FORWARD);
        c.frontRightDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backLeftDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backRightDirection.set(DcMotorSimple.Direction.FORWARD);
    });

    // Resultado del Foresight Tuner (correr DESPUÉS de Mecanum y Pinpoint)
    // Valores de ejemplo de la documentación: reemplázalos con los de tu chasis.
    public static ForesightConfig foresightConfig = new ForesightConfig(c -> {
        Controller primaryTranslationalForward = Controller.proportional(0.3);
        Controller secondaryTranslationalForward = Controller.proportional(0.1);
        Controller primaryTranslationalLateral = Controller.proportional(0.3);
        Controller secondaryTranslationalLateral = Controller.proportional(0.1);

        c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
        c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

        c.coast.set(Controller.proportionalFeedforward(0.01));
        c.brake.set(Controller.proportionalFeedforward(0.01));

        c.headingFeedback.set(Controller.proportional(5.0));
        c.headingBrakeCoefficients.set(Vector2D.cartesian(0.05, 0.005));

        c.linearBrakeCoefficients.set(Matrix.diag(0.1, 0.1));
        c.quadraticBrakeCoefficients.set(Matrix.diag(0.001, 0.001));

        c.maxAchievableForwardVelocity.set(60.0);
        c.maxAchievableStrafeVelocity.set(45.0);
        c.naturalForwardDeceleration.set(80.0);
        c.naturalStrafeDeceleration.set(100.0);
    });

    // Resultado del Pinpoint Tuner
    public static PinpointConfig localizerConfig = new PinpointConfig(c -> {
        c.name.set("pinpoint");
        c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        c.xPodOffset.set(0.0);
        c.yPodOffset.set(0.0);
        c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        c.globalDistanceUnit.set(DistanceUnit.INCH);
        c.offsetUnits.set(DistanceUnit.INCH);
    });

    // Este chasis usa SPARKmini
    public static Drivetrain drivetrain(HardwareMap h) {
        return new SparkMiniMecanum(h, drivetrainConfig);
    }
}
