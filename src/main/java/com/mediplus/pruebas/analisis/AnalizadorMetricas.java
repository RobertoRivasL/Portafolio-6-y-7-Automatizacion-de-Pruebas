package com.mediplus.pruebas.analisis;

import com.mediplus.pruebas.analisis.modelo.MetricaRendimiento;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Analizador de métricas de rendimiento
 * Procesa archivos JTL y genera análisis comparativos
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class AnalizadorMetricas {

    private static final Logger LOGGER = Logger.getLogger(AnalizadorMetricas.class.getName());
    private static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Procesa un archivo JTL y extrae métricas de rendimiento
     */
    public MetricaRendimiento procesarArchivoJTL(Path archivoJTL) throws IOException {
        if (!Files.exists(archivoJTL) || Files.size(archivoJTL) == 0) {
            LOGGER.warning("Archivo JTL vacío o no existe: " + archivoJTL);
            return null;
        }

        List<String> lineas = Files.readAllLines(archivoJTL);
        if (lineas.size() <= 1) { // Solo header o vacío
            LOGGER.warning("Archivo JTL sin datos: " + archivoJTL);
            return null;
        }

        // Parsear datos del JTL
        List<RegistroJTL> registros = parsearJTL(lineas);
        if (registros.isEmpty()) {
            return null;
        }

        // Calcular métricas
        return calcularMetricas(archivoJTL.getFileName().toString(), registros);
    }

    /**
     * Parsea las líneas de un archivo JTL
     */
    private List<RegistroJTL> parsearJTL(List<String> lineas) {
        List<RegistroJTL> registros = new ArrayList<>();

        // Saltar header (primera línea)
        for (int i = 1; i < lineas.size(); i++) {
            try {
                String linea = lineas.get(i);
                String[] campos = linea.split(",");

                if (campos.length >= 3) {
                    long timestamp = Long.parseLong(campos[0].trim());
                    long elapsed = Long.parseLong(campos[1].trim());
                    String label = campos.length > 2 ? campos[2].trim() : "Unknown";
                    boolean success = campos.length > 7 ? "true".equals(campos[7].trim()) : true;

                    registros.add(new RegistroJTL(timestamp, elapsed, label, success));
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error parseando línea JTL: " + lineas.get(i), e);
            }
        }

        return registros;
    }

    /**
     * Calcula métricas a partir de registros JTL
     */
    private MetricaRendimiento calcularMetricas(String nombreArchivo, List<RegistroJTL> registros) {
        if (registros.isEmpty()) return null;

        // Extraer información del nombre del archivo
        String escenario = extraerEscenarioDelNombre(nombreArchivo);
        int usuarios = extraerUsuariosDelNombre(nombreArchivo);

        // Calcular estadísticas
        List<Long> tiempos = registros.stream()
                .map(r -> r.elapsed)
                .sorted()
                .collect(Collectors.toList());

        double tiempoPromedio = tiempos.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long tiempoMinimo = tiempos.get(0);
        long tiempoMaximo = tiempos.get(tiempos.size() - 1);

        // Percentiles
        double percentil90 = calcularPercentil(tiempos, 90);
        double percentil95 = calcularPercentil(tiempos, 95);

        // Tasa de error
        long errores = registros.stream().mapToLong(r -> r.success ? 0 : 1).sum();
        double tasaError = (double) errores / registros.size() * 100.0;

        // Throughput (aproximado)
        long duracionMs = registros.stream()
                .mapToLong(r -> r.timestamp)
                .max().orElse(0) - registros.stream().mapToLong(r -> r.timestamp).min().orElse(0);
        double duracionSeg = duracionMs > 0 ? duracionMs / 1000.0 : 60.0; // fallback a 60 seg
        double throughput = registros.size() / duracionSeg;

        return new MetricaRendimiento.Builder()
                .nombreEscenario(escenario)
                .usuariosConcurrentes(usuarios)
                .tiempoPromedioMs(tiempoPromedio)
                .percentil90Ms(percentil90)
                .percentil95Ms(percentil95)
                .throughputReqSeg(throughput)
                .tasaErrorPorcentaje(tasaError)
                .tiempoMinimoMs(tiempoMinimo)
                .tiempoMaximoMs(tiempoMaximo)
                .duracionPruebaSegundos((int) duracionSeg)
                .fechaEjecucion(LocalDateTime.now())
                .build();
    }

    /**
     * Calcula percentil de una lista ordenada
     */
    private double calcularPercentil(List<Long> valoresOrdenados, int percentil) {
        if (valoresOrdenados.isEmpty()) return 0.0;

        int indice = (int) Math.ceil(percentil / 100.0 * valoresOrdenados.size()) - 1;
        indice = Math.max(0, Math.min(indice, valoresOrdenados.size() - 1));

        return valoresOrdenados.get(indice);
    }

    /**
     * Extrae el nombre del escenario del nombre del archivo
     */
    private String extraerEscenarioDelNombre(String nombreArchivo) {
        String nombre = nombreArchivo.toLowerCase();

        if (nombre.contains("get") && nombre.contains("post")) {
            return "GET+POST Combinado";
        } else if (nombre.contains("post")) {
            return "POST Masivo";
        } else if (nombre.contains("get")) {
            return "GET Masivo";
        } else if (nombre.contains("mixto") || nombre.contains("combined")) {
            return "GET+POST Combinado";
        }

        return "Escenario Detectado";
    }

    /**
     * Extrae el número de usuarios del nombre del archivo
     */
    private int extraerUsuariosDelNombre(String nombreArchivo) {
        // Buscar patrones como "10u", "50u", "100u" en el nombre
        String[] partes = nombreArchivo.split("[_\\-\\.]");

        for (String parte : partes) {
            if (parte.matches("\\d+u?")) {
                try {
                    return Integer.parseInt(parte.replaceAll("[^\\d]", ""));
                } catch (NumberFormatException e) {
                    // Continuar buscando
                }
            }
        }

        return 10; // Valor por defecto
    }

    /**
     * Compara múltiples métricas y genera análisis
     */
    public ComparacionMetricas compararMetricas(List<MetricaRendimiento> metricas) {
        if (metricas.isEmpty()) {
            return new ComparacionMetricas(Collections.emptyList(), "Sin métricas para comparar");
        }

        // Agrupar por escenario
        Map<String, List<MetricaRendimiento>> metricasPorEscenario = metricas.stream()
                .collect(Collectors.groupingBy(MetricaRendimiento::getNombreEscenario));

        // Encontrar mejor y peor rendimiento
        MetricaRendimiento mejorRendimiento = metricas.stream()
                .min(Comparator.comparingDouble(MetricaRendimiento::getTiempoPromedioMs))
                .orElse(null);

        MetricaRendimiento peorRendimiento = metricas.stream()
                .max(Comparator.comparingDouble(MetricaRendimiento::getTiempoPromedioMs))
                .orElse(null);

        // Generar análisis
        StringBuilder analisis = new StringBuilder();
        analisis.append("📊 ANÁLISIS COMPARATIVO DE MÉTRICAS\n");
        analisis.append("================================\n\n");

        if (mejorRendimiento != null) {
            analisis.append(String.format("🏆 Mejor rendimiento: %s con %d usuarios (%.0f ms)\n",
                    mejorRendimiento.getNombreEscenario(),
                    mejorRendimiento.getUsuariosConcurrentes(),
                    mejorRendimiento.getTiempoPromedioMs()));
        }

        if (peorRendimiento != null) {
            analisis.append(String.format("⚠️ Peor rendimiento: %s con %d usuarios (%.0f ms)\n\n",
                    peorRendimiento.getNombreEscenario(),
                    peorRendimiento.getUsuariosConcurrentes(),
                    peorRendimiento.getTiempoPromedioMs()));
        }

        // Análisis por escenario
        metricasPorEscenario.forEach((escenario, metricasEscenario) -> {
            analisis.append(String.format("🎯 %s:\n", escenario));

            metricasEscenario.stream()
                    .sorted(Comparator.comparingInt(MetricaRendimiento::getUsuariosConcurrentes))
                    .forEach(metrica -> {
                        String nivel = metrica.evaluarNivelRendimiento().getDescripcion();
                        analisis.append(String.format("   %d usuarios: %.0f ms (%s)\n",
                                metrica.getUsuariosConcurrentes(),
                                metrica.getTiempoPromedioMs(),
                                nivel));
                    });
            analisis.append("\n");
        });

        return new ComparacionMetricas(metricas, analisis.toString());
    }

    /**
     * Genera reporte completo de métricas
     */
    public void generarReporteCompleto(List<MetricaRendimiento> metricas, Path directorioSalida) {
        try {
            Files.createDirectories(directorioSalida);

            String timestamp = LocalDateTime.now().format(FORMATO_TIMESTAMP);
            Path archivoReporte = directorioSalida.resolve("analisis-metricas-" + timestamp + ".txt");

            try (BufferedWriter writer = Files.newBufferedWriter(archivoReporte)) {
                writer.write("📊 REPORTE COMPLETO DE MÉTRICAS - API MEDIPLUS\n");
                writer.write("=".repeat(80) + "\n");
                writer.write("📅 Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n");
                writer.write("👥 Autores: Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez\n\n");

                // Resumen ejecutivo
                writer.write("🎯 RESUMEN EJECUTIVO\n");
                writer.write("-".repeat(50) + "\n");
                writer.write("Total de métricas analizadas: " + metricas.size() + "\n");

                long metricasCriticas = metricas.stream()
                        .filter(m -> m.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.MALO ||
                                m.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.INACEPTABLE)
                        .count();

                writer.write("Métricas críticas: " + metricasCriticas + "\n");

                double tiempoPromedio = metricas.stream()
                        .mapToDouble(MetricaRendimiento::getTiempoPromedioMs)
                        .average()
                        .orElse(0.0);

                writer.write(String.format("Tiempo promedio general: %.0f ms\n\n", tiempoPromedio));

                // Detalles por métrica
                writer.write("📋 DETALLES POR MÉTRICA\n");
                writer.write("-".repeat(50) + "\n");

                metricas.stream()
                        .sorted(Comparator.comparing(MetricaRendimiento::getNombreEscenario)
                                .thenComparingInt(MetricaRendimiento::getUsuariosConcurrentes))
                        .forEach(metrica -> {
                            try {
                                writer.write(String.format("\n🎯 %s - %d usuarios:\n",
                                        metrica.getNombreEscenario(),
                                        metrica.getUsuariosConcurrentes()));
                                writer.write(String.format("   Tiempo promedio: %.0f ms\n", metrica.getTiempoPromedioMs()));
                                writer.write(String.format("   Percentil 90: %.0f ms\n", metrica.getPercentil90Ms()));
                                writer.write(String.format("   Percentil 95: %.0f ms\n", metrica.getPercentil95Ms()));
                                writer.write(String.format("   Throughput: %.1f req/s\n", metrica.getThroughputReqSeg()));
                                writer.write(String.format("   Tasa de error: %.1f%%\n", metrica.getTasaErrorPorcentaje()));
                                writer.write(String.format("   Nivel: %s\n", metrica.evaluarNivelRendimiento().getDescripcion()));
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "Error escribiendo métrica", e);
                            }
                        });

                writer.write("\n" + "=".repeat(80) + "\n");
                writer.write("Reporte generado automáticamente por AnalizadorMetricas.java\n");
            }

            LOGGER.info("📋 Reporte completo generado: " + archivoReporte);

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error generando reporte completo", e);
        }
    }

    /**
     * Clase para representar un registro de JTL
     */
    private static class RegistroJTL {
        final long timestamp;
        final long elapsed;
        final String label;
        final boolean success;

        RegistroJTL(long timestamp, long elapsed, String label, boolean success) {
            this.timestamp = timestamp;
            this.elapsed = elapsed;
            this.label = label;
            this.success = success;
        }
    }

    /**
     * Clase para almacenar resultado de comparación de métricas
     */
    public static class ComparacionMetricas {
        private final List<MetricaRendimiento> metricas;
        private final String analisisTexto;

        public ComparacionMetricas(List<MetricaRendimiento> metricas, String analisisTexto) {
            this.metricas = Collections.unmodifiableList(new ArrayList<>(metricas));
            this.analisisTexto = analisisTexto;
        }

        public List<MetricaRendimiento> getMetricas() {
            return metricas;
        }

        public String getAnalisisTexto() {
            return analisisTexto;
        }

        @Override
        public String toString() {
            return analisisTexto;
        }
    }
}