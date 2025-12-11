package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.seattlesolvers.solverslib.hardware.motors.MotorEx;

import org.firstinspires.ftc.teamcode.constants.ShooterConstants;

/**
 * Subsystem que controla el flywheel shooter.
 *
 * IMPORTANTE: El motor necesita recibir setVelocity() cada ciclo
 * para mantener control estable (igual que en FlywheelTuning).
 */
public class ShooterSubsystem extends SubsystemBase {

    public enum ShooterState {
        STOPPED,
        WARMING_UP,
        IDLE,
        SPIN_UP_CLOSE,
        SPIN_UP_FAR
    }

    // ===== HARDWARE =====
    private final MotorEx flywheel;

    // ===== ESTADO =====
    private ShooterState currentState;
    private double targetRpm;

    // ===== CONSTRUCTOR =====
    public ShooterSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, ShooterConstants.FLYWHEEL_NAME);
    }

    public ShooterSubsystem(HardwareMap hardwareMap, String flywheelName) {
        flywheel = new MotorEx(
                hardwareMap,
                flywheelName,
                (int) ShooterConstants.CPR,
                ShooterConstants.MAX_RPM
        );

        flywheel.setRunMode(MotorEx.RunMode.VelocityControl);
        flywheel.setVeloCoefficients(
                ShooterConstants.kP,
                ShooterConstants.kI,
                ShooterConstants.kD
        );
        flywheel.setFeedforwardCoefficients(
                ShooterConstants.kF,
                ShooterConstants.kV
        );

        currentState = ShooterState.STOPPED;
        targetRpm = 0.0;
    }

    // ===== MÉTODOS DE ACCIÓN (solo cambian estado interno) =====

    public void stop() {
        currentState = ShooterState.STOPPED;
        targetRpm = 0.0;
    }

    public void startWarmUp() {
        currentState = ShooterState.WARMING_UP;
        targetRpm = ShooterConstants.WARMUP_VELOCITY;
    }

    public void idle() {
        currentState = ShooterState.IDLE;
        targetRpm = ShooterConstants.IDLE_VELOCITY;
    }

    public void spinUpClose() {
        currentState = ShooterState.SPIN_UP_CLOSE;
        targetRpm = ShooterConstants.CLOSE_VELOCITY;
    }

    public void spinUpFar() {
        currentState = ShooterState.SPIN_UP_FAR;
        targetRpm = ShooterConstants.FAR_VELOCITY;
    }

    // ===== MÉTODOS DE CONSULTA =====

    public ShooterState getState() {
        return currentState;
    }

    public String getStateName() {
        return currentState.name();
    }

    public boolean isStopped() {
        return currentState == ShooterState.STOPPED;
    }

    public boolean isIdle() {
        return currentState == ShooterState.IDLE;
    }

    public boolean isWarmingUp() {
        return currentState == ShooterState.WARMING_UP;
    }

    public boolean isSpinningUp() {
        return currentState == ShooterState.SPIN_UP_CLOSE
                || currentState == ShooterState.SPIN_UP_FAR;
    }

    public boolean isReady() {
        if (!isSpinningUp()) {
            return false;
        }

        double error = Math.abs(targetRpm - getCurrentRpm());
        return error <= ShooterConstants.VELOCITY_TOLERANCE;
    }

    public double getCurrentRpm() {
        return flywheel.getVelocity() * ShooterConstants.COUNTS_TO_RPM;
    }

    public double getTargetRpm() {
        return targetRpm;
    }

    public double getVelocityError() {
        return targetRpm - getCurrentRpm();
    }

    public double getErrorPercent() {
        if (targetRpm == 0) return 0;
        return Math.abs(getVelocityError() / targetRpm * 100.0);
    }

    // ===== PERIODIC - APLICA LA VELOCIDAD CADA CICLO =====
    @Override
    public void periodic() {
        // Exactamente como en FlywheelTuning y TestFlywheelDirect
        double countsPerSecond = targetRpm * ShooterConstants.RPM_TO_COUNTS;
        flywheel.setVelocity(countsPerSecond);
    }
}
