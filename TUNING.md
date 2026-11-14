# Tuning de Pedro Pathing 3 — MinerZ

Guía paso a paso para tunear cada chasis. Aplica a las dos variantes:

| Variante | Rama | Motores del drivetrain |
|---|---|---|
| **HUB** | `chasis-hub` | goBILDA conectados directo al Control Hub |
| **SPARK** | `chasis-spark` | goBILDA conectados con REV SPARKmini (puertos de servo) |

Las dos usan **goBILDA Pinpoint** con pods goBILDA.

> **Regla de oro:** todo el tuning se pega en `TeamCode/.../pedro/ChasisConfig.java` **de la rama de chasis**.
> Nunca en `pedro-base`. Después se baja a las ramas de robot con `git merge`.

---

## 0. Antes de empezar

### En la computadora
```bash
git checkout chasis-hub      # o chasis-spark, según el robot que tienes enfrente
git pull
```
Compila y sube el código al robot (**Run** en Android Studio).

### En el robot — configuración (Driver Hub → Configure Robot)

**Pinpoint (ambas variantes)**
- Conectado en un puerto **I2C distinto del 0** (el 0 lo usa el IMU del Control Hub).
- Tipo de dispositivo: *goBILDA Pinpoint Odometry Computer*, nombre: `pinpoint`.
- Montado con la **etiqueta (lado de los puertos) hacia arriba**.
- Pod paralelo al largo del chasis (forward) → puerto **X**. Pod perpendicular (strafe) → puerto **Y**.

**Motores — variante HUB**
- Puertos de motor del Control Hub/Expansion Hub, tipo *goBILDA 5202/3/4 series*.
- Nombres: `left_front`, `right_front`, `left_back`, `right_back`.

**Motores — variante SPARK**
- Cada SPARKmini en un **puerto de servo**, tipo *REV SPARKmini Controller*.
- Mismos nombres: `left_front`, `right_front`, `left_back`, `right_back`.
- **Interruptor de cada SPARKmini en COAST.** Pedro sigue trayectorias en coast y frena aplicando
  potencia; el Foresight Tuner mide la desaceleración natural en coast. Con BRAKE los valores
  medidos no corresponden al modelo de Pedro.
- Consecuencia: en TeleOp este chasis no frena solo al soltar el stick (`manualBrakeMode` no aplica).

> Si cambias algún nombre en el Driver Hub, cámbialo también en `ChasisConfig.java`.

### Abrir AutoTune
1. Conecta la computadora al WiFi del robot.
2. Abre **http://192.168.43.1:10158**
3. Elige el *Procedure* que indica cada paso.

### Aplicar resultados (igual en todos los tuners)
1. Al terminar, abre la pestaña **Java**.
2. Copia **solo el contenido** de `c -> { ... }` y reemplaza el contenido del bloque correspondiente en `ChasisConfig.java`.
3. Compila, sube el código y haz commit:
   ```bash
   git commit -am "Tuning <chasis>: <paso>"
   ```

Batería cargada en todos los pasos. Los resultados cambian con batería baja.

---

## 1. Mecanum Tuner — nombres y direcciones de motores

**Procedure:** *Mecanum Tuner* · **Bloque:** `drivetrainConfig`

1. Escribe el nombre de cada motor según su posición y presiona **Submit**.
2. Robot **levantado** (ruedas sin tocar el piso).
3. Cada motor girará uno por uno. Usa el diagrama 3D para ver hacia dónde *debería* girar
   y responde si giró hacia adelante (*Forward*) o al revés (*Reversed*).
4. Si giró un motor distinto al indicado, el tuner aborta: revisa puertos y nombres.
5. Pega el resultado en `drivetrainConfig`.

| | HUB | SPARK |
|---|---|---|
| `c.manualBrakeMode.set(true);` | Conservarlo (el tuner no lo genera; agrégalo al pegar) | No aplica, no agregarlo |

## 2. Tests → Driving — verificar el drivetrain

**Procedure:** *Tests* → **Driving Test**

Maneja con el gamepad 1: stick izquierdo traslada, stick derecho gira.
Debe avanzar, deslizarse a la izquierda y girar en el sentido del stick. Si algo va invertido, repite el paso 1.

