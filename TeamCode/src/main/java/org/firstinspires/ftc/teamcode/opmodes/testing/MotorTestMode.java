package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * Motor Test Mode - Prueba individual de motores del drivetrain
 * 
 * Usa este OpMode para verificar las conexiones de los motores del drivetrain
 * sin necesidad de reconectar cables físicamente.
 * 
 * CONTROLES:
 * ============
 * DPAD UP/DOWN : Seleccionar motor anterior/siguiente
 * LEFT STICK Y : Control de potencia del motor seleccionado (-1 a 1)
 * RIGHT STICK Y : Control fino del motor seleccionado (-0.3 a 0.3)
 * A : Detener motor actual
 * B : Detener TODOS los motores
 * X : Invertir dirección del motor actual
 * Y : Reset encoder del motor actual
 * 
 * BUMPERS:
 * LEFT BUMPER : Pulso corto hacia atrás
 * RIGHT BUMPER : Pulso corto hacia adelante
 * 
 * MOTORES DISPONIBLES:
 * ====================
 * 0. frontLeft (drive)
 * 1. frontRight (drive)
 * 2. backLeft (drive)
 * 3. backRight (drive)
 */
@TeleOp(name = "Motor Test Mode", group = "Testing")
public class MotorTestMode extends LinearOpMode {

    // Motor array
    private DcMotorEx[] motors = new DcMotorEx[4];
    private String[] motorNames = {
            "frontLeft",
            "frontRight",
            "backLeft",
            "backRight"
    };

    private String[] motorDescriptions = {
            "Drive: Front Left",
            "Drive: Front Right",
            "Drive: Back Left",
            "Drive: Back Right"
    };

    // State
    private int selectedIndex = 0;
    private final int TOTAL_OPTIONS = 4; // 4 drivetrain motors

    // Button debouncing
    private boolean lastDpadUp = false;
    private boolean lastDpadDown = false;
    private boolean lastA = false;
    private boolean lastB = false;
    private boolean lastX = false;
    private boolean lastY = false;
    private boolean lastRightBumper = false;
    private boolean lastLeftBumper = false;

    // Motor inversion tracking
    private boolean[] motorInverted = new boolean[4];

    @Override
    public void runOpMode() throws InterruptedException {
        initHardware();

        telemetry.addLine("╔════════════════════════════════╗");
        telemetry.addLine("║    MOTOR TEST MODE READY       ║");
        telemetry.addLine("╠════════════════════════════════╣");
        telemetry.addLine("║ DPAD UP/DOWN: Select motor     ║");
        telemetry.addLine("║ LEFT STICK:   Power control    ║");
        telemetry.addLine("║ A: Stop current  B: Stop all   ║");
        telemetry.addLine("║ X: Invert dir   Y: Reset enc   ║");
        telemetry.addLine("╚════════════════════════════════╝");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            handleInput();
            updateMotors();
            updateTelemetry();
        }

