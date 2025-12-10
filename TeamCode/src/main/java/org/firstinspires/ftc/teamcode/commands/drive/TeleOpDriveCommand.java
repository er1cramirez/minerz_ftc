package org.firstinspires.ftc.teamcode.commands.drive;

import com.seattlesolvers.solverslib.command.CommandBase;

import org.firstinspires.ftc.teamcode.constants.DriveConstants;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * TeleOpDriveCommand - Control del robot durante TeleOp.
 * 
 * FEATURES:
 * ✅ Tres modos de velocidad: NORMAL, TURBO, SLOW
 * ✅ Toggle entre robot-centric y field-centric
 * ✅ Control suave con joysticks
 * 
 * USO:
 * Este comando típicamente se usa como DEFAULT COMMAND del DriveSubsystem.
 * Nunca termina (isFinished() retorna false).
 */
public class TeleOpDriveCommand extends CommandBase {
    
    // ===== MODOS DE VELOCIDAD =====
    public enum SpeedMode {
        SLOW,      // Precisión (40%)
        NORMAL,    // Base (75%)
        TURBO      // Máximo (100%)
    }
    
    // ===== SUBSYSTEM =====
    private final DriveSubsystem drive;
    
    // ===== SUPPLIERS DE ENTRADA =====
    private final DoubleSupplier forwardSupplier;
    private final DoubleSupplier strafeSupplier;
    private final DoubleSupplier rotationSupplier;
    
    // ===== SUPPLIERS DE MODOS =====
    private final BooleanSupplier robotCentricSupplier;  // Si es null, usa default
    
    // ===== ESTADO INTERNO =====
    private SpeedMode currentSpeedMode;
    private boolean isRobotCentric;
    
    // ===== CONSTRUCTOR COMPLETO =====
    
    /**
     * Constructor completo con control de todos los modos.
     * 
     * @param drive El DriveSubsystem
     * @param forwardSupplier Proveedor de velocidad forward (-1 a 1)
     * @param strafeSupplier Proveedor de velocidad strafe (-1 a 1)
     * @param rotationSupplier Proveedor de velocidad de rotación (-1 a 1)
     * @param robotCentricSupplier Proveedor booleano (true = robot-centric, false = field-centric)
     *                             Si es null, usa DriveConstants.DEFAULT_ROBOT_CENTRIC
     */
    public TeleOpDriveCommand(
            DriveSubsystem drive,
            DoubleSupplier forwardSupplier,
            DoubleSupplier strafeSupplier,
            DoubleSupplier rotationSupplier,
            BooleanSupplier robotCentricSupplier
    ) {
        this.drive = drive;
        this.forwardSupplier = forwardSupplier;
        this.strafeSupplier = strafeSupplier;
        this.rotationSupplier = rotationSupplier;
        this.robotCentricSupplier = robotCentricSupplier;
        
        // Estado inicial
        this.currentSpeedMode = SpeedMode.NORMAL;
        this.isRobotCentric = DriveConstants.DEFAULT_ROBOT_CENTRIC;
        
        addRequirements(drive);
    }
    
    // ===== CONSTRUCTOR SIMPLIFICADO =====
    
    /**
     * Constructor simplificado (siempre robot-centric).
     * 
     * @param drive El DriveSubsystem
     * @param forwardSupplier Proveedor de velocidad forward
     * @param strafeSupplier Proveedor de velocidad strafe
     * @param rotationSupplier Proveedor de velocidad de rotación
     */
    public TeleOpDriveCommand(
            DriveSubsystem drive,
            DoubleSupplier forwardSupplier,
            DoubleSupplier strafeSupplier,
            DoubleSupplier rotationSupplier
    ) {
        this(drive, forwardSupplier, strafeSupplier, rotationSupplier, null);
    }
    
    // ===== INITIALIZE =====
    
    @Override
    public void initialize() {
        // Asegurar que estamos en modo TeleOp
        drive.startTeleOpMode();
    }
    
    // ===== EXECUTE =====
    
