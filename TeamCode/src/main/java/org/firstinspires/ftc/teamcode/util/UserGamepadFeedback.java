package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Gamepad.LedEffect;
import com.qualcomm.robotcore.hardware.Gamepad.RumbleEffect;

/**
 * Catálogo de efectos de feedback para el gamepad.
 * 
 * Uso:
 * gamepad.runRumbleEffect(UserGamepadFeedback.ERROR_GENERIC);
 * gamepad.runLedEffect(UserGamepadFeedback.LED_ERROR);
 * // O usar helpers combinados:
 * UserGamepadFeedback.playError(gamepad);
 */
public class UserGamepadFeedback {

        // ==================== RUMBLE EFFECTS ====================

        /**
         * Error genérico: 2 pulsos cortos.
         * Uso: Acción no permitida.
         */
        public static final RumbleEffect ERROR_GENERIC = new RumbleEffect.Builder()
                        .addStep(0.8, 0.8, 100)
                        .addStep(0, 0, 80)
                        .addStep(0.8, 0.8, 100)
                        .build();

        /**
         * Warning: 1 pulso largo + 1 corto.
         * Uso: Spindexer lleno, batería baja.
         */
        public static final RumbleEffect WARNING_FULL = new RumbleEffect.Builder()
                        .addStep(1.0, 1.0, 300)
                        .addStep(0, 0, 100)
                        .addStep(0.6, 0.6, 100)
                        .build();

        /**
         * Éxito suave: 1 pulso breve y suave.
         * Uso: Ball detectada y guardada.
         */
        public static final RumbleEffect SUCCESS_INTAKE = new RumbleEffect.Builder()
                        .addStep(0.3, 0.3, 150)
                        .build();

        /**
         * Ready to shoot: 3 pulsos rápidos ascendentes.
         * Uso: Flywheel at speed + Target locked.
         */
        public static final RumbleEffect READY_TO_SHOOT = new RumbleEffect.Builder()
                        .addStep(0.4, 0.4, 80)
                        .addStep(0, 0, 50)
                        .addStep(0.7, 0.7, 80)
                        .addStep(0, 0, 50)
                        .addStep(1.0, 1.0, 100)
                        .build();

        /**
         * Target acquired: Rampa ascendente suave.
         * Uso: Turret locked on target.
         */
        public static final RumbleEffect TARGET_ACQUIRED = new RumbleEffect.Builder()
                        .addStep(0.2, 0.2, 100)
                        .addStep(0.4, 0.4, 100)
                        .addStep(0.6, 0.6, 100)
                        .addStep(0.8, 0.8, 100)
                        .build();

        /**
         * Target lost: Rampa descendente.
         * Uso: Turret perdió el target.
         */
        public static final RumbleEffect TARGET_LOST = new RumbleEffect.Builder()
                        .addStep(0.8, 0.8, 100)
                        .addStep(0.5, 0.5, 100)
                        .addStep(0.2, 0.2, 100)
                        .build();

        /**
         * Low battery warning: Pulso lento.
         * Uso: Voltaje bajo detectado.
         */
        public static final RumbleEffect LOW_BATTERY = new RumbleEffect.Builder()
                        .addStep(0.3, 0.3, 500)
                        .addStep(0, 0, 500)
                        .addStep(0.3, 0.3, 500)
                        .build();

        /**
         * Countdown tick: Pulso único corto.
         * Uso: Cuenta regresiva, confirmación.
         */
        public static final RumbleEffect TICK = new RumbleEffect.Builder()
                        .addStep(0.5, 0.5, 50)
                        .build();

        // ==================== LED EFFECTS ====================

        /**
         * Purple: detected purple ball
         * 157,0,255
         */
        public static final LedEffect LED_PURPLE = new LedEffect.Builder()
                        .addStep(157 / 255, 0, 255 / 255, 150) // Purple
                        .addStep(0, 0, 0, 100) // Off
                        .addStep(157 / 255, 0, 255 / 255, 150) // Purple
                        .addStep(0, 0, 0, 100) // Off
                        .build();

        /**
         * Green: detected green ball
         * 0,187,119
         */
        public static final LedEffect LED_GREEN = new LedEffect.Builder()
                        .addStep(0, 187 / 255, 119 / 255, 150) // Green
                        .addStep(0, 0, 0, 100) // Off
                        .addStep(0, 187 / 255, 119 / 255, 150) // Green
                        .addStep(0, 0, 0, 100) // Off
                        .build();

        /**
         * Error: Rojo parpadeante.
         */
        public static final LedEffect LED_ERROR = new LedEffect.Builder()
                        .addStep(1, 0, 0, 150) // Rojo
                        .addStep(0, 0, 0, 100) // Off
                        .addStep(1, 0, 0, 150) // Rojo
                        .addStep(0, 0, 0, 100) // Off
                        .build();

