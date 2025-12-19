package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.constants.FlywheelConstants;
import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;

/**
 * OpMode para probar y tunear el FlywheelSubsystem.
 * 
 * PROCESO DE TUNING RECOMENDADO:
 * ════════════════════════════════════════════════════════════
 * 
 * PASO 1: Tunear Feed-Forward (hace ~80% del trabajo)
 * ────────────────────────────────────────────────────────────
 * 1.1. Empezar con kS:
 *      - Presionar A para activar velocidad baja (2500 RPM)
 *      - Incrementar kS (GP2 DPAD UP) hasta que el motor apenas empiece a girar
 *      - Valor típico: 0.03 - 0.08
 * 
 * 1.2. Tunear kV:
 *      - Con kS establecido, el flywheel debería empezar a girar
 *      - Ajustar kV (GP2 DPAD LEFT/RIGHT) hasta que llegue cerca del target
 *      - El objetivo es que FF SOLO llegue a ~90-95% del target
 *      - Si hay overshoot con solo FF, reducir kV
 *      - Valor típico: 0.00012 - 0.00020
 * 
 * PASO 2: Tunear PID (corrección fina ~20%)
 * ────────────────────────────────────────────────────────────
 * 2.1. Tunear kP:
 *      - Con FF funcionando, debería haber un pequeño error residual
 *      - Incrementar kP (GP2 A/B) hasta eliminar ese error
 *      - Si oscila, reducir kP
 *      - Valor típico: 0.0002 - 0.0010
 * 
 * PASO 3: Verificar múltiples velocidades
 * ────────────────────────────────────────────────────────────
 *      - Probar todas las velocidades (A, B, X, Y)
 *      - Verificar que llegue a todas sin overshoot excesivo
 *      - Ajustar fino si es necesario
 * 
 * PASO 4: Verificar compensación de voltaje
 * ────────────────────────────────────────────────────────────
 *      - Observar el voltaje de batería en telemetría
 *      - Cuando la batería baje, verificar que la velocidad se mantenga
 * 
 * ════════════════════════════════════════════════════════════
 * 
 * CONTROLES GAMEPAD 1 (Control del flywheel):
 * ────────────────────────────────────────────────────────────
 * A              : Velocidad zona cercana mín (2500 RPM)
 * B              : Velocidad zona cercana máx (3200 RPM)
 * X              : Velocidad idle spin (2000 RPM)
 * Y              : Velocidad zona lejana (3600 RPM)
 * DPAD UP        : Incrementar target +100 RPM
 * DPAD DOWN      : Decrementar target -100 RPM
 * DPAD LEFT      : Incrementar target +25 RPM (fino)
 * DPAD RIGHT     : Decrementar target -25 RPM (fino)
 * Right Bumper   : STOP (apagar flywheel)
 * Left Bumper    : Toggle compensación de voltaje
 * 
 * CONTROLES GAMEPAD 2 (Tuning de constantes):
 * ────────────────────────────────────────────────────────────
 * DPAD UP        : Incrementar kS (+0.005)
 * DPAD DOWN      : Decrementar kS (-0.005)
 * DPAD RIGHT     : Incrementar kV (+0.00002)
 * DPAD LEFT      : Decrementar kV (-0.00002)
 * A              : Incrementar kP (+0.0001)
 * B              : Decrementar kP (-0.0001)
 * X              : Incrementar kA (+0.00005)
 * Y              : Reset constantes a valores default
 * Left Bumper    : Imprimir valores al log (para copiar)
 * Right Bumper   : Toggle entre vista simple/detallada
 * 
 * ════════════════════════════════════════════════════════════
 */
@TeleOp(name = "Flywheel Tuning", group = "Tuning")
public class FlywheelTuningOpMode extends OpMode {
    
    // ==================== INCREMENTOS PARA TUNING ====================
    private static final double KS_INCREMENT = 0.005;
    private static final double KV_INCREMENT = 0.00002;
    private static final double KA_INCREMENT = 0.00005;
    private static final double KP_INCREMENT = 0.0001;
    private static final double RPM_INCREMENT_COARSE = 100.0;
    private static final double RPM_INCREMENT_FINE = 25.0;
    
