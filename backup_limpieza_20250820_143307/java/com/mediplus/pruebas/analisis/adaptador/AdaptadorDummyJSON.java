package com.mediplus.pruebas.analisis.adaptador;

import com.mediplus.pruebas.analisis.modelo.MetricaRendimiento;
import com.mediplus.pruebas.analisis.configuracion.ConfiguracionAplicacion;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Adaptador para integrar el framework existente con DummyJSON
 * Permite ejecutar pruebas reales contra la API externa
 * 
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class AdaptadorDummyJSON {

    private static final Logger LOGGER = Logger.getLogger(AdaptadorDummyJSON.class.getName());
    private static final String BASE_URL = "https://dummyjson.com";
    
    private final HttpClient clienteHTTP;
    private final ConfiguracionAplicacion configuracion;
    private final ExecutorService ejecutorPruebas;

    public AdaptadorDummyJSON() {
        this.configuracion = ConfiguracionAplicacion.obtenerInstancia();
        this.clienteHTTP = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(configuracion.obtenerTimeoutLecturaSegundos()))
                .build();
        this.ejecutorPruebas = Executors.newFixedThreadPool(10);
    }

    /**
     * Ejecuta prueba de rendimiento GET masivo contra DummyJSON
     */
    public MetricaRendimiento ejecutarPruebaGETMasivo(int usuariosConcurrentes, int duracionSegundos) {
        return ejecutarPruebaConcurrente("GET Masivo", usuariosConcurrentes, duracionSegundos, 
            () -> ejecutarRequestGET("/products"));
    }

    /**
     * Ejecuta prueba de rendimiento POST masivo (autenticación)
     */
    public MetricaRendimiento ejecutarPruebaPOSTMasivo(int usuariosConcurrentes, int duracionSegundos) {
        return ejecutarPruebaConcurrente("POST Masivo", usuariosConcurrentes, duracionSegundos, 
            () -> ejecutarRequestPOST("/auth/login", crearCredencialesJSON()));
    }

    /**
     * Ejecuta prueba mixta GET+POST
     */
    public MetricaRendimiento ejecutarPruebaMixta(int usuariosConcurrentes, int duracionSegundos) {
        return ejecutarPruebaConcurrente("GET+POST Combinado", usuariosConcurrentes, duracionSegundos, 
            () -> {
                if (Math.random() > 0.5) {
                    return ejecutarRequestGET("/products");
                } else {
                    return ejecutarRequestPOST("/auth/login", crearCredencialesJSON());
                }
            });
    }

    /**
     * Método principal para ejecutar pruebas concurrentes
     */
    private MetricaRendimiento ejecutarPruebaConcurrente(String nombreEscenario, 
                                                       int usuariosConcurrentes, 
                                                       int duracionSegundos,
                                                       RequestExecutor executor) {
        
        LOGGER.info(String.format("🚀 Iniciando prueba: %s con %d usuarios por %d segundos", 
                                 nombreEscenario, usuariosConcurrentes, duracionSegundos));

        List<CompletableFuture<ResultadoRequest>> futuros = new ArrayList<>();
        List<ResultadoRequest> resultados = new ArrayList<>();
        
        long tiempoInicio = System.currentTimeMillis();
        long tiempoFin = tiempoInicio + (duracionSegundos * 1000L);

        // Lanzar usuarios concurrentes
        for (int i = 0; i < usuariosConcurrentes; i++) {
            CompletableFuture<ResultadoRequest> futuro = CompletableFuture.supplyAsync(() -> {
                List<ResultadoRequest> resultadosUsuario = new ArrayList<>();
                
                while (System.currentTimeMillis() < tiempoFin) {
                    try {
                        ResultadoRequest resultado = executor.ejecutar();
                        resultadosUsuario.add(resultado);
                        
                        // Pequeña pausa para simular comportamiento real
                        Thread.sleep(100 + (long)(Math.random() * 500));
                        
                    } catch (Exception e) {
                        LOGGER.warning("Error en request: " + e.getMessage());
                        resultadosUsuario.add(new ResultadoRequest(0, false, e.getMessage()));
                    }
                }
                
                return resultadosUsuario.isEmpty() ? 
                    new ResultadoRequest(0, false, "Sin requests") : 
                    resultadosUsuario.get(0); // Simplificado para el ejemplo
                    
            }, ejecutorPruebas);
            
            futuros.add(futuro);
        }

        // Recopilar resultados
        for (CompletableFuture<ResultadoRequest> futuro : futuros) {
            try {
                ResultadoRequest resultado = futuro.get();
                if (resultado != null) {
                    resultados.add(resultado);
                }
            } catch (Exception e) {
                LOGGER.warning("Error obteniendo resultado: " + e.getMessage());
            }
        }

        // Calcular métricas
        return calcularMetricas(nombreEscenario, usuariosConcurrentes, duracionSegundos, resultados);
    }

    /**
     * Ejecuta un request GET
     */
    private ResultadoRequest ejecutarRequestGET(String endpoint) throws IOException, InterruptedException {
        long inicio = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Accept", "application/json")
                .header("User-Agent", "MediPlus-Testing/1.0")
                .GET()
                .build();

        HttpResponse<String> response = clienteHTTP.send(request, HttpResponse.BodyHandlers.ofString());
        
        long tiempoTranscurrido = System.currentTimeMillis() - inicio;
        boolean exitoso = response.statusCode() >= 200 && response.statusCode() < 300;
        
        return new ResultadoRequest(tiempoTranscurrido, exitoso, 
                                  exitoso ? "OK" : "HTTP " + response.statusCode());
    }

    /**
     * Ejecuta un request POST
     */
    private ResultadoRequest ejecutarRequestPOST(String endpoint, String body) throws IOException, InterruptedException {
        long inicio = System.currentTimeMillis();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "MediPlus-Testing/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = clienteHTTP.send(request, HttpResponse.BodyHandlers.ofString());
        
        long tiempoTranscurrido = System.currentTimeMillis() - inicio;
        boolean exitoso = response.statusCode() >= 200 && response.statusCode() < 300;
        
        return new ResultadoRequest(tiempoTranscurrido, exitoso, 
                                  exitoso ? "OK" : "HTTP " + response.statusCode());
    }

    /**
     * Crea JSON de credenciales para autenticación
     */
    private String crearCredencialesJSON() {
        // Rotar entre diferentes usuarios para simular carga real
        String[] usuarios = {"emilys", "michaelw", "sophiab", "jamesdoe", "emmaj"};
        String[] passwords = {"emilyspass", "michaelwpass", "sophiabpass", "jamesdoepass", "emmajpass"};
        
        int indice = (int)(Math.random() * usuarios.length);
        
        return String.format("""
            {
                "username": "%s",
                "password": "%s"
            }
            """, usuarios[indice], passwords[indice]);
    }

    /**
     * Calcula métricas a partir de los resultados
     */
    private MetricaRendimiento calcularMetricas(String nombreEscenario, 
                                               int usuariosConcurrentes, 
                                               int duracionSegundos,
                                               List<ResultadoRequest> resultados) {
        
        if (resultados.isEmpty()) {
            LOGGER.warning("No hay resultados para calcular métricas");
            return crearMetricaVacia(nombreEscenario, usuariosConcurrentes, duracionSegundos);
        }

        // Filtrar solo resultados exitosos para cálculos de tiempo
        List<Long> tiemposExitosos = resultados.stream()
                .filter(ResultadoRequest::isExitoso)
                .map(ResultadoRequest::getTiempoMs)
                .toList();

        if (tiemposExitosos.isEmpty()) {
            LOGGER.warning("No hay resultados exitosos");
            return crearMetricaConErrores(nombreEscenario, usuariosConcurrentes, duracionSegundos, resultados.size());
        }

        // Calcular estadísticas
        double tiempoPromedio = tiemposExitosos.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long tiempoMinimo = tiemposExitosos.stream().mapToLong(Long::longValue).min().orElse(0L);
        long tiempoMaximo = tiemposExitosos.stream().mapToLong(Long::longValue).max().orElse(0L);
        
        // Calcular percentiles
        List<Long> tiemposOrdenados = tiemposExitosos.stream().sorted().toList();
        double percentil90 = calcularPercentil(tiemposOrdenados, 90);
        double percentil95 = calcularPercentil(tiemposOrdenados, 95);
        
        // Calcular tasa de error y throughput
        long totalRequests = resultados.size();
        long requestsExitosos = tiemposExitosos.size();
        double tasaError = ((double)(totalRequests - requestsExitosos) / totalRequests) * 100.0;
        double throughput = (double)requestsExitosos / duracionSegundos;

        LOGGER.info(String.format("📊 Métricas calculadas: %d requests, %.1f%% errores, %.1f req/s", 
                                 totalRequests, tasaError, throughput));

        return new MetricaRendimiento.Builder()
                .nombreEscenario(nombreEscenario)
                .usuariosConcurrentes(usuariosConcurrentes)
                .tiempoPromedioMs(tiempoPromedio)
                .percentil90Ms(percentil90)
                .percentil95Ms(percentil95)
                .throughputReqSeg(throughput)
                .tasaErrorPorcentaje(tasaError)
                .tiempoMinimoMs(tiempoMinimo)
                .tiempoMaximoMs(tiempoMaximo)
                .duracionPruebaSegundos(duracionSegundos)
                .fechaEjecucion(LocalDateTime.now())
                .build();
    }

    private double calcularPercentil(List<Long> valores, int percentil) {
        if (valores.isEmpty()) return 0.0;
        int indice = (int) Math.ceil((percentil / 100.0) * valores.size()) - 1;
        indice = Math.max(0, Math.min(indice, valores.size() - 1));
        return valores.get(indice);
    }

    private MetricaRendimiento crearMetricaVacia(String nombreEscenario, int usuarios, int duracion) {
        return new MetricaRendimiento.Builder()
                .nombreEscenario(nombreEscenario)
                .usuariosConcurrentes(usuarios)
                .tiempoPromedioMs(0.0)
                .percentil90Ms(0.0)
                .percentil95Ms(0.0)
                .throughputReqSeg(0.0)
                .tasaErrorPorcentaje(100.0)
                .tiempoMinimoMs(0)
                .tiempoMaximoMs(0)
                .duracionPruebaSegundos(duracion)
                .fechaEjecucion(LocalDateTime.now())
                .build();
    }

    private MetricaRendimiento crearMetricaConErrores(String nombreEscenario, int usuarios, int duracion, int totalRequests) {
        return new MetricaRendimiento.Builder()
                .nombreEscenario(nombreEscenario)
                .usuariosConcurrentes(usuarios)
                .tiempoPromedioMs(5000.0) // Tiempo alto por errores
                .percentil90Ms(6000.0)
                .percentil95Ms(7000.0)
                .throughputReqSeg((double)totalRequests / duracion)
                .tasaErrorPorcentaje(100.0)
                .tiempoMinimoMs(0)
                .tiempoMaximoMs(10000)
                .duracionPruebaSegundos(duracion)
                .fechaEjecucion(LocalDateTime.now())
                .build();
    }

    public void cerrar() {
        ejecutorPruebas.shutdown();
    }

    // Interfaces y clases auxiliares
    @FunctionalInterface
    private interface RequestExecutor {
        ResultadoRequest ejecutar() throws Exception;
    }

    private static class ResultadoRequest {
        private final long tiempoMs;
        private final boolean exitoso;
        private final String mensaje;

        public ResultadoRequest(long tiempoMs, boolean exitoso, String mensaje) {
            this.tiempoMs = tiempoMs;
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }

        public long getTiempoMs() { return tiempoMs; }
        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
    }
}