        /**
         * Warning: Amarillo sólido.
         */
        public static final LedEffect LED_WARNING = new LedEffect.Builder()
                        .addStep(1, 0.7, 0, 500) // Amarillo/Naranja
                        .build();

        /**
         * Ready: Verde sólido.
         */
        public static final LedEffect LED_READY = new LedEffect.Builder()
                        .addStep(0, 1, 0, 200) // Verde
                        .addStep(1, 1, 1, 100) // Flash blanco
                        .addStep(0, 1, 0, 500) // Verde mantenido
                        .build();

        /**
         * Targeting: Azul pulsante (buscando).
         */
        public static final LedEffect LED_TARGETING = new LedEffect.Builder()
                        .addStep(0, 0, 1, 200) // Azul
                        .addStep(0, 0, 0.3, 200) // Azul tenue
                        .addStep(0, 0, 1, 200) // Azul
                        .addStep(0, 0, 0.3, 200) // Azul tenue
                        .build();

        /**
         * Locked: Verde pulsante (target locked).
         */
        public static final LedEffect LED_LOCKED = new LedEffect.Builder()
                        .addStep(0, 1, 0, 150) // Verde
                        .addStep(0, 0.5, 0, 150) // Verde tenue
                        .addStep(0, 1, 0, 150) // Verde
                        .addStep(0, 0.5, 0, 150) // Verde tenue
                        .addStep(0, 1, 0, 300) // Verde final
                        .build();

        /**
         * Success: Flash verde rápido.
         */
        public static final LedEffect LED_SUCCESS = new LedEffect.Builder()
                        .addStep(0, 1, 0, 100) // Verde
                        .addStep(1, 1, 1, 50) // Flash blanco
                        .addStep(0, 1, 0, 150) // Verde
                        .build();

        /**
         * Rainbow: Ciclo de colores (idle/celebration).
         */
        public static final LedEffect LED_RAINBOW = new LedEffect.Builder()
                        .addStep(1, 0, 0, 150) // Rojo
                        .addStep(1, 0.5, 0, 150) // Naranja
                        .addStep(1, 1, 0, 150) // Amarillo
                        .addStep(0, 1, 0, 150) // Verde
                        .addStep(0, 0, 1, 150) // Azul
                        .addStep(0.5, 0, 1, 150) // Violeta
                        .build();

        // ==================== COMBINED EFFECTS (HELPERS) ====================

        /**
         * Reproduce efecto de error (rumble + LED rojo).
         */
        public static void playError(Gamepad gamepad) {
                gamepad.runRumbleEffect(ERROR_GENERIC);
                gamepad.runLedEffect(LED_ERROR);
        }

        /**
         * Reproduce efecto de warning (rumble + LED amarillo).
         */
        public static void playWarning(Gamepad gamepad) {
                gamepad.runRumbleEffect(WARNING_FULL);
                gamepad.runLedEffect(LED_WARNING);
        }

        /**
         * Reproduce efecto de éxito/intake (rumble suave + LED verde).
         */
        public static void playSuccess(Gamepad gamepad) {
                gamepad.runRumbleEffect(SUCCESS_INTAKE);
                gamepad.runLedEffect(LED_SUCCESS);
        }

        /**
         * Reproduce efecto de ready to shoot (rumble ascendente + LED verde).
         */
        public static void playReady(Gamepad gamepad) {
                gamepad.runRumbleEffect(READY_TO_SHOOT);
                gamepad.runLedEffect(LED_READY);
        }

        /**
         * Reproduce efecto de target acquired (rampa + LED locked).
         */
        public static void playTargetAcquired(Gamepad gamepad) {
                gamepad.runRumbleEffect(TARGET_ACQUIRED);
                gamepad.runLedEffect(LED_LOCKED);
        }

        /**
         * Reproduce efecto de target lost (rampa descendente).
         */
        public static void playTargetLost(Gamepad gamepad) {
                gamepad.runRumbleEffect(TARGET_LOST);
                // No LED - solo vibración para no distraer
        }

        /**
         * Reproduce efecto de batería baja.
         */
        public static void playLowBattery(Gamepad gamepad) {
                gamepad.runRumbleEffect(LOW_BATTERY);
                gamepad.runLedEffect(LED_WARNING);
        }

        /**
         * Green ball detected
         */
        public static void playGreenBall(Gamepad gamepad) {
                gamepad.runRumbleEffect(SUCCESS_INTAKE);
                gamepad.runLedEffect(LED_GREEN);
        }

        /**
         * Purple ball detected
         */
        public static void playPurpleBall(Gamepad gamepad) {
                gamepad.runRumbleEffect(SUCCESS_INTAKE);
                gamepad.runLedEffect(LED_PURPLE);
        }
}
