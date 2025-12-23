package org.firstinspires.ftc.teamcode.opmodes.tuning;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.constants.TurretConstants;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;

/**
 * OpMode para probar y tunear el TurretSubsystem.
 * Versión SIN FTC Dashboard - usa solo telemetría estándar.
 * 
 * CONTROLES:
 * ═══════════════════════════════════════════════════════
 * Gamepad 1:
 * - Left Stick X    : Control manual de la torreta
 * - A               : Ir a posición 0° (frente)
 * - B               : Ir a posición +45° (izquierda)
 * - X               : Ir a posición -45° (derecha)
 * - Y               : Ir a posición +90° (izquierda completa)
 * - DPAD UP         : Incrementar target +10°
 * - DPAD DOWN       : Decrementar target -10°
 * - DPAD LEFT       : Incrementar target +1° (fino)
 * - DPAD RIGHT      : Decrementar target -1° (fino)
 * - Left Bumper     : Establecer posición actual como HOME
 * - Right Bumper    : Parar y resetear a IDLE
 * - Start           : Resetear encoder
 * 
 * Gamepad 2 (TUNING PID EN TIEMPO REAL):
 * - DPAD UP         : Incrementar kP (+0.005)
 * - DPAD DOWN       : Decrementar kP (-0.005)
 * - DPAD RIGHT      : Incrementar kD (+0.001)
 * - DPAD LEFT       : Decrementar kD (-0.001)
 * - A               : Incrementar kF (+0.005)
 * - B               : Decrementar kF (-0.005)
 * - Y               : Reset PID a valores por defecto
 * - Left Bumper     : Guardar valores actuales (los imprime)
 * ═══════════════════════════════════════════════════════
 */
@Disabled
@TeleOp(name = "Turret Tuning (No Dashboard)", group = "Tuning")
public class TurretTuning extends OpMode {
    
    // ==================== CONFIGURACIÓN DE TUNING ====================
    private static final double TEST_POSITION_1 = 0.0;
    private static final double TEST_POSITION_2 = 45.0;
    private static final double TEST_POSITION_3 = -45.0;
    private static final double TEST_POSITION_4 = 90.0;
    
    private static final double MANUAL_DEADZONE = 0.1;
    
    // Incrementos para tuning
    private static final double KP_INCREMENT = 0.005;
    private static final double KD_INCREMENT = 0.001;
    private static final double KF_INCREMENT = 0.005;
    
    // Valores por defecto para reset
    private static final double DEFAULT_KP = 0.02;
    private static final double DEFAULT_KI = 0.0;
    private static final double DEFAULT_KD = 0.0;
    private static final double DEFAULT_KF = 0.0;
    
    // ==================== SUBSYSTEM ====================
    private TurretSubsystem turret;
    
    // ==================== UTILIDADES ====================
    private ElapsedTime loopTimer;
    private double loopTimeMs;
    
    // Para detectar cambios de botón - Gamepad 1
    private boolean lastAPressed = false;
    private boolean lastBPressed = false;
    private boolean lastXPressed = false;
    private boolean lastYPressed = false;
    private boolean lastDpadUpPressed = false;
    private boolean lastDpadDownPressed = false;
    private boolean lastDpadLeftPressed = false;
    private boolean lastDpadRightPressed = false;
    private boolean lastLBPressed = false;
    private boolean lastRBPressed = false;
    private boolean lastStartPressed = false;
    
    // Para detectar cambios de botón - Gamepad 2 (PID tuning)
    private boolean lastG2DpadUpPressed = false;
    private boolean lastG2DpadDownPressed = false;
    private boolean lastG2DpadLeftPressed = false;
    private boolean lastG2DpadRightPressed = false;
    private boolean lastG2APressed = false;
    private boolean lastG2BPressed = false;
    private boolean lastG2YPressed = false;
    private boolean lastG2LBPressed = false;
    
    // Target manual para ajuste fino
    private double manualTargetDeg = 0.0;
    
