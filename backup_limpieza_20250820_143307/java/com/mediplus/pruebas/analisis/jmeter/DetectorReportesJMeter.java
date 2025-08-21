package com.mediplus.pruebas.analisis.jmeter;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Detector automático de reportes JMeter existentes en el sistema
 * Busca archivos JTL y reportes HTML generados previamente por JMeter
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class DetectorReportesJMeter {

    private static final Logger LOGGER = Logger.getLogger(DetectorReportesJMeter.class.getName());

    // Patrones para identificar archivos JMeter
    private static final Pattern PATRON_JTL = Pattern.compile(".*\\.(jtl|csv)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_HTML_JMETER = Pattern.compile(".*jmeter.*\\.html$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATRON_ESCENARIO = Pattern.compile("(get|post|put|delete|mixto|combined|masivo)", Pattern.CASE_INSENSITIVE);

    // Directorios comunes donde JMeter guarda resultados
    private static final String[] DIRECTORIOS_BUSQUEDA = {
            ".", "results", "jmeter", "jmeter-results", "target", "build",
            "resultados", "evidencias", "reportes", "test-results",
            "src/test/resources", "src/main/resources", "jmeter-reports"
    };

    private final List<ReporteJMeterEncontrado> reportesEncontrados;
    private final List<ArchivoJTLEncontrado> archivosJTLEncontrados;

    public DetectorReportesJMeter() {
        this.reportesEncontrados = new ArrayList<>();
        this.archivosJTLEncontrados = new ArrayList<>();
    }

    /**
     * Detecta y procesa todos los reportes JMeter disponibles
     */
    public ResultadoDeteccion detectarYProcesarReportes() {
        LOGGER.info("🔍 Iniciando detección de reportes JMeter existentes...");

        try {
            // Limpiar listas previas
            reportesEncontrados.clear();
            archivosJTLEncontrados.clear();

            // Buscar en todos los directorios configurados
            for (String directorio : DIRECTORIOS_BUSQUEDA) {
                buscarEnDirectorio(Paths.get(directorio));
            }

            // Buscar recursivamente en directorios específicos
            buscarRecursivamente();

            // Procesar y validar archivos encontrados
            validarArchivosEncontrados();

            // Generar resultado
            boolean exitoso = !reportesEncontrados.isEmpty() || !archivosJTLEncontrados.isEmpty();

            String mensaje = String.format(
                    "Detección completada: %d reportes HTML, %d archivos JTL encontrados",
                    reportesEncontrados.size(), archivosJTLEncontrados.size()
            );

            if (exitoso) {
                LOGGER.info("✅ " + mensaje);
                mostrarDetallesEncontrados();
            } else {
                LOGGER.info("ℹ️ No se encontraron reportes JMeter existentes");
            }

            return new ResultadoDeteccion(exitoso, mensaje,
                    new ArrayList<>(reportesEncontrados),
                    new ArrayList<>(archivosJTLEncontrados));

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error durante detección de reportes", e);
            return new ResultadoDeteccion(false, "Error: " + e.getMessage(),
                    Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * Busca archivos JMeter en un directorio específico
     */
    private void buscarEnDirectorio(Path directorio) {
        if (!Files.exists(directorio) || !Files.isDirectory(directorio)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directorio)) {
            for (Path archivo : stream) {
                if (Files.isRegularFile(archivo)) {
                    analizarArchivo(archivo);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Error accediendo a directorio: " + directorio, e);
        }
    }

    /**
     * Búsqueda recursiva en directorios específicos
     */
    private void buscarRecursivamente() {
        String[] directoriosRecursivos = {"target", "build", "jmeter-reports", "evidencias"};

        for (String dir : directoriosRecursivos) {
            Path path = Paths.get(dir);
            if (Files.exists(path) && Files.isDirectory(path)) {
                try {
                    Files.walk(path, 3) // Máximo 3 niveles de profundidad
                            .filter(Files::isRegularFile)
                            .forEach(this::analizarArchivo);
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Error en búsqueda recursiva: " + dir, e);
                }
            }
        }
    }

    /**
     * Analiza un archivo para determinar si es un reporte JMeter
     */
    private void analizarArchivo(Path archivo) {
        String nombreArchivo = archivo.getFileName().toString().toLowerCase();

        try {
            // Verificar si es archivo JTL
            if (PATRON_JTL.matcher(nombreArchivo).matches()) {
                analizarArchivoJTL(archivo);
            }

            // Verificar si es reporte HTML de JMeter
            if (PATRON_HTML_JMETER.matcher(nombreArchivo).matches() ||
                    nombreArchivo.contains("report") || nombreArchivo.contains("dashboard")) {
                analizarReporteHTML(archivo);
            }

        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error analizando archivo: " + archivo, e);
        }
    }

    /**
     * Analiza un archivo JTL potencial
     */
    private void analizarArchivoJTL(Path archivo) throws IOException {
        // Verificar que el archivo tenga contenido
        if (Files.size(archivo) < 100) { // Muy pequeño para ser JTL válido
            return;
        }

        // Leer las primeras líneas para validar formato JTL
        List<String> primeras5Lineas = Files.lines(archivo)
                .limit(5)
                .collect(Collectors.toList());

        boolean esJTLValido = validarFormatoJTL(primeras5Lineas);

        if (esJTLValido) {
            String escenario = extraerEscenarioDeNombre(archivo.getFileName().toString());
            String tamaño = formatearTamaño(Files.size(archivo));
            LocalDateTime fechaModificacion = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(archivo).toInstant(),
                    java.time.ZoneId.systemDefault()
            );

            ArchivoJTLEncontrado jtl = new ArchivoJTLEncontrado(
                    archivo.toString(),
                    escenario,
                    tamaño,
                    fechaModificacion
            );

            archivosJTLEncontrados.add(jtl);
            LOGGER.info("🎯 JTL encontrado: " + archivo.getFileName() + " (" + escenario + ")");
        }
    }

    /**
     * Analiza un reporte HTML potencial
     */
    private void analizarReporteHTML(Path archivo) throws IOException {
        // Leer contenido del HTML para verificar si es de JMeter
        String contenido = Files.readString(archivo, java.nio.charset.StandardCharsets.UTF_8);

        if (esReporteJMeterValido(contenido)) {
            String escenario = extraerEscenarioDeContenido(contenido, archivo.getFileName().toString());
            String tamaño = formatearTamaño(Files.size(archivo));
            LocalDateTime fechaModificacion = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(archivo).toInstant(),
                    java.time.ZoneId.systemDefault()
            );

            ReporteJMeterEncontrado reporte = new ReporteJMeterEncontrado(
                    archivo.toString(),
                    escenario,
                    tamaño,
                    fechaModificacion,
                    extraerMetricasDeHTML(contenido)
            );

            reportesEncontrados.add(reporte);
            LOGGER.info("📊 Reporte HTML encontrado: " + archivo.getFileName() + " (" + escenario + ")");
        }
    }

    /**
     * Valida si las líneas corresponden a formato JTL válido
     */
    private boolean validarFormatoJTL(List<String> lineas) {
        if (lineas.isEmpty()) {
            return false;
        }

        // Verificar header JTL típico
        String primeraLinea = lineas.get(0).toLowerCase();
        if (primeraLinea.contains("timestamp") && primeraLinea.contains("elapsed")) {
            return true;
        }

        // Verificar formato de datos (segunda línea debe tener números)
        if (lineas.size() > 1) {
            String segundaLinea = lineas.get(1);
            String[] campos = segundaLinea.split(",");

            if (campos.length >= 3) {
                try {
                    // Intentar parsear timestamp y elapsed time
                    Long.parseLong(campos[0].trim());
                    Long.parseLong(campos[1].trim());
                    return true;
                } catch (NumberFormatException e) {
                    // No es formato JTL válido
                }
            }
        }

        return false;
    }

    /**
     * Verifica si el HTML es un reporte JMeter válido
     */
    private boolean esReporteJMeterValido(String contenido) {
        String contenidoLower = contenido.toLowerCase();

        return contenidoLower.contains("jmeter") &&
                (contenidoLower.contains("dashboard") ||
                        contenidoLower.contains("test results") ||
                        contenidoLower.contains("summary report") ||
                        contenidoLower.contains("aggregate report") ||
                        contenidoLower.contains("response time") ||
                        contenidoLower.contains("throughput"));
    }

    /**
     * Extrae el nombre del escenario del nombre del archivo
     */
    private String extraerEscenarioDeNombre(String nombreArchivo) {
        Matcher matcher = PATRON_ESCENARIO.matcher(nombreArchivo);
        if (matcher.find()) {
            String escenario = matcher.group(1);
            return formatearNombreEscenario(escenario);
        }

        // Intentar extraer de patrones comunes
        if (nombreArchivo.contains("get")) return "GET Masivo";
        if (nombreArchivo.contains("post")) return "POST Masivo";
        if (nombreArchivo.contains("mixto") || nombreArchivo.contains("combined")) return "GET+POST Combinado";

        return "Escenario Detectado";
    }

    /**
     * Extrae escenario del contenido HTML
     */
    private String extraerEscenarioDeContenido(String contenido, String nombreArchivo) {
        // Buscar en el contenido HTML
        if (contenido.toLowerCase().contains("get") && contenido.toLowerCase().contains("post")) {
            return "GET+POST Combinado";
        } else if (contenido.toLowerCase().contains("post")) {
            return "POST Masivo";
        } else if (contenido.toLowerCase().contains("get")) {
            return "GET Masivo";
        }

        // Fallback al nombre del archivo
        return extraerEscenarioDeNombre(nombreArchivo);
    }

    /**
     * Extrae métricas básicas del HTML
     */
    private Map<String, String> extraerMetricasDeHTML(String contenido) {
        Map<String, String> metricas = new HashMap<>();

        // Buscar patrones comunes de métricas en HTML
        extraerMetricaPorPatron(contenido, "samples?[^>]*>\\s*(\\d+)", "total_requests", metricas);
        extraerMetricaPorPatron(contenido, "average[^>]*>\\s*(\\d+(?:\\.\\d+)?)", "tiempo_promedio", metricas);
        extraerMetricaPorPatron(contenido, "error%?[^>]*>\\s*(\\d+(?:\\.\\d+)?)", "tasa_error", metricas);
        extraerMetricaPorPatron(contenido, "throughput[^>]*>\\s*(\\d+(?:\\.\\d+)?)", "throughput", metricas);

        return metricas;
    }

    /**
     * Extrae una métrica específica usando patrón regex
     */
    private void extraerMetricaPorPatron(String contenido, String patron, String clave, Map<String, String> metricas) {
        try {
            Pattern p = Pattern.compile(patron, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(contenido);
            if (m.find()) {
                metricas.put(clave, m.group(1));
            }
        } catch (Exception e) {
            // Ignorar errores de extracción
        }
    }

    /**
     * Formatea el nombre del escenario
     */
    private String formatearNombreEscenario(String escenario) {
        return switch (escenario.toLowerCase()) {
            case "get" -> "GET Masivo";
            case "post" -> "POST Masivo";
            case "mixto", "combined" -> "GET+POST Combinado";
            case "put" -> "PUT Masivo";
            case "delete" -> "DELETE Masivo";
            default -> escenario.substring(0, 1).toUpperCase() + escenario.substring(1) + " Masivo";
        };
    }

    /**
     * Valida y limpia archivos encontrados
     */
    private void validarArchivosEncontrados() {
        // Remover duplicados de JTL
        archivosJTLEncontrados.removeIf(jtl -> {
            try {
                return !Files.exists(Paths.get(jtl.rutaArchivo)) ||
                        Files.size(Paths.get(jtl.rutaArchivo)) < 100;
            } catch (IOException e) {
                return true;
            }
        });

        // Remover duplicados de reportes HTML
        reportesEncontrados.removeIf(reporte -> {
            try {
                return !Files.exists(Paths.get(reporte.rutaArchivo));
            } catch (Exception e) {
                return true;
            }
        });
    }

    /**
     * Muestra detalles de los archivos encontrados
     */
    private void mostrarDetallesEncontrados() {
        if (!archivosJTLEncontrados.isEmpty()) {
            LOGGER.info("📁 Archivos JTL detectados:");
            for (ArchivoJTLEncontrado jtl : archivosJTLEncontrados) {
                LOGGER.info(String.format("  📄 %s (%s) - %s",
                        Paths.get(jtl.rutaArchivo).getFileName(),
                        jtl.escenario,
                        jtl.tamaño));
            }
        }

        if (!reportesEncontrados.isEmpty()) {
            LOGGER.info("📊 Reportes HTML detectados:");
            for (ReporteJMeterEncontrado reporte : reportesEncontrados) {
                LOGGER.info(String.format("  🌐 %s (%s) - %s",
                        Paths.get(reporte.rutaArchivo).getFileName(),
                        reporte.escenario,
                        reporte.tamaño));
            }
        }
    }

    /**
     * Formatea el tamaño del archivo
     */
    private String formatearTamaño(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // Clases de datos para resultados

    /**
     * Resultado de la detección completa
     */
    public static class ResultadoDeteccion {
        public final boolean exitoso;
        public final String mensaje;
        public final List<ReporteJMeterEncontrado> reportes;
        public final List<ArchivoJTLEncontrado> archivosJTL;

        public ResultadoDeteccion(boolean exitoso, String mensaje,
                                  List<ReporteJMeterEncontrado> reportes,
                                  List<ArchivoJTLEncontrado> archivosJTL) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.reportes = Collections.unmodifiableList(reportes);
            this.archivosJTL = Collections.unmodifiableList(archivosJTL);
        }
    }

    /**
     * Información de un reporte JMeter HTML encontrado
     */
    public static class ReporteJMeterEncontrado {
        public final String rutaArchivo;
        public final String escenario;
        public final String tamaño;
        public final LocalDateTime fechaModificacion;
        public final Map<String, String> metricas;

        public ReporteJMeterEncontrado(String rutaArchivo, String escenario, String tamaño,
                                       LocalDateTime fechaModificacion, Map<String, String> metricas) {
            this.rutaArchivo = rutaArchivo;
            this.escenario = escenario;
            this.tamaño = tamaño;
            this.fechaModificacion = fechaModificacion;
            this.metricas = Collections.unmodifiableMap(new HashMap<>(metricas));
        }
    }

    /**
     * Información de un archivo JTL encontrado
     */
    public static class ArchivoJTLEncontrado {
        public final String rutaArchivo;
        public final String escenario;
        public final String tamaño;
        public final LocalDateTime fechaModificacion;

        public ArchivoJTLEncontrado(String rutaArchivo, String escenario, String tamaño,
                                    LocalDateTime fechaModificacion) {
            this.rutaArchivo = rutaArchivo;
            this.escenario = escenario;
            this.tamaño = tamaño;
            this.fechaModificacion = fechaModificacion;
        }
    }
}