package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.robotcore.hardware.Gamepad.LedEffect;
import com.qualcomm.robotcore.hardware.Gamepad.RumbleEffect;

public class UserGamepadFeedback {

    public static final RumbleEffect test = new RumbleEffect.Builder()
        .addStep(0.5, 0.5, 100)
        .addStep(0,0,50)
        .addStep(1, 1, 200).build();

    public static final LedEffect readyToShootLedEff = new LedEffect.Builder()
            .addStep(0,1,0,200)
            .addStep(1, 1, 1, 100)
            .addStep(0, 1, 0, 100).build();
}
