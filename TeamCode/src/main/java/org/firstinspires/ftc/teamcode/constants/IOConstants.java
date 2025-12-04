package org.firstinspires.ftc.teamcode.constants;

import com.seattlesolvers.solverslib.gamepad.GamepadKeys;

/**
 * Operator Interface constants - button and axis mappings.
 */
public final class IOConstants {
    
    // Prevent instantiation
    private IOConstants() {}
    
    /**
     * Driver controller (gamepad1) mappings
     */
    public static final class Driver {
        // Drive controls
        // Left stick Y = forward/backward
        // Left stick X = strafe left/right
        // Right stick X = rotate
        
        // Speed mode buttons (hold)
        public static final GamepadKeys.Button PRECISION_MODE_BUTTON = GamepadKeys.Button.LEFT_BUMPER;
        public static final GamepadKeys.Button TURBO_MODE_BUTTON = GamepadKeys.Button.RIGHT_BUMPER;
        
        // Field relative toggle (not implemented yet, for future use)
        public static final GamepadKeys.Button FIELD_RELATIVE_TOGGLE = GamepadKeys.Button.START;
        
        // Gyro reset
        public static final GamepadKeys.Button RESET_GYRO_BUTTON = GamepadKeys.Button.BACK;
    }
    
    /**
     * Operator controller (gamepad2) mappings
     */
    public static final class Operator {
        // Intake controls
        public static final GamepadKeys.Trigger INTAKE_TRIGGER = GamepadKeys.Trigger.RIGHT_TRIGGER;
        public static final GamepadKeys.Trigger OUTTAKE_TRIGGER = GamepadKeys.Trigger.LEFT_TRIGGER;
        
        // Future shooter controls
        public static final GamepadKeys.Button SHOOT_BUTTON = GamepadKeys.Button.A;
        public static final GamepadKeys.Button SHOOTER_TOGGLE = GamepadKeys.Button.B;

        // Future indexer controls
        public static final GamepadKeys.Button INDEXER_ADVANCE_BUTTON = GamepadKeys.Button.X;
        public static final GamepadKeys.Button INDEXER_REVERSE_BUTTON = GamepadKeys.Button.Y;
    }
    
    /**
     * Trigger thresholds
     */
    public static final class Thresholds {
        // Minimum trigger value to activate
        public static final double TRIGGER_THRESHOLD = 0.1;
    }
}
