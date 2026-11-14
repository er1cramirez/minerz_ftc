package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;

import org.firstinspires.ftc.teamcode.pedro.procedures.ForesightTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.MecanumTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.PinpointTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.Tests;

/**
 * Asistentes de Pedro. Con el robot encendido y conectado a su WiFi abre:
 * http://192.168.43.1:10158
 */
public class Tuning {

    @Tuner
    public static Procedure mecanumTuner() {
        return new MecanumTuner();
    }

    @Tuner
    public static Procedure pinpointTuner() {
        return new PinpointTuner();
    }

    // Requiere Mecanum y Pinpoint ya tuneados en ChasisConfig
    @Tuner
    public static Procedure foresightTuner() {
        return new ForesightTuner(Constants::localizer, Constants::drivetrain);
    }

    // Driving: solo drivetrain. Localization/Odometry: + Pinpoint.
    // Hold/Line/Curve/Interpolation: + Foresight (requiere el Foresight Tuner).
    @Tuner
    public static Procedure tests() {
        return new Tests(Constants::drivetrain, Constants::localizer, Constants::algorithm);
    }
}