    // ==================== VALORES DEFAULT PARA RESET ====================
    private static final double DEFAULT_KS = 0.05;
    private static final double DEFAULT_KV = 0.00016;
    private static final double DEFAULT_KA = 0.0001;
    private static final double DEFAULT_KP = 0.0005;
    private static final double DEFAULT_KI = 0.0;
    private static final double DEFAULT_KD = 0.0;
    
    // ==================== SUBSYSTEM ====================
    private FlywheelSubsystem flywheel;
    
    // ==================== UTILIDADES ====================
    private ElapsedTime loopTimer;
    private double loopTimeMs;
    private boolean detailedView = true;
    
    // ==================== TARGET MANUAL ====================
    private double manualTargetRPM = 0;
    
    // ==================== ANÁLISIS ====================
    private double maxOvershoot = 0;
    private double spinUpStartTime = 0;
    private double lastSpinUpTime = 0;
    private boolean wasSpinningUp = false;
    
    // ==================== DETECCIÓN DE BOTONES - GP1 ====================
    private boolean lastG1APressed = false;
    private boolean lastG1BPressed = false;
    private boolean lastG1XPressed = false;
    private boolean lastG1YPressed = false;
    private boolean lastG1DpadUpPressed = false;
    private boolean lastG1DpadDownPressed = false;
    private boolean lastG1DpadLeftPressed = false;
    private boolean lastG1DpadRightPressed = false;
    private boolean lastG1RBPressed = false;
    private boolean lastG1LBPressed = false;
    
    // ==================== DETECCIÓN DE BOTONES - GP2 ====================
    private boolean lastG2DpadUpPressed = false;
    private boolean lastG2DpadDownPressed = false;
    private boolean lastG2DpadLeftPressed = false;
    private boolean lastG2DpadRightPressed = false;
    private boolean lastG2APressed = false;
    private boolean lastG2BPressed = false;
    private boolean lastG2XPressed = false;
    private boolean lastG2YPressed = false;
    private boolean lastG2LBPressed = false;
    private boolean lastG2RBPressed = false;
    
    @Override
    public void init() {
        // Inicializar subsystem
        flywheel = new FlywheelSubsystem(hardwareMap);
        
        // Inicializar timer
        loopTimer = new ElapsedTime();
        
        // Mostrar instrucciones
        telemetry.addLine("╔══════════════════════════════════════╗");
        telemetry.addLine("║       FLYWHEEL TUNING MODE           ║");
        telemetry.addLine("╠══════════════════════════════════════╣");
        telemetry.addLine("║ GP1: Control de velocidad            ║");
        telemetry.addLine("║ GP2: Ajuste de constantes            ║");
        telemetry.addLine("╠══════════════════════════════════════╣");
        telemetry.addLine("║ GP1: A=2500 B=3200 X=idle Y=3600    ║");
        telemetry.addLine("║ GP1: DPAD ↑↓=±100  ←→=±25 RPM       ║");
        telemetry.addLine("║ GP1: RB=STOP  LB=Toggle VoltComp    ║");
        telemetry.addLine("╠══════════════════════════════════════╣");
        telemetry.addLine("║ GP2: DPAD ↑↓=kS  ←→=kV              ║");
        telemetry.addLine("║ GP2: A/B=kP  X=kA  Y=Reset          ║");
        telemetry.addLine("║ GP2: LB=Print  RB=Toggle View       ║");
        telemetry.addLine("╚══════════════════════════════════════╝");
        telemetry.update();
    }
    
    @Override
    public void init_loop() {
        telemetry.addData("Batería", "%.2f V", flywheel.getBatteryVoltage());
        telemetry.addLine();
        telemetry.addLine("Presiona START para comenzar");
        telemetry.update();
    }
    
    @Override
    public void start() {
        loopTimer.reset();
        manualTargetRPM = 0;
    }
    
    @Override
    public void loop() {
        // Medir tiempo de loop
        loopTimeMs = loopTimer.milliseconds();
        loopTimer.reset();
        
        // Procesar controles
        handleGamepad1Controls();
        handleGamepad2Tuning();
        
        // Actualizar subsystem
        flywheel.periodic();
        
        // Actualizar análisis
        updateAnalytics();
        
        // Mostrar telemetría
        if (detailedView) {
            updateDetailedTelemetry();
        } else {
            updateSimpleTelemetry();
        }
    }
    
