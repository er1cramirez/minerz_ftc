package org.firstinspires.ftc.teamcode.commands.sequences;

import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.seattlesolvers.solverslib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.commands.turret.RotateToGoalDirectionCommand;
import org.firstinspires.ftc.teamcode.commands.vision.DetectSequenceCommand;
import org.firstinspires.ftc.teamcode.commands.vision.UpdateRobotPoseCommand;
import org.firstinspires.ftc.teamcode.subsystems.DriveSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.TurretSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.VisionSubsystem.Alliance;

import java.util.function.Consumer;

/**
 * Secuencia de inicialización del autónomo que:
 * 1. Detecta la secuencia del obelisco
 * 2. Rota la torreta hacia el goal
 * 3. Actualiza la pose del robot usando el AprilTag del goal
 * 
 * Esta secuencia debe ejecutarse al inicio del autónomo para:
 * - Conocer la secuencia de colores del match
 * - Mejorar la precisión del autónomo con pose absoluta
 */
public class AutoInitSequenceCommand extends SequentialCommandGroup {

    private String detectedSequence;
    private boolean poseUpdated;

    /**
     * Crea la secuencia de inicialización.
     * 
     * @param vision Subsystem de visión
     * @param drive Subsystem de drivetrain
     * @param turret Subsystem de torreta
     * @param alliance Alianza del match (RED o BLUE)
     * @param onSequenceDetected Callback cuando se detecta secuencia
     */
    public AutoInitSequenceCommand(VisionSubsystem vision,
                                    DriveSubsystem drive,
                                    TurretSubsystem turret,
                                    Alliance alliance,
                                    Consumer<String> onSequenceDetected) {

        addCommands(
            // Paso 1: Detectar secuencia del obelisco
            // La torreta debe estar mirando hacia el obelisco inicialmente
            new DetectSequenceCommand(vision, sequence -> {
                this.detectedSequence = sequence;
                if (onSequenceDetected != null) {
                    onSequenceDetected.accept(sequence);
                }
            }),

            // Paso 2: Pequeña pausa para estabilizar
            new WaitCommand(100),

            // Paso 3: Rotar torreta hacia el goal de nuestra alianza
            // Este comando debe ser creado según tu implementación de TurretSubsystem
            // Ejemplo: rota a posición conocida hacia el goal
            new RotateToGoalDirectionCommand(turret, alliance),

            // Paso 4: Pequeña pausa para estabilizar después de rotación
            new WaitCommand(200),

            // Paso 5: Actualizar pose del robot usando el AprilTag del goal
            new UpdateRobotPoseCommand(vision, drive, turret, pose -> {
                this.poseUpdated = (pose != null);
            }, 2000),  // 2 segundos de timeout

            // Paso 6: Log del resultado (opcional)
            new InstantCommand(() -> {
                System.out.println("Auto Init Complete:");
                System.out.println("  Sequence: " + detectedSequence);
                System.out.println("  Pose Updated: " + poseUpdated);
            })
        );

        // Agregar requirements de todos los subsystems usados
        addRequirements(vision, turret);
    }

    /**
     * Obtiene la secuencia detectada.
     * Solo válido después de que el comando termine.
     */
    public String getDetectedSequence() {
        return detectedSequence;
    }

    /**
     * Verifica si la pose fue actualizada exitosamente.
     */
    public boolean wasPoseUpdated() {
        return poseUpdated;
    }
}
