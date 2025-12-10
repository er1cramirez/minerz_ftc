package org.firstinspires.ftc.teamcode.constants;

import com.pedropathing.control.PIDFCoefficients;
import com.pedropathing.control.FilteredPIDFCoefficients;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

/**
 * Constantes para el DriveSubsystem.
 * Incluye configuración de Pedro Pathing y constantes adicionales para TeleOp.
 */
public class DriveConstants {
    
    // ========== HARDWARE NAMES ==========
    public static final String LEFT_FRONT = "frontLeft";
    public static final String LEFT_REAR = "backLeft";
    public static final String RIGHT_FRONT = "frontRight";
    public static final String RIGHT_REAR = "backRight";
    
    // ========== TELEOP SPEED MODES ==========
    /**
     * Velocidad base normal (70-80% según preferencia)
     */
    public static final double NORMAL_SPEED = 0.75;
    
    /**
     * Velocidad turbo (máxima)
     */
    public static final double TURBO_SPEED = 1.0;
    
    /**
     * Velocidad lenta/precisión
     */
    public static final double SLOW_SPEED = 0.4;
    
    // ========== DRIVE MODES ==========
    /**
     * Modo por defecto al iniciar TeleOp
     */
    public static final boolean DEFAULT_ROBOT_CENTRIC = true;
    
    // ========== PEDRO PATHING CONFIGURATION ==========
    
    /**
     * Constantes del Follower (control del robot)
     */
    public static final FollowerConstants   FOLLOWER_CONSTANTS = new FollowerConstants()
            .mass(16.2)
            .forwardZeroPowerAcceleration(-25.9346931313679598)
            .lateralZeroPowerAcceleration(-67.342491844080064)
            .translationalPIDFCoefficients(new PIDFCoefficients(
                    0.03,
                    0,
                    0,
                    0.015
            ))
            .translationalPIDFSwitch(4)
            .secondaryTranslationalPIDFCoefficients(new PIDFCoefficients(
                    0.4,
                    0,
                    0.005,
                    0.0006
            ))
            .headingPIDFCoefficients(new PIDFCoefficients(
                    0.8,
                    0,
                    0,
                    0.01
            ))
            .secondaryHeadingPIDFCoefficients(new PIDFCoefficients(
                    2.5,
                    0,
                    0.1,
                    0.0005
            ))
            .drivePIDFCoefficients(new FilteredPIDFCoefficients(
                    0.1,
                    0,
                    0.00035,
                    0.6,
                    0.015
            ))
            .secondaryDrivePIDFCoefficients(new FilteredPIDFCoefficients(
                    0.02,
                    0,
                    0.000005,
                    0.6,
                    0.01
            ))
            .drivePIDFSwitch(15)
            .centripetalScaling(0.0005);
    
    /**
     * Constantes del drivetrain Mecanum
     */
    public static final MecanumConstants MECANUM_CONSTANTS = new MecanumConstants()
            .leftFrontMotorName(LEFT_FRONT)
            .leftRearMotorName(LEFT_REAR)
            .rightFrontMotorName(RIGHT_FRONT)
            .rightRearMotorName(RIGHT_REAR)
            .leftFrontMotorDirection(DcMotorSimple.Direction.REVERSE)
            .leftRearMotorDirection(DcMotorSimple.Direction.REVERSE)
            .rightFrontMotorDirection(DcMotorSimple.Direction.FORWARD)
            .rightRearMotorDirection(DcMotorSimple.Direction.FORWARD)
            .xVelocity(78.261926752421046666666666666667)
            .yVelocity(61.494551922189565);
    
    /**
     * Constantes del localizador Pinpoint
     */
    public static final PinpointConstants LOCALIZER_CONSTANTS = new PinpointConstants()
            .forwardPodY(0.75)
            .strafePodX(-6.6)
            .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.FORWARD)
            .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED);
    
    /**
     * PathConstraints para autonomous paths.
     * 
     * Orden de parámetros:
     * tValueConstraint, velocityConstraint, translationalConstraint, headingConstraint,
     * timeoutConstraint, brakingStrength, BEZIER_CURVE_SEARCH_LIMIT, brakingStart
     */
    public static final PathConstraints PATH_CONSTRAINTS = new PathConstraints(
            0.995,  // tValueConstraint
            0.1,    // velocityConstraint
            0.1,    // translationalConstraint
            0.009,  // headingConstraint
            50,     // timeoutConstraint
            1.25,   // brakingStrength
            10,     // BEZIER_CURVE_SEARCH_LIMIT (no cambiar típicamente)
            1       // brakingStart
    );
    
    // ========== FOLLOWER FACTORY ==========
    
    /**
     * Crea una instancia configurada del Follower.
     * Usado tanto en auto como en teleop.
     * 
     * @param hardwareMap El hardware map del OpMode
     * @return Follower configurado y listo para usar
     */
    public static Follower createFollower(HardwareMap hardwareMap) {
        return new FollowerBuilder(FOLLOWER_CONSTANTS, hardwareMap)
                .mecanumDrivetrain(MECANUM_CONSTANTS)
                .pinpointLocalizer(LOCALIZER_CONSTANTS)
                .pathConstraints(PATH_CONSTRAINTS)
                .build();
    }
}