    @Override
    public void execute() {
        // Obtener inputs crudos
        double forward = forwardSupplier.getAsDouble();
        double strafe = strafeSupplier.getAsDouble();
        double rotation = rotationSupplier.getAsDouble();
        
        // Aplicar multiplicador de velocidad según modo
        double speedMultiplier = getSpeedMultiplier();
        forward *= speedMultiplier;
        strafe *= speedMultiplier;
        rotation *= speedMultiplier;
        
        // Actualizar modo robot/field centric si hay supplier
        if (robotCentricSupplier != null) {
            isRobotCentric = robotCentricSupplier.getAsBoolean();
        }
        
        // Enviar comandos al drivetrain
        drive.setTeleOpDrive(forward, strafe, rotation, isRobotCentric);
    }
    
    // ===== IS_FINISHED =====
    
    @Override
    public boolean isFinished() {
        // Este comando nunca termina (es un default command)
        return false;
    }
    
    // ===== MÉTODOS PÚBLICOS PARA CONTROL DE MODOS =====
    
    /**
     * Cambia el modo de velocidad.
     * 
     * @param mode El nuevo modo de velocidad
     */
    public void setSpeedMode(SpeedMode mode) {
        this.currentSpeedMode = mode;
    }
    
    /**
     * Obtiene el modo de velocidad actual.
     */
    public SpeedMode getSpeedMode() {
        return currentSpeedMode;
    }
    
    /**
     * Cambia a modo SLOW.
     */
    public void enableSlowMode() {
        setSpeedMode(SpeedMode.SLOW);
    }
    
    /**
     * Cambia a modo NORMAL.
     */
    public void enableNormalMode() {
        setSpeedMode(SpeedMode.NORMAL);
    }
    
    /**
     * Cambia a modo TURBO.
     */
    public void enableTurboMode() {
        setSpeedMode(SpeedMode.TURBO);
    }
    
    /**
     * Toggle entre los tres modos de velocidad (SLOW → NORMAL → TURBO → SLOW).
     */
    public void toggleSpeedMode() {
        switch (currentSpeedMode) {
            case SLOW:
                setSpeedMode(SpeedMode.NORMAL);
                break;
            case NORMAL:
                setSpeedMode(SpeedMode.TURBO);
                break;
            case TURBO:
                setSpeedMode(SpeedMode.SLOW);
                break;
        }
    }
    
    /**
     * Toggle simple entre NORMAL y SLOW.
     */
    public void toggleSlowMode() {
        if (currentSpeedMode == SpeedMode.SLOW) {
            setSpeedMode(SpeedMode.NORMAL);
        } else {
            setSpeedMode(SpeedMode.SLOW);
        }
    }
    
    /**
     * Toggle simple entre NORMAL y TURBO.
     */
    public void toggleTurboMode() {
        if (currentSpeedMode == SpeedMode.TURBO) {
            setSpeedMode(SpeedMode.NORMAL);
        } else {
            setSpeedMode(SpeedMode.TURBO);
        }
    }
    
    /**
     * Establece manualmente el modo robot/field centric.
     * Solo tiene efecto si NO hay robotCentricSupplier.
     * 
     * @param robotCentric true para robot-centric, false para field-centric
     */
    public void setRobotCentric(boolean robotCentric) {
        if (robotCentricSupplier == null) {
            this.isRobotCentric = robotCentric;
        }
    }
    
    /**
     * Toggle entre robot-centric y field-centric.
     */
    public void toggleCentricMode() {
        if (robotCentricSupplier == null) {
            isRobotCentric = !isRobotCentric;
        }
    }
    
    /**
     * Verifica si está en modo robot-centric.
     */
    public boolean isRobotCentric() {
        return isRobotCentric;
    }
    
    // ===== MÉTODOS PRIVADOS =====
    
    /**
     * Obtiene el multiplicador de velocidad según el modo actual.
     */
    private double getSpeedMultiplier() {
        switch (currentSpeedMode) {
            case SLOW:
                return DriveConstants.SLOW_SPEED;
            case TURBO:
                return DriveConstants.TURBO_SPEED;
            case NORMAL:
            default:
                return DriveConstants.NORMAL_SPEED;
        }
    }
}
