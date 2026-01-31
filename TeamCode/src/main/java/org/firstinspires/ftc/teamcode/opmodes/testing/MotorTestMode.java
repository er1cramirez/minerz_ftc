package org.firstinspires.ftc.teamcode.opmodes.testing;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.DigitalChannel;

import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.constants.IntakeConstants;
import org.firstinspires.ftc.teamcode.constants.FlywheelConstants;
import org.firstinspires.ftc.teamcode.constants.TurretConstants;
import org.firstinspires.ftc.teamcode.constants.SpindexerConstants;
import org.firstinspires.ftc.teamcode.constants.EjectorConstants;

/**
 * Motor Test Mode - Prueba individual de cada motor
 * 
 * Usa este OpMode para verificar las conexiones de los motores sin
 * necesidad de reconectar cables físicamente.
 * 
 * CONTROLES:
 * ============
 * DPAD UP/DOWN    : Seleccionar motor anterior/siguiente
 * LEFT STICK Y    : Control de potencia del motor seleccionado (-1 a 1)
 * RIGHT STICK Y   : Control fino del motor seleccionado (-0.3 a 0.3)
 * A               : Detener motor actual
 * B               : Detener TODOS los motores
 * X               : Invertir dirección del motor actual
 * Y               : Reset encoder del motor actual
 * 
 * BUMPERS:
 * LEFT BUMPER     : Pulso corto hacia atrás
 * RIGHT BUMPER    : Pulso corto hacia adelante
 * 
 * MOTORES DISPONIBLES:
 * ====================
 * 0. frontLeft (drive)
 * 1. backLeft (drive)
 * 2. frontRight (drive)
 * 3. backRight (drive)
 * 4. intakeMotor
 * 5. flywheelMotor
 * 6. turretMotor
 * 7. spindexMotor
 * 8. ejector (SERVO)
 * 
 * NOTA: El servo ejector se controla de forma especial (posición 0-1)
 */
@TeleOp(name = "Motor Test Mode", group = "Testing")
public class MotorTestMode extends LinearOpMode {
    
    // Motor array
    private DcMotorEx[] motors = new DcMotorEx[8];
    private String[] motorNames = {
        DriveConstants.LEFT_FRONT,    // 0
        DriveConstants.LEFT_REAR,     // 1
        DriveConstants.RIGHT_FRONT,   // 2
        DriveConstants.RIGHT_REAR,    // 3
        IntakeConstants.MOTOR_NAME,   // 4
        FlywheelConstants.MOTOR_NAME, // 5
        TurretConstants.MOTOR_NAME,   // 6
        SpindexerConstants.MOTOR_NAME // 7
    };
    
    private String[] motorDescriptions = {
        "Drive: Front Left",
        "Drive: Back Left", 
        "Drive: Front Right",
        "Drive: Back Right",
        "Intake Motor",
        "Flywheel Motor",
        "Turret Motor",
        "Spindexer Motor"
    };
    
    // Servo
    private Servo ejectorServo;
    
    // Limit switch for spindexer
    private DigitalChannel spindexerLimit;
    
    // State
    private int selectedIndex = 0;
    private final int TOTAL_OPTIONS = 9; // 8 motors + 1 servo
    private double servoPosition = 0.5;
    
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
    private boolean[] motorInverted = new boolean[8];
    
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
        
        // Initialize servo
        try {
            ejectorServo = hardwareMap.get(Servo.class, EjectorConstants.SERVO_NAME);
            servoPosition = ejectorServo.getPosition();
        } catch (Exception e) {
            telemetry.addLine("ERROR: ejector servo not found!");
        }
        
        // Initialize limit switch
        try {
            spindexerLimit = hardwareMap.get(DigitalChannel.class, SpindexerConstants.LIMIT_SWITCH_NAME);
            spindexerLimit.setMode(DigitalChannel.Mode.INPUT);
        } catch (Exception e) {
            telemetry.addLine("WARN: spindexer limit switch not found");
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
            if (selectedIndex < 8 && motors[selectedIndex] != null) {
                motors[selectedIndex].setPower(0);
            } else if (selectedIndex == 8 && ejectorServo != null) {
                servoPosition = 0.5;
            }
        }
        
