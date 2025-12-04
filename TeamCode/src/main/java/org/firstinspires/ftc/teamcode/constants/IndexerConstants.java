package org.firstinspires.ftc.teamcode.constants;

/**
 * Constants for the ArtifactIndexer subsystem.
 * The indexer is a 3-slot wheel controlled by a servo with 300° range.
 * Each slot has two positions: intake (loading) and outtake (shooting).
 * The outtake position is offset by 60° from intake.
 */
public final class IndexerConstants {
    
    // Prevent instantiation
    private IndexerConstants() {}
    
    /**
     * Servo configuration
     */
    public static final class Servo {
        // Total range of the servo in degrees
        public static final double SERVO_RANGE_DEGREES = 300.0;
        
        // Degrees between each slot (360° / 3 slots)
        public static final double DEGREES_PER_SLOT = 120.0;
        
        // Offset between intake and outtake position for each slot
        public static final double INTAKE_TO_OUTTAKE_OFFSET_DEGREES = 60.0;
    }
    
    /**
     * Slot positions in degrees.
     * Intake positions are where the slot aligns with the intake mechanism.
     * Outtake positions are where the slot aligns with the shooter.
     */
    public static final class Positions {
        // Slot 1 positions
        public static final double SLOT_1_INTAKE_DEGREES = 0.0;
        public static final double SLOT_1_OUTTAKE_DEGREES = 60.0;
        
        // Slot 2 positions
        public static final double SLOT_2_INTAKE_DEGREES = 120.0;
        public static final double SLOT_2_OUTTAKE_DEGREES = 180.0;
        
        // Slot 3 positions
        public static final double SLOT_3_INTAKE_DEGREES = 240.0;
        public static final double SLOT_3_OUTTAKE_DEGREES = 300.0;
    }
    
    /**
     * Hardware name
     */
    public static final String SERVO_NAME = "indexer_servo";
}
