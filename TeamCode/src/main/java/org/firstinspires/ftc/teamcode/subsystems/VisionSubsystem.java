package org.firstinspires.ftc.teamcode.subsystems;

import android.util.Size;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.seattlesolvers.solverslib.command.SubsystemBase;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;
import org.firstinspires.ftc.teamcode.constants.VisionConstants;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Vision Subsystem for AprilTag detection.
 */
public class VisionSubsystem extends SubsystemBase {
    public enum VisionState {
        IDLE,           // No initialized or closed
        DISABLED,       // Streaming paused
        ACTIVE         // Detecting, no valid detection
    }

    public enum Alliance {
        RED(VisionConstants.TAG_GOAL_RED),
        BLUE(VisionConstants.TAG_GOAL_BLUE);

        public final int goalTagId;

        Alliance(int goalTagId) {
            this.goalTagId = goalTagId;
        }
    }

    /**
     * Range-Bearing-Elevation relative to a target.
     */
    public static class RBE {
        public final double range;
        public final double bearing;
        public final double elevation;
        public final double confidence;

        public RBE(double range, double bearing, double elevation, double confidence) {
            this.range = range;
            this.bearing = bearing;
            this.elevation = elevation;
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return String.format("RBE(R=%.1f\", B=%.1f°, E=%.1f°, C=%.2f)",
                    range, bearing, elevation, confidence);
        }
    }

    /**
     * Robot pose in field coordinates.
     */
    public static class FieldPose {
        public final double x;
        public final double y;
        public final double heading;
        public final int sourceTagId;

        public FieldPose(double x, double y, double heading, int sourceTagId) {
            this.x = x;
            this.y = y;
            this.heading = heading;
            this.sourceTagId = sourceTagId;
        }

        public org.firstinspires.ftc.robotcore.external.navigation.Pose2D toPose2D() {
            return new org.firstinspires.ftc.robotcore.external.navigation.Pose2D(
                    DistanceUnit.INCH, x, y, AngleUnit.DEGREES, heading);
        }

        @Override
        public String toString() {
            return String.format("Pose(X=%.1f, Y=%.1f, H=%.1f° from tag %d)",
                    x, y, heading, sourceTagId);
        }
    }

    private final AprilTagProcessor aprilTagProcessor;
    private final VisionPortal visionPortal;

    private VisionState currentState;
    private AprilTagDetection lastValidDetection;

    private static final Position CAMERA_POSITION = new Position(
            DistanceUnit.INCH, 0, 0, 15, 0);
    private static final YawPitchRollAngles CAMERA_ORIENTATION = new YawPitchRollAngles(
            AngleUnit.DEGREES, 0, -90
            , 0, 0);

    public VisionSubsystem(HardwareMap hardwareMap) {
        this(hardwareMap, VisionConstants.CAMERA_NAME);
    }

