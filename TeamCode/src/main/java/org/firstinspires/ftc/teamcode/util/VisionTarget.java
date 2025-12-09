// util/VisionTarget.java
package org.firstinspires.ftc.teamcode.util;

/**
 * Representa un AprilTag detectado con información de pose.
 * Inmutable y thread-safe.
 */
public class VisionTarget {

    public final int id;
    public final double range;         // Distancia en inches
    public final double bearing;       // Ángulo horizontal en grados (relativo a cámara)
    public final double elevation;     // Ángulo vertical en grados
    private final boolean isValid;     // Cambié a private para forzar uso del getter
    public final long timestamp;       // Cuándo se detectó (System.currentTimeMillis())

    /**
     * Constructor principal.
     */
    public VisionTarget(int id, double range, double bearing, double elevation,
                        boolean isValid, long timestamp) {
        this.id = id;
        this.range = range;
        this.bearing = bearing;
        this.elevation = elevation;
        this.isValid = isValid;
        this.timestamp = timestamp;
    }

    // ===== GETTERS =====

    /**
     * Verifica si la detección es válida.
     * @return true si el target fue detectado correctamente
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Verifica si la detección es reciente.
     * @param maxAgeMs Edad máxima en milisegundos
     * @return true si timestamp está dentro del límite
     */
    public boolean isRecent(long maxAgeMs) {
        if (!isValid) return false;
        return (System.currentTimeMillis() - timestamp) < maxAgeMs;
    }

    /**
     * Calcula la edad de la detección.
     * @return Edad en milisegundos
     */
    public long getAge() {
        if (!isValid) return Long.MAX_VALUE;
        return System.currentTimeMillis() - timestamp;
    }

    // ===== FACTORY METHODS =====

    /**
     * Crea un target inválido (sin detección).
     * Usado cuando no hay detección disponible.
     */
    public static VisionTarget invalid() {
        return new VisionTarget(-1, 0, 0, 0, false, 0);
    }

    /**
     * Crea un target válido con los datos proporcionados.
     * Helper para hacer el código más legible.
     */
    public static VisionTarget create(int id, double range, double bearing, double elevation) {
        return new VisionTarget(id, range, bearing, elevation, true, System.currentTimeMillis());
    }

    // ===== UTILIDADES =====

    @Override
    public String toString() {
        if (!isValid) return "VisionTarget{INVALID}";
        return String.format("VisionTarget{id=%d, range=%.1f\", bearing=%.1f°, age=%dms}",
                id, range, bearing, getAge());
    }

    /**
     * Verifica si dos targets son del mismo AprilTag.
     */
    public boolean isSameTag(VisionTarget other) {
        if (other == null || !isValid || !other.isValid) return false;
        return this.id == other.id;
    }
}