    /**
     * Controles del Gamepad 1 - Control del flywheel.
     */
    private void handleGamepad1Controls() {
        // A -> Velocidad zona cercana mínima
        if (gamepad1.a && !lastG1APressed) {
            manualTargetRPM = FlywheelConstants.NEAR_ZONE_MIN_RPM;
            flywheel.setTargetRPM(manualTargetRPM);
            resetAnalytics();
        }
        lastG1APressed = gamepad1.a;
        
        // B -> Velocidad zona cercana máxima
        if (gamepad1.b && !lastG1BPressed) {
            manualTargetRPM = FlywheelConstants.NEAR_ZONE_MAX_RPM;
            flywheel.setTargetRPM(manualTargetRPM);
            resetAnalytics();
        }
        lastG1BPressed = gamepad1.b;
        
        // X -> Idle spin
        if (gamepad1.x && !lastG1XPressed) {
            manualTargetRPM = FlywheelConstants.IDLE_SPIN_RPM;
            flywheel.setTargetRPM(manualTargetRPM);
            resetAnalytics();
        }
        lastG1XPressed = gamepad1.x;
        
        // Y -> Velocidad zona lejana
        if (gamepad1.y && !lastG1YPressed) {
            manualTargetRPM = FlywheelConstants.FAR_ZONE_RPM;
            flywheel.setTargetRPM(manualTargetRPM);
            resetAnalytics();
        }
        lastG1YPressed = gamepad1.y;
        
        // DPAD UP -> +100 RPM
        if (gamepad1.dpad_up && !lastG1DpadUpPressed) {
            manualTargetRPM += RPM_INCREMENT_COARSE;
            flywheel.setTargetRPM(manualTargetRPM);
        }
        lastG1DpadUpPressed = gamepad1.dpad_up;
        
        // DPAD DOWN -> -100 RPM
        if (gamepad1.dpad_down && !lastG1DpadDownPressed) {
            manualTargetRPM = Math.max(0, manualTargetRPM - RPM_INCREMENT_COARSE);
            flywheel.setTargetRPM(manualTargetRPM);
        }
        lastG1DpadDownPressed = gamepad1.dpad_down;
        
        // DPAD LEFT -> +25 RPM (fino)
        if (gamepad1.dpad_left && !lastG1DpadLeftPressed) {
            manualTargetRPM += RPM_INCREMENT_FINE;
            flywheel.setTargetRPM(manualTargetRPM);
        }
        lastG1DpadLeftPressed = gamepad1.dpad_left;
        
        // DPAD RIGHT -> -25 RPM (fino)
        if (gamepad1.dpad_right && !lastG1DpadRightPressed) {
            manualTargetRPM = Math.max(0, manualTargetRPM - RPM_INCREMENT_FINE);
            flywheel.setTargetRPM(manualTargetRPM);
        }
        lastG1DpadRightPressed = gamepad1.dpad_right;
        
        // Right Bumper -> STOP
        if (gamepad1.right_bumper && !lastG1RBPressed) {
            flywheel.stop();
            manualTargetRPM = 0;
            resetAnalytics();
        }
        lastG1RBPressed = gamepad1.right_bumper;
        
        // Left Bumper -> Toggle compensación de voltaje
        if (gamepad1.left_bumper && !lastG1LBPressed) {
            FlywheelConstants.VOLTAGE_COMPENSATION_ENABLED = 
                !FlywheelConstants.VOLTAGE_COMPENSATION_ENABLED;
        }
        lastG1LBPressed = gamepad1.left_bumper;
    }
    
