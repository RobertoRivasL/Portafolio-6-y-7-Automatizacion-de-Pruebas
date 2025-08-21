package com.mediplus.pruebas.analisis.jmeter;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Generador de reportes HTML de JMeter desde archivos JTL
 * Utiliza el comando jmeter -g para generar dashboards HTML
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class GeneradorReportesHTML {

    private static final Logger LOGGER = Logger.getLogger(GeneradorReportesHTML.class.getName());
    private static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final String rutaJMeter;
    private final Path directorioResultados;
    private final Path directorioReportes;

    public GeneradorReportesHTML() throws IOException {
        this.rutaJMeter = detectarJMeter();
        this.directorioResultados = Paths.get("jmeter-results");
        this.directorioReportes = Paths.get("jmeter-reports");

        Files.createDirectories(directorioReportes);

        if (rutaJMeter == null) {
            throw new IOException("JMeter no encontrado para generar reportes HTML");
        }
    }

    /**
     * Genera reportes HTML desde todos los archivos JTL encontrados
     */
    public List<String> generarReportesHTML() {
        List<String> reportesGenerados = new ArrayList<>();

        try {
            List<Path> archivosJTL = buscarArchivosJTL();

            if (archivosJTL.isEmpty()) {
                LOGGER.warning("No se encontraron archivos JTL para procesar");
                return reportesGenerados;
            }

            for (Path archivoJTL : archivosJTL) {
                try {
                    String reporte = generarReporteIndividual(archivoJTL);
                    if (reporte != null) {
                        reportesGenerados.add(reporte);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error generando reporte para: " + archivoJTL.getFileName(), e);
                }
            }

            // Generar reporte consolidado si hay múltiples JTL
            if (archivosJTL.size() > 1) {
                String reporteConsolidado = generarReporteConsolidado(archivosJTL);
                if (reporteConsolidado != null) {
                    reportesGenerados.add(reporteConsolidado);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generando reportes HTML", e);
        }

        return reportesGenerados;
    }

    /**
     * Busca todos los archivos JTL en el directorio de resultados
     */
    private List<Path> buscarArchivosJTL() throws IOException {
        List<Path> archivosJTL = new ArrayList<>();

        if (!Files.exists(directorioResultados)) {
            return archivosJTL;
        }

        Files.walk(directorioResultados)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".jtl"))
                .filter(p -> {
                    try {
                        return Files.size(p) > 0; // Solo archivos no vacíos
                    } catch (IOException e) {
                        return false;
                    }
                })
                .sorted((a, b) -> {
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .forEach(archivosJTL::add);

        LOGGER.info("📁 Encontrados " + archivosJTL.size() + " archivos JTL válidos");
        return archivosJTL;
    }

    /**
     * Genera un reporte HTML individual para un archivo JTL
     */
    private String generarReporteIndividual(Path archivoJTL) throws IOException, InterruptedException {
        String nombreBase = archivoJTL.getFileName().toString().replace(".jtl", "");
        String timestamp = LocalDateTime.now().format(FORMATO_TIMESTAMP);
        Path directorioReporte = directorioReportes.resolve("reporte-" + nombreBase + "-" + timestamp);

        // Crear directorio para el reporte
        Files.createDirectories(directorioReporte);

        List<String> comando = Arrays.asList(
                rutaJMeter,
                "-g", archivoJTL.toAbsolutePath().toString(),
                "-o", directorioReporte.toAbsolutePath().toString()
        );

        LOGGER.info("📊 Generando reporte HTML para: " + archivoJTL.getFileName());

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(true);

        Process proceso = pb.start();

        // Capturar salida
        StringBuilder salida = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                salida.append(linea).append("\n");
                if (linea.contains("Writing report") || linea.contains("Report generated")) {
                    LOGGER.info("📈 " + linea);
                }
            }
        }

        boolean terminado = proceso.waitFor(120, TimeUnit.SECONDS);

        if (!terminado) {
            proceso.destroyForcibly();
            throw new InterruptedException("Timeout generando reporte HTML");
        }

        if (proceso.exitValue() == 0) {
            // Verificar que se generó el index.html
            Path indexHtml = directorioReporte.resolve("index.html");
            if (Files.exists(indexHtml)) {
                LOGGER.info("✅ Reporte HTML generado: " + directorioReporte.getFileName());
                return directorioReporte.toAbsolutePath().toString();
            } else {
                LOGGER.warning("⚠️ No se encontró index.html en: " + directorioReporte);
                return null;
            }
        } else {
            LOGGER.warning("⚠️ Error generando reporte HTML. Código de salida: " + proceso.exitValue());
            LOGGER.warning("Salida del proceso: " + salida.toString());
            return null;
        }
    }

    /**
     * Genera un reporte consolidado combinando múltiples archivos JTL
     */
    private String generarReporteConsolidado(List<Path> archivosJTL) throws IOException, InterruptedException {
        String timestamp = LocalDateTime.now().format(FORMATO_TIMESTAMP);
        Path archivoConsolidado = directorioResultados.resolve("consolidado-" + timestamp + ".jtl");
        Path directorioReporte = directorioReportes.resolve("reporte-consolidado-" + timestamp);

        // Combinar archivos JTL
        combinarArchivosJTL(archivosJTL, archivoConsolidado);

        // Crear directorio para el reporte
        Files.createDirectories(directorioReporte);

        List<String> comando = Arrays.asList(
                rutaJMeter,
                "-g", archivoConsolidado.toAbsolutePath().toString(),
                "-o", directorioReporte.toAbsolutePath().toString()
        );

        LOGGER.info("📊 Generando reporte HTML consolidado...");

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(true);

        Process proceso = pb.start();

        // Capturar salida
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.contains("Writing report") || linea.contains("Report generated")) {
                    LOGGER.info("📈 " + linea);
                }
            }
        }

        boolean terminado = proceso.waitFor(180, TimeUnit.SECONDS);

        if (!terminado) {
            proceso.destroyForcibly();
            throw new InterruptedException("Timeout generando reporte consolidado");
        }

        if (proceso.exitValue() == 0) {
            Path indexHtml = directorioReporte.resolve("index.html");
            if (Files.exists(indexHtml)) {
                LOGGER.info("✅ Reporte HTML consolidado generado: " + directorioReporte.getFileName());
                return directorioReporte.toAbsolutePath().toString();
            }
        }

        return null;
    }

    /**
     * Combina múltiples archivos JTL en uno solo
     */
    private void combinarArchivosJTL(List<Path> archivosJTL, Path archivoSalida) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(archivoSalida)) {
            boolean primeraLinea = true;

            for (Path archivo : archivosJTL) {
                if (!Files.exists(archivo) || Files.size(archivo) == 0) {
                    continue;
                }

                List<String> lineas = Files.readAllLines(archivo);

                for (int i = 0; i < lineas.size(); i++) {
                    String linea = lineas.get(i);

                    // Escribir header solo una vez
                    if (i == 0 && linea.contains("timeStamp")) {
                        if (primeraLinea) {
                            writer.write(linea);
                            writer.newLine();
                            primeraLinea = false;
                        }
                        continue;
                    }

                    // Escribir datos
                    if (i > 0 || !linea.contains("timeStamp")) {
                        writer.write(linea);
                        writer.newLine();
                    }
                }
            }
        }

        LOGGER.info("📄 Archivo JTL consolidado creado: " + archivoSalida.getFileName());
    }

    /**
     * Detecta la ruta de JMeter (reutiliza lógica del EjecutorJMeterReal)
     */
    private String detectarJMeter() {
        String[] rutasPosibles = {
                "jmeter",
                "/opt/jmeter/bin/jmeter",
                "/usr/local/jmeter/bin/jmeter",
                "C:\\apache-jmeter\\bin\\jmeter.bat",
                "C:\\jmeter\\bin\\jmeter.bat"
        };

        // Verificar variable de entorno
        String jmeterHome = System.getenv("JMETER_HOME");
        if (jmeterHome != null) {
            String extension = System.getProperty("os.name").toLowerCase().contains("win") ? "jmeter.bat" : "jmeter";
            Path jmeterBin = Paths.get(jmeterHome, "bin", extension);
            if (Files.exists(jmeterBin)) {
                return jmeterBin.toString();
            }
        }

        // Probar rutas conocidas
        for (String ruta : rutasPosibles) {
            try {
                ProcessBuilder pb = new ProcessBuilder(ruta, "--version");
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Process proceso = pb.start();
                if (proceso.waitFor(5, TimeUnit.SECONDS) && proceso.exitValue() == 0) {
                    return ruta;
                }
            } catch (Exception e) {
                // Continuar con la siguiente ruta
            }
        }

        return null;
    }

    /**
     * Método main para generar reportes de forma independiente
     */
    public static void main(String[] args) {
        System.out.println("📊 Generador de Reportes HTML JMeter");
        System.out.println("====================================");

        try {
            GeneradorReportesHTML generador = new GeneradorReportesHTML();
            List<String> reportes = generador.generarReportesHTML();

            if (reportes.isEmpty()) {
                System.out.println("❌ No se generaron reportes HTML");
                System.out.println("💡 Verificar que existan archivos .jtl en el directorio jmeter-results/");
            } else {
                System.out.println("✅ Reportes HTML generados exitosamente:");
                for (String reporte : reportes) {
                    System.out.println("📁 " + reporte);
                    Path indexPath = Paths.get(reporte, "index.html");
                    if (Files.exists(indexPath)) {
                        System.out.println("🌐 Abrir: " + indexPath.toAbsolutePath());
                    }
                }

                System.out.println("\n💡 Para ver los reportes:");
                System.out.println("1. Navegar al directorio del reporte");
                System.out.println("2. Abrir index.html en un navegador web");
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método de utilidad para verificar si hay archivos JTL disponibles
     */
    public static boolean hayArchivosJTLDisponibles() {
        try {
            Path directorioResultados = Paths.get("jmeter-results");
            if (!Files.exists(directorioResultados)) {
                return false;
            }

            return Files.walk(directorioResultados)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jtl"))
                    .anyMatch(p -> {
                        try {
                            return Files.size(p) > 0;
                        } catch (IOException e) {
                            return false;
                        }
                    });
        } catch (IOException e) {
            return false;
        }
    }
}