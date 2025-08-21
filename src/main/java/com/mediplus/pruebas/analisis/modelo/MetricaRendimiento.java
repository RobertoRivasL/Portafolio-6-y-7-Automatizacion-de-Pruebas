package com.mediplus.pruebas.analisis.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modelo que representa una métrica de rendimiento de un escenario de prueba
 * Contiene todas las métricas relevantes para análisis de performance
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class MetricaRendimiento {

    private final String nombreEscenario;
    private final int usuariosConcurrentes;
    private final double tiempoPromedioMs;
    private final double percentil90Ms;
    private final double percentil95Ms;
    private final double throughputReqSeg;
    private final double tasaErrorPorcentaje;
    private final double tiempoMinimoMs;
    private final double tiempoMaximoMs;
    private final int duracionPruebaSegundos;
    private final LocalDateTime fechaEjecucion;

    private MetricaRendimiento(Builder builder) {
        this.nombreEscenario = validarNoVacio(builder.nombreEscenario, "nombreEscenario");
        this.usuariosConcurrentes = validarPositivo(builder.usuariosConcurrentes, "usuariosConcurrentes");
        this.tiempoPromedioMs = validarNoNegativo(builder.tiempoPromedioMs, "tiempoPromedioMs");
        this.percentil90Ms = validarNoNegativo(builder.percentil90Ms, "percentil90Ms");
        this.percentil95Ms = validarNoNegativo(builder.percentil95Ms, "percentil95Ms");
        this.throughputReqSeg = validarNoNegativo(builder.throughputReqSeg, "throughputReqSeg");
        this.tasaErrorPorcentaje = validarRango(builder.tasaErrorPorcentaje, 0, 100, "tasaErrorPorcentaje");
        this.tiempoMinimoMs = validarNoNegativo(builder.tiempoMinimoMs, "tiempoMinimoMs");
        this.tiempoMaximoMs = validarNoNegativo(builder.tiempoMaximoMs, "tiempoMaximoMs");
        this.duracionPruebaSegundos = validarPositivo(builder.duracionPruebaSegundos, "duracionPruebaSegundos");
        this.fechaEjecucion = builder.fechaEjecucion != null ? builder.fechaEjecucion : LocalDateTime.now();
    }

    /**
     * Evalúa el nivel de rendimiento basado en umbrales definidos
     */
    public NivelRendimiento evaluarNivelRendimiento() {
        // Criterios de evaluación basados en tiempo de respuesta y tasa de error
        if (tasaErrorPorcentaje > 15.0) {
            return NivelRendimiento.INACEPTABLE;
        }

        if (tiempoPromedioMs > 3000 || tasaErrorPorcentaje > 10.0) {
            return NivelRendimiento.MALO;
        }

        if (tiempoPromedioMs > 2000 || tasaErrorPorcentaje > 5.0) {
            return NivelRendimiento.REGULAR;
        }

        if (tiempoPromedioMs > 1000 || tasaErrorPorcentaje > 2.0) {
            return NivelRendimiento.BUENO;
        }

        return NivelRendimiento.EXCELENTE;
    }

    /**
     * Calcula la eficiencia del escenario (throughput vs tiempo)
     */
    public double calcularEficiencia() {
        if (tiempoPromedioMs == 0) return 0.0;
        return throughputReqSeg / (tiempoPromedioMs / 1000.0); // Requests por segundo por segundo de latencia
    }

    /**
     * Determina si el escenario es escalable basado en la carga
     */
    public boolean esEscalable() {
        // Un escenario es considerado escalable si mantiene buen rendimiento con alta carga
        return usuariosConcurrentes >= 50 &&
                evaluarNivelRendimiento().ordinal() <= NivelRendimiento.BUENO.ordinal();
    }

    /**
     * Genera reporte textual de la métrica
     */
    public String generarReporte() {
        StringBuilder reporte = new StringBuilder();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        reporte.append("📊 MÉTRICA DE RENDIMIENTO\n");
        reporte.append("=========================\n");
        reporte.append("Escenario: ").append(nombreEscenario).append("\n");
        reporte.append("Usuarios Concurrentes: ").append(usuariosConcurrentes).append("\n");
        reporte.append("Fecha de Ejecución: ").append(fechaEjecucion.format(formato)).append("\n\n");

        reporte.append("⏱️ TIEMPOS DE RESPUESTA:\n");
        reporte.append("  Promedio: ").append(String.format("%.0f ms", tiempoPromedioMs)).append("\n");
        reporte.append("  Mínimo: ").append(String.format("%.0f ms", tiempoMinimoMs)).append("\n");
        reporte.append("  Máximo: ").append(String.format("%.0f ms", tiempoMaximoMs)).append("\n");
        reporte.append("  Percentil 90: ").append(String.format("%.0f ms", percentil90Ms)).append("\n");
        reporte.append("  Percentil 95: ").append(String.format("%.0f ms", percentil95Ms)).append("\n\n");

        reporte.append("📈 THROUGHPUT Y ERRORES:\n");
        reporte.append("  Throughput: ").append(String.format("%.1f req/s", throughputReqSeg)).append("\n");
        reporte.append("  Tasa de Error: ").append(String.format("%.1f%%", tasaErrorPorcentaje)).append("\n");
        reporte.append("  Duración: ").append(duracionPruebaSegundos).append(" segundos\n\n");

        reporte.append("🎯 EVALUACIÓN:\n");
        reporte.append("  Nivel de Rendimiento: ").append(evaluarNivelRendimiento().getDescripcion()).append("\n");
        reporte.append("  Eficiencia: ").append(String.format("%.2f", calcularEficiencia())).append("\n");
        reporte.append("  Escalable: ").append(esEscalable() ? "Sí" : "No").append("\n");

        return reporte.toString();
    }

    // Métodos de validación
    private String validarNoVacio(String valor, String nombreCampo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(nombreCampo + " no puede ser nulo o vacío");
        }
        return valor.trim();
    }

    private int validarPositivo(int valor, String nombreCampo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(nombreCampo + " debe ser positivo");
        }
        return valor;
    }

    private double validarNoNegativo(double valor, String nombreCampo) {
        if (valor < 0) {
            throw new IllegalArgumentException(nombreCampo + " no puede ser negativo");
        }
        return valor;
    }

    private double validarRango(double valor, double min, double max, String nombreCampo) {
        if (valor < min || valor > max) {
            throw new IllegalArgumentException(nombreCampo + " debe estar entre " + min + " y " + max);
        }
        return valor;
    }

    // Getters
    public String getNombreEscenario() { return nombreEscenario; }
    public int getUsuariosConcurrentes() { return usuariosConcurrentes; }
    public double getTiempoPromedioMs() { return tiempoPromedioMs; }
    public double getPercentil90Ms() { return percentil90Ms; }
    public double getPercentil95Ms() { return percentil95Ms; }
    public double getThroughputReqSeg() { return throughputReqSeg; }
    public double getTasaErrorPorcentaje() { return tasaErrorPorcentaje; }
    public double getTiempoMinimoMs() { return tiempoMinimoMs; }
    public double getTiempoMaximoMs() { return tiempoMaximoMs; }
    public int getDuracionPruebaSegundos() { return duracionPruebaSegundos; }
    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }

    /**
     * Enum para representar niveles de rendimiento
     */
    public enum NivelRendimiento {
        EXCELENTE("Excelente"),
        BUENO("Bueno"),
        REGULAR("Regular"),
        MALO("Malo"),
        INACEPTABLE("Inaceptable");

        private final String descripcion;

        NivelRendimiento(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    /**
     * Builder para construcción fluida de métricas
     */
    public static class Builder {
        private String nombreEscenario;
        private int usuariosConcurrentes;
        private double tiempoPromedioMs;
        private double percentil90Ms;
        private double percentil95Ms;
        private double throughputReqSeg;
        private double tasaErrorPorcentaje;
        private double tiempoMinimoMs;
        private double tiempoMaximoMs;
        private int duracionPruebaSegundos;
        private LocalDateTime fechaEjecucion;

        public Builder nombreEscenario(String nombreEscenario) {
            this.nombreEscenario = nombreEscenario;
            return this;
        }

        public Builder usuariosConcurrentes(int usuariosConcurrentes) {
            this.usuariosConcurrentes = usuariosConcurrentes;
            return this;
        }

        public Builder tiempoPromedioMs(double tiempoPromedioMs) {
            this.tiempoPromedioMs = tiempoPromedioMs;
            return this;
        }

        public Builder percentil90Ms(double percentil90Ms) {
            this.percentil90Ms = percentil90Ms;
            return this;
        }

        public Builder percentil95Ms(double percentil95Ms) {
            this.percentil95Ms = percentil95Ms;
            return this;
        }

        public Builder throughputReqSeg(double throughputReqSeg) {
            this.throughputReqSeg = throughputReqSeg;
            return this;
        }

        public Builder tasaErrorPorcentaje(double tasaErrorPorcentaje) {
            this.tasaErrorPorcentaje = tasaErrorPorcentaje;
            return this;
        }

        public Builder tiempoMinimoMs(double tiempoMinimoMs) {
            this.tiempoMinimoMs = tiempoMinimoMs;
            return this;
        }

        public Builder tiempoMaximoMs(double tiempoMaximoMs) {
            this.tiempoMaximoMs = tiempoMaximoMs;
            return this;
        }

        public Builder duracionPruebaSegundos(int duracionPruebaSegundos) {
            this.duracionPruebaSegundos = duracionPruebaSegundos;
            return this;
        }

        public Builder fechaEjecucion(LocalDateTime fechaEjecucion) {
            this.fechaEjecucion = fechaEjecucion;
            return this;
        }

        public MetricaRendimiento build() {
            return new MetricaRendimiento(this);
        }
    }

    // Métodos de comparación y equals/hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetricaRendimiento that = (MetricaRendimiento) o;
        return usuariosConcurrentes == that.usuariosConcurrentes &&
                Double.compare(that.tiempoPromedioMs, tiempoPromedioMs) == 0 &&
                Objects.equals(nombreEscenario, that.nombreEscenario) &&
                Objects.equals(fechaEjecucion, that.fechaEjecucion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombreEscenario, usuariosConcurrentes, tiempoPromedioMs, fechaEjecucion);
    }

    @Override
    public String toString() {
        return String.format("MetricaRendimiento{escenario='%s', usuarios=%d, tiempo=%.0fms, throughput=%.1f, nivel=%s}",
                nombreEscenario, usuariosConcurrentes, tiempoPromedioMs, throughputReqSeg,
                evaluarNivelRendimiento().getDescripcion());
    }
}