    // Historial de errores para análisis
    private double maxError = 0.0;
    private double settlingStartTime = -1;
    private double settlingTime = 0.0;
    private boolean wasAtTarget = false;
    
    @Override
    public void init() {
        // Inicializar subsystem
        turret = new TurretSubsystem(hardwareMap);
        
        // Inicializar timer
        loopTimer = new ElapsedTime();
        
        // Telemetría de inicialización
        telemetry.addLine("╔═══════════════════════════════════════╗");
        telemetry.addLine("║   TURRET TUNING MODE (No Dashboard)   ║");
        telemetry.addLine("╠═══════════════════════════════════════╣");
        telemetry.addLine("║ GP1: Control torreta                  ║");
        telemetry.addLine("║ GP2: Ajuste PID en tiempo real        ║");
        telemetry.addLine("╠═══════════════════════════════════════╣");
        telemetry.addLine("║ GP1 LStick X = Manual | LB = Set Home ║");
        telemetry.addLine("║ GP1 A=0° | B=+45° | X=-45° | Y=90°    ║");
        telemetry.addLine("║ GP1 DPAD ↑↓=±10° | ←→=±1°             ║");
        telemetry.addLine("╠═══════════════════════════════════════╣");
        telemetry.addLine("║ GP2 DPAD ↑↓=kP | ←→=kD | AB=kF        ║");
        telemetry.addLine("║ GP2 Y=Reset PID | LB=Imprimir valores ║");
        telemetry.addLine("╚═══════════════════════════════════════╝");
        telemetry.update();
    }
    
    @Override
    public void init_loop() {
        // Mostrar posición actual durante init
        telemetry.addData("Raw Encoder", "%.1f ticks", turret.getRawEncoderTicks());
        telemetry.addData("Posición", "%.2f°", turret.getCurrentAngleDeg());
        telemetry.addLine();
        telemetry.addLine("Posiciona la torreta en HOME y presiona START");
        telemetry.update();
    }
    
    @Override
    public void start() {
        loopTimer.reset();
        // Establecer posición actual como home al iniciar
        turret.setCurrentPositionAsHome();
        manualTargetDeg = 0.0;
        maxError = 0.0;
    }
    
    @Override
    public void loop() {
        // Medir tiempo de loop
        loopTimeMs = loopTimer.milliseconds();
        loopTimer.reset();
        
        // Procesar controles
        handleGamepad1Controls();
        handleGamepad2PIDTuning();
        
        // Actualizar subsystem (ejecuta el control PID si está en modo POSITION)
        turret.periodic();
        
        // Actualizar métricas de análisis
        updateAnalytics();
        
        // Actualizar telemetría
        updateTelemetry();
    }
    
