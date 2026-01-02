# Post-Mortem & Roadmap: MinerZ FTC

Documento de análisis tras el regional de GDL y registro de mejoras técnicas implementadas para la siguiente etapa.

## 1. Post-Mortem: Regional GDL (Fallas y Oportunidades)

Lista de problemas críticos detectados que impactaron el rendimiento en competencia y causaron *minor faults*.

### 🔴 Fallas Críticas (Software & Lógica)

- **Secuenciación de Disparo Incorrecta:** Error en la lógica de identificación de slots del Spindexer. El robot lanzaba en el orden de color incorrecto o no elegía la ruta más corta, retrasando el ciclo.
- **Sobrellenado del Intake (Minor Fault):** El intake no se detenía automáticamente al tener 3 elementos, permitiendo la entrada de un cuarto objeto y causando penalizaciones/daños potenciales.
- **Inestabilidad del Flywheel:** El control de velocidad no compensaba la caída de voltaje de la batería. El driver podía disparar sin que el sistema verificara si el flywheel había alcanzado la velocidad objetivo (Target Velocity), provocando atascos o tiros cortos.
- **Congelamiento de Secuencias:** La falta de manejo de estados concurrentes (como el conflicto en `IntakeAndIndexCommand`) causaba que el robot dejara de responder en medio de una secuencia.

### 🔴 Fallas de Navegación (Autonomous)

- **Calibración de Pedro Pathing:** El drivetrain no estaba correctamente calibrado para la librería Pedro Pathing.
  - *Consecuencia:* Incapacidad para ejecutar trayectorias complejas. El autónomo se limitó a movimientos básicos por tiempo/dead-reckoning.
  - *Resultado:* Máximo de 3 anotaciones por partida.
- **Falta de Alineación Automática:** Pérdida total de puntos en partidas donde el robot fue golpeado y desalineado, al no contar con sistema de visión para corrección de pose.

### 🔴 Experiencia del Driver (TeleOp)

- **Alta Carga Cognitiva:** El operador debía micro-gestionar cada acción (activar intake, girar spindexer, indexar, disparar). Eran demasiadas decisiones por segundo, resultando en ciclos lentos y fatiga.
- **Falta de Feedback:** Ausencia de indicadores legales (LEDs/Rumble) que informaran al driver sobre el estado del robot.

---

## 2. Roadmap: Soluciones e Implementaciones

Mejoras técnicas desarrolladas para resolver los problemas anteriores y añadir nuevas capacidades ("Power-Ups").

### ✅ Automatización y Lógica (Driver Assist)

*Objetivo: Reducir la carga cognitiva y proteger al robot.*

- [x] **Detección Robusta (Color/Presencia):** Implementación de `CheckSlotCommand` (Fast Check) y `DetectBallCommand` mejorados para un etiquetado de slots 100% fiable.
- [x] **Secuencia Intake Inteligente:** El sistema ahora conoce el estado de cada slot. **Bloquea** el intake si ya hay 3 elementos o si detecta una bola entrando cuando debería estar lleno.
- [x] **Secuencia de Lanzamiento Optimizada (Shortest Path):**
  - Nueva lógica que aprovecha el rango continuo del servo (0-300°).
  - Evita "vueltas largas" (ej. de 300° a 0° recorriendo todo el camino).
  - **Modo Fast (Secuencial):** Optimizado para ciclos rápidos (Intake 0° -> 120° -> 240° -> Outtake Slot 1 @300° -> Slot 0 @180° -> Slot 2 @60° -> Vuelta corta a Intake).
  - **Modo Ordenado:** Variante para cumplir requisitos de orden de color cuando sea necesario.

### ✅ Control Avanzado (Física)

*Objetivo: Consistencia mecánica y precisión.*

- [x] **Control Flywheel P + Feedforward (Voltage Comp):**
  - Implementación de controlador personalizado con compensación de voltaje (no nativo en librerías estándar).
  - Estabilización mucho más rápida.
- [x] **Bloqueo de Disparo:** El sistema impide físicamente que el Spindexer alimente bolas si `CurrentVelocity < TargetThreshold`.

### ✅ Navegación y Visión (Autonomous++)

*Objetivo: Ciclos autónomos consistentes (Min 9, Target 15).*

- [x] **Calibración Pedro Pathing:** Odometría ajustada para coincidir con la realidad física. Habilita trayectorias complejas confiables.
- [x] **Subsistema de Visión Custom (No-Limelight):**
  - **AprilTag Start:** Lee la configuración de la partida (Secuencia de color).
  - **AprilTag Goal:** Detecta el tag del objetivo para **Auto-Aim** (Torreta) + Calculo dinámico de velocidad de disparo (RBE).
  - **Relocalización Global:** Calcula la pose absoluta del robot para resetear la odometría en tiempo real (incluye matemáticas de transformación para compensar el movimiento de la cámara en la torreta).
- [x] **Rutina Autónoma Planeada:** Preload (3) + Movimiento a Pose Conocida + Intake (3) + Regreso + Disparo.

### ✅ Feedback y Nuevas Características

*Objetivo: Comunicación Robot-Humano.*

- [x] **Sistema de Feedback Sensorial:**
  - Vibración de Gamepad (Rumble) para alertas críticas.
  - Patrones de LEDs en Controlador (ya que no se permiten en robot) para indicar estados (Cargado, Listo para disparar, Error).
- [x] **Control de Torreta:** Integración completa de visión y motor de torreta para apuntado automático.
