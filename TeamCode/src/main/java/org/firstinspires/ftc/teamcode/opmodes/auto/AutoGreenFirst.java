package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.RunCommand;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;
import com.seattlesolvers.solverslib.pedroCommand.FollowPathCommand;
import org.firstinspires.ftc.teamcode.commands.sequences.SequenceAutoShootCommand;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

@Autonomous(name = "Auto Green First (0-1-2)", group = "A")
public class AutoGreenFirst extends CommandOpMode {
    
    // Subsystems
    private Follower follower;
    private EjectorSubsystem ejector;
    private SpindexerSubsystem spindexer;
    private ShooterSubsystem shooter;

    // Poses
    private final Pose startPose = new Pose(0, 0, 0);
    private final Pose endPose = new Pose(10, 0, 0); 

    // Paths
    private PathChain basicMovementPath;

    @Override
    public void initialize() {
        follower = DriveConstants.createFollower(hardwareMap);
        follower.setStartingPose(startPose);
        ejector = new EjectorSubsystem(hardwareMap);
        shooter = new ShooterSubsystem(hardwareMap);
        try {
            spindexer = new SpindexerSubsystem(hardwareMap, true);
        } catch (Exception e) {
            spindexer = new SpindexerSubsystem(hardwareMap, false);
            telemetry.addLine("Warning: Spindexer sensor not found, using manual mode wrapper");
        }
        buildPaths();
        register(ejector, shooter);

        // Create the Sequence
        SequentialCommandGroup autoSequence = new SequentialCommandGroup();

        // 1. Basic Movement (Time-based fallback)
        autoSequence.addCommands(
            new WaitCommand(500),
            new InstantCommand(() -> follower.startTeleopDrive()),
            new RunCommand(() -> follower.setTeleOpDrive(-0.3, 0, 0, false)).withTimeout(1500), 
            new InstantCommand(() -> follower.setTeleOpDrive(0, 0, 0, false))
        );

        // 2. Shoot Green First (0, 1, 2)
        autoSequence.addCommands(
            new RunCommand(() -> shooter.spinUpClose(), shooter).withTimeout(1000), 
            new SequenceAutoShootCommand(ejector, spindexer, shooter, 0, 1, 2),
            new RunCommand(() -> shooter.stop(), shooter).withTimeout(50)
        );

        schedule(autoSequence);
        
        telemetry.addLine("Initialized Auto Green First (0-1-2)");
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
        super.run(); 
        follower.update(); 
        
        telemetry.addData("X", follower.getPose().getX());
        telemetry.addData("Y", follower.getPose().getY());
        telemetry.addData("Heading", follower.getPose().getHeading());
        telemetry.update();
    }
}
