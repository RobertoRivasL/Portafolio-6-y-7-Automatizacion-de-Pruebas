package com.mediplus.pruebas.analisis.evidencias;

import com.mediplus.pruebas.analisis.modelo.MetricaRendimiento;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generador de gráficas para métricas de rendimiento
 * Crea gráficas en formato ASCII y HTML
 * 
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class GeneradorGraficas {

    private final Path directorioGraficas;

    public GeneradorGraficas() throws IOException {
        this.directorioGraficas = Paths.get("evidencias", "graficas");
        Files.createDirectories(directorioGraficas);
    }

    /**
     * Genera todas las gráficas con datos de ejemplo
     */
    public void generarTodasLasGraficas() throws IOException {
        List<MetricaRendimiento> metricasEjemplo = crearMetricasEjemplo();
        
        generarGraficaTiempoRespuesta(metricasEjemplo);
        generarGraficaThroughput(metricasEjemplo);
        generarGraficaTasaError(metricasEjemplo);
        generarGraficaComparativa(metricasEjemplo);
        generarReporteHTML(metricasEjemplo);
    }

    private List<MetricaRendimiento> crearMetricasEjemplo() {
        return Arrays.asList(
            // GET Masivo
            crearMetrica("GET Masivo", 10, 506.0, 0.0, 1.0),
            crearMetrica("GET Masivo", 50, 1124.0, 0.5, 1.0),
            crearMetrica("GET Masivo", 100, 2397.0, 13.0, 1.0),
            
            // POST Masivo
            crearMetrica("POST Masivo", 10, 644.0, 0.0, 1.0),
            crearMetrica("POST Masivo", 50, 1497.0, 3.5, 1.0),
            crearMetrica("POST Masivo", 100, 3714.0, 12.5, 1.0),
            
            // Mixto
            crearMetrica("GET+POST Combinado", 10, 580.0, 0.0, 1.0),
            crearMetrica("GET+POST Combinado", 50, 1366.0, 1.5, 1.0),
            crearMetrica("GET+POST Combinado", 100, 3142.0, 12.0, 1.0)
        );
    }

    private MetricaRendimiento crearMetrica(String escenario, int usuarios, double tiempo, double error, double throughput) {
        return new MetricaRendimiento.Builder()
                .nombreEscenario(escenario)
                .usuariosConcurrentes(usuarios)
                .tiempoPromedioMs(tiempo)
                .percentil90Ms(tiempo * 1.3)
                .percentil95Ms(tiempo * 1.5)
                .throughputReqSeg(throughput)
                .tasaErrorPorcentaje(error)
                .tiempoMinimoMs(tiempo * 0.3)
                .tiempoMaximoMs(tiempo * 2.5)
                .duracionPruebaSegundos(60)
                .fechaEjecucion(LocalDateTime.now())
                .build();
    }

    private void generarGraficaTiempoRespuesta(List<MetricaRendimiento> metricas) throws IOException {
        Path archivo = directorioGraficas.resolve("tiempo-respuesta-vs-usuarios.txt");
        
        try (BufferedWriter writer = Files.newBufferedWriter(archivo)) {
            writer.write("📊 TIEMPO DE RESPUESTA vs USUARIOS CONCURRENTES\n");
            writer.write("=".repeat(70) + "\n\n");
            
            Map<String, List<MetricaRendimiento>> metricasPorEscenario = agruparPorEscenario(metricas);
            
            for (Map.Entry<String, List<MetricaRendimiento>> entrada : metricasPorEscenario.entrySet()) {
                writer.write("🎯 " + entrada.getKey() + ":\n");
                
                entrada.getValue().stream()
                    .sorted(Comparator.comparingInt(MetricaRendimiento::getUsuariosConcurrentes))
                    .forEach(metrica -> {
                        try {
                            int barras = (int) (metrica.getTiempoPromedioMs() / 50);
                            String barra = "█".repeat(Math.min(barras, 60));
                            String nivel = obtenerIndicadorNivel(metrica.evaluarNivelRendimiento());
                            
                            writer.write(String.format("%3d usuarios: %s %.0f ms %s\n",
                                metrica.getUsuariosConcurrentes(), 
                                barra.isEmpty() ? "▌" : barra, 
                                metrica.getTiempoPromedioMs(), 
                                nivel));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                writer.write("\n");
            }
            
            writer.write("Leyenda: ✅=Excelente, 🟢=Bueno, 🟡=Regular, 🟠=Malo, 🔴=Inaceptable\n");
            writer.write("Escala: Cada █ representa ~50ms\n");
        }
    }

    private void generarGraficaThroughput(List<MetricaRendimiento> metricas) throws IOException {
        Path archivo = directorioGraficas.resolve("throughput-vs-carga.txt");
        
        try (BufferedWriter writer = Files.newBufferedWriter(archivo)) {
            writer.write("📈 THROUGHPUT vs CARGA DE USUARIOS\n");
            writer.write("=".repeat(70) + "\n\n");
            
            Map<String, List<MetricaRendimiento>> metricasPorEscenario = agruparPorEscenario(metricas);
            
            for (Map.Entry<String, List<MetricaRendimiento>> entrada : metricasPorEscenario.entrySet()) {
                writer.write("📊 " + entrada.getKey() + ":\n");
                
                entrada.getValue().stream()
                    .sorted(Comparator.comparingInt(MetricaRendimiento::getUsuariosConcurrentes))
                    .forEach(metrica -> {
                        try {
                            int barras = (int) (metrica.getThroughputReqSeg() * 10);
                            String barra = "▓".repeat(Math.min(barras, 40));
                            
                            writer.write(String.format("%3d usuarios: %s %.1f req/s\n",
                                metrica.getUsuariosConcurrentes(),
                                barra.isEmpty() ? "▌" : barra,
                                metrica.getThroughputReqSeg()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                writer.write("\n");
            }
            
            writer.write("Escala: Cada ▓ representa ~0.1 req/s\n");
        }
    }

    private void generarGraficaTasaError(List<MetricaRendimiento> metricas) throws IOException {
        Path archivo = directorioGraficas.resolve("tasa-error-por-escenario.txt");
        
        try (BufferedWriter writer = Files.newBufferedWriter(archivo)) {
            writer.write("🚨 TASA DE ERROR POR ESCENARIO\n");
            writer.write("=".repeat(70) + "\n\n");
            
            Map<String, List<MetricaRendimiento>> metricasPorEscenario = agruparPorEscenario(metricas);
            
            for (Map.Entry<String, List<MetricaRendimiento>> entrada : metricasPorEscenario.entrySet()) {
                writer.write("⚠️ " + entrada.getKey() + ":\n");
                
                entrada.getValue().stream()
                    .sorted(Comparator.comparingInt(MetricaRendimiento::getUsuariosConcurrentes))
                    .forEach(metrica -> {
                        try {
                            int barras = (int) metrica.getTasaErrorPorcentaje();
                            String barra = "▒".repeat(Math.min(barras, 20));
                            String alerta = metrica.getTasaErrorPorcentaje() > 5.0 ? " ⚠️ CRÍTICO" : "";
                            
                            writer.write(String.format("%3d usuarios: %s %.1f%%%s\n",
                                metrica.getUsuariosConcurrentes(),
                                barra.isEmpty() ? "▌" : barra,
                                metrica.getTasaErrorPorcentaje(),
                                alerta));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                writer.write("\n");
            }
            
            writer.write("Escala: Cada ▒ representa ~1% error\n");
            writer.write("⚠️ CRÍTICO: Tasa de error > 5%\n");
        }
    }

    private void generarGraficaComparativa(List<MetricaRendimiento> metricas) throws IOException {
        Path archivo = directorioGraficas.resolve("comparativa-general.txt");
        
        try (BufferedWriter writer = Files.newBufferedWriter(archivo)) {
            writer.write("📊 ANÁLISIS COMPARATIVO GENERAL\n");
            writer.write("=".repeat(80) + "\n\n");
            
            writer.write("| ESCENARIO           | USUARIOS | TIEMPO (ms) | THROUGHPUT | ERROR % | NIVEL      |\n");
            writer.write("|---------------------|----------|-------------|------------|---------|------------|\n");
            
            metricas.stream()
                .sorted((m1, m2) -> {
                    int comp = m1.getNombreEscenario().compareTo(m2.getNombreEscenario());
                    return comp != 0 ? comp : Integer.compare(m1.getUsuariosConcurrentes(), m2.getUsuariosConcurrentes());
                })
                .forEach(metrica -> {
                    try {
                        writer.write(String.format("| %-19s | %8d | %11.0f | %10.1f | %7.1f | %-10s |\n",
                            metrica.getNombreEscenario().length() > 19 ? 
                                metrica.getNombreEscenario().substring(0, 19) : metrica.getNombreEscenario(),
                            metrica.getUsuariosConcurrentes(),
                            metrica.getTiempoPromedioMs(),
                            metrica.getThroughputReqSeg(),
                            metrica.getTasaErrorPorcentaje(),
                            metrica.evaluarNivelRendimiento().getDescripcion()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            
            writer.write("\n📈 RESUMEN EJECUTIVO:\n");
            writer.write("- Mejor rendimiento: GET Masivo con 10 usuarios (506ms)\n");
            writer.write("- Peor rendimiento: POST Masivo con 100 usuarios (3714ms)\n");
            writer.write("- Escenario más estable: GET+POST Combinado\n");
            writer.write("- Usuarios críticos: >50 usuarios en todos los escenarios\n");
        }
    }

    private void generarReporteHTML(List<MetricaRendimiento> metricas) throws IOException {
        Path archivo = directorioGraficas.resolve("reporte-metricas.html");
        
        try (BufferedWriter writer = Files.newBufferedWriter(archivo)) {
            writer.write("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n");
            writer.write("<meta charset=\"UTF-8\">\n");
            writer.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            writer.write("<title>📊 Reporte de Métricas - API MediPlus</title>\n");
            writer.write("<style>\n");
            writer.write("body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n");
            writer.write(".container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }\n");
            writer.write(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }\n");
            writer.write(".metric-card { border: 1px solid #ddd; padding: 15px; margin: 10px 0; border-radius: 5px; }\n");
            writer.write(".excelente { border-left: 5px solid #28a745; }\n");
            writer.write(".bueno { border-left: 5px solid #17a2b8; }\n");
            writer.write(".regular { border-left: 5px solid #ffc107; }\n");
            writer.write(".malo { border-left: 5px solid #fd7e14; }\n");
            writer.write(".inaceptable { border-left: 5px solid #dc3545; }\n");
            writer.write("table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
            writer.write("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
            writer.write("th { background-color: #f2f2f2; }\n");
            writer.write("</style>\n</head>\n<body>\n");
            
            writer.write("<div class=\"container\">\n");
            writer.write("<div class=\"header\">\n");
            writer.write("<h1>📊 Reporte de Métricas de Rendimiento</h1>\n");
            writer.write("<h2>API REST MediPlus</h2>\n");
            writer.write("<p>Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "</p>\n");
            writer.write("</div>\n");
            
            writer.write("<h2>📈 Resumen de Métricas</h2>\n");
            writer.write("<table>\n<tr><th>Escenario</th><th>Usuarios</th><th>Tiempo (ms)</th><th>Throughput</th><th>Error %</th><th>Nivel</th></tr>\n");
            
            for (MetricaRendimiento metrica : metricas) {
                String claseCSS = obtenerClaseCSS(metrica.evaluarNivelRendimiento());
                writer.write(String.format("<tr class=\"%s\"><td>%s</td><td>%d</td><td>%.0f</td><td>%.1f</td><td>%.1f</td><td>%s</td></tr>\n",
                    claseCSS,
                    metrica.getNombreEscenario(),
                    metrica.getUsuariosConcurrentes(),
                    metrica.getTiempoPromedioMs(),
                    metrica.getThroughputReqSeg(),
                    metrica.getTasaErrorPorcentaje(),
                    metrica.evaluarNivelRendimiento().getDescripcion()));
            }
            
            writer.write("</table>\n");
            writer.write("<p><em>Reporte generado automáticamente por GeneradorGraficas.java</em></p>\n");
            writer.write("</div>\n</body>\n</html>\n");
        }
    }

    private Map<String, List<MetricaRendimiento>> agruparPorEscenario(List<MetricaRendimiento> metricas) {
        Map<String, List<MetricaRendimiento>> agrupadas = new HashMap<>();
        for (MetricaRendimiento metrica : metricas) {
            agrupadas.computeIfAbsent(metrica.getNombreEscenario(), k -> new ArrayList<>()).add(metrica);
        }
        return agrupadas;
    }

    private String obtenerIndicadorNivel(MetricaRendimiento.NivelRendimiento nivel) {
        return switch (nivel) {
            case EXCELENTE -> "✅";
            case BUENO -> "🟢";
            case REGULAR -> "🟡";
            case MALO -> "🟠";
            case INACEPTABLE -> "🔴";
        };
    }

    private String obtenerClaseCSS(MetricaRendimiento.NivelRendimiento nivel) {
        return switch (nivel) {
            case EXCELENTE -> "excelente";
            case BUENO -> "bueno";
            case REGULAR -> "regular";
            case MALO -> "malo";
            case INACEPTABLE -> "inaceptable";
        };
    }

    public static void main(String[] args) {
        try {
            GeneradorGraficas generador = new GeneradorGraficas();
            generador.generarTodasLasGraficas();
            
            System.out.println("✅ Gráficas generadas exitosamente en: evidencias/graficas/");
            System.out.println("📊 Archivos creados:");
            System.out.println("  - tiempo-respuesta-vs-usuarios.txt");
            System.out.println("  - throughput-vs-carga.txt");
            System.out.println("  - tasa-error-por-escenario.txt");
            System.out.println("  - comparativa-general.txt");
            System.out.println("  - reporte-metricas.html");
            
        } catch (Exception e) {
            System.err.println("❌ Error generando gráficas: " + e.getMessage());
            e.printStackTrace();
        }
    }
}