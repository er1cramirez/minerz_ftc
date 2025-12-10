package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.drive.TeleOpDriveCommand;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;

/**
 * TeleOp Principal - Ejemplo completo de uso del DriveSubsystem.
 * 
 * FEATURES:
 * ✅ Control con joysticks (left stick = movimiento, right stick = rotación)
 * ✅ Toggle slow mode (LB)
 * ✅ Toggle turbo mode (RB)
 * ✅ Toggle robot/field centric (Back button)
 * ✅ Telemetría con info del drive
 * 
 * CONTROLS:
 * - Left Stick Y/X: Forward/Strafe
 * - Right Stick X: Rotation
 * - Left Bumper (LB): Toggle Slow Mode
 * - Right Bumper (RB): Toggle Turbo Mode
 * - Back Button: Toggle Robot/Field Centric
 */
@TeleOp(name = "Main TeleOp", group = "Competition")
public class MainTeleOp extends CommandOpMode {
    
    // ===== SUBSYSTEMS =====
    private DriveSubsystem drive;
    
    // ===== COMMANDS =====
    private TeleOpDriveCommand driveCommand;
    
    // ===== GAMEPADS =====
    private GamepadEx driverGamepad;
    
    // ===== FOLLOWER =====
    private Follower follower;
    
    // ===== INITIALIZATION =====
    
    @Override
    public void initialize() {
        // Crear Follower
        follower = DriveConstants.createFollower(hardwareMap);
        
        // Inicializar subsystems
        drive = new DriveSubsystem(follower);
        
        // Inicializar gamepads
        driverGamepad = new GamepadEx(gamepad1);
        
        // Crear comando de drive
        driveCommand = new TeleOpDriveCommand(
                drive,
                () -> -driverGamepad.getLeftY(),      // Forward (invertido porque joystick)
                () -> -driverGamepad.getLeftX(),      // Strafe (invertido porque joystick)
                () -> -driverGamepad.getRightX()      // Rotation (invertido porque joystick)
        );
        
        // Establecer como default command
        drive.setDefaultCommand(driveCommand);
        
        // Configurar button bindings
        configureButtonBindings();
        
        // Mensaje de inicio
        telemetry.addLine("✅ Robot Ready!");
        telemetry.addLine("Controls:");
        telemetry.addLine("  LB = Slow Mode");
        telemetry.addLine("  RB = Turbo Mode");
        telemetry.addLine("  Back = Toggle Centric Mode");
        telemetry.update();
    }
    
    // ===== BUTTON BINDINGS =====
    
    private void configureButtonBindings() {
        
        // ===== LEFT BUMPER: Toggle Slow Mode =====
        driverGamepad.getGamepadButton(GamepadKeys.Button.LEFT_BUMPER)
                .whenPressed(new InstantCommand(() -> {
                    driveCommand.toggleSlowMode();
                    
                    // Feedback auditivo (opcional)
                    if (driveCommand.getSpeedMode() == TeleOpDriveCommand.SpeedMode.SLOW) {
                        telemetry.speak("Slow mode");
                    } else {
                        telemetry.speak("Normal mode");
                    }
                }));
        
        // ===== RIGHT BUMPER: Toggle Turbo Mode =====
        driverGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
                .whenPressed(new InstantCommand(() -> {
                    driveCommand.toggleTurboMode();
                    
                    // Feedback auditivo (opcional)
                    if (driveCommand.getSpeedMode() == TeleOpDriveCommand.SpeedMode.TURBO) {
                        telemetry.speak("Turbo mode");
                    } else {
                        telemetry.speak("Normal mode");
                    }
                }));
        
        // ===== BACK BUTTON: Toggle Robot/Field Centric =====
        driverGamepad.getGamepadButton(GamepadKeys.Button.BACK)
                .whenPressed(new InstantCommand(() -> {
                    driveCommand.toggleCentricMode();
                    
                    // Feedback auditivo
                    if (driveCommand.isRobotCentric()) {
                        telemetry.speak("Robot centric");
                    } else {
                        telemetry.speak("Field centric");
                    }
                }));
        
        // ===== EJEMPLO: A Button - Automated Action =====
        // Descomenta esto si quieres probar seguir un path durante teleop
        /*
        driverGamepad.getGamepadButton(GamepadKeys.Button.A)
                .whenPressed(() -> {
                    // Crear path de ejemplo
                    PathChain examplePath = follower.pathBuilder()
                            .addPath(new BezierLine(
                                    follower.getPose(),
                                    new Pose(24, 24)
                            ))
                            .build();
                    
                    // Seguir el path (interrumpe el control manual)
                    return new FollowPathCommand(drive, examplePath);
                });
        */
    }
    
    // ===== LOOP (opcional - solo para telemetría adicional) =====
    
    @Override
    public void run() {
        // Llamar al run del CommandOpMode (CRÍTICO)
        super.run();
        
        // Telemetría adicional
        updateTelemetry();
    }
    
    private void updateTelemetry() {
        telemetry.addData("━━━━━ DRIVE ━━━━━", "");
        
        // Modo de velocidad con emoji
        String speedEmoji = getSpeedModeEmoji();
        telemetry.addData("Speed Mode", speedEmoji + " " + driveCommand.getSpeedMode());
        
        // Modo centric
        String centricMode = driveCommand.isRobotCentric() ? "🤖 Robot" : "🌍 Field";
        telemetry.addData("Control", centricMode);
        
        // Pose actual (útil para debugging)
        telemetry.addData("Pose", String.format("(%.1f, %.1f, %.1f°)",
                drive.getPose().getX(),
                drive.getPose().getY(),
                Math.toDegrees(drive.getHeading())
        ));
        
        // Velocidad actual
        telemetry.addData("Velocity", String.format("%.1f",
                0.0
        ));
        
        telemetry.update();
    }
    
    /**
     * Helper para mostrar emoji según modo de velocidad.
     */
    private String getSpeedModeEmoji() {
        switch (driveCommand.getSpeedMode()) {
            case SLOW:
                return "🐢";  // Tortuga
            case TURBO:
                return "🚀";  // Cohete
            case NORMAL:
            default:
                return "▶️";  // Play normal
        }
    }
}