    public VisionSubsystem(HardwareMap hardwareMap, String cameraName) {
        aprilTagProcessor = new AprilTagProcessor.Builder()
                .setDrawTagOutline(true)
                .setDrawTagID(false)
                .setDrawCubeProjection(false)
                .setDrawAxes(false)
                .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                .setTagLibrary(AprilTagGameDatabase.getDecodeTagLibrary())
                .setOutputUnits(VisionConstants.DISTANCE_UNIT, VisionConstants.ANGLE_UNIT)
                .setLensIntrinsics(VisionConstants.FX, VisionConstants.FY,
                        VisionConstants.CX, VisionConstants.CY)
                .setCameraPose(CAMERA_POSITION, CAMERA_ORIENTATION)
                .build();

        visionPortal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, cameraName))
                .addProcessor(aprilTagProcessor)
                .enableLiveView(VisionConstants.ENABLE_LIVE_VIEW)
                .setStreamFormat(VisionPortal.StreamFormat.MJPEG)
                 .setCameraResolution(new Size(VisionConstants.STREAM_WIDTH, VisionConstants.STREAM_HEIGHT))
                .build();
        currentState = VisionState.IDLE;
        lastValidDetection = null;
    }

    public void enable() {
        if (visionPortal.getCameraState() == VisionPortal.CameraState.STREAMING) {
            aprilTagProcessor.setDecimation(2);
        }
        visionPortal.resumeStreaming();
        currentState = VisionState.ACTIVE;
    }

    public void disable() {
        visionPortal.stopStreaming();
        currentState = VisionState.DISABLED;
        lastValidDetection = null;
    }

    public void close() {
        visionPortal.close();
        currentState = VisionState.IDLE;
    }

    public boolean setManualExposure(int exposureMs, int gain) {
        if (visionPortal == null ||
                visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            return false;
        }

        try {
            ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
            if (exposureControl.getMode() != ExposureControl.Mode.Manual) {
                exposureControl.setMode(ExposureControl.Mode.Manual);
            }
            exposureControl.setExposure(exposureMs, TimeUnit.MILLISECONDS);

            GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
            gainControl.setGain(gain);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int[] getExposureLimits() {
        if (visionPortal == null ||
                visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            return null;
        }

        try {
            ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
            int min = (int) exposureControl.getMinExposure(TimeUnit.MILLISECONDS) + 1;
            int max = (int) exposureControl.getMaxExposure(TimeUnit.MILLISECONDS);
            return new int[]{min, max};
        } catch (Exception e) {
            return null;
        }
    }

    public int[] getGainLimits() {
        if (visionPortal == null ||
                visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            return null;
        }

        try {
            GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
            int min = gainControl.getMinGain();
            int max = gainControl.getMaxGain();
            return new int[]{min, max};
        } catch (Exception e) {
            return null;
        }
    }

    public int getCurrentExposure() {
        try {
            ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
            return (int) exposureControl.getExposure(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return -1;
        }
    }

    public int getCurrentGain() {
        try {
            GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
            return gainControl.getGain();
        } catch (Exception e) {
            return -1;
        }
    }

    public String getSequence() {
        AprilTagDetection detection = findDetectionByIds(
                VisionConstants.TAG_SEQUENCE_GPP,
                VisionConstants.TAG_SEQUENCE_PGP,
                VisionConstants.TAG_SEQUENCE_PPG
        );

        if (detection != null && isValidDetection(detection)) {
            return VisionConstants.getSequenceFromTagId(detection.id);
        }
        return null;
    }

    public boolean canSeeSequenceTag() {
        return getSequence() != null;
    }

    public RBE getGoalRBE(Alliance alliance) {
        AprilTagDetection detection = findDetectionById(alliance.goalTagId);

        if (detection != null && isValidDetection(detection)) {
            return createRBE(detection);
        }
        return null;
    }

    public boolean canSeeGoal(Alliance alliance) {
        return getGoalRBE(alliance) != null;
    }

    public RBE getRBEForTag(int tagId) {
        AprilTagDetection detection = findDetectionById(tagId);

        if (detection != null && isValidDetection(detection)) {
            return createRBE(detection);
        }
        return null;
    }




    public Pose2D getCameraGlobalPose() {
        for (AprilTagDetection detection : aprilTagProcessor.getDetections()) {
            if (detection.robotPose != null && detection.metadata != null 
                    && !detection.metadata.name.contains("Obelisk") && isValidDetection(detection)) {
                return new Pose2D(
                        DistanceUnit.INCH,
                        detection.robotPose.getPosition().x,
                        detection.robotPose.getPosition().y,
                        AngleUnit.RADIANS,
                        detection.robotPose.getOrientation().getYaw(AngleUnit.RADIANS)
                );
            }
        }
        return null;
    }

    public VisionState getState() {
        return currentState;
    }

    public boolean isDisabled() {
        return currentState == VisionState.DISABLED;
    }

    public AprilTagDetection getLastValidDetection() {
        return lastValidDetection;
    }

    public List<AprilTagDetection> getAllDetections() {
        return aprilTagProcessor.getDetections();
    }

    public VisionPortal getVisionPortal() {
        return visionPortal;
    }



    private AprilTagDetection findDetectionById(int tagId) {
        for (AprilTagDetection detection : aprilTagProcessor.getDetections()) {
            if (detection.id == tagId) {
                return detection;
            }
        }
        return null;
    }

    private AprilTagDetection findDetectionByIds(int... tagIds) {
        for (AprilTagDetection detection : aprilTagProcessor.getDetections()) {
            for (int id : tagIds) {
                if (detection.id == id) {
                    return detection;
                }
            }
        }
        return null;
    }

    private boolean isValidDetection(AprilTagDetection detection) {
        if (detection.ftcPose == null) {
            return false;
        }

        if (detection.decisionMargin < 1.0 / VisionConstants.MAX_AMBIGUITY) {
            return false;
        }

        double range = detection.ftcPose.range;
        if (range < VisionConstants.MIN_DETECTION_RANGE_INCHES ||
                range > VisionConstants.MAX_DETECTION_RANGE_INCHES) {
            return false;
        }

        return true;
    }

    private RBE createRBE(AprilTagDetection detection) {
        double confidence = Math.min(1.0, detection.decisionMargin / 100.0);

        return new RBE(
                detection.ftcPose.range,
                detection.ftcPose.bearing,
                detection.ftcPose.elevation,
                confidence
        );
    }
}