    /**
     * Procesa controles del Gamepad 1 (control de torreta).
     */
    private void handleGamepad1Controls() {
        // ===== CONTROL MANUAL =====
        double manualInput = -gamepad1.left_stick_x;  // Invertir si es necesario
        if (Math.abs(manualInput) > MANUAL_DEADZONE) {
            turret.setManualPower(manualInput);
            manualTargetDeg = turret.getCurrentAngleDeg();
            resetAnalytics();
        } else if (turret.getState() == TurretSubsystem.TurretState.MANUAL) {
            turret.setTargetPosition(turret.getCurrentAngleDeg());
        }
        
        // ===== POSICIONES PREDEFINIDAS =====
        if (gamepad1.a && !lastAPressed) {
            turret.setTargetPosition(TEST_POSITION_1);
            manualTargetDeg = TEST_POSITION_1;
            resetAnalytics();
        }
        lastAPressed = gamepad1.a;
        
        if (gamepad1.b && !lastBPressed) {
            turret.setTargetPosition(TEST_POSITION_2);
            manualTargetDeg = TEST_POSITION_2;
            resetAnalytics();
        }
        lastBPressed = gamepad1.b;
        
        if (gamepad1.x && !lastXPressed) {
            turret.setTargetPosition(TEST_POSITION_3);
            manualTargetDeg = TEST_POSITION_3;
            resetAnalytics();
        }
        lastXPressed = gamepad1.x;
        
        if (gamepad1.y && !lastYPressed) {
            turret.setTargetPosition(TEST_POSITION_4);
            manualTargetDeg = TEST_POSITION_4;
            resetAnalytics();
        }
        lastYPressed = gamepad1.y;
        
        // ===== AJUSTE FINO CON DPAD =====
        if (gamepad1.dpad_up && !lastDpadUpPressed) {
            manualTargetDeg += 10.0;
            turret.setTargetPosition(manualTargetDeg);
            resetAnalytics();
        }
        lastDpadUpPressed = gamepad1.dpad_up;
        
        if (gamepad1.dpad_down && !lastDpadDownPressed) {
            manualTargetDeg -= 10.0;
            turret.setTargetPosition(manualTargetDeg);
            resetAnalytics();
        }
        lastDpadDownPressed = gamepad1.dpad_down;
        
        if (gamepad1.dpad_left && !lastDpadLeftPressed) {
            manualTargetDeg += 1.0;
            turret.setTargetPosition(manualTargetDeg);
            resetAnalytics();
        }
        lastDpadLeftPressed = gamepad1.dpad_left;
        
        if (gamepad1.dpad_right && !lastDpadRightPressed) {
            manualTargetDeg -= 1.0;
            turret.setTargetPosition(manualTargetDeg);
            resetAnalytics();
        }
        lastDpadRightPressed = gamepad1.dpad_right;
        
        // ===== CONTROLES DE SISTEMA =====
        if (gamepad1.left_bumper && !lastLBPressed) {
            turret.setCurrentPositionAsHome();
            manualTargetDeg = 0.0;
        }
        lastLBPressed = gamepad1.left_bumper;
        
        if (gamepad1.right_bumper && !lastRBPressed) {
            turret.stop();
        }
        lastRBPressed = gamepad1.right_bumper;
        
        if (gamepad1.start && !lastStartPressed) {
            turret.resetHome();
            manualTargetDeg = 0.0;
        }
        lastStartPressed = gamepad1.start;
    }
    
    /**
     * Procesa controles del Gamepad 2 (tuning PID en tiempo real).
     */
    private void handleGamepad2PIDTuning() {
        // DPAD UP -> Incrementar kP
        if (gamepad2.dpad_up && !lastG2DpadUpPressed) {
            TurretConstants.kP += KP_INCREMENT;
            resetAnalytics();
        }
        lastG2DpadUpPressed = gamepad2.dpad_up;
        
        // DPAD DOWN -> Decrementar kP
        if (gamepad2.dpad_down && !lastG2DpadDownPressed) {
            TurretConstants.kP = Math.max(0, TurretConstants.kP - KP_INCREMENT);
            resetAnalytics();
        }
        lastG2DpadDownPressed = gamepad2.dpad_down;
        
        // DPAD RIGHT -> Incrementar kD
        if (gamepad2.dpad_right && !lastG2DpadRightPressed) {
            TurretConstants.kD += KD_INCREMENT;
            resetAnalytics();
        }
        lastG2DpadRightPressed = gamepad2.dpad_right;
        
        // DPAD LEFT -> Decrementar kD
        if (gamepad2.dpad_left && !lastG2DpadLeftPressed) {
            TurretConstants.kD = Math.max(0, TurretConstants.kD - KD_INCREMENT);
            resetAnalytics();
        }
        lastG2DpadLeftPressed = gamepad2.dpad_left;
        
        // A -> Incrementar kF
        if (gamepad2.a && !lastG2APressed) {
            TurretConstants.kF += KF_INCREMENT;
            resetAnalytics();
        }
        lastG2APressed = gamepad2.a;
        
        // B -> Decrementar kF
        if (gamepad2.b && !lastG2BPressed) {
            TurretConstants.kF = Math.max(0, TurretConstants.kF - KF_INCREMENT);
            resetAnalytics();
        }
        lastG2BPressed = gamepad2.b;
        
        // Y -> Reset PID a valores por defecto
        if (gamepad2.y && !lastG2YPressed) {
            TurretConstants.kP = DEFAULT_KP;
            TurretConstants.kI = DEFAULT_KI;
            TurretConstants.kD = DEFAULT_KD;
            TurretConstants.kF = DEFAULT_KF;
            resetAnalytics();
        }
        lastG2YPressed = gamepad2.y;
        
        // Left Bumper -> Imprimir valores actuales (para copiar al código)
        if (gamepad2.left_bumper && !lastG2LBPressed) {
            System.out.println("===== TURRET PID VALUES =====");
            System.out.println("public static double kP = " + TurretConstants.kP + ";");
            System.out.println("public static double kI = " + TurretConstants.kI + ";");
            System.out.println("public static double kD = " + TurretConstants.kD + ";");
            System.out.println("public static double kF = " + TurretConstants.kF + ";");
            System.out.println("=============================");
        }
        lastG2LBPressed = gamepad2.left_bumper;
    }
    
