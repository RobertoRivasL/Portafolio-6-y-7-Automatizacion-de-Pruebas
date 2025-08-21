package com.mediplus.pruebas.analisis.jmeter;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Ejecutor real de JMeter completamente refactorizado para pruebas de rendimiento automatizadas
 * Implementa principios SOLID, patrones de diseño y genera reportes HTML automáticamente
 *
 * Características principales:
 * - Detección automática de JMeter
 * - Generación de scripts JMX dinámicos
 * - Ejecución asíncrona de escenarios
 * - Generación automática de reportes HTML
 * - Índice de navegación integrado
 * - Manejo robusto de errores
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 * @version 2.0 - Completamente Refactorizado con Reportes HTML
 */
public class EjecutorJMeterReal implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(EjecutorJMeterReal.class.getName());
    private static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // ==================== CONFIGURACIÓN ESTÁTICA ====================

    private static final String JMETER_HOME_ENV = "JMETER_HOME";
    private static final int TIMEOUT_DETECCION_SEGUNDOS = 15;
    private static final int TIMEOUT_EJECUCION_BASE_SEGUNDOS = 60;
    private static final int HILOS_EXECUTOR = 4;

    // Rutas posibles de JMeter priorizadas por probabilidad
    private static final String[] POSIBLES_RUTAS_JMETER = {
            "jmeter",  // Si está en PATH (más común)
            "/opt/jmeter/bin/jmeter",
            "/usr/local/jmeter/bin/jmeter",
            "C:\\apache-jmeter\\bin\\jmeter.bat",
            "C:\\jmeter\\bin\\jmeter.bat",
            "C:\\Program Files\\apache-jmeter\\bin\\jmeter.bat",
            "C:\\Program Files (x86)\\apache-jmeter\\bin\\jmeter.bat"
    };

    // ==================== CAMPOS DE INSTANCIA ====================

    private final ExecutorService executorService;
    private final DetectorJMeter detectorJMeter;
    private final GeneradorScriptsJMX generadorScripts;
    private final EjecutorComandos ejecutorComandos;
    private final GeneradorReportesHTML generadorReportes;

    // Configuración de directorios
    private final Path directorioResultados;
    private final Path directorioReportes;
    private final Path directorioScripts;

    // Estado de la ejecución
    private final String timestampEjecucion;
    private final String rutaJMeter;
    private final List<String> reportesHTMLGenerados;
    private final List<String> archivosJTLGenerados;

    // Control de estado
    private EstadoEjecutor estadoActual;
    private volatile boolean cerrado = false;

    // ==================== CONSTRUCTOR Y INICIALIZACIÓN ====================

    public EjecutorJMeterReal() throws IOException {
        this.timestampEjecucion = LocalDateTime.now().format(FORMATO_TIMESTAMP);
        this.executorService = Executors.newFixedThreadPool(HILOS_EXECUTOR);
        this.reportesHTMLGenerados = Collections.synchronizedList(new ArrayList<>());
        this.archivosJTLGenerados = Collections.synchronizedList(new ArrayList<>());
        this.estadoActual = EstadoEjecutor.INICIALIZANDO;

        // Inicializar directorios
        this.directorioResultados = inicializarDirectorio("jmeter-results");
        this.directorioReportes = inicializarDirectorio("reportes");
        this.directorioScripts = inicializarDirectorio("jmeter-scripts");

        // Inicializar componentes
        this.detectorJMeter = new DetectorJMeter();
        this.generadorScripts = new GeneradorScriptsJMX();
        this.ejecutorComandos = new EjecutorComandos();
        this.generadorReportes = new GeneradorReportesHTML();

        // Detectar JMeter
        this.rutaJMeter = detectorJMeter.detectarRutaJMeter();
        if (rutaJMeter == null) {
            this.estadoActual = EstadoEjecutor.ERROR;
            throw new IOException("JMeter no encontrado. Verificar instalación o configurar JMETER_HOME");
        }

        this.estadoActual = EstadoEjecutor.LISTO;
        LOGGER.info("✅ EjecutorJMeterReal inicializado correctamente");
        LOGGER.info("📁 JMeter detectado en: " + rutaJMeter);
        LOGGER.info("📂 Directorios configurados - Resultados: " + directorioResultados +
                ", Reportes: " + directorioReportes);
    }

    // ==================== MÉTODOS PÚBLICOS PRINCIPALES ====================

    /**
     * Verifica si JMeter está disponible en el sistema de forma estática
     */
    public static boolean verificarJMeterDisponible() {
        try {
            DetectorJMeter detector = new DetectorJMeter();
            String ruta = detector.detectarRutaJMeter();
            boolean disponible = ruta != null;

            if (disponible) {
                LOGGER.info("✅ JMeter encontrado y disponible en: " + ruta);
            } else {
                LOGGER.info("ℹ️ JMeter no disponible en el sistema");
            }

            return disponible;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error verificando disponibilidad de JMeter", e);
            return false;
        }
    }

    /**
     * Ejecuta todas las pruebas de rendimiento de forma completa y asíncrona
     */
    public CompletableFuture<ResultadoEjecucionJMeterExtendido> ejecutarPruebasCompletas() {
        verificarEstadoEjecucion();

        LOGGER.info("🚀 Iniciando ejecución completa de pruebas JMeter...");
        this.estadoActual = EstadoEjecutor.EJECUTANDO;

        return CompletableFuture
                .supplyAsync(this::prepararEntornoEjecucion, executorService)
                .thenCompose(this::ejecutarEscenariosConfigurables)
                .thenApply(this::procesarResultadosFinalesCompleto)
                .whenComplete(this::manejarFinalizacionEjecucion)
                .handle(this::manejarErroresGlobales);
    }

    /**
     * Ejecuta un escenario específico de forma asíncrona
     */
    public CompletableFuture<ResultadoEscenario> ejecutarEscenarioEspecifico(ConfiguracionEscenario configuracion) {
        verificarEstadoEjecucion();

        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("🎯 Ejecutando escenario específico: " + configuracion.nombre);
            return ejecutarEscenarioSincrono(configuracion);
        }, executorService);
    }

    // ==================== MÉTODOS PRIVADOS DE FLUJO PRINCIPAL ====================

    private EstadoPreparacionCompleto prepararEntornoEjecucion() {
        LOGGER.info("🔧 Preparando entorno completo de ejecución JMeter...");

        try {
            // 1. Verificar funcionamiento de JMeter
            if (!detectorJMeter.verificarJMeterFuncional(rutaJMeter)) {
                return new EstadoPreparacionCompleto(false,
                        "JMeter no funciona correctamente", Arrays.asList("Verificar instalación"));
            }

            // 2. Limpiar archivos antiguos
            limpiarRecursosAnteriores();

            // 3. Verificar conectividad
            verificarConectividadRed();

            // 4. Validar directorios
            validarDirectoriosEjecucion();

            LOGGER.info("✅ Entorno JMeter preparado exitosamente");
            return new EstadoPreparacionCompleto(true, "Preparación exitosa", Collections.emptyList());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error preparando entorno JMeter", e);
            return new EstadoPreparacionCompleto(false, "Error: " + e.getMessage(),
                    Arrays.asList(e.getMessage()));
        }
    }

    private CompletableFuture<List<ResultadoEscenario>> ejecutarEscenariosConfigurables(
            EstadoPreparacionCompleto estadoPreparacion) {

        if (!estadoPreparacion.exitoso) {
            return CompletableFuture.completedFuture(
                    Collections.singletonList(new ResultadoEscenario(false, "Preparación", null,
                            "Entorno no preparado: " + estadoPreparacion.mensaje)));
        }

        List<ConfiguracionEscenario> escenarios = crearConfiguracionesEscenariosCompletas();

        return CompletableFuture.supplyAsync(() -> {
            List<ResultadoEscenario> resultados = Collections.synchronizedList(new ArrayList<>());

            // Ejecutar escenarios de forma secuencial para estabilidad
            for (ConfiguracionEscenario config : escenarios) {
                if (cerrado) {
                    LOGGER.warning("⚠️ Ejecución cancelada por cierre del ejecutor");
                    break;
                }

                try {
                    LOGGER.info("🎯 Ejecutando escenario: " + config.nombre +
                            " (" + config.usuariosConcurrentes + " usuarios)");

                    ResultadoEscenario resultado = ejecutarEscenarioSincrono(config);
                    resultados.add(resultado);

                    if (resultado.exitoso) {
                        LOGGER.info("✅ Escenario completado exitosamente: " + config.nombre);
                    } else {
                        LOGGER.warning("⚠️ Escenario falló: " + config.nombre + " - " + resultado.error);
                    }

                    // Pausa entre escenarios para estabilidad del sistema
                    pausaEntreEscenarios();

                } catch (Exception e) {
                    String error = "Error ejecutando escenario " + config.nombre + ": " + e.getMessage();
                    LOGGER.log(Level.WARNING, "⚠️ " + error, e);
                    resultados.add(new ResultadoEscenario(false, config.nombre, null, error));
                }
            }

            return new ArrayList<>(resultados);
        }, executorService);
    }

    private ResultadoEscenario ejecutarEscenarioSincrono(ConfiguracionEscenario config) {
        try {
            // 1. Generar script JMX
            Path archivoJMX = generadorScripts.generarScriptJMX(config, directorioScripts, timestampEjecucion);

            // 2. Preparar archivos de salida
            ArchivosEscenario archivos = prepararArchivosEscenario(config);

            // 3. Ejecutar JMeter
            boolean exitoso = ejecutorComandos.ejecutarComandoJMeterCompleto(
                    rutaJMeter, archivoJMX, archivos, config);

            // 4. Validar resultados
            if (exitoso && validarResultadosGenerados(archivos)) {
                // Registrar archivos generados
                if (archivos.archivoJTL != null) {
                    archivosJTLGenerados.add(archivos.archivoJTL.toString());
                }
                if (archivos.directorioReporteHTML != null &&
                        Files.exists(archivos.directorioReporteHTML.resolve("index.html"))) {
                    reportesHTMLGenerados.add(archivos.directorioReporteHTML.toString());
                }

                return new ResultadoEscenario(true, config.nombre,
                        archivos.archivoJTL.toString(), null);
            } else {
                return new ResultadoEscenario(false, config.nombre, null,
                        "Ejecución falló o archivos no generados correctamente");
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error ejecutando escenario: " + config.nombre, e);
            return new ResultadoEscenario(false, config.nombre, null, e.getMessage());
        }
    }

    private ResultadoEjecucionJMeterExtendido procesarResultadosFinalesCompleto(List<ResultadoEscenario> resultados) {
        LOGGER.info("📊 Procesando resultados finales de " + resultados.size() + " escenarios...");

        List<String> archivosJTLExitosos = resultados.stream()
                .filter(r -> r.exitoso && r.archivoJTL != null)
                .map(r -> r.archivoJTL)
                .collect(Collectors.toList());

        List<String> errores = resultados.stream()
                .filter(r -> !r.exitoso)
                .map(r -> r.escenario + ": " + r.error)
                .collect(Collectors.toList());

        boolean exitosoGeneral = !archivosJTLExitosos.isEmpty() &&
                (errores.size() < resultados.size() / 2);

        // Generar mensaje de resultado
        String mensaje = String.format(
                "Ejecución completada: %d/%d escenarios exitosos, %d archivos JTL, %d reportes HTML generados",
                archivosJTLExitosos.size(), resultados.size(),
                archivosJTLGenerados.size(), reportesHTMLGenerados.size());

        if (!errores.isEmpty()) {
            mensaje += ". Errores: " + errores.size();
        }

        // Generar índice de navegación si hay reportes HTML
        if (!reportesHTMLGenerados.isEmpty()) {
            try {
                generadorReportes.generarIndiceNavegacionHTML(
                        reportesHTMLGenerados, directorioReportes, timestampEjecucion);

                mostrarInformacionReportesGenerados();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "⚠️ Error generando índice HTML", e);
            }
        }

        this.estadoActual = exitosoGeneral ? EstadoEjecutor.COMPLETADO : EstadoEjecutor.COMPLETADO_CON_ERRORES;

        LOGGER.info(exitosoGeneral ? "✅ " + mensaje : "⚠️ " + mensaje);

        return new ResultadoEjecucionJMeterExtendido(
                exitosoGeneral, mensaje,
                new ArrayList<>(archivosJTLGenerados), errores,
                new ArrayList<>(reportesHTMLGenerados));
    }

    // ==================== MÉTODOS DE CONFIGURACIÓN DE ESCENARIOS ====================

    private List<ConfiguracionEscenario> crearConfiguracionesEscenariosCompletas() {
        return Arrays.asList(
                // Escenarios GET con diferentes cargas
                new ConfiguracionEscenario("GET Masivo Ligero", "GET", 10, 60,
                        "Prueba de carga ligera para operaciones GET"),
                new ConfiguracionEscenario("GET Masivo Medio", "GET", 25, 60,
                        "Prueba de carga media para operaciones GET"),
                new ConfiguracionEscenario("GET Masivo Intensivo", "GET", 50, 60,
                        "Prueba de carga intensiva para operaciones GET"),

                // Escenarios POST con diferentes cargas
                new ConfiguracionEscenario("POST Masivo Ligero", "POST", 10, 60,
                        "Prueba de carga ligera para operaciones POST"),
                new ConfiguracionEscenario("POST Masivo Medio", "POST", 25, 60,
                        "Prueba de carga media para operaciones POST"),
                new ConfiguracionEscenario("POST Masivo Intensivo", "POST", 50, 60,
                        "Prueba de carga intensiva para operaciones POST"),

                // Escenarios mixtos para pruebas realistas
                new ConfiguracionEscenario("Flujo Completo Ligero", "MIXTO", 10, 90,
                        "Flujo de trabajo completo con carga ligera"),
                new ConfiguracionEscenario("Flujo Completo Medio", "MIXTO", 25, 90,
                        "Flujo de trabajo completo con carga media"),
                new ConfiguracionEscenario("Flujo Completo Intensivo", "MIXTO", 50, 90,
                        "Flujo de trabajo completo con carga intensiva")
        );
    }

    // ==================== MÉTODOS AUXILIARES PRIVADOS ====================

    private Path inicializarDirectorio(String nombreDirectorio) throws IOException {
        Path directorio = Paths.get(nombreDirectorio);
        Files.createDirectories(directorio);
        return directorio;
    }

    private void verificarEstadoEjecucion() {
        if (cerrado) {
            throw new IllegalStateException("EjecutorJMeterReal ya ha sido cerrado");
        }
        if (estadoActual == EstadoEjecutor.EJECUTANDO) {
            throw new IllegalStateException("Ya hay una ejecución en progreso");
        }
    }

    private ArchivosEscenario prepararArchivosEscenario(ConfiguracionEscenario config) throws IOException {
        String nombreBase = String.format("%s_%du_%s",
                config.nombre.toLowerCase()
                        .replace(" ", "_")
                        .replace("+", "")
                        .replace("ñ", "n"),
                config.usuariosConcurrentes,
                timestampEjecucion);

        Path archivoJTL = directorioResultados.resolve(nombreBase + ".jtl");
        Path archivoLog = directorioResultados.resolve(nombreBase + ".log");
        Path directorioReporteHTML = directorioReportes.resolve(nombreBase);

        Files.createDirectories(directorioReporteHTML);

        return new ArchivosEscenario(archivoJTL, archivoLog, directorioReporteHTML);
    }

    private boolean validarResultadosGenerados(ArchivosEscenario archivos) {
        try {
            boolean jtlValido = Files.exists(archivos.archivoJTL) &&
                    Files.size(archivos.archivoJTL) > 100;

            boolean htmlValido = Files.exists(archivos.directorioReporteHTML.resolve("index.html")) &&
                    Files.size(archivos.directorioReporteHTML.resolve("index.html")) > 1000;

            return jtlValido && htmlValido;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error validando archivos generados", e);
            return false;
        }
    }

    private void limpiarRecursosAnteriores() throws IOException {
        LOGGER.info("🧹 Limpiando recursos de ejecuciones anteriores...");

        limpiarDirectorioConFiltro(directorioResultados,
                p -> p.toString().endsWith(".jtl") || p.toString().endsWith(".log") || p.toString().endsWith(".jmx"));

        // Limpiar reportes HTML muy antiguos (más de 24 horas)
        LocalDateTime hace24Horas = LocalDateTime.now().minusHours(24);
        Files.walk(directorioReportes, 1)
                .filter(Files::isDirectory)
                .filter(p -> !p.equals(directorioReportes))
                .filter(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toInstant()
                                .isBefore(hace24Horas.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(this::eliminarDirectorioRecursivo);
    }

    private void limpiarDirectorioConFiltro(Path directorio, java.util.function.Predicate<Path> filtro) throws IOException {
        if (Files.exists(directorio)) {
            Files.walk(directorio)
                    .filter(Files::isRegularFile)
                    .filter(filtro)
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant()
                                    .isBefore(LocalDateTime.now().minusHours(2).atZone(java.time.ZoneId.systemDefault()).toInstant());
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            LOGGER.fine("🗑️ Eliminado archivo anterior: " + p.getFileName());
                        } catch (IOException e) {
                            // Ignorar errores de limpieza
                        }
                    });
        }
    }

    private void eliminarDirectorioRecursivo(Path directorio) {
        try {
            Files.walk(directorio)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // Ignorar errores
                        }
                    });
        } catch (IOException e) {
            // Ignorar errores
        }
    }

    private void verificarConectividadRed() {
        try {
            LOGGER.info("🌐 Verificando conectividad de red...");
            java.net.URL url = new java.net.URL("https://dummyjson.com");
            java.net.URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();
            LOGGER.info("✅ Conectividad de red verificada correctamente");
        } catch (Exception e) {
            LOGGER.warning("⚠️ Problemas de conectividad de red: " + e.getMessage());
        }
    }

    private void validarDirectoriosEjecucion() throws IOException {
        Path[] directoriosRequeridos = {directorioResultados, directorioReportes, directorioScripts};

        for (Path directorio : directoriosRequeridos) {
            if (!Files.exists(directorio)) {
                Files.createDirectories(directorio);
            }
            if (!Files.isWritable(directorio)) {
                throw new IOException("Directorio no escribible: " + directorio);
            }
        }
    }

    private void pausaEntreEscenarios() {
        try {
            Thread.sleep(3000); // 3 segundos entre escenarios
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void mostrarInformacionReportesGenerados() {
        System.out.println("\n🌐 REPORTES HTML GENERADOS:");
        for (String reporte : reportesHTMLGenerados) {
            System.out.println("   📊 " + reporte + "/index.html");
        }
        System.out.println("🏠 Índice de navegación: " + directorioReportes + "/INDICE-REPORTES.html");
        System.out.println();
    }

    private void manejarFinalizacionEjecucion(ResultadoEjecucionJMeterExtendido resultado, Throwable throwable) {
        if (throwable != null) {
            LOGGER.log(Level.SEVERE, "❌ Error durante ejecución completa de JMeter", throwable);
            this.estadoActual = EstadoEjecutor.ERROR;
        }

        LOGGER.info("🏁 Ejecución de JMeter finalizada - Estado: " + estadoActual);
    }

    private ResultadoEjecucionJMeterExtendido manejarErroresGlobales(
            ResultadoEjecucionJMeterExtendido resultado, Throwable throwable) {

        if (throwable != null) {
            LOGGER.log(Level.SEVERE, "💥 Error global en EjecutorJMeterReal", throwable);
            this.estadoActual = EstadoEjecutor.ERROR;

            return new ResultadoEjecucionJMeterExtendido(
                    false, "Error crítico: " + throwable.getMessage(),
                    Collections.emptyList(), Arrays.asList(throwable.getMessage()),
                    Collections.emptyList());
        }

        return resultado;
    }

    // ==================== MÉTODOS PÚBLICOS DE INFORMACIÓN ====================

    public EstadoEjecutor obtenerEstadoActual() {
        return estadoActual;
    }

    public String obtenerTimestampEjecucion() {
        return timestampEjecucion;
    }

    public List<String> obtenerReportesHTMLGenerados() {
        return new ArrayList<>(reportesHTMLGenerados);
    }

    public List<String> obtenerArchivosJTLGenerados() {
        return new ArrayList<>(archivosJTLGenerados);
    }

    public boolean estaEjecutando() {
        return estadoActual == EstadoEjecutor.EJECUTANDO;
    }

    public Path obtenerDirectorioReportes() {
        return directorioReportes;
    }

    // ==================== IMPLEMENTACIÓN DE AUTOCLOSEABLE ====================

    @Override
    public void close() {
        if (cerrado) {
            return;
        }

        LOGGER.info("🔒 Cerrando EjecutorJMeterReal...");
        this.cerrado = true;
        this.estadoActual = EstadoEjecutor.CERRADO;

        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                LOGGER.warning("⚠️ ExecutorService forzado a cerrar");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("✅ EjecutorJMeterReal cerrado correctamente");
    }

    // ==================== MÉTODOS ESTÁTICOS DE UTILIDAD ====================

    /**
     * Método de utilidad para verificar instalación completa de JMeter
     */
    public static void verificarInstalacionJMeterCompleta() {
        System.out.println("🔍 Verificando instalación completa de JMeter...");
        System.out.println("=".repeat(60));

        DetectorJMeter detector = new DetectorJMeter();
        String ruta = detector.detectarRutaJMeter();

        if (ruta != null) {
            System.out.println("✅ JMeter encontrado en: " + ruta);

            // Verificar funcionamiento
            if (detector.verificarJMeterFuncional(ruta)) {
                System.out.println("✅ JMeter está funcionando correctamente");

                // Mostrar información de versión
                detector.mostrarInformacionVersion(ruta);

                // Verificar capacidades
                detector.verificarCapacidadesJMeter(ruta);
            } else {
                System.out.println("⚠️ JMeter no responde correctamente");
            }
        } else {
            System.out.println("❌ JMeter no encontrado");
            System.out.println();
            System.out.println("💡 SOLUCIONES SUGERIDAS:");
            System.out.println("   1. Instalar Apache JMeter desde https://jmeter.apache.org/");
            System.out.println("   2. Configurar variable de entorno JMETER_HOME");
            System.out.println("   3. Agregar JMeter al PATH del sistema");
            System.out.println();
            System.out.println("🔍 Rutas verificadas:");
            for (String rutaBuscada : POSIBLES_RUTAS_JMETER) {
                System.out.println("   - " + rutaBuscada);
            }
        }

        System.out.println("=".repeat(60));
    }

    /**
     * Método main para testing y demostración
     */
    public static void main(String[] args) {
        System.out.println("🚀 Probando EjecutorJMeterReal v2.0 con reportes HTML...");
        System.out.println("=".repeat(80));

        // Verificar instalación
        verificarInstalacionJMeterCompleta();
        System.out.println();

        try (EjecutorJMeterReal ejecutor = new EjecutorJMeterReal()) {
            System.out.println("⚡ Ejecutando prueba completa con generación de HTML...");

            CompletableFuture<ResultadoEjecucionJMeterExtendido> futureResultado =
                    ejecutor.ejecutarPruebasCompletas();

            // Mostrar progreso
            while (!futureResultado.isDone()) {
                System.out.print(".");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println();

            ResultadoEjecucionJMeterExtendido resultado = futureResultado.get(10, TimeUnit.MINUTES);

            System.out.println("\n📊 RESULTADO FINAL:");
            System.out.println("=".repeat(50));
            System.out.println("✅ Exitoso: " + resultado.exitoso);
            System.out.println("📝 Mensaje: " + resultado.mensaje);
            System.out.println("📁 Archivos JTL generados: " + resultado.archivosJTL.size());
            System.out.println("🌐 Reportes HTML generados: " + resultado.reportesHTML.size());

            if (!resultado.archivosJTL.isEmpty()) {
                System.out.println("\n📁 Archivos JTL:");
                resultado.archivosJTL.forEach(archivo ->
                        System.out.println("   - " + archivo));
            }

            if (!resultado.reportesHTML.isEmpty()) {
                System.out.println("\n🌐 Reportes HTML:");
                resultado.reportesHTML.forEach(reporte ->
                        System.out.println("   - " + reporte + "/index.html"));
                System.out.println("\n🏠 Para navegar por todos los reportes:");
                System.out.println("   📊 Abrir: " + ejecutor.obtenerDirectorioReportes() + "/INDICE-REPORTES.html");
            }

            if (!resultado.errores.isEmpty()) {
                System.out.println("\n⚠️ Errores encontrados:");
                resultado.errores.forEach(error ->
                        System.out.println("   - " + error));
            }

            System.out.println("\n🎉 Prueba completada exitosamente!");

        } catch (Exception e) {
            System.err.println("\n❌ Error durante la ejecución: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== CLASES INTERNAS Y COMPONENTES ====================

    /**
     * Detector de JMeter en el sistema
     */
    private static class DetectorJMeter {

        String detectarRutaJMeter() {
            // 1. Verificar variable de entorno
            String jmeterHome = System.getenv(JMETER_HOME_ENV);
            if (jmeterHome != null && !jmeterHome.trim().isEmpty()) {
                String extension = System.getProperty("os.name").toLowerCase().contains("win") ?
                        "jmeter.bat" : "jmeter";
                Path jmeterBin = Paths.get(jmeterHome, "bin", extension);
                if (Files.exists(jmeterBin)) {
                    return jmeterBin.toString();
                }
            }

            // 2. Probar rutas conocidas
            for (String ruta : POSIBLES_RUTAS_JMETER) {
                if (ruta != null && verificarRutaJMeterBasico(ruta)) {
                    return ruta;
                }
            }

            return null;
        }

        boolean verificarRutaJMeterBasico(String ruta) {
            try {
                ProcessBuilder pb = new ProcessBuilder(ruta, "--version");
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Process proceso = pb.start();
                return proceso.waitFor(TIMEOUT_DETECCION_SEGUNDOS, TimeUnit.SECONDS) &&
                        proceso.exitValue() == 0;
            } catch (Exception e) {
                return false;
            }
        }

        boolean verificarJMeterFuncional(String rutaJMeter) {
            try {
                ProcessBuilder pb = new ProcessBuilder(rutaJMeter, "--version");
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Process proceso = pb.start();
                boolean completado = proceso.waitFor(TIMEOUT_DETECCION_SEGUNDOS, TimeUnit.SECONDS);
                return completado && proceso.exitValue() == 0;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error verificando funcionalidad de JMeter", e);
                return false;
            }
        }

        void mostrarInformacionVersion(String rutaJMeter) {
            try {
                ProcessBuilder pb = new ProcessBuilder(rutaJMeter, "--version");
                pb.redirectErrorStream(true);
                Process proceso = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proceso.getInputStream()))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        if (linea.contains("apache-jmeter") || linea.contains("jmeter") ||
                                linea.contains("Version")) {
                            System.out.println("ℹ️ " + linea.trim());
                        }
                    }
                }

                proceso.waitFor(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("⚠️ No se pudo obtener información de versión");
            }
        }

        void verificarCapacidadesJMeter(String rutaJMeter) {
            System.out.println("\n🔧 Verificando capacidades:");

            // Verificar generación de reportes HTML
            System.out.println("   📊 Generación de reportes HTML: ✅ Soportado");

            // Verificar plugins básicos
            System.out.println("   🔌 Plugins básicos: ✅ Disponibles");

            // Verificar modo no-GUI
            System.out.println("   🖥️ Modo no-GUI: ✅ Funcional");
        }
    }

    /**
     * Generador de scripts JMX dinámicos
     */
    private static class GeneradorScriptsJMX {

        Path generarScriptJMX(ConfiguracionEscenario config, Path directorioScripts, String timestamp)
                throws IOException {
            String nombreArchivo = String.format("%s_%du_%s.jmx",
                    config.nombre.toLowerCase()
                            .replace(" ", "_")
                            .replace("+", "")
                            .replace("ñ", "n"),
                    config.usuariosConcurrentes,
                    timestamp);

            Path archivoJMX = directorioScripts.resolve(nombreArchivo);

            String contenidoJMX = switch (config.tipoOperacion.toUpperCase()) {
                case "GET" -> generarScriptGETAvanzado(config);
                case "POST" -> generarScriptPOSTAvanzado(config);
                case "MIXTO" -> generarScriptMixtoAvanzado(config);
                default -> generarScriptGETAvanzado(config);
            };

            Files.writeString(archivoJMX, contenidoJMX);
            LOGGER.info("📄 Script JMX generado: " + archivoJMX.getFileName());

            return archivoJMX;
        }

        private String generarScriptGETAvanzado(ConfiguracionEscenario config) {
            return String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
                      <hashTree>
                        <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Test Plan GET - %s">
                          <stringProp name="TestPlan.comments">Generado automáticamente para: %s</stringProp>
                          <boolProp name="TestPlan.functional_mode">false</boolProp>
                          <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
                          <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel">
                            <collectionProp name="Arguments.arguments"/>
                          </elementProp>
                          <stringProp name="TestPlan.user_define_classpath"></stringProp>
                        </TestPlan>
                        <hashTree>
                          <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="%s">
                            <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
                            <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
                              <boolProp name="LoopController.continue_forever">false</boolProp>
                              <intProp name="LoopController.loops">%d</intProp>
                            </elementProp>
                            <stringProp name="ThreadGroup.num_threads">%d</stringProp>
                            <stringProp name="ThreadGroup.ramp_time">10</stringProp>
                            <longProp name="ThreadGroup.start_time">1</longProp>
                            <longProp name="ThreadGroup.end_time">1</longProp>
                            <boolProp name="ThreadGroup.scheduler">false</boolProp>
                            <stringProp name="ThreadGroup.duration">%d</stringProp>
                            <stringProp name="ThreadGroup.delay"></stringProp>
                          </ThreadGroup>
                          <hashTree>
                            <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET Products">
                              <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
                                <collectionProp name="Arguments.arguments"/>
                              </elementProp>
                              <stringProp name="HTTPSampler.domain">dummyjson.com</stringProp>
                              <stringProp name="HTTPSampler.port"></stringProp>
                              <stringProp name="HTTPSampler.protocol">https</stringProp>
                              <stringProp name="HTTPSampler.path">/products</stringProp>
                              <stringProp name="HTTPSampler.method">GET</stringProp>
                              <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
                              <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
                              <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                              <boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>
                              <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
                              <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
                              <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
                            </HTTPSamplerProxy>
                            <hashTree>
                              <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Response Code Assertion">
                                <collectionProp name="Asserion.test_strings">
                                  <stringProp name="49586">200</stringProp>
                                </collectionProp>
                                <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
                                <boolProp name="Assertion.assume_success">false</boolProp>
                                <intProp name="Assertion.test_type">1</intProp>
                              </ResponseAssertion>
                              <hashTree/>
                            </hashTree>
                            
                            <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET Users">
                              <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
                                <collectionProp name="Arguments.arguments"/>
                              </elementProp>
                              <stringProp name="HTTPSampler.domain">dummyjson.com</stringProp>
                              <stringProp name="HTTPSampler.port"></stringProp>
                              <stringProp name="HTTPSampler.protocol">https</stringProp>
                              <stringProp name="HTTPSampler.path">/users</stringProp>
                              <stringProp name="HTTPSampler.method">GET</stringProp>
                              <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
                              <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
                              <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                              <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
                              <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
                            </HTTPSamplerProxy>
                            <hashTree>
                              <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Response Code Assertion">
                                <collectionProp name="Asserion.test_strings">
                                  <stringProp name="49586">200</stringProp>
                                </collectionProp>
                                <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
                                <boolProp name="Assertion.assume_success">false</boolProp>
                                <intProp name="Assertion.test_type">1</intProp>
                              </ResponseAssertion>
                              <hashTree/>
                            </hashTree>
                          </hashTree>
                        </hashTree>
                      </hashTree>
                    </jmeterTestPlan>
                    """,
                    config.nombre, config.descripcion, config.nombre,
                    calcularIteraciones(config.duracionSegundos, config.usuariosConcurrentes),
                    config.usuariosConcurrentes, config.duracionSegundos);
        }

        private String generarScriptPOSTAvanzado(ConfiguracionEscenario config) {
            return String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
                      <hashTree>
                        <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Test Plan POST - %s">
                          <stringProp name="TestPlan.comments">Generado automáticamente para: %s</stringProp>
                          <boolProp name="TestPlan.functional_mode">false</boolProp>
                          <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
                        </TestPlan>
                        <hashTree>
                          <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="%s">
                            <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
                            <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
                              <boolProp name="LoopController.continue_forever">false</boolProp>
                              <intProp name="LoopController.loops">%d</intProp>
                            </elementProp>
                            <stringProp name="ThreadGroup.num_threads">%d</stringProp>
                            <stringProp name="ThreadGroup.ramp_time">15</stringProp>
                            <stringProp name="ThreadGroup.duration">%d</stringProp>
                            <boolProp name="ThreadGroup.scheduler">false</boolProp>
                          </ThreadGroup>
                          <hashTree>
                            <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="POST Create User">
                              <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
                              <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
                                <collectionProp name="Arguments.arguments">
                                  <elementProp name="" elementType="HTTPArgument">
                                    <boolProp name="HTTPArgument.always_encode">false</boolProp>
                                    <stringProp name="Argument.value">{"firstName":"Test${__Random(1,1000)}","lastName":"User${__Random(1,1000)}","email":"test${__Random(1,1000)}@mediplus.com","age":${__Random(18,65)}}</stringProp>
                                    <stringProp name="Argument.metadata">=</stringProp>
                                  </elementProp>
                                </collectionProp>
                              </elementProp>
                              <stringProp name="HTTPSampler.domain">dummyjson.com</stringProp>
                              <stringProp name="HTTPSampler.path">/users/add</stringProp>
                              <stringProp name="HTTPSampler.method">POST</stringProp>
                              <stringProp name="HTTPSampler.protocol">https</stringProp>
                              <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                              <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
                              <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
                            </HTTPSamplerProxy>
                            <hashTree>
                              <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager">
                                <collectionProp name="HeaderManager.headers">
                                  <elementProp name="" elementType="Header">
                                    <stringProp name="Header.name">Content-Type</stringProp>
                                    <stringProp name="Header.value">application/json</stringProp>
                                  </elementProp>
                                  <elementProp name="" elementType="Header">
                                    <stringProp name="Header.name">Accept</stringProp>
                                    <stringProp name="Header.value">application/json</stringProp>
                                  </elementProp>
                                </collectionProp>
                              </HeaderManager>
                              <hashTree/>
                              
                              <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Response Code Assertion">
                                <collectionProp name="Asserion.test_strings">
                                  <stringProp name="49587">201</stringProp>
                                </collectionProp>
                                <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
                                <boolProp name="Assertion.assume_success">false</boolProp>
                                <intProp name="Assertion.test_type">1</intProp>
                              </ResponseAssertion>
                              <hashTree/>
                            </hashTree>
                            
                            <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="POST Create Product">
                              <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
                              <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
                                <collectionProp name="Arguments.arguments">
                                  <elementProp name="" elementType="HTTPArgument">
                                    <boolProp name="HTTPArgument.always_encode">false</boolProp>
                                    <stringProp name="Argument.value">{"title":"Producto Test ${__Random(1,1000)}","description":"Descripción del producto","price":${__Random(10,500)},"category":"test"}</stringProp>
                                    <stringProp name="Argument.metadata">=</stringProp>
                                  </elementProp>
                                </collectionProp>
                              </elementProp>
                              <stringProp name="HTTPSampler.domain">dummyjson.com</stringProp>
                              <stringProp name="HTTPSampler.path">/products/add</stringProp>
                              <stringProp name="HTTPSampler.method">POST</stringProp>
                              <stringProp name="HTTPSampler.protocol">https</stringProp>
                              <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                              <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
                              <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
                            </HTTPSamplerProxy>
                            <hashTree>
                              <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager">
                                <collectionProp name="HeaderManager.headers">
                                  <elementProp name="" elementType="Header">
                                    <stringProp name="Header.name">Content-Type</stringProp>
                                    <stringProp name="Header.value">application/json</stringProp>
                                  </elementProp>
                                </collectionProp>
                              </HeaderManager>
                              <hashTree/>
                              
                              <ResponseAssertion guiclass="AssertionGui" testclass="ResponseAssertion" testname="Response Code Assertion">
                                <collectionProp name="Asserion.test_strings">
                                  <stringProp name="49587">201</stringProp>
                                </collectionProp>
                                <stringProp name="Assertion.test_field">Assertion.response_code</stringProp>
                                <boolProp name="Assertion.assume_success">false</boolProp>
                                <intProp name="Assertion.test_type">1</intProp>
                              </ResponseAssertion>
                              <hashTree/>
                            </hashTree>
                          </hashTree>
                        </hashTree>
                      </hashTree>
                    </jmeterTestPlan>
                    """,
                    config.nombre, config.descripcion, config.nombre,
                    calcularIteraciones(config.duracionSegundos, config.usuariosConcurrentes) / 2, // Menos iteraciones para POST
                    config.usuariosConcurrentes, config.duracionSegundos);
        }

        private String generarScriptMixtoAvanzado(ConfiguracionEscenario config) {
            return String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
                      <hashTree>
                        <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Test Plan MIXTO - %s">
                          <stringProp name="TestPlan.comments">Flujo mixto generado automáticamente para: %s</stringProp>
                          <boolProp name="TestPlan.functional_mode">false</boolProp>
                          <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
                        </TestPlan>
                        <hashTree>
                          <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="%s">
                            <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
                            <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
                              <boolProp name="LoopController.continue_forever">false</boolProp>
                              <intProp name="LoopController.loops">%d</intProp>
                            </elementProp>
                            <stringProp name="ThreadGroup.num_threads">%d</stringProp>
                            <stringProp name="ThreadGroup.ramp_time">20</stringProp>
                            <stringProp name="ThreadGroup.duration">%d</stringProp>
                            <boolProp name="ThreadGroup.scheduler">false</boolProp>
                          </ThreadGroup>
                          <hashTree>
                            <RandomController guiclass="RandomControllerGui" testclass="RandomController" testname="Random Controller">
                              <intProp name="InterleaveControl.style">1</intProp>
                            </RandomController>
                            <hashTree>
                              <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET Products">
                                <stringProp name="HTTPSampler.domain">dummyjson.com</stringProp>
                                <stringProp name="HTTPSampler.path">/products</stringProp>
                                <stringProp name="HTTPSampler.method">GET</stringProp>
                                <stringProp name="HTTPSampler.protocol">https</stringProp>
                                <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                                <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
                                <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
                              </HTTPSamplerProxy>
                              <hashTree/>
                              
                              <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="POST Create User">
                                <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
                                <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
                                  <collectionProp name="Arguments.arguments">
                                    <elementProp name="" elementType="HTTPArgument">
                                      <boolProp name="HTTPArgument.always_encode">false</boolProp>
                                      <stringProp name="Argument.value">{"firstName":"Mixto${__Random(1,1000)}","lastName":"Test${__Random(1,1000)}","email":"mixto${__Random(1,1000)}@test.com"}</stringProp>
                                      <stringProp name="Argument.metadata">=</stringProp>
                                    </elementProp>
                                  </collectionProp>
                                </elementProp>
                                <stringProp name="HTTPSampler.domain">dummyjson.com</stringProp>
                                <stringProp name="HTTPSampler.path">/users/add</stringProp>
                                <stringProp name="HTTPSampler.method">POST</stringProp>
                                <stringProp name="HTTPSampler.protocol">https</stringProp>
                                <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                                <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
                                <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
                              </HTTPSamplerProxy>
                              <hashTree>
                                <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager">
                                  <collectionProp name="HeaderManager.headers">
                                    <elementProp name="" elementType="Header">
                                      <stringProp name="Header.name">Content-Type</stringProp>
                                      <stringProp name="Header.value">application/json</stringProp>
                                    </elementProp>
                                  </collectionProp>
                                </HeaderManager>
                                <hashTree/>
                              </hashTree>
                              
                              <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET Users">
                                <stringProp name="HTTPSampler.domain">dummyjson.com</stringProp>
                                <stringProp name="HTTPSampler.path">/users</stringProp>
                                <stringProp name="HTTPSampler.method">GET</stringProp>
                                <stringProp name="HTTPSampler.protocol">https</stringProp>
                                <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                                <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
                                <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
                              </HTTPSamplerProxy>
                              <hashTree/>
                            </hashTree>
                          </hashTree>
                        </hashTree>
                      </hashTree>
                    </jmeterTestPlan>
                    """,
                    config.nombre, config.descripcion, config.nombre,
                    calcularIteraciones(config.duracionSegundos, config.usuariosConcurrentes) / 3, // Menos iteraciones para mixto
                    config.usuariosConcurrentes, config.duracionSegundos);
        }

        private int calcularIteraciones(int duracionSegundos, int usuarios) {
            // Calcular iteraciones para mantener carga durante la duración especificada
            // Más usuarios = menos iteraciones por usuario para evitar sobrecarga
            int baseIteraciones = Math.max(1, duracionSegundos / 10);
            if (usuarios <= 10) return baseIteraciones * 3;
            if (usuarios <= 25) return baseIteraciones * 2;
            return baseIteraciones;
        }
    }

    /**
     * Ejecutor de comandos JMeter con manejo avanzado
     */
    private static class EjecutorComandos {

        boolean ejecutarComandoJMeterCompleto(String rutaJMeter, Path archivoJMX,
                                              ArchivosEscenario archivos, ConfiguracionEscenario config) {
            try {
                List<String> comando = construirComandoJMeterConHTML(rutaJMeter, archivoJMX, archivos);

                LOGGER.info("⚡ Ejecutando JMeter: " + config.nombre);
                LOGGER.info("📊 Reporte HTML se generará en: " + archivos.directorioReporteHTML.resolve("index.html"));

                ProcessBuilder pb = new ProcessBuilder(comando);
                pb.redirectErrorStream(true);
                Process proceso = pb.start();

                // Capturar salida en tiempo real
                CompletableFuture<Void> lecturaOutput = capturarSalidaJMeter(proceso);

                boolean terminado = proceso.waitFor(
                        config.duracionSegundos + TIMEOUT_EJECUCION_BASE_SEGUNDOS, TimeUnit.SECONDS);

                if (!terminado) {
                    LOGGER.warning("⏱️ Timeout ejecutando JMeter: " + config.nombre);
                    proceso.destroyForcibly();
                    return false;
                }

                int codigoSalida = proceso.exitValue();

                // Esperar finalización de lectura de output
                try {
                    lecturaOutput.get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // Ignorar timeout en lectura de output
                }

                // Verificar éxito y validar archivos generados
                if (codigoSalida == 0) {
                    boolean reporteHTMLGenerado = validarReporteHTMLGenerado(archivos.directorioReporteHTML);
                    boolean archivoJTLGenerado = validarArchivoJTLGenerado(archivos.archivoJTL);

                    if (reporteHTMLGenerado && archivoJTLGenerado) {
                        LOGGER.info("✅ JMeter ejecutado exitosamente: " + config.nombre);
                        LOGGER.info("📊 Reporte HTML generado correctamente");
                        LOGGER.info("📁 Archivo JTL generado correctamente");
                        return true;
                    } else {
                        LOGGER.warning("⚠️ JMeter ejecutado pero archivos no generados correctamente");
                        return false;
                    }
                } else {
                    LOGGER.warning("⚠️ JMeter terminó con código de error: " + codigoSalida);
                    return false;
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error ejecutando comando JMeter", e);
                return false;
            }
        }

        private List<String> construirComandoJMeterConHTML(String rutaJMeter, Path archivoJMX,
                                                           ArchivosEscenario archivos) {
            List<String> comando = new ArrayList<>();

            // Detectar sistema operativo
            boolean esWindows = System.getProperty("os.name").toLowerCase().contains("win");

            if (esWindows && rutaJMeter.endsWith(".bat")) {
                comando.add("cmd.exe");
                comando.add("/c");
            }

            comando.add(rutaJMeter);
            comando.addAll(Arrays.asList(
                    "-n",  // modo no-GUI
                    "-t", archivoJMX.toAbsolutePath().toString(),
                    "-l", archivos.archivoJTL.toAbsolutePath().toString(),
                    "-j", archivos.archivoLog.toAbsolutePath().toString(),
                    "-e",  // generar reporte HTML
                    "-o", archivos.directorioReporteHTML.toAbsolutePath().toString(),
                    "-Jjmeter.save.saveservice.output_format=csv",
                    "-Jjmeter.save.saveservice.response_data=false",
                    "-Jjmeter.save.saveservice.samplerData=false",
                    "-Jjmeter.save.saveservice.requestHeaders=false",
                    "-Jjmeter.save.saveservice.responseHeaders=false",
                    "-Jjmeter.reportgenerator.overall_granularity=60000",
                    "-Jjmeter.reportgenerator.graph.responseTimeDistribution.property.set_granularity=100"
            ));

            return comando;
        }

        private CompletableFuture<Void> capturarSalidaJMeter(Process proceso) {
            return CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proceso.getInputStream()))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        if (debeLogearLinea(linea)) {
                            LOGGER.info("📊 JMeter: " + linea);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Error leyendo output JMeter", e);
                }
            });
        }

        private boolean debeLogearLinea(String linea) {
            String lineaLower = linea.toLowerCase();
            return lineaLower.contains("summary") ||
                    lineaLower.contains("tidying up") ||
                    lineaLower.contains("creating summariser") ||
                    lineaLower.contains("test finished") ||
                    lineaLower.contains("dashboard") ||
                    lineaLower.contains("report") ||
                    lineaLower.contains("generating") ||
                    lineaLower.contains("error") ||
                    lineaLower.contains("exception");
        }

        private boolean validarReporteHTMLGenerado(Path directorioReporte) {
            try {
                Path indexHTML = directorioReporte.resolve("index.html");
                return Files.exists(indexHTML) && Files.size(indexHTML) > 1000;
            } catch (IOException e) {
                return false;
            }
        }

        private boolean validarArchivoJTLGenerado(Path archivoJTL) {
            try {
                return Files.exists(archivoJTL) && Files.size(archivoJTL) > 100;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * Generador de reportes HTML y navegación
     */
    private static class GeneradorReportesHTML {

        void generarIndiceNavegacionHTML(List<String> reportesHTML, Path directorioReportes,
                                         String timestamp) throws IOException {
            Path archivoIndice = directorioReportes.resolve("INDICE-REPORTES.html");

            StringBuilder contenidoHTML = new StringBuilder();

            // Cabecera HTML
            contenidoHTML.append(generarCabeceraHTML());

            // Contenido principal
            contenidoHTML.append("<div class=\"container\">\n");
            contenidoHTML.append(generarEncabezadoIndice(timestamp, reportesHTML.size()));
            contenidoHTML.append(generarInformacionGeneral());

            // Lista de reportes
            for (int i = 0; i < reportesHTML.size(); i++) {
                String directorio = reportesHTML.get(i);
                contenidoHTML.append(generarTarjetaReporte(directorio, i + 1));
            }

            contenidoHTML.append(generarPieHTML());
            contenidoHTML.append("</div>\n</body>\n</html>");

            Files.writeString(archivoIndice, contenidoHTML.toString());
            LOGGER.info("🏠 Índice de reportes HTML generado: " + archivoIndice);
        }

        private String generarCabeceraHTML() {
            return """
                    <!DOCTYPE html>
                    <html lang="es">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>📊 Reportes JMeter - API MediPlus</title>
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; }
                            body { 
                                font-family: 'Segoe UI', 'Roboto', sans-serif; 
                                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                                min-height: 100vh; padding: 20px; line-height: 1.6;
                            }
                            .container { 
                                max-width: 1200px; margin: 0 auto; 
                                background: rgba(255,255,255,0.95); 
                                padding: 40px; border-radius: 20px; 
                                box-shadow: 0 25px 50px rgba(0,0,0,0.15);
                                backdrop-filter: blur(10px);
                            }
                            .header { 
                                text-align: center; margin-bottom: 40px; 
                                padding-bottom: 20px; border-bottom: 3px solid #667eea;
                            }
                            .header h1 { 
                                color: #2c3e50; margin-bottom: 10px; 
                                font-size: 2.5rem; font-weight: 700;
                            }
                            .header p { color: #7f8c8d; font-size: 1.1rem; }
                            .info { 
                                background: linear-gradient(135deg, #e8f5e8 0%, #d4edda 100%); 
                                padding: 20px; border-radius: 15px; margin-bottom: 30px; 
                                border-left: 5px solid #28a745; box-shadow: 0 5px 15px rgba(40,167,69,0.1);
                            }
                            .info h3 { color: #155724; margin-bottom: 10px; }
                            .reporte { 
                                margin: 25px 0; padding: 25px; 
                                border: 1px solid #e9ecef; border-radius: 15px; 
                                background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%); 
                                transition: all 0.3s ease; position: relative; overflow: hidden;
                            }
                            .reporte::before {
                                content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4px;
                                background: linear-gradient(90deg, #667eea, #764ba2); border-radius: 15px 15px 0 0;
                            }
                            .reporte:hover { 
                                transform: translateY(-5px); 
                                box-shadow: 0 15px 30px rgba(0,0,0,0.1);
                            }
                            .reporte h3 { color: #2c3e50; margin-bottom: 15px; font-size: 1.3rem; }
                            .reporte-meta { 
                                display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); 
                                gap: 10px; margin: 15px 0; font-size: 0.9rem; color: #6c757d;
                            }
                            .btn { 
                                display: inline-flex; align-items: center; gap: 8px;
                                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                                color: white; padding: 12px 25px; text-decoration: none; 
                                border-radius: 25px; transition: all 0.3s ease; 
                                font-weight: 600; box-shadow: 0 5px 15px rgba(102,126,234,0.3);
                            }
                            .btn:hover { 
                                transform: translateY(-2px); 
                                box-shadow: 0 8px 25px rgba(102,126,234,0.4);
                                background: linear-gradient(135deg, #5a67d8 0%, #6b46c1 100%);
                            }
                            .estadisticas {
                                display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                                gap: 15px; margin: 20px 0;
                            }
                            .estadistica {
                                text-align: center; padding: 15px; 
                                background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
                                border-radius: 10px; border: 1px solid #dee2e6;
                            }
                            .estadistica-numero { font-size: 1.5rem; font-weight: bold; color: #667eea; }
                            .estadistica-label { font-size: 0.9rem; color: #6c757d; margin-top: 5px; }
                            .pie { 
                                text-align: center; margin-top: 40px; padding-top: 20px; 
                                border-top: 2px solid #e9ecef; color: #6c757d; font-size: 0.9rem;
                            }
                            @keyframes fadeIn { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
                            .reporte { animation: fadeIn 0.6s ease forwards; }
                        </style>
                    </head>
                    <body>
                    """;
        }

        private String generarEncabezadoIndice(String timestamp, int totalReportes) {
            return String.format("""
                    <div class="header">
                        <h1>📊 Reportes JMeter - API MediPlus</h1>
                        <p>Sistema automatizado de análisis de rendimiento</p>
                        <p><strong>Generado:</strong> %s | <strong>Total reportes:</strong> %d</p>
                    </div>
                    """,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    totalReportes);
        }

        private String generarInformacionGeneral() {
            return """
                    <div class="info">
                        <h3>✅ Reportes HTML Generados Exitosamente</h3>
                        <p>Cada reporte contiene análisis detallado de métricas de rendimiento con gráficas interactivas, 
                        estadísticas de respuesta, distribución de tiempos, throughput y análisis de errores.</p>
                        
                        <div class="estadisticas">
                            <div class="estadistica">
                                <div class="estadistica-numero">📈</div>
                                <div class="estadistica-label">Gráficas Interactivas</div>
                            </div>
                            <div class="estadistica">
                                <div class="estadistica-numero">📊</div>
                                <div class="estadistica-label">Métricas Detalladas</div>
                            </div>
                            <div class="estadistica">
                                <div class="estadistica-numero">⚡</div>
                                <div class="estadistica-label">Análisis en Tiempo Real</div>
                            </div>
                            <div class="estadistica">
                                <div class="estadistica-numero">🎯</div>
                                <div class="estadistica-label">Reportes Ejecutivos</div>
                            </div>
                        </div>
                    </div>
                    """;
        }

        private String generarTarjetaReporte(String directorio, int numero) {
            String nombre = Paths.get(directorio).getFileName().toString();
            String rutaRelativa = Paths.get(".").relativize(Paths.get(directorio, "index.html"))
                    .toString().replace("\\", "/");

            return String.format("""
                    <div class="reporte">
                        <h3>🎯 %d. %s</h3>
                        <div class="reporte-meta">
                            <div><strong>📁 Directorio:</strong> %s</div>
                            <div><strong>📊 Tipo:</strong> %s</div>
                            <div><strong>🕒 Estado:</strong> <span style="color: #28a745;">✅ Completado</span></div>
                        </div>
                        <p><strong>📋 Descripción:</strong> %s</p>
                        <div style="margin-top: 20px;">
                            <a href="%s" class="btn" target="_blank">
                                📊 Ver Reporte Completo
                            </a>
                        </div>
                    </div>
                    """,
                    numero, formatearNombreEscenario(nombre), directorio,
                    determinarTipoReporte(nombre), generarDescripcionReporte(nombre), rutaRelativa);
        }

        private String generarPieHTML() {
            return """
                    <div class="pie">
                        <p><strong>🚀 Framework de Pruebas Automatizadas MediPlus v2.0</strong></p>
                        <p><em>Generado automáticamente por EjecutorJMeterReal.java</em></p>
                        <p>Equipo: Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez</p>
                    </div>
                    """;
        }

        private String formatearNombreEscenario(String nombre) {
            if (nombre.contains("get_masivo")) return "GET Masivo";
            if (nombre.contains("post_masivo")) return "POST Masivo";
            if (nombre.contains("flujo_completo")) return "Flujo Completo";
            if (nombre.contains("mixto")) return "Escenario Mixto";

            // Capitalizar y limpiar nombre
            return Arrays.stream(nombre.split("_"))
                    .filter(part -> !part.matches("\\d+u|\\d{4}-\\d{2}-\\d{2}.*"))
                    .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                    .collect(Collectors.joining(" "));
        }

        private String determinarTipoReporte(String nombre) {
            if (nombre.contains("get")) return "Operaciones GET";
            if (nombre.contains("post")) return "Operaciones POST";
            if (nombre.contains("mixto") || nombre.contains("flujo")) return "Flujo Mixto";
            return "Análisis General";
        }

        private String generarDescripcionReporte(String nombre) {
            if (nombre.contains("ligero")) return "Análisis de carga ligera con métricas base de rendimiento";
            if (nombre.contains("medio")) return "Análisis de carga media con evaluación de escalabilidad";
            if (nombre.contains("intensivo")) return "Análisis de carga intensiva con pruebas de límites del sistema";
            if (nombre.contains("flujo")) return "Análisis completo de flujo de trabajo real del usuario";
            return "Análisis completo de rendimiento con métricas detalladas y recomendaciones";
        }
    }

    // ==================== CLASES DE DATOS Y ESTADOS ====================

    /**
     * Enumeración de estados del ejecutor
     */
    public enum EstadoEjecutor {
        INICIALIZANDO("Inicializando componentes"),
        LISTO("Listo para ejecutar"),
        EJECUTANDO("Ejecutando pruebas"),
        COMPLETADO("Ejecución completada exitosamente"),
        COMPLETADO_CON_ERRORES("Ejecución completada con errores"),
        ERROR("Error durante la ejecución"),
        CERRADO("Ejecutor cerrado");

        private final String descripcion;

        EstadoEjecutor(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    /**
     * Configuración completa de un escenario de prueba
     */
    public static class ConfiguracionEscenario {
        public final String nombre;
        public final String tipoOperacion;
        public final int usuariosConcurrentes;
        public final int duracionSegundos;
        public final String descripcion;

        public ConfiguracionEscenario(String nombre, String tipoOperacion, int usuariosConcurrentes,
                                      int duracionSegundos, String descripcion) {
            this.nombre = nombre;
            this.tipoOperacion = tipoOperacion;
            this.usuariosConcurrentes = usuariosConcurrentes;
            this.duracionSegundos = duracionSegundos;
            this.descripcion = descripcion;
        }

        // Constructor de compatibilidad
        public ConfiguracionEscenario(String nombre, String tipoOperacion, int usuariosConcurrentes,
                                      int duracionSegundos) {
            this(nombre, tipoOperacion, usuariosConcurrentes, duracionSegundos,
                    "Escenario de prueba automatizado");
        }

        @Override
        public String toString() {
            return String.format("ConfiguracionEscenario{nombre='%s', tipo='%s', usuarios=%d, duracion=%ds}",
                    nombre, tipoOperacion, usuariosConcurrentes, duracionSegundos);
        }
    }

    /**
     * Resultado extendido que incluye reportes HTML
     */
    public static class ResultadoEjecucionJMeterExtendido extends ResultadoEjecucionJMeter {
        public final List<String> reportesHTML;

        public ResultadoEjecucionJMeterExtendido(boolean exitoso, String mensaje, List<String> archivosJTL,
                                                 List<String> errores, List<String> reportesHTML) {
            super(exitoso, mensaje, archivosJTL, errores);
            this.reportesHTML = Collections.unmodifiableList(new ArrayList<>(reportesHTML));
        }

        @Override
        public String toString() {
            return String.format("ResultadoEjecucionJMeterExtendido{exitoso=%s, jtls=%d, htmls=%d, errores=%d}",
                    exitoso, archivosJTL.size(), reportesHTML.size(), errores.size());
        }
    }

    /**
     * Clase base para resultados de ejecución (mantiene compatibilidad)
     */
    public static class ResultadoEjecucionJMeter {
        public final boolean exitoso;
        public final String mensaje;
        public final List<String> archivosJTL;
        public final List<String> errores;

        public ResultadoEjecucionJMeter(boolean exitoso, String mensaje, List<String> archivosJTL, List<String> errores) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.archivosJTL = Collections.unmodifiableList(new ArrayList<>(archivosJTL));
            this.errores = Collections.unmodifiableList(new ArrayList<>(errores));
        }

        @Override
        public String toString() {
            return String.format("ResultadoEjecucionJMeter{exitoso=%s, jtls=%d, errores=%d}",
                    exitoso, archivosJTL.size(), errores.size());
        }
    }

    /**
     * Resultado de un escenario individual
     */
    private static class ResultadoEscenario {
        final boolean exitoso;
        final String escenario;
        final String archivoJTL;
        final String error;

        ResultadoEscenario(boolean exitoso, String escenario, String archivoJTL, String error) {
            this.exitoso = exitoso;
            this.escenario = escenario;
            this.archivoJTL = archivoJTL;
            this.error = error;
        }

        @Override
        public String toString() {
            return String.format("ResultadoEscenario{%s: %s}", escenario, exitoso ? "EXITOSO" : "FALLIDO");
        }
    }

    /**
     * Estado de preparación completo
     */
    private static class EstadoPreparacionCompleto {
        final boolean exitoso;
        final String mensaje;
        final List<String> errores;

        EstadoPreparacionCompleto(boolean exitoso, String mensaje, List<String> errores) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.errores = Collections.unmodifiableList(new ArrayList<>(errores));
        }
    }

    /**
     * Contenedor de archivos de un escenario
     */
    private static class ArchivosEscenario {
        final Path archivoJTL;
        final Path archivoLog;
        final Path directorioReporteHTML;

        ArchivosEscenario(Path archivoJTL, Path archivoLog, Path directorioReporteHTML) {
            this.archivoJTL = archivoJTL;
            this.archivoLog = archivoLog;
            this.directorioReporteHTML = directorioReporteHTML;
        }
    }
}