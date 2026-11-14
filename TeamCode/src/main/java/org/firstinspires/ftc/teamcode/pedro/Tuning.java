package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;

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

    // Driving solo usa el drivetrain. Localization/Odometry usan también el Pinpoint.
    // El algoritmo queda en null hasta el siguiente paso de tuning.
    @Tuner
    public static Procedure tests() {
        return new Tests(Constants::drivetrain, Constants::localizer, null);
    }
}
