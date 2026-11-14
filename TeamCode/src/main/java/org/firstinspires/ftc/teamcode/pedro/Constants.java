package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Algorithm;
import com.pedropathing.algorithm.Foresight;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Localizer;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Arma los componentes de Pedro a partir de ChasisConfig.
 * Este archivo es lógica común: se modifica solo en pedro-base.
 * Los valores de cada chasis van en ChasisConfig, en su rama de chasis.
 */
public class Constants {

    public static Drivetrain drivetrain(HardwareMap h) {
        return ChasisConfig.drivetrain(h);
    }

    public static Localizer localizer(HardwareMap h) {
        return new PinpointLocalizer(h, ChasisConfig.localizerConfig);
    }

    public static Algorithm algorithm() {
        return new Foresight(ChasisConfig.foresightConfig);
    }

    public static Follower create(HardwareMap h) {
        return new Follower(localizer(h), drivetrain(h), algorithm());
    }
}
