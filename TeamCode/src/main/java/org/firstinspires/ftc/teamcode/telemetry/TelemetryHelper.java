package org.firstinspires.ftc.teamcode.telemetry;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * Helper para construir telemetría compacta optimizada para la Driver Station.
 * 
 * Soporta tres modos de display:
 * - CLASSIC: Texto plano estándar
 * - MONOSPACE: Fuente monoespaciada (mejor alineación)
 * - HTML: Texto enriquecido con colores y estilos
 * 
 * Uso típico:
 *   TelemetryHelper helper = new TelemetryHelper(telemetry);
 *   helper.setDisplayMode(DisplayMode.HTML);
 *   
 *   // En el loop:
 *   helper.begin();
 *   helper.addHeader("MINERZ FTC");
 *   helper.addDriveLine(driveMode, heading, isFieldCentric);
 *   helper.addLine(spindexer.getCompactStatus());
 *   helper.end();
 */
public class TelemetryHelper {
    
    public enum DisplayMode {
        CLASSIC,
        MONOSPACE,
        HTML
    }
    
    private final Telemetry telemetry;
    private DisplayMode currentMode = DisplayMode.CLASSIC;
    private StringBuilder lineBuilder = new StringBuilder();
    
    // Símbolos Unicode para telemetría compacta
    public static final String ICON_DRIVE = "🚗";
    public static final String ICON_SPINDEXER = "📥";
    public static final String ICON_TURRET = "🔄";
    public static final String ICON_FLYWHEEL = "🔥";
    public static final String ICON_EJECTOR = "📤";
    public static final String ICON_BATTERY = "🔋";
    public static final String ICON_TIMER = "⏱";
    
    public static final String ICON_TARGET = "🎯";
    public static final String ICON_SEARCH = "🔍";
    public static final String ICON_HOLD = "⏸";
    
    public static final String CHECK = "✓";
    public static final String CROSS = "✗";
    
    public static final String SEPARATOR_DOUBLE = "═══════════════════════════";
    public static final String SEPARATOR_SINGLE = "───────────────────────────";
    public static final String SEPARATOR_DOTTED = "╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌";
    
    public TelemetryHelper(Telemetry telemetry) {
        this.telemetry = telemetry;
    }
    
    // ==================== CONFIGURATION ====================
    
    /**
     * Establece el modo de display.
     * Debe llamarse antes de begin().
     */
    public void setDisplayMode(DisplayMode mode) {
        this.currentMode = mode;
        switch (mode) {
            case MONOSPACE:
                telemetry.setDisplayFormat(Telemetry.DisplayFormat.MONOSPACE);
                break;
            case HTML:
                telemetry.setDisplayFormat(Telemetry.DisplayFormat.HTML);
                break;
            default:
                telemetry.setDisplayFormat(Telemetry.DisplayFormat.CLASSIC);
        }
    }
    
    public DisplayMode getDisplayMode() {
        return currentMode;
    }
    
    /**
     * Cicla al siguiente modo de display.
     * @return El nuevo modo activo.
     */
    public DisplayMode cycleDisplayMode() {
        DisplayMode[] modes = DisplayMode.values();
        int next = (currentMode.ordinal() + 1) % modes.length;
        setDisplayMode(modes[next]);
        return currentMode;
    }
    
    /**
     * Desactiva autoClear para items retenidos.
     */
    public void setAutoClear(boolean autoClear) {
        telemetry.setAutoClear(autoClear);
    }
    
    /**
     * Establece el intervalo de transmisión en ms.
     */
    public void setTransmissionInterval(int ms) {
        telemetry.setMsTransmissionInterval(ms);
    }
    
    // ==================== BUILDING LINES ====================
    
    /**
     * Limpia la telemetría antes de construir.
     */
    public void begin() {
        telemetry.clear();
    }
    
    /**
     * Envía la telemetría construida a la DS.
     */
    public void end() {
        telemetry.update();
    }
    
    /**
     * Agrega un header principal con separadores.
     * En HTML usa texto grande y negrita.
     */
    public void addHeader(String title) {
        if (currentMode == DisplayMode.HTML) {
            telemetry.addLine("<big><b>" + SEPARATOR_DOUBLE.substring(0, 3) + " " + title + " " + SEPARATOR_DOUBLE.substring(0, 3) + "</b></big>");
        } else {
            telemetry.addLine(SEPARATOR_DOUBLE.substring(0, 3) + " " + title + " " + SEPARATOR_DOUBLE.substring(0, 3));
        }
    }
    
    /**
     * Agrega un header de sección con color.
     * @param title Título de la sección
     * @param htmlColor Color del texto (solo aplica en HTML)
     */
    public void addSectionHeader(String title, String htmlColor) {
        if (currentMode == DisplayMode.HTML) {
            telemetry.addLine("<font color='" + htmlColor + "'><b>━━ " + title + " ━━</b></font>");
        } else {
            telemetry.addLine("── " + title + " ──");
        }
    }
    
    /**
     * Agrega un header de sección con color por defecto (cyan).
     */
    public void addSectionHeader(String title) {
        addSectionHeader(title, "#00BFFF");
    }
    
    /**
     * Agrega un separador visual.
     */
    public void addSeparator() {
        if (currentMode == DisplayMode.HTML) {
            telemetry.addLine("<font color='gray'>" + SEPARATOR_DOTTED + "</font>");
        } else {
            telemetry.addLine(SEPARATOR_DOTTED);
        }
    }
    
    /**
     * Agrega una línea vacía para espaciado.
     */
    public void addSpace() {
        telemetry.addLine("");
    }
    
    /**
     * Agrega una línea simple.
     */
    public void addLine(String text) {
        telemetry.addLine(text);
    }
    
