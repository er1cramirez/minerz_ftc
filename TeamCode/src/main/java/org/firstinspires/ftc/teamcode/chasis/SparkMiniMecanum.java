package org.firstinspires.ftc.teamcode.chasis;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.utils.Utils;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Drivetrain mecanum para motores conectados con REV SPARKmini (PWM en puertos de servo).
 *
 * Usa la misma matemática que com.pedropathing.revhub.drivetrains.Mecanum (Pedro 3.0.2),
 * pero con DcMotorSimple en lugar de DcMotorEx.
 *
 * Diferencias con el Mecanum normal:
 *  - El modo brake/coast NO se controla por software. Pon el interruptor de cada SPARKmini en COAST:
 *    Pedro sigue trayectorias en coast y frena aplicando potencia (así mide el Foresight Tuner).
 *  - Por lo anterior, config.manualBrakeMode se ignora.
 *  - No hay lectura de corriente (currentAmps) porque el SPARKmini no la reporta.
 *
 * En la configuración del robot, cada SPARKmini se agrega en un puerto de servo
 * como "REV SPARKmini Controller".
 */
public class SparkMiniMecanum implements Drivetrain {
    private static final int FL = 0;
    private static final int FR = 1;
    private static final int BL = 2;
    private static final int BR = 3;

    public final MecanumConfig config;
    public final double[] wheelPowers = new double[4];

    private final DcMotorSimple[] motors;
    private final double[] lastPowers = new double[4];
    private final double powerThreshold;

    private double powerScale = 1.0;
    private DrivePowers drivePowers = DrivePowers.zero();

    public SparkMiniMecanum(HardwareMap map, MecanumConfig config) {
        this.config = config;
        this.powerThreshold = config.powerThreshold.get();

        motors = new DcMotorSimple[]{
                map.get(DcMotorSimple.class, config.frontLeftName.get()),
                map.get(DcMotorSimple.class, config.frontRightName.get()),
                map.get(DcMotorSimple.class, config.backLeftName.get()),
                map.get(DcMotorSimple.class, config.backRightName.get())
        };

        motors[FL].setDirection(config.frontLeftDirection.get());
        motors[FR].setDirection(config.frontRightDirection.get());
        motors[BL].setDirection(config.backLeftDirection.get());
        motors[BR].setDirection(config.backRightDirection.get());

        for (DcMotorSimple motor : motors) {
            motor.setPower(0);
        }
    }

    /** Solo escribe al hardware si la potencia cambió lo suficiente (igual que CachedMotor). */
    private void setMotorPower(int i, double power) {
        if (Double.isNaN(power) || Double.isInfinite(power)) return;

        double desired = Utils.clamp(power, -1, 1);
        boolean exceedsThreshold = Math.abs(lastPowers[i] - desired) >= powerThreshold;
        boolean switchesSigns = Math.signum(lastPowers[i]) != Math.signum(desired);

        if (exceedsThreshold || switchesSigns) {
            lastPowers[i] = desired;
            motors[i].setPower(desired);
        }
    }

    public void applyDrive(DrivePowers powers) {
        drivePowers = powers;

        double[] unnormalized = computeWheelPowersUnnormalized(powers);

        double maxPower = 1.0;
        for (double power : unnormalized) {
            maxPower = Math.max(maxPower, Math.abs(power));
        }
        powerScale = 1.0 / maxPower;

        for (int i = 0; i < unnormalized.length; i++) {
            wheelPowers[i] = unnormalized[i] / maxPower;
            setMotorPower(i, wheelPowers[i]);
        }
    }

    public double[] computeWheelPowersUnnormalized(DrivePowers powers) {
        double forward = powers.forward();
        double strafe = powers.strafe();
        double turn = powers.turn();

        double[] result = new double[4];
        result[FL] = forward - strafe - turn;
        result[FR] = forward + strafe + turn;
        result[BL] = forward + strafe - turn;
        result[BR] = forward - strafe + turn;
        return result;
    }

    @Override
    public double maxScaling(DrivePowers current, DrivePowers delta) {
        double lambda = 1.0;

        double[] currentPowers = computeWheelPowersUnnormalized(current);
        double[] deltaPowers = computeWheelPowersUnnormalized(delta);

        for (int i = 0; i < 4; i++) {
            double a = currentPowers[i];
            double b = deltaPowers[i];

            if (Math.abs(b) < 1e-9) continue;

            double t1 = (1.0 - a) / b;
            double t2 = (-1.0 - a) / b;

            if (t1 >= 0.0 && t1 < lambda) lambda = t1;
            if (t2 >= 0.0 && t2 < lambda) lambda = t2;
        }

        return Utils.clamp(lambda, 0.0, 1.0);
    }

    @Override
    public void drive(DrivePowers powers, boolean manual) {
        // brake/coast lo decide el interruptor físico del SPARKmini
        applyDrive(powers);
    }

    @Override
    public void stop() {
        stop(true);
    }

    @Override
    public void stop(boolean brake) {
        // brake/coast lo decide el interruptor físico del SPARKmini; aquí solo se apagan los motores
        for (int i = 0; i < motors.length; i++) {
            setMotorPower(i, 0);
        }
    }

    @Override
    public Map<String, Object> debug() {
        Map<String, Object> map = new HashMap<>();
        map.put("forward", drivePowers.forward());
        map.put("strafe", drivePowers.strafe());
        map.put("turn", drivePowers.turn());
        map.put("powerScale", powerScale);
        map.put("leftFrontWheelPower", wheelPowers[FL]);
        map.put("rightFrontWheelPower", wheelPowers[FR]);
        map.put("leftBackWheelPower", wheelPowers[BL]);
        map.put("rightBackWheelPower", wheelPowers[BR]);
        return map;
    }

    @Override
    public double interpolateVelocity(double xRadius, double yRadius, double theta) {
        return 1.0 / (Math.abs(Math.cos(theta)) / xRadius + Math.abs(Math.sin(theta)) / yRadius);
    }
}