    /**
     * Controles del Gamepad 2 - Tuning de constantes.
     */
    private void handleGamepad2Tuning() {
        // DPAD UP -> Incrementar kS
        if (gamepad2.dpad_up && !lastG2DpadUpPressed) {
            FlywheelConstants.kS += KS_INCREMENT;
            resetAnalytics();
        }
        lastG2DpadUpPressed = gamepad2.dpad_up;
        
        // DPAD DOWN -> Decrementar kS
        if (gamepad2.dpad_down && !lastG2DpadDownPressed) {
            FlywheelConstants.kS = Math.max(0, FlywheelConstants.kS - KS_INCREMENT);
            resetAnalytics();
        }
        lastG2DpadDownPressed = gamepad2.dpad_down;
        
        // DPAD RIGHT -> Incrementar kV
        if (gamepad2.dpad_right && !lastG2DpadRightPressed) {
            FlywheelConstants.kV += KV_INCREMENT;
            resetAnalytics();
        }
        lastG2DpadRightPressed = gamepad2.dpad_right;
        
        // DPAD LEFT -> Decrementar kV
        if (gamepad2.dpad_left && !lastG2DpadLeftPressed) {
            FlywheelConstants.kV = Math.max(0, FlywheelConstants.kV - KV_INCREMENT);
            resetAnalytics();
        }
        lastG2DpadLeftPressed = gamepad2.dpad_left;
        
        // A -> Incrementar kP
        if (gamepad2.a && !lastG2APressed) {
            FlywheelConstants.kP += KP_INCREMENT;
            resetAnalytics();
        }
        lastG2APressed = gamepad2.a;
        
        // B -> Decrementar kP
        if (gamepad2.b && !lastG2BPressed) {
            FlywheelConstants.kP = Math.max(0, FlywheelConstants.kP - KP_INCREMENT);
            resetAnalytics();
        }
        lastG2BPressed = gamepad2.b;
        
        // X -> Incrementar kA
        if (gamepad2.x && !lastG2XPressed) {
            FlywheelConstants.kA += KA_INCREMENT;
            resetAnalytics();
        }
        lastG2XPressed = gamepad2.x;
        
        // Y -> Reset a valores default
        if (gamepad2.y && !lastG2YPressed) {
            FlywheelConstants.kS = DEFAULT_KS;
            FlywheelConstants.kV = DEFAULT_KV;
            FlywheelConstants.kA = DEFAULT_KA;
            FlywheelConstants.kP = DEFAULT_KP;
            FlywheelConstants.kI = DEFAULT_KI;
            FlywheelConstants.kD = DEFAULT_KD;
            resetAnalytics();
        }
        lastG2YPressed = gamepad2.y;
        
        // Left Bumper -> Imprimir valores al log
        if (gamepad2.left_bumper && !lastG2LBPressed) {
            printValuesToLog();
        }
        lastG2LBPressed = gamepad2.left_bumper;
        
        // Right Bumper -> Toggle vista simple/detallada
        if (gamepad2.right_bumper && !lastG2RBPressed) {
            detailedView = !detailedView;
        }
        lastG2RBPressed = gamepad2.right_bumper;
    }
    
    /**
     * Actualiza métricas de análisis.
     */
    private void updateAnalytics() {
        // Detectar inicio de spin-up
        if (flywheel.isSpinningUp() && !wasSpinningUp) {
            spinUpStartTime = System.currentTimeMillis();
        }
        
        // Detectar fin de spin-up
        if (!flywheel.isSpinningUp() && wasSpinningUp) {
            lastSpinUpTime = (System.currentTimeMillis() - spinUpStartTime) / 1000.0;
        }
        
        wasSpinningUp = flywheel.isSpinningUp();
        
        // Trackear overshoot
        double error = flywheel.getVelocityError();
        if (error < 0) {  // Overshoot = error negativo
            maxOvershoot = Math.max(maxOvershoot, Math.abs(error));
        }
    }
    
    /**
     * Resetea las métricas de análisis.
     */
    private void resetAnalytics() {
        maxOvershoot = 0;
        spinUpStartTime = System.currentTimeMillis();
        lastSpinUpTime = 0;
    }
    
    /**
     * Imprime los valores actuales al log para copiar.
     */
    private void printValuesToLog() {
        System.out.println("═══════════════════════════════════════");
        System.out.println("     FLYWHEEL TUNING VALUES");
        System.out.println("═══════════════════════════════════════");
        System.out.println("// Feed-Forward");
        System.out.println("public static double kS = " + FlywheelConstants.kS + ";");
        System.out.println("public static double kV = " + FlywheelConstants.kV + ";");
        System.out.println("public static double kA = " + FlywheelConstants.kA + ";");
        System.out.println("");
        System.out.println("// PID");
        System.out.println("public static double kP = " + FlywheelConstants.kP + ";");
        System.out.println("public static double kI = " + FlywheelConstants.kI + ";");
        System.out.println("public static double kD = " + FlywheelConstants.kD + ";");
        System.out.println("═══════════════════════════════════════");
    }
    
