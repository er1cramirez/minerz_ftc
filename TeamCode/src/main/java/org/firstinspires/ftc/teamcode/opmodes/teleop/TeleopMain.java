@TeleOp(name = "Main TeleOp")
public class TeleOpMain extends CommandOpMode {
    
    private DriveSubsystem drive;
    private IntakeSubsystem intake;
    private SpindexerSubsystem spindexer;
    
    private GamepadEx driverGamepad;
    private GamepadEx operatorGamepad;
    
    @Override
    public void initialize() {
        // Subsystems
        drive = new DriveSubsystem(hardwareMap);
        intake = new IntakeSubsystem(hardwareMap);
        spindexer = new SpindexerSubsystem(hardwareMap);
        
        // Gamepads
        driverGamepad = new GamepadEx(gamepad1);
        operatorGamepad = new GamepadEx(gamepad2);
        
        // Default commands
        drive.setDefaultCommand(
            new RunCommand(
                () -> drive.drive(
                    driverGamepad.getLeftY(),
                    driverGamepad.getLeftX(),
                    driverGamepad.getRightX()
                ),
                drive
            )
        );
        
        // Button bindings
        operatorGamepad.getGamepadButton(GamepadKeys.Button.RIGHT_BUMPER)
            .whileHeld(new IntakeCommand(intake, spindexer));
        
        operatorGamepad.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new ShootSequenceCommand(spindexer, shooter, ejector));
    }
}