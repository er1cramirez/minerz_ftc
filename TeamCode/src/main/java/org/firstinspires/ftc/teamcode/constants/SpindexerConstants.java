
package org.firstinspires.ftc.teamcode.constants;

public class SpindexerConstants {
    
    // Hardware names
    public static final String SERVO_NAME = "spindexerServo";

    
    // Lista de posiciones del servo del spindexer en valores normalizados (0.0 a 1.0)
    
    private static final double maxDegree = 300;
    public static final double SLOT_0_INTAKE_POSITION = 0.0 / maxDegree;
    public static final double SLOT_0_OUTTAKE_POSITION = 60.0 / maxDegree;
    public static final double SLOT_1_INTAKE_POSITION = 120.0 / maxDegree;
    public static final double SLOT_1_OUTTAKE_POSITION = 180.0 / maxDegree;
    public static final double SLOT_2_INTAKE_POSITION = 240.0 / maxDegree;
    public static final double SLOT_2_OUTTAKE_POSITION = 300.0 / maxDegree;

    
    // Umbral de distancia para detectar pelota (en cm)
    private static final double BALL_DETECTION_DISTANCE = 4.0;
    
    // Umbrales de color normalizados (AJUSTAR SEGÚN PRUEBAS)
    private static final double YELLOW_RED_MIN = 0.35;
    private static final double YELLOW_GREEN_MIN = 0.35;
    private static final double YELLOW_BLUE_MAX = 0.25;
    
    private static final double PURPLE_BLUE_MIN = 0.40;
    private static final double PURPLE_RED_MIN = 0.30;
    private static final double PURPLE_GREEN_MAX = 0.30;
}