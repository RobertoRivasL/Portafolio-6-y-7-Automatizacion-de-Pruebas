package com.mediplus.pruebas.analisis.configuracion;



import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;



/**
 * Configuración centralizada de la aplicación
 * Implementa el patrón Singleton para configuración global
 */
public class ConfiguracionAplicacion {

    private static ConfiguracionAplicacion instancia;
    private final Properties propiedades;

    // Configuraciones por defecto
    private static final String DIRECTORIO_RESULTADOS_DEFAULT = "resultados";
    private static final String DIRECTORIO_REPORTES_DEFAULT = "reportes";
    private static final String FORMATO_FECHA_DEFAULT = "dd/MM/yyyy HH:mm";
    private static final int TIMEOUT_LECTURA_DEFAULT = 30;

    private ConfiguracionAplicacion() {
        this.propiedades = new Properties();
        cargarConfiguracion();
    }

    public static synchronized ConfiguracionAplicacion obtenerInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracionAplicacion();
        }
        return instancia;
    }

    private void cargarConfiguracion() {
        // Intentar cargar desde archivo de propiedades
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("aplicacion.properties")) {
            if (input != null) {
                propiedades.load(input);
            }
        } catch (IOException e) {
            System.err.println("Advertencia: No se pudo cargar aplicacion.properties, usando valores por defecto");
        }

        // Establecer valores por defecto si no están definidos
        establecerValoresPorDefecto();
    }

    private void establecerValoresPorDefecto() {
        propiedades.putIfAbsent("directorio.resultados", DIRECTORIO_RESULTADOS_DEFAULT);
        propiedades.putIfAbsent("directorio.reportes", DIRECTORIO_REPORTES_DEFAULT);
        propiedades.putIfAbsent("formato.fecha", FORMATO_FECHA_DEFAULT);
        propiedades.putIfAbsent("timeout.lectura.segundos", String.valueOf(TIMEOUT_LECTURA_DEFAULT));
        propiedades.putIfAbsent("nivel.log", "INFO");
        propiedades.putIfAbsent("generar.graficas.ascii", "true");
        propiedades.putIfAbsent("umbral.error.critico", "10.0");
        propiedades.putIfAbsent("umbral.tiempo.critico", "2000.0");
    }

    // Getters para configuraciones específicas
    public Path obtenerDirectorioResultados() {
        return Paths.get(propiedades.getProperty("directorio.resultados"));
    }

    public Path obtenerDirectorioReportes() {
        return Paths.get(propiedades.getProperty("directorio.reportes"));
    }

    public String obtenerFormatoFecha() {
        return propiedades.getProperty("formato.fecha");
    }

    public int obtenerTimeoutLecturaSegundos() {
        return Integer.parseInt(propiedades.getProperty("timeout.lectura.segundos"));
    }

    public boolean debeGenerarGraficasASCII() {
        return Boolean.parseBoolean(propiedades.getProperty("generar.graficas.ascii"));
    }

    public double obtenerUmbralErrorCritico() {
        return Double.parseDouble(propiedades.getProperty("umbral.error.critico"));
    }

    public double obtenerUmbralTiempoCritico() {
        return Double.parseDouble(propiedades.getProperty("umbral.tiempo.critico"));
    }

    public String obtenerNivelLog() {
        return propiedades.getProperty("nivel.log");
    }

    /**
     * Permite override de configuración en tiempo de ejecución
     */
    public void establecerPropiedad(String clave, String valor) {
        propiedades.setProperty(clave, valor);
    }

    /**
     * Obtiene una propiedad específica
     */
    public String obtenerPropiedad(String clave, String valorPorDefecto) {
        return propiedades.getProperty(clave, valorPorDefecto);
    }

    /**
     * Valida que las configuraciones sean válidas
     */
    public void validarConfiguracion() throws IllegalStateException {
        // Validar que los umbrales sean positivos
        if (obtenerUmbralErrorCritico() < 0 || obtenerUmbralErrorCritico() > 100) {
            throw new IllegalStateException("El umbral de error crítico debe estar entre 0 y 100");
        }

        if (obtenerUmbralTiempoCritico() <= 0) {
            throw new IllegalStateException("El umbral de tiempo crítico debe ser positivo");
        }

        if (obtenerTimeoutLecturaSegundos() <= 0) {
            throw new IllegalStateException("El timeout de lectura debe ser positivo");
        }
    }
}