    /**
     * Agrega línea con formato condicional HTML.
     */
    public void addLine(String text, String htmlColor) {
        if (currentMode == DisplayMode.HTML && htmlColor != null) {
            telemetry.addLine("<font color='" + htmlColor + "'>" + text + "</font>");
        } else {
            telemetry.addLine(text);
        }
    }
    
    /**
     * Agrega línea en negrita (solo HTML).
     */
    public void addBoldLine(String text) {
        if (currentMode == DisplayMode.HTML) {
            telemetry.addLine("<b>" + text + "</b>");
        } else {
            telemetry.addLine(text);
        }
    }
    
    /**
     * Agrega línea con texto grande (solo HTML).
     */
    public void addBigLine(String text) {
        if (currentMode == DisplayMode.HTML) {
            telemetry.addLine("<big>" + text + "</big>");
        } else {
            telemetry.addLine(text);
        }
    }
    
    /**
     * Agrega línea con texto grande, negrita y color.
     */
    public void addBigBoldLine(String text, String htmlColor) {
        if (currentMode == DisplayMode.HTML) {
            telemetry.addLine("<big><b><font color='" + htmlColor + "'>" + text + "</font></b></big>");
        } else {
            telemetry.addLine(text);
        }
    }
    
    // ==================== FORMATTED LINES ====================
    
    /**
     * Línea de Drive: modo, heading, centric mode.
     * En HTML usa texto grande para mejor visibilidad.
     */
    public void addDriveLine(String driveMode, double headingDeg, boolean isFieldCentric) {
        String centricIcon = isFieldCentric ? "⬛" : "🤖";
        String centricText = isFieldCentric ? "FIELD" : "ROBOT";
        String line = String.format("%s %s | 🧭%.0f° | %s %s", 
                ICON_DRIVE, driveMode, headingDeg, centricIcon, centricText);
        
        if (currentMode == DisplayMode.HTML) {
            String modeColor = driveMode.equals("SLOW") ? "yellow" : 
                              (driveMode.equals("FAST") ? "red" : "lime");
            telemetry.addLine(String.format("<big>%s <font color='%s'><b>%s</b></font> | 🧭%.0f° | %s %s</big>",
                    ICON_DRIVE, modeColor, driveMode, headingDeg, centricIcon, centricText));
        } else {
            telemetry.addLine(line);
        }
    }
    
    /**
     * Línea de Flywheel: RPM actual/target, estado ready.
     * En HTML usa texto grande para el estado ready.
     */
    public void addFlywheelLine(double currentRPM, double targetRPM, boolean isReady) {
        String readyStr = isReady ? CHECK + "READY" : CROSS + "SPIN";
        String line = String.format("%s %.0f/%.0fRPM %s", 
                ICON_FLYWHEEL, currentRPM, targetRPM, readyStr);
        
        if (currentMode == DisplayMode.HTML) {
            String color = isReady ? "lime" : "orange";
            telemetry.addLine(String.format("<big>%s %.0f/%.0fRPM <font color='%s'><b>%s</b></font></big>",
                    ICON_FLYWHEEL, currentRPM, targetRPM, color, readyStr));
        } else {
            telemetry.addLine(line);
        }
    }
    
    /**
     * Línea de Turret: ángulo, estado de tracking.
     * En HTML usa colores para distinguir estados.
     */
    public void addTurretLine(double angleDeg, String visionState, boolean isLocked) {
        String stateIcon = isLocked ? ICON_TARGET : (visionState.contains("SEARCH") ? ICON_SEARCH : ICON_HOLD);
        String line = String.format("%s ↻%.0f° %s%s", 
                ICON_TURRET, angleDeg, stateIcon, visionState);
        
        if (currentMode == DisplayMode.HTML) {
            String color = isLocked ? "lime" : "cyan";
            telemetry.addLine(String.format("<big>%s ↻%.0f° <font color='%s'><b>%s%s</b></font></big>",
                    ICON_TURRET, angleDeg, color, stateIcon, visionState));
        } else {
            telemetry.addLine(line);
        }
    }
    
    /**
     * Línea de sistema: tiempo, loop time, batería.
     */
    public void addSystemLine(double voltage) {
        String line = String.format("%s%.1fV",
                ICON_BATTERY, voltage);
        
        if (currentMode == DisplayMode.HTML) {
            String voltColor = voltage < 11.5 ? "red" : (voltage < 12.0 ? "yellow" : "lime");
            telemetry.addLine(String.format("%s<font color='%s'>%.1fV</font>",
                    ICON_BATTERY, voltColor, voltage));
        } else {
            telemetry.addLine(line);
        }
    }
    
    // ==================== DATA ITEMS ====================
    
    /**
     * Agrega un item de datos con caption y valor.
     */
    public void addData(String caption, Object value) {
        telemetry.addData(caption, value);
    }
    
    /**
     * Agrega un item de datos con formato.
     */
    public void addData(String caption, String format, Object... args) {
        telemetry.addData(caption, format, args);
    }
    
    // ==================== UTILITY ====================
    
    /**
     * Formatea tiempo en mm:ss.
     */
    public static String formatTime(double seconds) {
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%d:%02d", mins, secs);
    }
    
    /**
     * Crea una barra de progreso visual.
     * @param value Valor actual
     * @param max Valor máximo
     * @param width Ancho en caracteres
     */
    public static String progressBar(double value, double max, int width) {
        int filled = (int) ((value / max) * width);
        filled = Math.max(0, Math.min(width, filled));
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }
    
    /**
     * Retorna el raw Telemetry para uso avanzado.
     */
    public Telemetry getRawTelemetry() {
        return telemetry;
    }
}
