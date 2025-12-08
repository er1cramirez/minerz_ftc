package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

@TeleOp(name = "Main TeleOp")
public class TeleOpMain extends CommandOpMode {
    
//    private DriveSubsystem drive;
    private IntakeSubsystem intake;
    private SpindexerSubsystem spindexer;
    
    private GamepadEx driverGamepad;
    private GamepadEx operatorGamepad;
    
    @Override
    public void initialize() {
        // Subsystems
//        drive = new DriveSubsystem(hardwareMap);
        intake = new IntakeSubsystem(hardwareMap);
        spindexer = new SpindexerSubsystem(hardwareMap,true);
        
        // Gamepads
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);
        
        // Default commands
//        drive.setDefaultCommand(
//            new RunCommand(
//                () -> drive.drive(
//                    driverGamepad.getLeftY(),
//                    driverGamepad.getLeftX(),
//                    driverGamepad.getRightX()
//                ),
//                drive
//            )
//        );
        
        // Button bindings
//        operatorGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
//            .whileHeld(new IntakeCommand(intake, spindexer));
        

    }
}