    /**
     * Actualiza métricas para análisis de respuesta.
     */
    private void updateAnalytics() {
        double currentError = Math.abs(turret.getPositionErrorDeg());
        
        // Trackear error máximo
        if (currentError > maxError) {
            maxError = currentError;
        }
        
        // Medir tiempo de asentamiento
        boolean atTarget = turret.isAtTarget();
        if (!wasAtTarget && atTarget) {
            // Acaba de llegar al target
            if (settlingStartTime > 0) {
                settlingTime = (System.currentTimeMillis() - settlingStartTime) / 1000.0;
            }
        } else if (wasAtTarget && !atTarget) {
            // Salió del target (overshoot)
            settlingStartTime = System.currentTimeMillis();
        }
        wasAtTarget = atTarget;
    }
    
    /**
     * Resetea las métricas de análisis.
     */
    private void resetAnalytics() {
        maxError = Math.abs(turret.getPositionErrorDeg());
        settlingStartTime = System.currentTimeMillis();
        settlingTime = 0.0;
        wasAtTarget = false;
    }
    
    /**
     * Actualiza la telemetría en el Driver Station.
     */
    private void updateTelemetry() {
        // ===== ESTADO =====
        telemetry.addLine("═══════ ESTADO ═══════");
        telemetry.addData("Estado", turret.getStateName());
        telemetry.addData("Homed", turret.isHomed() ? "✓" : "✗");
        
        // ===== POSICIÓN =====
        telemetry.addLine();
        telemetry.addLine("═══════ POSICIÓN ═══════");
        telemetry.addData("Actual", "%.2f°", turret.getCurrentAngleDeg());
        telemetry.addData("Target", "%.2f°", turret.getTargetAngleDeg());
        telemetry.addData("Error", "%.2f°", turret.getPositionErrorDeg());
        telemetry.addData("En Target", turret.isAtTarget() ? "✓" : "✗");
        
        // ===== MOTOR =====
        telemetry.addLine();
        telemetry.addLine("═══════ MOTOR ═══════");
        telemetry.addData("Power", "%.3f", turret.getMotorPower());
        telemetry.addData("Velocidad", "%.1f °/s", turret.getVelocityDegPerSec());
        
        // ===== PID (editable con GP2) =====
        telemetry.addLine();
        telemetry.addLine("═══════ PID (GP2 para editar) ═══════");
        telemetry.addData("kP (↑↓)", "%.4f", TurretConstants.kP);
        telemetry.addData("kD (←→)", "%.4f", TurretConstants.kD);
        telemetry.addData("kF (AB)", "%.4f", TurretConstants.kF);
        
        // ===== ANÁLISIS =====
        telemetry.addLine();
        telemetry.addLine("═══════ ANÁLISIS ═══════");
        telemetry.addData("Max Error", "%.2f°", maxError);
        telemetry.addData("Settling Time", "%.2f s", settlingTime);
        
        // ===== SISTEMA =====
        telemetry.addLine();
        telemetry.addData("Loop", "%.1f ms", loopTimeMs);
        
        telemetry.update();
    }
    
    @Override
    public void stop() {
        turret.stop();
    }
}