## 3. Pinpoint Tuner — direcciones y offsets de odometría

**Procedure:** *Pinpoint Tuner* · **Bloque:** `localizerConfig`

1. **HardwareMap Name:** `pinpoint`. **Pod Type:** el modelo goBILDA instalado (*Four Bar* o *Swing Arm*). **Submit**.
2. **Forward Direction:** Continue → **empuja el robot recto hacia adelante** → detén el OpMode.
3. **Strafe Direction:** Continue → **empuja el robot hacia la izquierda** → detén el OpMode.
4. **Offsets:** Continue → **gira el robot en su lugar 180° en sentido antihorario** → detén el OpMode.
5. Pega el resultado en `localizerConfig`.

> Los offsets también se pueden medir a mano (pulgadas, respecto al centro de rotación),
> pero el tuner suele ser más preciso.

## 4. Tests → Odometry / Localization — verificar el Pinpoint

**Procedure:** *Tests*

- **Odometry Test** (automático): el robot avanza, se desliza a la izquierda y gira antihorario.
  Debe mostrar `xPod: Good`, `yPod: Good`, `Heading: Good`.
  - `Flipped` → dirección del pod invertida (repite el paso 3).
  - `Too high` / `Too low` → tipo de pod incorrecto.
- **Localization Test** (manual): maneja con el gamepad y observa la pose.
  Hacia adelante **aumenta x**; hacia la izquierda **aumenta y**; antihorario **aumenta heading**.

**No sigas al paso 5 hasta que la odometría esté bien:** Foresight depende de ella.

## 5. Foresight Tuner — algoritmo de seguimiento

**Procedure:** *Foresight Tuner* · **Bloque:** `foresightConfig`

Necesitas espacio libre: **al menos 48 in** al frente y a la izquierda, y espacio alrededor
para las pruebas de frenado. Coloca el robot con espacio en todas direcciones.

Valores que pide (puedes dejar los default):
- Distance (velocidad máxima): **48 in**
- Velocity (desaceleración): **30 in/s**
- Distance (frenado): **36 in**

Secuencia de subpruebas (Continue entre cada una):

| # | Prueba | Qué hace el robot |
|---|---|---|
| 1 | Forward Velocity | Avanza la distancia a máxima potencia (se pasa un poco: es normal) |
| 2 | Strafe Velocity | Igual, hacia la izquierda |
| 3 | Forward Deceleration | Avanza hasta la velocidad indicada y se deja ir |
| 4 | Strafe Deceleration | Igual, hacia la izquierda |
| 5 | Heading Braking | Gira de un lado a otro en su lugar |
| 6 | Heading Tuner | Gira en su lugar un par de segundos |
| 7 | Forward Braking | Avanza, frena y regresa varias veces a distintas potencias |
| 8 | Strafe Braking | Igual, izquierda/derecha |
| 9 | Forward Translational | Se mueve ~12–24 in al frente (también calcula `coast` y `brake`) |
| 10 | Strafe Translational | Se mueve ~12–24 in a los lados |

Pega el resultado completo en `foresightConfig` (incluye las líneas `Controller ... = ...` del inicio).

## 6. Tests finales — Follower completo

**Procedure:** *Tests*

| Test | Qué verificar |
|---|---|
| **Hold** | Empuja el robot: debe regresar a su posición y heading |
| **Line** | Va y vuelve 48 in en línea recta; debe detenerse cerca del punto final sin oscilar |
| **Curve** | Cuarto de círculo hasta (48, 48) con heading tangente, y de regreso |
| **Interpolation** | Misma curva con distintos modos de heading |

Si Line se pasa del punto final, normalmente es frenado (repite el paso 5 con batería cargada).
Si oscila en heading, revisa el paso 4.

---

## 7. Cerrar el tuning

```bash
git commit -am "Tuning <chasis> completo"
git push
# Bajar el tuning al robot que usa este chasis:
git checkout robot-<nombre>
git merge chasis-<hub|spark>
git push
```

## Resumen del orden

```
Configurar robot → 1 Mecanum → 2 Driving → 3 Pinpoint → 4 Odometry/Localization
                 → 5 Foresight → 6 Hold/Line/Curve → 7 commit + merge
```
