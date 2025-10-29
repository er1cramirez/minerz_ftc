package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.Motor;
import com.seattlesolvers.solverslib.hardware.motors.Motor.ZeroPowerBehavior;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

import org.firstinspires.ftc.teamcode.constants.IntakeConstants;
import org.firstinspires.ftc.teamcode.constants.RobotConstants;

/**
 * Subsystem for the intake mechanism.
 * Controls a single motor for intaking and outtaking game elements.
 */
public class IntakeSubsystem extends SubsystemBase {

    private final MotorEx intakeMotor;

    /**
     * Creates a new IntakeSubsystem.
     * 
     * @param hardwareMap The hardware map from the OpMode
     */
    public IntakeSubsystem(HardwareMap hardwareMap) {
        intakeMotor = new MotorEx(hardwareMap, 
                RobotConstants.HardwareNames.INTAKE_MOTOR, 
                Motor.GoBILDA.RPM_312);
        intakeMotor.setZeroPowerBehavior(ZeroPowerBehavior.BRAKE);
    }

    /**
     * Runs the intake to collect game elements.
     */
    public void intake() {
        intakeMotor.set(IntakeConstants.Power.INTAKE_POWER);
    }

    /**
     * Runs the intake in reverse to eject game elements.
     */
    public void outtake() {
        intakeMotor.set(IntakeConstants.Power.OUTTAKE_POWER);
    }

    /**
     * Stops the intake motor.
     */
    public void stop() {
        intakeMotor.set(0.0);
    }

    /**
     * Sets the intake motor power directly.
     * 
     * @param power The power to set (-1.0 to 1.0)
     */
    public void setPower(double power) {
        intakeMotor.set(power);
    }

    /**
     * Checks if the intake is currently running.
     * 
     * @return true if the motor power is not zero
     */
    public boolean isRunning() {
        return Math.abs(intakeMotor.get()) > 0.01;
    }
}
