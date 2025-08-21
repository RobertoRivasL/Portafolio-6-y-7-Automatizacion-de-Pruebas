package com.mediplus.pruebas.analisis.recolector;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * Recolector de resultados de tests para captura automática
 * Permite que main acceda a resultados sin depender directamente de JUnit
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class RecolectorResultadosTest {

    private static final Logger LOGGER = Logger.getLogger(RecolectorResultadosTest.class.getName());

    // Lista thread-safe para capturar resultados
    private static final List<ResultadoTestCapturado> resultadosCapturados =
            new CopyOnWriteArrayList<>();

    private static boolean capturaActiva = false;

    /**
     * Inicia la captura de resultados de tests
     */
    public static void iniciarCaptura() {
        capturaActiva = true;
        resultadosCapturados.clear();
        LOGGER.info("📋 Captura de resultados iniciada");
    }

    /**
     * Detiene la captura de resultados
     */
    public static void detenerCaptura() {
        capturaActiva = false;
        LOGGER.info("⏹️ Captura de resultados detenida. Total capturados: " + resultadosCapturados.size());
    }

    /**
     * Registra un resultado de test
     */
    public static void registrarResultado(String nombre, String metodo, String endpoint,
                                          int statusCode, boolean exitoso, long tiempoMs, String detalles) {
        if (!capturaActiva) return;

        ResultadoTestCapturado resultado = new ResultadoTestCapturado(
                nombre, metodo, endpoint, statusCode, exitoso, tiempoMs, detalles, LocalDateTime.now());

        resultadosCapturados.add(resultado);
        LOGGER.fine("✅ Test registrado: " + nombre + " - " + (exitoso ? "PASS" : "FAIL"));
    }

    /**
     * Obtiene todos los resultados capturados
     */
    public static List<ResultadoTestCapturado> obtenerResultadosCapturados() {
        return new ArrayList<>(resultadosCapturados);
    }

    /**
     * Verifica si hay resultados disponibles
     */
    public static boolean hayResultadosDisponibles() {
        return !resultadosCapturados.isEmpty();
    }

    /**
     * Limpia todos los resultados capturados
     */
    public static void limpiarResultados() {
        resultadosCapturados.clear();
        LOGGER.info("🧹 Resultados capturados limpiados");
    }

    /**
     * Clase que representa un resultado de test capturado
     */
    public static class ResultadoTestCapturado {
        public final String nombre;
        public final String metodo;
        public final String endpoint;
        public final int statusCode;
        public final boolean exitoso;
        public final long tiempoRespuestaMs;
        public final String detalles;
        public final LocalDateTime timestamp;

        public ResultadoTestCapturado(String nombre, String metodo, String endpoint,
                                      int statusCode, boolean exitoso, long tiempoRespuestaMs,
                                      String detalles, LocalDateTime timestamp) {
            this.nombre = nombre;
            this.metodo = metodo;
            this.endpoint = endpoint;
            this.statusCode = statusCode;
            this.exitoso = exitoso;
            this.tiempoRespuestaMs = tiempoRespuestaMs;
            this.detalles = detalles;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("%s [%s %s] - %s (%dms) at %s",
                    nombre, metodo, endpoint, exitoso ? "PASS" : "FAIL",
                    tiempoRespuestaMs, timestamp);
        }
    }
}