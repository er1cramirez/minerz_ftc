package org.firstinspires.ftc.teamcode.commands;

import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;

import org.firstinspires.ftc.teamcode.constants.IOConstants;
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem;

import java.util.function.DoubleSupplier;

/**
 * Command for controlling the intake with triggers.
 * Control scheme:
 * - Right trigger: Intake (pull in game elements)
 * - Left trigger: Outtake (eject game elements)
 * - Neither pressed: Stop
 */
public class IntakeCommand extends CommandBase {

    private final IntakeSubsystem intakeSubsystem;
    private final DoubleSupplier intakeTriggerSupplier;
    private final DoubleSupplier outtakeTriggerSupplier;

    /**
     * Creates a new IntakeCommand.
     * @param intakeSubsystem The intake subsystem to control
     * @param intakeTriggerSupplier Supplier for intake trigger value (right trigger)
     * @param outtakeTriggerSupplier Supplier for outtake trigger value (left trigger)
     */
    public IntakeCommand(
            IntakeSubsystem intakeSubsystem,
            DoubleSupplier intakeTriggerSupplier,
            DoubleSupplier outtakeTriggerSupplier) {
        
        this.intakeSubsystem = intakeSubsystem;
        this.intakeTriggerSupplier = intakeTriggerSupplier;
        this.outtakeTriggerSupplier = outtakeTriggerSupplier;
        
        addRequirements(intakeSubsystem);
    }

    /**
     * Convenience constructor using GamepadEx.
     * @param intakeSubsystem The intake subsystem to control
     * @param gamepad The gamepad for input (typically gamepad2)
     */
    public IntakeCommand(IntakeSubsystem intakeSubsystem, GamepadEx gamepad) {
        this(
            intakeSubsystem,
            () -> gamepad.getTrigger(IOConstants.Operator.INTAKE_TRIGGER),
            () -> gamepad.getTrigger(IOConstants.Operator.OUTTAKE_TRIGGER)
        );
    }

    @Override
    public void execute() {
        double intakeTrigger = intakeTriggerSupplier.getAsDouble();
        double outtakeTrigger = outtakeTriggerSupplier.getAsDouble();
        
        // Prioritize intake if both triggers are pressed
        if (intakeTrigger > IOConstants.Thresholds.TRIGGER_THRESHOLD) {
            intakeSubsystem.intake();
        } else if (outtakeTrigger > IOConstants.Thresholds.TRIGGER_THRESHOLD) {
            intakeSubsystem.outtake();
        } else {
            intakeSubsystem.stop();
        }
    }

    @Override
    public void end(boolean interrupted) {
        // Stop the intake when command ends
        intakeSubsystem.stop();
    }

    @Override
    public boolean isFinished() {
        // This command never finishes on its own (runs until interrupted)
        return false;
    }
}
