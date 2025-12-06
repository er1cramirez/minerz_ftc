package org.firstinspires.ftc.teamcode;

import com.seattlesolvers.solverslib.hardware.motors.MotorEx;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.List;

/**
 * OpMode SIMPLE de DEBUG para ajustar flywheel
 *
 * CONTROLES:
 * - A: Encender/Apagar motor
 * - DPAD_UP: Subir velocidad (+100 RPM)
 * - DPAD_DOWN: Bajar velocidad (-100 RPM)
 * - DPAD_LEFT: Bajar kP (-0.5)
 * - DPAD_RIGHT: Subir kP (+0.5)
 * - LEFT_BUMPER: Subir velocidad rápido (+500 RPM)
 * - RIGHT_BUMPER: Bajar velocidad rápido (-500 RPM)
 * - X: Reset kP a valor inicial
 * - Y: Velocidad máxima (5000 RPM)
 * - B: Velocidad a 0
 */
@TeleOp(name = "Flywheel Debug Simple", group = "Debug")
public class FlywheelDebugSimple extends LinearOpMode {

    private MotorEx flywheel;
    private GamepadEx gamepadEx;

    // Control
    private boolean motorOn = false;
    private double targetVelocity = 0.0;
    private double currentVelocity = 0.0;

    // Modo de operación
    private boolean rawMode = false;  // false = VelocityControl, true = RawPower
    private double rawPower = 0.0;    // Power directo (0.0 - 1.0)
    private boolean showDiagnostics = true; // Mostrar diagnóstico de encoder

    // Parámetros PID (para counts/s, no RPM)
    private double kP = 0.1;       // Valor inicial - mucho menor que para RPM
    private static final double kI = 0.0;
    private static final double kD = 0.0;
    private static final double kF = 0.0;
    private static final double kV = 0.00017;  // Para counts/s (≈ 1/max_counts_per_sec)
    // Si max es 2800 counts/s: kV ≈ 1/2800 = 0.00036

    // Conversión de encoder
    private static final double CPR = 28.0;
    private static final double COUNTS_PER_SEC_TO_RPM = 60.0 / CPR;  // 2.1429
    private static final double RPM_TO_COUNTS_PER_SEC = CPR / 60.0;  // 0.4667

    // Tracking de máximos
    private double maxVelocityReached = 0.0;
    private double maxVelocityAtFullPower = 0.0;

    // Datos raw del encoder para verificación
    private int currentPosition = 0;
    private int previousPosition = 0;
    private long currentTime = 0;
    private long previousTime = 0;
    private double calculatedRPM = 0.0;
    private double encoderVelocity = 0.0; // Velocity raw (counts/sec o ticks/sec)

    // Incrementos
    private static final double VELOCITY_INCREMENT_SMALL = 100.0;  // RPM por press
    private static final double VELOCITY_INCREMENT_LARGE = 500.0;  // RPM por press
    private static final double KP_INCREMENT = 0.01;  // Incremento para kP (counts/s units)

    // Velocidad máxima
    private static final double MAX_VELOCITY = 5500.0;

    // Timer para modo "hold" (mantener presionado)
    private ElapsedTime holdTimer = new ElapsedTime();
    private static final double HOLD_THRESHOLD = 0.3; // segundos antes de activar modo hold
    private static final double HOLD_REPEAT_RATE = 0.1; // segundos entre incrementos en modo hold

    @Override
    public void runOpMode() throws InterruptedException {
        // Inicializar GamepadEx
        gamepadEx = new GamepadEx(gamepad1);

        // Configurar motor Rev Hex (28 CPR, 6000 RPM)
        flywheel = new MotorEx(hardwareMap, "flywheel", 28, 6000);
        flywheel.setRunMode(MotorEx.RunMode.VelocityControl);
        flywheel.setVeloCoefficients(kP, kI, kD);
        flywheel.setFeedforwardCoefficients(kF, kV);

        // Descomentar si el motor gira al revés
        // flywheel.setInverted(true);

        // Configurar bulk caching
        List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
        hubs.forEach(hub -> hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL));

        telemetry.addLine("=== FLYWHEEL DEBUG SIMPLE ===");
        telemetry.addLine("A: ON/OFF");
        telemetry.addLine("DPAD ↑/↓: Velocidad ±100 RPM");
        telemetry.addLine("LB/RB: Velocidad ±500 RPM");
        telemetry.addLine("DPAD ←/→: kP ±0.01");
        telemetry.addLine("X: Reset kP | Y: Max vel | B: Stop");
        telemetry.addLine();
        telemetry.addLine("=== MODO RAW (sin PID) ===");
        telemetry.addLine("LEFT_STICK_BUTTON: Toggle Raw Mode");
        telemetry.addLine("RIGHT_STICK_BUTTON: Reset Máximos");
        telemetry.addLine("RIGHT_TRIGGER: Power + (en raw mode)");
        telemetry.addLine("LEFT_TRIGGER: Power - (en raw mode)");
        telemetry.addLine();
        telemetry.addLine("=== DIAGNÓSTICO ===");
        telemetry.addLine("BACK: Toggle diagnóstico encoder");
        telemetry.addLine();
        telemetry.addLine("Presiona START");
        telemetry.update();