    /**
     * Telemetría detallada para tuning.
     */
    private void updateDetailedTelemetry() {
        // Estado
        telemetry.addLine("═══════ ESTADO ═══════");
        telemetry.addData("Estado", flywheel.getStateName());
        telemetry.addData("Ready to Shoot", flywheel.isReadyToShoot() ? "✓ SÍ" : "✗ NO");
        
        // Velocidad
        telemetry.addLine();
        telemetry.addLine("═══════ VELOCIDAD ═══════");
        telemetry.addData("Actual", "%.0f RPM", flywheel.getCurrentRPM());
        telemetry.addData("Target", "%.0f RPM", flywheel.getTargetRPM());
        telemetry.addData("Error", "%.0f RPM", flywheel.getVelocityError());
        
        // Indicador visual de error
        double errorPercent = (flywheel.getTargetRPM() > 0) ? 
            (flywheel.getVelocityError() / flywheel.getTargetRPM()) * 100 : 0;
        telemetry.addData("Error %", "%.1f%%", errorPercent);
        
        // Control
        telemetry.addLine();
        telemetry.addLine("═══════ CONTROL ═══════");
        telemetry.addData("Motor Power", "%.3f", flywheel.getMotorPower());
        telemetry.addData("Feed-Forward", "%.3f", flywheel.getLastFeedForward());
        telemetry.addData("PID Output", "%.4f", flywheel.getLastPIDOutput());
        
        // Voltaje
        telemetry.addLine();
        telemetry.addLine("═══════ VOLTAJE ═══════");
        telemetry.addData("Batería", "%.2f V", flywheel.getBatteryVoltage());
        telemetry.addData("Compensación", "%.3f", flywheel.getVoltageCompensation());
        telemetry.addData("Comp. Enabled", FlywheelConstants.VOLTAGE_COMPENSATION_ENABLED ? "✓" : "✗");
        
        // Constantes (editable con GP2)
        telemetry.addLine();
        telemetry.addLine("═══════ FF (GP2 DPAD) ═══════");
        telemetry.addData("kS (↑↓)", "%.4f", FlywheelConstants.kS);
        telemetry.addData("kV (←→)", "%.6f", FlywheelConstants.kV);
        telemetry.addData("kA (X)", "%.6f", FlywheelConstants.kA);
        
        telemetry.addLine();
        telemetry.addLine("═══════ PID (GP2 A/B) ═══════");
        telemetry.addData("kP", "%.5f", FlywheelConstants.kP);
        
        // Análisis
        telemetry.addLine();
        telemetry.addLine("═══════ ANÁLISIS ═══════");
        telemetry.addData("Spin-up Time", "%.2f s", lastSpinUpTime);
        telemetry.addData("Max Overshoot", "%.0f RPM", maxOvershoot);
        
        // Sistema
        telemetry.addLine();
        telemetry.addData("Loop", "%.1f ms", loopTimeMs);
        
        telemetry.update();
    }
    
    /**
     * Telemetría simple para uso normal.
     */
    private void updateSimpleTelemetry() {
        telemetry.addData("Estado", flywheel.getStateName());
        telemetry.addData("RPM", "%.0f / %.0f", flywheel.getCurrentRPM(), flywheel.getTargetRPM());
        telemetry.addData("Error", "%.0f RPM", flywheel.getVelocityError());
        telemetry.addData("Ready", flywheel.isReadyToShoot() ? "✓" : "✗");
        telemetry.addLine();
        telemetry.addData("Power", "%.2f", flywheel.getMotorPower());
        telemetry.addData("Batería", "%.1f V", flywheel.getBatteryVoltage());
        telemetry.addLine();
        telemetry.addLine("GP2 RB para vista detallada");
        telemetry.update();
    }
    
    @Override
    public void stop() {
        flywheel.stop();
    }
}
