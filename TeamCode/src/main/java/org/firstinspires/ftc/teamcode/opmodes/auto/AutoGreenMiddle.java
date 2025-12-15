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
import org.firstinspires.ftc.teamcode.commands.sequences.SequenceAutoShootCommand;
import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.ShooterSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;

@Autonomous(name = "Auto Green Middle (1-0-2)", group = "A")
public class AutoGreenMiddle extends CommandOpMode {
    
    // Subsystems
    private Follower follower;
    private EjectorSubsystem ejector;
    private SpindexerSubsystem spindexer;
    private ShooterSubsystem shooter;

    private final Pose startPose = new Pose(0, 0, 0);

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
        register(ejector, shooter);

        SequentialCommandGroup autoSequence = new SequentialCommandGroup();

        // 1. Basic Movement
        autoSequence.addCommands(
            new WaitCommand(500),
            new InstantCommand(() -> follower.startTeleopDrive()),
            new RunCommand(() -> follower.setTeleOpDrive(-0.3, 0, 0, false)).withTimeout(1500), 
            new InstantCommand(() -> follower.setTeleOpDrive(0, 0, 0, false))
        );

        // 2. Shoot Green Middle (1, 0, 2)
        autoSequence.addCommands(
            new RunCommand(() -> shooter.spinUpClose(), shooter).withTimeout(1000), 
            new SequenceAutoShootCommand(ejector, spindexer, shooter, 1, 0, 2),
            new RunCommand(() -> shooter.stop(), shooter).withTimeout(50)
        );

        schedule(autoSequence);
        
        telemetry.addLine("Initialized Auto Green Middle (1-0-2)");
        telemetry.update();
    }
    
    @Override
    public void run() {
        super.run(); 
        follower.update(); 
        telemetry.update();
    }
}