        // B - Stop all
        if (gamepad1.b && !lastB) {
            stopAllMotors();
        }
        
        // X - Invert direction
        if (gamepad1.x && !lastX) {
            if (selectedIndex < 8 && motors[selectedIndex] != null) {
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
            if (selectedIndex < 8 && motors[selectedIndex] != null) {
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
        if (selectedIndex < 8 && motors[selectedIndex] != null) {
            motors[selectedIndex].setPower(power);
            // Note: In a LinearOpMode we can't use sleep without blocking
            // We'll handle this via a timed pulse in updateMotors
        } else if (selectedIndex == 8 && ejectorServo != null) {
            servoPosition = power > 0 ? 1.0 : 0.0;
        }
    }
    
    private void updateMotors() {
        // Get stick values
        double coarsePower = -gamepad1.left_stick_y;  // Inverted for intuitive control
        double finePower = -gamepad1.right_stick_y * 0.3;
        double totalPower = coarsePower + finePower;
        totalPower = Math.max(-1, Math.min(1, totalPower));
        
        if (selectedIndex < 8) {
            // DC Motor control
            if (motors[selectedIndex] != null) {
                // Only apply power if stick is moved
                if (Math.abs(totalPower) > 0.05) {
                    motors[selectedIndex].setPower(totalPower);
                }
            }
        } else {
            // Servo control
            if (ejectorServo != null) {
                // Servo position control with sticks
                if (Math.abs(coarsePower) > 0.05) {
                    servoPosition += coarsePower * 0.01;  // Slow movement
                    servoPosition = Math.max(0, Math.min(1, servoPosition));
                    ejectorServo.setPosition(servoPosition);
                }
            }
        }
    }
    
    private void stopAllMotors() {
        for (int i = 0; i < motors.length; i++) {
            if (motors[i] != null) {
                motors[i].setPower(0);
            }
        }
        if (ejectorServo != null) {
            servoPosition = 0.5;
            ejectorServo.setPosition(servoPosition);
        }
    }
    
    private void updateTelemetry() {
        telemetry.clear();
        
        // Header
        telemetry.addLine("════════ MOTOR TEST MODE ════════");
        telemetry.addLine();
        
        // Current selection
        String selectionName;
        if (selectedIndex < 8) {
            selectionName = motorDescriptions[selectedIndex];
        } else {
            selectionName = "Ejector (SERVO)";
        }
        telemetry.addData("▶ SELECTED", "[%d] %s", selectedIndex, selectionName);
        telemetry.addLine();
        
        // Motor/Servo info
        if (selectedIndex < 8 && motors[selectedIndex] != null) {
            DcMotorEx motor = motors[selectedIndex];
            telemetry.addData("  Power", "%.2f", motor.getPower());
            telemetry.addData("  Position", "%d ticks", motor.getCurrentPosition());
            telemetry.addData("  Velocity", "%.1f tps", motor.getVelocity());
            telemetry.addData("  Direction", motor.getDirection().toString());
            telemetry.addData("  Inverted?", motorInverted[selectedIndex] ? "YES" : "NO");
            
            // Special info for spindexer
            if (selectedIndex == 7 && spindexerLimit != null) {
                telemetry.addData("  Limit Switch", !spindexerLimit.getState() ? "PRESSED" : "open");
            }
        } else if (selectedIndex == 8 && ejectorServo != null) {
            telemetry.addData("  Position", "%.3f", servoPosition);
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
        
        // Servo status
        String servoMarker = (selectedIndex == 8) ? "▶" : " ";
        telemetry.addLine(String.format("%s8. %-14s Pos:%.2f", 
            servoMarker, "ejector", servoPosition));
        
        telemetry.addLine();
        telemetry.addLine("────────────────────────────");
        telemetry.addData("Left Stick Y", "%.2f (power)", -gamepad1.left_stick_y);
        telemetry.addData("Right Stick Y", "%.2f (fine)", -gamepad1.right_stick_y * 0.3);
        
        telemetry.update();
    }
}
