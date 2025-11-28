package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.seattlesolvers.solverslib.command.SubsystemBase;

/**
 * A gripper mechanism that grabs a stone from the quarry.
 * Centered around the Skystone game for FTC that was done in the 2019
 * to 2020 season.
 */
public class ArtifactFeeder extends SubsystemBase {

    private final Servo triggerServo;

    public ArtifactFeeder(final HardwareMap hMap, final String name) {
        triggerServo = hMap.get(Servo.class, name);
    }

    /**
     * Feeds a ball.
     */
    public void feed() {
        triggerServo.setPosition(0.76);
    }

    /**
     * Back to ball holding position.
     */
    public void stow() {
        triggerServo.setPosition(0);
    }

}