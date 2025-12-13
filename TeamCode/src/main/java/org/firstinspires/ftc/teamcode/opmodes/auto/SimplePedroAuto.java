package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.RunCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.pedroCommand.FollowPathCommand;
import org.firstinspires.ftc.teamcode.commands.sequences.ThreeBallAutoShootCommand;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

@Autonomous(name = "Simple Pedro Auto", group = "A")
public class SimplePedroAuto extends CommandOpMode {
    
    // Subsystems
    private Follower follower;
    private EjectorSubsystem ejector;
    private SpindexerSubsystem spindexer;
    private ShooterSubsystem shooter;

    // Poses
    private final Pose startPose = new Pose(0, 0, 0);
    private final Pose endPose = new Pose(10, 0, 0); // Move 10 inches forward

    // Paths
    private PathChain basicMovementPath;

    @Override
    public void initialize() {
        // Initialize Follower (Drive)
        follower = DriveConstants.createFollower(hardwareMap);
        follower.setStartingPose(startPose);

        // Initialize Mechanisms
        ejector = new EjectorSubsystem(hardwareMap);
        shooter = new ShooterSubsystem(hardwareMap);
        
        // Try to initialize Spindexer with sensor if possible, otherwise manual
        try {
            spindexer = new SpindexerSubsystem(hardwareMap, true);
        } catch (Exception e) {
            spindexer = new SpindexerSubsystem(hardwareMap, false);
            telemetry.addLine("Warning: Spindexer sensor not found, using manual mode wrapper");
        }

        // Build Paths
        buildPaths();

        // Register subsystems that need periodic updates
        // Note: CommandOpMode automatically runs commands, but we might need to register subsystems 
        // if they have a periodic() method that needs to run even when no command is active.
        register(ejector, shooter); // Spindexer might also need it if it has periodic logic

        // Create the Sequence
        SequentialCommandGroup autoSequence = new SequentialCommandGroup(
            // 1. Shoot 3 balls (using the sequence provided)
            // We pass 2, 1, 0 implicitly because the command handles "current slot -> next -> next"
            // If we want to enforce 2, 1, 0, we might need to ensure Spindexer starts at a specific slot 
            // or pass args. The current ThreeBallAutoShootCommand figures it out based on current position.
            // For safety, let's spin up the shooter first.
            new RunCommand(() -> shooter.setTargetVelocity(ShooterConstants.CLOSE_VELOCITY), shooter).withTimeout(100), // Start shooting motor
            
            // Wait for shooter to be ready is handled inside ThreeBallAutoShootCommand usually, 
            // but we need to make sure the motor is ON. ThreeBallAutoShootCommand assumes "shooter is already up to speed".
            // Let's verify ShooterSubsystem to see if we need a command to Start it.
            // Assuming we can just set a default target velocity here or use a specific start command.
            // I'll stick to a simple lambda to start it.
            
            new ThreeBallAutoShootCommand(ejector, spindexer, shooter),
            
            // 2. Stop Shooter
            new RunCommand(() -> shooter.stop(), shooter).withTimeout(50),

            // 3. Basic Movement
            new WaitCommand(500),
            new FollowPathCommand(follower, basicMovementPath)
        );

        schedule(autoSequence);
        
        telemetry.addLine("Initialized Simple Pedro Auto");
        telemetry.update();
    }

    private void buildPaths() {
        basicMovementPath = follower.pathBuilder()
                .addPath(new BezierLine(startPose, endPose))
                .setLinearHeadingInterpolation(startPose.getHeading(), endPose.getHeading())
                .build();
    }
    
    @Override
    public void run() {
        super.run(); // Updates commands
        follower.update(); // Updates Pedro Pathing
        
        telemetry.addData("X", follower.getPose().getX());
        telemetry.addData("Y", follower.getPose().getY());
        telemetry.addData("Heading", follower.getPose().getHeading());
        telemetry.update();
    }
}