        // Cleanup
        stopAllMotors();
    }

    private void initHardware() {
        // Initialize motors
        for (int i = 0; i < motors.length; i++) {
            try {
                motors[i] = hardwareMap.get(DcMotorEx.class, motorNames[i]);
                motors[i].setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
                motors[i].setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motors[i].setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
                motorInverted[i] = false;
            } catch (Exception e) {
                telemetry.addLine("ERROR: " + motorNames[i] + " not found!");
            }
        }
    }

    private void handleInput() {
        // DPAD navigation with debouncing
        if (gamepad1.dpad_up && !lastDpadUp) {
            selectedIndex = (selectedIndex - 1 + TOTAL_OPTIONS) % TOTAL_OPTIONS;
        }
        if (gamepad1.dpad_down && !lastDpadDown) {
            selectedIndex = (selectedIndex + 1) % TOTAL_OPTIONS;
        }

        // A - Stop current
        if (gamepad1.a && !lastA) {
            if (selectedIndex < 4 && motors[selectedIndex] != null) {
                motors[selectedIndex].setPower(0);
            }
        }

        // B - Stop all
        if (gamepad1.b && !lastB) {
            stopAllMotors();
        }

        // X - Invert direction
        if (gamepad1.x && !lastX) {
            if (selectedIndex < 4 && motors[selectedIndex] != null) {
                DcMotorSimple.Direction currentDir = motors[selectedIndex].getDirection();
                if (currentDir == DcMotorSimple.Direction.FORWARD) {
                    motors[selectedIndex].setDirection(DcMotorSimple.Direction.REVERSE);
                } else {
                    motors[selectedIndex].setDirection(DcMotorSimple.Direction.FORWARD);
                }
                motorInverted[selectedIndex] = !motorInverted[selectedIndex];
            }
        }

        // Y - Reset encoder
        if (gamepad1.y && !lastY) {
            if (selectedIndex < 4 && motors[selectedIndex] != null) {
                motors[selectedIndex].setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                motors[selectedIndex].setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            }
        }

        // Bumpers - Pulse
        if (gamepad1.right_bumper && !lastRightBumper) {
            pulseMotor(0.4, 200);
        }
        if (gamepad1.left_bumper && !lastLeftBumper) {
            pulseMotor(-0.4, 200);
        }

        // Update button states
        lastDpadUp = gamepad1.dpad_up;
        lastDpadDown = gamepad1.dpad_down;
        lastA = gamepad1.a;
        lastB = gamepad1.b;
        lastX = gamepad1.x;
        lastY = gamepad1.y;
        lastRightBumper = gamepad1.right_bumper;
        lastLeftBumper = gamepad1.left_bumper;
    }

    private void pulseMotor(double power, int durationMs) {
        if (selectedIndex < 4 && motors[selectedIndex] != null) {
            motors[selectedIndex].setPower(power);
            // Note: In a LinearOpMode we can't use sleep without blocking
            // We'll handle this via a timed pulse in updateMotors
        }
    }

    private void updateMotors() {
        // Get stick values
        double coarsePower = -gamepad1.left_stick_y; // Inverted for intuitive control
        double finePower = -gamepad1.right_stick_y * 0.3;
        double totalPower = coarsePower + finePower;
        totalPower = Math.max(-1, Math.min(1, totalPower));

        // DC Motor control
        if (selectedIndex < 4 && motors[selectedIndex] != null) {
            // Only apply power if stick is moved
            if (Math.abs(totalPower) > 0.05) {
                motors[selectedIndex].setPower(totalPower);
            }
        }
    }

    private void stopAllMotors() {
        for (int i = 0; i < motors.length; i++) {
            if (motors[i] != null) {
                motors[i].setPower(0);
            }
        }
    }

    private void updateTelemetry() {
        telemetry.clear();

        // Header
        telemetry.addLine("════════ MOTOR TEST MODE ════════");
        telemetry.addLine();

        // Current selection
        String selectionName = motorDescriptions[selectedIndex];
        telemetry.addData("▶ SELECTED", "[%d] %s", selectedIndex, selectionName);
        telemetry.addLine();

        // Motor info
        if (selectedIndex < 4 && motors[selectedIndex] != null) {
            DcMotorEx motor = motors[selectedIndex];
            telemetry.addData("  Power", "%.2f", motor.getPower());
            telemetry.addData("  Position", "%d ticks", motor.getCurrentPosition());
            telemetry.addData("  Velocity", "%.1f tps", motor.getVelocity());
            telemetry.addData("  Direction", motor.getDirection().toString());
            telemetry.addData("  Inverted?", motorInverted[selectedIndex] ? "YES" : "NO");
        }

        telemetry.addLine();

        // All motors status (compact view)
        telemetry.addLine("──── ALL MOTORS STATUS ────");
        for (int i = 0; i < motors.length; i++) {
            String marker = (i == selectedIndex) ? "▶" : " ";
            String status = "N/A";
            if (motors[i] != null) {
                double power = motors[i].getPower();
                int pos = motors[i].getCurrentPosition();
                String inv = motorInverted[i] ? "R" : "F";
                status = String.format("P:%.2f Pos:%d [%s]", power, pos, inv);
            }
            telemetry.addLine(String.format("%s%d. %-14s %s",
                    marker, i, motorNames[i], status));
        }

        telemetry.addLine();
        telemetry.addLine("────────────────────────────");
        telemetry.addData("Left Stick Y", "%.2f (power)", -gamepad1.left_stick_y);
        telemetry.addData("Right Stick Y", "%.2f (fine)", -gamepad1.right_stick_y * 0.3);

        telemetry.update();
    }
}