        waitForStart();
        holdTimer.reset();

        while (!isStopRequested() && opModeIsActive()) {
            // Limpiar cache
            hubs.forEach(LynxModule::clearBulkCache);

            // IMPORTANTE: Leer botones PRIMERO
            gamepadEx.readButtons();

            // === CONTROLES CON EDGE DETECTION ===

            // Toggle Raw Mode (cambiar entre VelocityControl y RawPower)
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.LEFT_STICK_BUTTON)) {
                rawMode = !rawMode;

                if (rawMode) {
                    // Cambiar a modo raw power
                    flywheel.setRunMode(MotorEx.RunMode.RawPower);
                    rawPower = 0.0;
                } else {
                    // Volver a velocity control
                    flywheel.setRunMode(MotorEx.RunMode.VelocityControl);
                    flywheel.setVeloCoefficients(kP, kI, kD);
                    flywheel.setFeedforwardCoefficients(kF, kV);
                }

                gamepad1.rumble(200); // Feedback
            }

            // Reset max velocities
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.RIGHT_STICK_BUTTON)) {
                maxVelocityReached = 0.0;
                maxVelocityAtFullPower = 0.0;
                gamepad1.rumble(100);
            }

            // Toggle diagnostics display
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.BACK)) {
                showDiagnostics = !showDiagnostics;
            }

            // Encender/Apagar motor (solo cuando se presiona, no cuando se mantiene)
            if (gamepadEx.wasJustPressed(GamepadKeys.Button.A)) {
                motorOn = !motorOn;
                if (!motorOn) {
                    targetVelocity = 0;
                    rawPower = 0.0;
                }
            }

            // === CONTROLES SEGÚN MODO ===

            if (rawMode) {
                // *** MODO RAW POWER ***
                // Controlar power directamente con triggers
                if (motorOn) {
                    // Usar triggers para ajustar power
                    double leftTrigger = gamepad1.left_trigger;
                    double rightTrigger = gamepad1.right_trigger;

                    if (rightTrigger > 0.1) {
                        // Aumentar power
                        rawPower = rightTrigger; // Usar trigger como power directo
                    } else if (leftTrigger > 0.1) {
                        // Disminuir power (o usar trigger inverso)
                        rawPower = Math.max(0, rawPower - leftTrigger * 0.01);
                    }

                    // También permitir ajuste con DPAD
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_UP)) {
                        rawPower = Math.min(rawPower + 0.05, 1.0);
                        holdTimer.reset();
                    }
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_DOWN)) {
                        rawPower = Math.max(rawPower - 0.05, 0.0);
                        holdTimer.reset();
                    }

                    // Modo "hold" para DPAD
                    if (gamepadEx.isDown(GamepadKeys.Button.DPAD_UP) && holdTimer.seconds() > HOLD_THRESHOLD) {
                        rawPower = Math.min(rawPower + 0.05, 1.0);
                        sleep((long)(HOLD_REPEAT_RATE * 1000));
                    }
                    if (gamepadEx.isDown(GamepadKeys.Button.DPAD_DOWN) && holdTimer.seconds() > HOLD_THRESHOLD) {
                        rawPower = Math.max(rawPower - 0.05, 0.0);
                        sleep((long)(HOLD_REPEAT_RATE * 1000));
                    }

                    // Y = Full power
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.Y)) {
                        rawPower = 1.0;
                    }

                    // B = Stop
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.B)) {
                        rawPower = 0.0;
                    }
                }

            } else {
                // *** MODO VELOCITY CONTROL ***
                // Solo permitir ajustes si el motor está encendido
                if (motorOn) {
                    // Ajustar velocidad - incremento pequeño
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_UP)) {
                        targetVelocity = Math.min(targetVelocity + VELOCITY_INCREMENT_SMALL, MAX_VELOCITY);
                        holdTimer.reset();
                    }
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_DOWN)) {
                        targetVelocity = Math.max(targetVelocity - VELOCITY_INCREMENT_SMALL, 0);
                        holdTimer.reset();
                    }

                    // Modo "hold" - si se mantiene presionado más de HOLD_THRESHOLD
                    if (gamepadEx.isDown(GamepadKeys.Button.DPAD_UP) && holdTimer.seconds() > HOLD_THRESHOLD) {
                        targetVelocity = Math.min(targetVelocity + VELOCITY_INCREMENT_SMALL, MAX_VELOCITY);
                        sleep((long)(HOLD_REPEAT_RATE * 1000));
                    }
                    if (gamepadEx.isDown(GamepadKeys.Button.DPAD_DOWN) && holdTimer.seconds() > HOLD_THRESHOLD) {
                        targetVelocity = Math.max(targetVelocity - VELOCITY_INCREMENT_SMALL, 0);
                        sleep((long)(HOLD_REPEAT_RATE * 1000));
                    }

                    // Ajustar velocidad - incremento grande
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.LEFT_BUMPER)) {
                        targetVelocity = Math.min(targetVelocity + VELOCITY_INCREMENT_LARGE, MAX_VELOCITY);
                    }
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.RIGHT_BUMPER)) {
                        targetVelocity = Math.max(targetVelocity - VELOCITY_INCREMENT_LARGE, 0);
                    }

                    // Velocidad máxima
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.Y)) {
                        targetVelocity = MAX_VELOCITY;
                    }

                    // Detener (velocidad = 0)
                    if (gamepadEx.wasJustPressed(GamepadKeys.Button.B)) {
                        targetVelocity = 0;
                    }
                }

                // Ajustar kP (funciona siempre en velocity mode, incluso con motor apagado)
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_RIGHT)) {
                    kP += KP_INCREMENT;
                    flywheel.setVeloCoefficients(kP, kI, kD);
                    holdTimer.reset();
                }
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.DPAD_LEFT)) {
                    kP = Math.max(0, kP - KP_INCREMENT);
                    flywheel.setVeloCoefficients(kP, kI, kD);
                    holdTimer.reset();
                }

                // Modo "hold" para kP
                if (gamepadEx.isDown(GamepadKeys.Button.DPAD_RIGHT) && holdTimer.seconds() > HOLD_THRESHOLD) {
                    kP += KP_INCREMENT;
                    flywheel.setVeloCoefficients(kP, kI, kD);
                    sleep((long)(HOLD_REPEAT_RATE * 1000));
                }
                if (gamepadEx.isDown(GamepadKeys.Button.DPAD_LEFT) && holdTimer.seconds() > HOLD_THRESHOLD) {
                    kP = Math.max(0, kP - KP_INCREMENT);
                    flywheel.setVeloCoefficients(kP, kI, kD);
                    sleep((long)(HOLD_REPEAT_RATE * 1000));
                }

                // Reset kP
                if (gamepadEx.wasJustPressed(GamepadKeys.Button.X)) {
                    kP = 0.1;
                    flywheel.setVeloCoefficients(kP, kI, kD);
                }

            } // Fin del else - velocity control mode

            // === CONTROL DEL MOTOR ===
            if (motorOn) {
                if (rawMode) {
                    // Modo Raw Power
                    flywheel.set(rawPower);
                } else {
                    // Modo Velocity Control - convertir RPM a counts/s
                    flywheel.setVelocity(targetVelocity * RPM_TO_COUNTS_PER_SEC);
                }
            } else {
                flywheel.stopMotor();
            }

            // Leer velocidad actual y convertir de counts/s a RPM
            currentVelocity = flywheel.getVelocity() * COUNTS_PER_SEC_TO_RPM;

            // Leer datos raw del encoder
            currentPosition = flywheel.motor.getCurrentPosition();
            currentTime = System.nanoTime();

            // Obtener velocity en counts/s (raw)
            encoderVelocity = flywheel.getVelocity();

            // Calcular RPM manualmente para verificar
            if (previousTime != 0) {
                double deltaTime = (currentTime - previousTime) / 1e9; // convertir a segundos
                double deltaCounts = currentPosition - previousPosition;

                if (deltaTime > 0) {
                    double countsPerSecond = deltaCounts / deltaTime;
                    double revolutionsPerSecond = countsPerSecond / 28.0; // CPR = 28
                    calculatedRPM = revolutionsPerSecond * 60.0;
                }
            }

            previousPosition = currentPosition;
            previousTime = currentTime;

            // Actualizar máximo alcanzado
            if (currentVelocity > maxVelocityReached) {
                maxVelocityReached = currentVelocity;
            }

            // Trackear máximo a full power en modo raw
            if (rawMode && rawPower > 0.95 && currentVelocity > maxVelocityAtFullPower) {
                maxVelocityAtFullPower = currentVelocity;
            }

            // === TELEMETRÍA ===
            telemetry.addLine("=== ESTADO ===");
            telemetry.addData("Motor", motorOn ? "🟢 ON" : "⚫ OFF");
            telemetry.addData("Modo", rawMode ? "⚡ RAW POWER" : "🎯 VELOCITY CONTROL");
            telemetry.addLine();

            if (rawMode) {
                // *** TELEMETRÍA MODO RAW ***
                telemetry.addLine("=== MODO RAW POWER ===");
                telemetry.addData("Power", "%.2f (%.0f%%)", rawPower, rawPower * 100);
                telemetry.addData("Velocidad Actual", "%.0f RPM", currentVelocity);
                telemetry.addData("Max @ Full Power", "%.0f RPM", maxVelocityAtFullPower);
                telemetry.addLine();
                telemetry.addLine("💡 Usa RT para aumentar power");
                telemetry.addLine("💡 Presiona Y para full power (1.0)");
                telemetry.addLine("💡 O usa DPAD ↑/↓ para ±0.05");

            } else {
                // *** TELEMETRÍA VELOCITY CONTROL ***
                telemetry.addLine("=== VELOCIDAD (RPM) ===");
                telemetry.addData("Target", "%.0f", targetVelocity);
                telemetry.addData("Actual", "%.0f", currentVelocity);
                telemetry.addData("Error", "%.0f RPM", targetVelocity - currentVelocity);

                // Calcular error porcentual
                double errorPercent = 0;
                if (targetVelocity > 0) {
                    errorPercent = Math.abs((targetVelocity - currentVelocity) / targetVelocity * 100);
                }
                telemetry.addData("Error %", "%.1f%%", errorPercent);
                telemetry.addLine();

                telemetry.addLine("=== PARÁMETROS PID ===");
                telemetry.addData("kP", "%.3f", kP);
                telemetry.addData("kV", "%.5f (fijo)", kV);
                telemetry.addLine();

                // Indicador visual simple
                String status;
                if (!motorOn) {
                    status = "⚫ Apagado";
                } else if (errorPercent < 2) {
                    status = "🟢 En target (<2%)";
                } else if (errorPercent < 5) {
                    status = "🟡 Cerca (2-5%)";
                } else {
                    status = "🔴 Lejos (>5%)";
                }
                telemetry.addData("Status", status);
                telemetry.addLine();
            }

            // Información general (ambos modos)
            telemetry.addLine("=== VELOCIDADES MÁXIMAS ===");
            telemetry.addData("Max Alcanzado", "%.0f RPM", maxVelocityReached);
            telemetry.addData("Max @ Full Power", "%.0f RPM", maxVelocityAtFullPower);
            telemetry.addData("Teórico (6000 RPM)", maxVelocityAtFullPower > 0 ?
                    String.format("%.1f%% del teórico", (maxVelocityAtFullPower / 6000.0) * 100) : "N/A");
            telemetry.addLine();

            // *** DIAGNÓSTICO DE ENCODER (togglable con BACK) ***
            if (showDiagnostics) {
                telemetry.addLine("=== DIAGNÓSTICO ENCODER ===");
                telemetry.addData("Posición Encoder", currentPosition);
                telemetry.addData("Velocity (raw)", "%.2f counts/s", encoderVelocity);
                telemetry.addData("Velocity convertida", "%.0f RPM", currentVelocity);
                telemetry.addData("RPM Manual", "%.0f RPM", calculatedRPM);

                // Verificar conversión
                telemetry.addData("Factor usado", "%.4f", COUNTS_PER_SEC_TO_RPM);
                telemetry.addData("Factor esperado", "2.1429 (60/28)");
                telemetry.addLine();

                // Ayuda de interpretación
                if (encoderVelocity != 0) {
                    double convertedRPM = encoderVelocity * COUNTS_PER_SEC_TO_RPM;
                    telemetry.addLine("💡 Conversión:");
                    telemetry.addLine(String.format("   %.0f counts/s × 2.1429 = %.0f RPM",
                            encoderVelocity, convertedRPM));
                }
                telemetry.addLine();
                telemetry.addData("BACK", "Ocultar diagnóstico");
            } else {
                telemetry.addData("BACK", "Mostrar diagnóstico encoder");
            }
            telemetry.addLine();

            // Controles
            telemetry.addLine("=== CONTROLES ===");
            telemetry.addData("LSB", "Toggle modo (Raw/Velocity)");
            telemetry.addData("RSB", "Reset máximos");

            if (rawMode) {
                telemetry.addData("RT", "Power +");
                telemetry.addData("LT", "Power -");
                telemetry.addData("DPAD ↑/↓", "Power ±0.05");
                telemetry.addData("Y", "Full Power (1.0)");
                telemetry.addData("B", "Stop (0.0)");
            } else {
                telemetry.addData("A", "ON/OFF");
                telemetry.addData("DPAD ↑/↓", "±100 RPM");
                telemetry.addData("LB/RB", "±500 RPM");
                telemetry.addData("DPAD ←/→", "kP ±0.01");
                telemetry.addData("Y", "Max (5500)");
                telemetry.addData("B", "Stop (0)");
                telemetry.addData("X", "Reset kP");
            }

            telemetry.update();
        }

        // Apagar motor al salir
        flywheel.stopMotor();
    }
}