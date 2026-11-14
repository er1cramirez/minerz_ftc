# Arquitectura del código — MinerZ FTC 2026-27 (BIOBUZZ)

## Stack

| Pieza | Qué es | Versión |
|---|---|---|
| FTC SDK | Base (fork de FtcRobotController) | 12.0.0 |
| Pedro Pathing | Localización y seguimiento de paths | `revhub:3.0.2` + `tuning:1.0.0` |
| Ivy | Scheduler de comandos (del ecosistema Pedro) | `ivy:pedro:1.1.1` |
| NextFTC v2 | Framework robot/mechanisms/opmodes sobre Ivy | `0.3.0` |
| Kotlin | Lenguaje del código nuevo | plugin `2.4.20` |

El código nuevo se escribe en **Kotlin**. El paquete `pedro/` queda en **Java** porque
los tuners de Pedro generan código Java para pegar.

## Ramas (las de código; `master` solo sigue al SDK)

```
master (SDK) → pedro-base → chasis-hub   → <ramas de robot 1>
                          → chasis-spark → <ramas de robot 2>
```

- **pedro-base**: infraestructura compartida. Mechanisms comunes, OpModes compartidos,
  plantillas. Los cambios fluyen SOLO hacia abajo (merge de pedro-base en las ramas hijas).
- **chasis-***: valores de tuning del chasis (`pedro/ChasisConfig.java`) y hardware
  específico (p. ej. `SparkMiniMecanum`).
- **ramas de robot**: mechanisms del robot (shooter, turret, hood...) y su `Robot.kt`.

## Archivos "plantilla" (se editan en la rama hija, NUNCA otra vez en pedro-base)

| Archivo | Se llena en |
|---|---|
| `pedro/ChasisConfig.java` | rama de chasis (pegar salida de los tuners) |
| `Robot.kt` | rama de robot (listar sus mechanisms, quitar `Demo`) |

Romper esta regla = conflictos en cada merge hacia abajo.

## Layout de paquetes (`org.firstinspires.ftc.teamcode`)

```
Robot.kt            ← LA clase NextRobot (debe existir exactamente UNA)
mechanisms/         ← un archivo por subsystem (hardware + sus comandos)
opmodes/teleop/
opmodes/auto/
pedro/              ← config y armado del Follower (Java, no tocar la lógica)
chasis/             ← drivetrains específicos (solo en su rama de chasis)
```

NextFTC v2 descubre `Robot` y los OpModes escaneando el classpath: no hay listas de
registro. Un OpMode es `@NextTeleop`/`@NextAutonomous` + subclase de `NextOpMode`
que recibe `Robot` por constructor.

## OpModes de verificación

- **Next Smoke Test**: sin hardware; valida Sloth + Ivy + NextFTC + Kotlin.
- **Chasis TeleOp**: drive robot-centric con hold de pose; requiere tuning de Pedro.
- **Tuners de Pedro**: registrados por `pedro/Tuning.java` (ver `TUNING.md`).

## Referencias

- Pedro Pathing: https://pedropathing.com/docs — NextFTC v2: https://nextftc.dev
- Ejemplo BIOBUZZ Kotlin con este mismo stack: https://github.com/ftc13100/BioBuzz26-27
