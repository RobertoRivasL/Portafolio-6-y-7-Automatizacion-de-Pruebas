package com.mediplus.pruebas.analisis.jmeter;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Ejecutor real de JMeter para pruebas de rendimiento automatizadas
 * Versión optimizada anti-freeze
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class EjecutorJMeterReal {

    private static final Logger LOGGER = Logger.getLogger(EjecutorJMeterReal.class.getName());
    private static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    // Configuración por defecto
    private static final String JMETER_HOME_ENV = "JMETER_HOME";
    private static final String[] POSIBLES_RUTAS_JMETER = {
            "/opt/jmeter/bin/jmeter",
            "/usr/local/jmeter/bin/jmeter",
            "C:\\apache-jmeter\\bin\\jmeter.bat",
            "C:\\jmeter\\bin\\jmeter.bat",
            "jmeter"  // Si está en PATH
    };

    private final ExecutorService executorService;
    private final String rutaJMeter;
    private final Path directorioResultados;
    private final String timestampEjecucion;

    public EjecutorJMeterReal() throws IOException {
        this.executorService = Executors.newFixedThreadPool(2);
        this.rutaJMeter = detectarRutaJMeter();
        this.directorioResultados = Paths.get("jmeter-results");
        this.timestampEjecucion = LocalDateTime.now().format(FORMATO_TIMESTAMP);

        // Crear directorio de resultados
        Files.createDirectories(directorioResultados);

        if (rutaJMeter == null) {
            throw new IOException("JMeter no encontrado. Instalar JMeter o configurar JMETER_HOME");
        }

        LOGGER.info("✅ JMeter detectado en: " + rutaJMeter);
    }

    /**
     * Verifica si JMeter está disponible en el sistema
     */
    public static boolean verificarJMeterDisponible() {
        return detectarRutaJMeter() != null;
    }

    /**
     * Ejecuta todas las pruebas de rendimiento de forma completa
     */
    public CompletableFuture<ResultadoEjecucionJMeter> ejecutarPruebasCompletas() {
        LOGGER.info("🚀 Iniciando ejecución completa de pruebas JMeter...");

        return CompletableFuture
                .supplyAsync(this::prepararEjecucion)
                .thenCompose(this::ejecutarEscenariosSecuencial)
                .thenApply(this::procesarResultadosFinales)
                .whenComplete(this::limpiarRecursos);
    }

    private EstadoPreparacion prepararEjecucion() {
        LOGGER.info("🔧 Preparando entorno de ejecución JMeter...");
        try {
            if (!verificarJMeterFuncional()) {
                return new EstadoPreparacion(false, "JMeter no funciona correctamente");
            }

            limpiarResultadosAnteriores();
            LOGGER.info("✅ Entorno JMeter preparado");
            return new EstadoPreparacion(true, "Preparación exitosa");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error preparando entorno JMeter", e);
            return new EstadoPreparacion(false, "Error: " + e.getMessage());
        }
    }

    private CompletableFuture<List<ResultadoEscenario>> ejecutarEscenariosSecuencial(EstadoPreparacion estado) {
        if (!estado.exitoso) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // Configurar escenarios básicos
        List<ConfiguracionEscenario> escenarios = Arrays.asList(
                new ConfiguracionEscenario("GET Masivo", "GET", 10, 60),
                new ConfiguracionEscenario("POST Masivo", "POST", 10, 60),
                new ConfiguracionEscenario("GET+POST Combinado", "MIXTO", 10, 60)
        );

        List<ResultadoEscenario> resultados = new ArrayList<>();

        for (ConfiguracionEscenario config : escenarios) {
            try {
                ResultadoEscenario resultado = ejecutarEscenarioSincrono(config);
                resultados.add(resultado);

                // Pausa entre escenarios
                Thread.sleep(2000);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error en escenario: " + config.nombre, e);
                resultados.add(new ResultadoEscenario(false, config.nombre, null, e.getMessage()));
            }
        }

        return CompletableFuture.completedFuture(resultados);
    }

    private ResultadoEscenario ejecutarEscenarioSincrono(ConfiguracionEscenario config) {
        try {
            LOGGER.info("🎯 Ejecutando escenario: " + config.nombre);

            // Generar script JMX simple
            Path archivoJMX = generarScriptJMXSimple(config);

            // Configurar archivos de salida
            Path archivoJTL = directorioResultados.resolve(
                    String.format("%s_%du_%s.jtl",
                            config.nombre.toLowerCase().replace(" ", "_"),
                            config.usuariosConcurrentes,
                            timestampEjecucion)
            );

            Path archivoLog = directorioResultados.resolve(
                    String.format("%s_%du_%s.log",
                            config.nombre.toLowerCase().replace(" ", "_"),
                            config.usuariosConcurrentes,
                            timestampEjecucion)
            );

            // Ejecutar JMeter
            boolean exitoso = ejecutarComandoJMeter(archivoJMX, archivoJTL, archivoLog, config);

            if (exitoso && Files.exists(archivoJTL) && Files.size(archivoJTL) > 0) {
                LOGGER.info("✅ Escenario completado: " + config.nombre);
                return new ResultadoEscenario(true, config.nombre, archivoJTL.toString(), null);
            } else {
                String error = "Ejecución falló o archivo JTL vacío";
                LOGGER.warning("⚠️ " + error + ": " + config.nombre);
                return new ResultadoEscenario(false, config.nombre, null, error);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error ejecutando escenario: " + config.nombre, e);
            return new ResultadoEscenario(false, config.nombre, null, e.getMessage());
        }
    }

    private Path generarScriptJMXSimple(ConfiguracionEscenario config) throws IOException {
        String nombreArchivo = String.format("%s_%du_%s.jmx",
                config.nombre.toLowerCase().replace(" ", "_"),
                config.usuariosConcurrentes,
                timestampEjecucion);

        Path archivoJMX = directorioResultados.resolve(nombreArchivo);

        String contenidoJMX = """
            <?xml version="1.0" encoding="UTF-8"?>
            <jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
              <hashTree>
                <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Test Plan">
                  <elementProp name="TestPlan.arguments" elementType="Arguments">
                    <collectionProp name="Arguments.arguments"/>
                  </elementProp>
                </TestPlan>
                <hashTree>
                  <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Thread Group">
                    <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
                    <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
                      <boolProp name="LoopController.continue_forever">false</boolProp>
                      <intProp name="LoopController.loops">5</intProp>
                    </elementProp>
                    <stringProp name="ThreadGroup.num_threads">""" + config.usuariosConcurrentes + """
            </stringProp>
                    <stringProp name="ThreadGroup.ramp_time">10</stringProp>
                  </ThreadGroup>
                  <hashTree>
                    <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="HTTP Request">
                      <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
                        <collectionProp name="Arguments.arguments"/>
                      </elementProp>
                      <stringProp name="HTTPSampler.domain">dummyjson.com</stringProp>
                      <stringProp name="HTTPSampler.protocol">https</stringProp>
                      <stringProp name="HTTPSampler.path">/products</stringProp>
                      <stringProp name="HTTPSampler.method">GET</stringProp>
                      <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                    </HTTPSamplerProxy>
                    <hashTree/>
                  </hashTree>
                </hashTree>
              </hashTree>
            </jmeterTestPlan>
            """;

        Files.writeString(archivoJMX, contenidoJMX);
        LOGGER.info("📝 Script JMX generado: " + archivoJMX.getFileName());
        return archivoJMX;
    }

    private boolean ejecutarComandoJMeter(Path archivoJMX, Path archivoJTL, Path archivoLog, ConfiguracionEscenario config) {
        try {
            // Construir comando optimizado
            List<String> comando = construirComandoJMeter(archivoJMX, archivoJTL, archivoLog);

            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.redirectErrorStream(true);
            pb.directory(new File("."));

            LOGGER.info("⚡ Ejecutando: " + config.nombre + " con " + config.usuariosConcurrentes + " usuarios");

            Process proceso = pb.start();

            // Lectura asíncrona anti-freeze
            CompletableFuture<Void> lecturaOutput = CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                    String linea;
                    while ((linea = reader.readLine()) != null) {
                        if (linea.contains("summary") || linea.contains("Tidying up") ||
                                linea.contains("Creating summariser")) {
                            LOGGER.info("📊 " + linea);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.FINE, "Error leyendo output JMeter", e);
                }
            });

            // Timeout generoso
            boolean terminado = proceso.waitFor(config.duracionSegundos + 120, TimeUnit.SECONDS);

            if (!terminado) {
                LOGGER.warning("⏱️ Timeout alcanzado para: " + config.nombre);
                proceso.destroyForcibly();
                proceso.waitFor(10, TimeUnit.SECONDS);
                return false;
            }

            // Esperar lectura de output
            try {
                lecturaOutput.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Timeout en lectura de output", e);
            }

            int codigoSalida = proceso.exitValue();
            boolean exitoso = codigoSalida == 0;

            if (exitoso) {
                LOGGER.info("✅ JMeter completado exitosamente: " + config.nombre);
            } else {
                LOGGER.warning("⚠️ JMeter completado con código: " + codigoSalida);
            }

            return exitoso;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error ejecutando JMeter", e);
            return false;
        }
    }

    private List<String> construirComandoJMeter(Path archivoJMX, Path archivoJTL, Path archivoLog) {
        List<String> comando = new ArrayList<>();

        // Detectar Windows .bat
        if (rutaJMeter.endsWith(".bat")) {
            comando.addAll(Arrays.asList("cmd.exe", "/c"));
        }

        comando.add(rutaJMeter);
        comando.addAll(Arrays.asList(
                "-n",  // modo no-GUI
                "-t", archivoJMX.toAbsolutePath().toString(),
                "-l", archivoJTL.toAbsolutePath().toString(),
                "-j", archivoLog.toAbsolutePath().toString(),
                "-Jjmeter.save.saveservice.output_format=csv"
        ));

        return comando;
    }

    private ResultadoEjecucionJMeter procesarResultadosFinales(List<ResultadoEscenario> resultados) {
        List<String> archivosJTL = resultados.stream()
                .filter(r -> r.exitoso && r.archivoJTL != null)
                .map(r -> r.archivoJTL)
                .toList();

        List<String> errores = resultados.stream()
                .filter(r -> !r.exitoso)
                .map(r -> r.escenario + ": " + r.error)
                .toList();

        boolean exitosoGeneral = !archivosJTL.isEmpty();
        String mensaje = String.format("Ejecución JMeter completada: %d/%d escenarios exitosos",
                archivosJTL.size(), resultados.size());

        LOGGER.info(exitosoGeneral ? "✅ " + mensaje : "⚠️ " + mensaje);
        return new ResultadoEjecucionJMeter(exitosoGeneral, mensaje, archivosJTL, errores);
    }

    // Métodos auxiliares
    private static String detectarRutaJMeter() {
        // Verificar variable de entorno
        String jmeterHome = System.getenv(JMETER_HOME_ENV);
        if (jmeterHome != null) {
            Path jmeterBin = Paths.get(jmeterHome, "bin", "jmeter");
            if (Files.exists(jmeterBin) || Files.exists(Paths.get(jmeterHome, "bin", "jmeter.bat"))) {
                return jmeterBin.toString();
            }
        }

        // Probar rutas conocidas
        for (String ruta : POSIBLES_RUTAS_JMETER) {
            if (verificarRutaJMeter(ruta)) {
                return ruta;
            }
        }

        return null;
    }

    private static boolean verificarRutaJMeter(String ruta) {
        try {
            ProcessBuilder pb = new ProcessBuilder(ruta, "--version");
            pb.redirectErrorStream(true);
            Process proceso = pb.start();
            return proceso.waitFor(5, TimeUnit.SECONDS) && proceso.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verificarJMeterFuncional() {
        try {
            ProcessBuilder pb = new ProcessBuilder(rutaJMeter, "--version");
            pb.redirectErrorStream(true);
            Process proceso = pb.start();
            boolean completado = proceso.waitFor(10, TimeUnit.SECONDS);
            return completado && proceso.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void limpiarResultadosAnteriores() throws IOException {
        if (Files.exists(directorioResultados)) {
            Files.walk(directorioResultados)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jtl") || p.toString().endsWith(".log"))
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
                        } catch (IOException e) {
                            // Ignorar errores de limpieza
                        }
                    });
        }
    }

    private void limpiarRecursos(ResultadoEjecucionJMeter resultado, Throwable throwable) {
        if (throwable != null) {
            LOGGER.log(Level.SEVERE, "Error durante ejecución JMeter", throwable);
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        LOGGER.info("🧹 Recursos JMeter limpiados");
    }

    // Clases auxiliares
    public static class ConfiguracionEscenario {
        public final String nombre;
        public final String tipoOperacion;
        public final int usuariosConcurrentes;
        public final int duracionSegundos;

        public ConfiguracionEscenario(String nombre, String tipoOperacion, int usuariosConcurrentes, int duracionSegundos) {
            this.nombre = nombre;
            this.tipoOperacion = tipoOperacion;
            this.usuariosConcurrentes = usuariosConcurrentes;
            this.duracionSegundos = duracionSegundos;
        }
    }

    public static class ResultadoEjecucionJMeter {
        public final boolean exitoso;
        public final String mensaje;
        public final List<String> archivosJTL;
        public final List<String> errores;

        public ResultadoEjecucionJMeter(boolean exitoso, String mensaje, List<String> archivosJTL, List<String> errores) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.archivosJTL = Collections.unmodifiableList(archivosJTL);
            this.errores = Collections.unmodifiableList(errores);
        }
    }

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
    }

    private static class EstadoPreparacion {
        final boolean exitoso;
        final String mensaje;

        EstadoPreparacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }
    }
}