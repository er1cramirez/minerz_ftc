package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

import org.firstinspires.ftc.teamcode.commands.sequences.ShootingSequence;
import org.firstinspires.ftc.teamcode.commands.spindexer.SpindexerMoveCommand;
import org.firstinspires.ftc.teamcode.subsystems.EjectorSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.SpindexerSubsystem.SlotState;

/**
 * Test OpMode for ShootingSequence strategies.
 * 
 * SETUP:
 * - Assumes GREEN ball is in Slot 0.
 * - Assumes PURPLE balls (or others) in Slot 1 and 2.
 * 
 * CONTROLS:
 * - A: Reset to INTAKE Slot 2 (Start Position for FAST cycle) & Reset Colors.
 * - X: Run FASTEST Strategy (1 -> 0 -> 2).
 * - Y: Run GREEN_FIRST Strategy.
 * - B: Run GREEN_LAST Strategy.
 * - DPAD_UP: Run GREEN_MIDDLE Strategy.
 */
@TeleOp(name = "🧪 Shooting Sequence Test", group = "Tests")
public class ShootingSequenceTest extends CommandOpMode {

    private SpindexerSubsystem spindexer;
    private FlywheelSubsystem flywheel;
    private EjectorSubsystem ejector;
    private GamepadEx gamepad;
    
    private ShootingSequence.ShootingStrategy lastStrategy = null;

    @Override
    public void initialize() {
        spindexer = new SpindexerSubsystem(hardwareMap);
        flywheel = new FlywheelSubsystem(hardwareMap);
        ejector = new EjectorSubsystem(hardwareMap);
        
        gamepad = new GamepadEx(gamepad1);
        
        // Initial Defaults
        resetSimulation();

        // Bindings
        
        // A: Reset
        gamepad.getGamepadButton(GamepadKeys.Button.A)
            .whenPressed(new InstantCommand(this::resetSimulation));
            
        // X: FASTEST
        gamepad.getGamepadButton(GamepadKeys.Button.X)
            .whenPressed(new InstantCommand(() -> runSequence(ShootingSequence.ShootingStrategy.FASTEST)));
            
        // Y: GREEN_FIRST
        gamepad.getGamepadButton(GamepadKeys.Button.Y)
            .whenPressed(new InstantCommand(() -> runSequence(ShootingSequence.ShootingStrategy.GREEN_FIRST)));
            
        // B: GREEN_LAST
        gamepad.getGamepadButton(GamepadKeys.Button.B)
            .whenPressed(new InstantCommand(() -> runSequence(ShootingSequence.ShootingStrategy.GREEN_LAST)));
            
        // DPAD_UP: GREEN_MIDDLE
        gamepad.getGamepadButton(GamepadKeys.Button.DPAD_UP)
            .whenPressed(new InstantCommand(() -> runSequence(ShootingSequence.ShootingStrategy.GREEN_MIDDLE)));
            
        telemetry.addLine("Ready. Press A to Reset.");
        telemetry.update();
    }
    
    private void resetSimulation() {
        // Reset Spindexer to INTAKE 2 position (simulates 240 deg start)
        spindexer.moveToIntakePosition(2);
        
        // Reset Colors: Slot 0 = GREEN, others = PURPLE
        spindexer.setInitialState(SlotState.GREEN, SlotState.PURPLE, SlotState.PURPLE);
        
        // Reset Flywheel? Maybe not needed if we want to test shooting speed.
        // Let's spin it up to idle so sequences don't wait forever
        flywheel.spinUp(3000); // 3000 RPM arbitrary idle
        
        lastStrategy = null;
        
        telemetry.addData("Status", "RESET COMPLETE");
        telemetry.addData("Pos", "Intake 2");
        telemetry.addData("Slots", "0:G, 1:P, 2:P");
        telemetry.update();
    }
    
    private void runSequence(ShootingSequence.ShootingStrategy strategy) {
        lastStrategy = strategy;
        schedule(new ShootingSequence(spindexer, flywheel, ejector, strategy));
    }
    
    @Override
    public void run() {
        super.run();
        telemetry.addData("Last Strategy", lastStrategy);
        telemetry.addData("Current Slot", spindexer.getCurrentSlotIndex());
        telemetry.addData("Emoji 0", spindexer.getSlotEmoji(0));
        telemetry.addData("Emoji 1", spindexer.getSlotEmoji(1));
        telemetry.addData("Emoji 2", spindexer.getSlotEmoji(2));
        telemetry.update();
    }
}
