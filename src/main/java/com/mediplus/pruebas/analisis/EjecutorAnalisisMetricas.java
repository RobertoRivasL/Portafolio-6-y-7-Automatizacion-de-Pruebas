package com.mediplus.pruebas.analisis;

import com.mediplus.pruebas.analisis.modelo.MetricaRendimiento;
import com.mediplus.pruebas.analisis.AnalizadorMetricas;
import com.mediplus.pruebas.analisis.configuracion.ConfiguracionAplicacion;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Clase principal para ejecutar el análisis de métricas de rendimiento
 * Punto de entrada de la aplicación de análisis
 * Version 2.0 - Sin dependencias a clases obsoletas
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class EjecutorAnalisisMetricas {

    private static final Logger LOGGER = Logger.getLogger(EjecutorAnalisisMetricas.class.getName());

    public static void main(String[] args) {
        try {
            EjecutorAnalisisMetricas ejecutor = new EjecutorAnalisisMetricas();
            ejecutor.ejecutarAnalisisCompleto();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error durante la ejecución del análisis", e);
            System.exit(1);
        }
    }

    /**
     * Ejecuta el análisis completo de métricas
     */
    public void ejecutarAnalisisCompleto() throws Exception {
        LOGGER.info("🚀 Iniciando análisis de métricas de rendimiento API MediPlus");

        // Configuración de directorios
        ConfiguracionAplicacion configuracion = ConfiguracionAplicacion.obtenerInstancia();
        Path directorioResultados = configuracion.obtenerDirectorioResultados();
        Path directorioReportes = configuracion.obtenerDirectorioReportes();

        // Validar que existan los directorios
        validarDirectorios(directorioResultados);

        // Inicializar analizador de métricas (CORREGIDO - sin LectorArchivosJTL)
        AnalizadorMetricas analizadorMetricas = new AnalizadorMetricas();

        // Procesar archivos JTL
        LOGGER.info("📊 Procesando archivos de resultados JTL...");
        List<MetricaRendimiento> metricas = procesarArchivosJTL(directorioResultados, analizadorMetricas);

        if (metricas.isEmpty()) {
            LOGGER.warning("⚠️ No se encontraron métricas válidas para analizar");
            System.out.println("⚠️ No se encontraron archivos .jtl para procesar");
            System.out.println("💡 Tip: Ejecuta primero las pruebas JMeter o usa datos simulados");
            return;
        }

        LOGGER.info(String.format("✅ Se procesaron %d métricas exitosamente", metricas.size()));

        // Mostrar resumen en consola
        mostrarResumenEnConsola(metricas);

        // Generar análisis comparativo
        LOGGER.info("🔍 Generando análisis comparativo...");
        AnalizadorMetricas.ComparacionMetricas comparacion = analizadorMetricas.compararMetricas(metricas);

        // Generar reportes (CORREGIDO - usar AnalizadorMetricas)
        LOGGER.info("📋 Generando reportes completos...");
        analizadorMetricas.generarReporteCompleto(metricas, directorioReportes);

        // Mostrar recomendaciones principales
        mostrarRecomendacionesPrincipales(comparacion);

        LOGGER.info("🎉 Análisis completado exitosamente. Revisa el directorio 'reportes' para ver los resultados detallados.");
    }

    private void validarDirectorios(Path directorioResultados) {
        if (!directorioResultados.toFile().exists()) {
            LOGGER.warning("⚠️ El directorio de resultados no existe: " + directorioResultados);
            LOGGER.info("💡 Creando directorio de resultados y generando datos simulados...");

            try {
                Files.createDirectories(directorioResultados);
                generarDatosSimuladosParaDemostracion();
            } catch (Exception e) {
                throw new IllegalArgumentException("No se pudo crear directorio de resultados: " + directorioResultados);
            }
        }
    }

    /**
     * Genera datos simulados para demostración cuando no hay archivos JTL reales
     */
    private void generarDatosSimuladosParaDemostracion() {
        LOGGER.info("🎭 Generando métricas simuladas para demostración...");
        // Este método podría crear archivos JTL simulados si fuera necesario
        // Por ahora, el sistema manejará la ausencia de archivos automáticamente
    }

    private List<MetricaRendimiento> procesarArchivosJTL(Path directorioResultados, AnalizadorMetricas analizador) {
        List<MetricaRendimiento> metricas = new ArrayList<>();

        try {
            Files.list(directorioResultados)
                    .filter(p -> p.toString().endsWith(".jtl"))
                    .forEach(archivo -> {
                        try {
                            System.out.println("📊 Procesando: " + archivo.getFileName());
                            MetricaRendimiento metrica = analizador.procesarArchivoJTL(archivo);
                            if (metrica != null) {
                                metricas.add(metrica);
                            }
                        } catch (Exception e) {
                            System.err.println("⚠️ Error procesando " + archivo.getFileName() + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error listando archivos JTL", e);
        }

        return metricas;
    }

    private void mostrarResumenEnConsola(List<MetricaRendimiento> metricas) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 RESUMEN EJECUTIVO DE MÉTRICAS");
        System.out.println("=".repeat(80));

        System.out.printf("%-25s %-10s %-15s %-15s %-12s %-10s%n",
                "ESCENARIO", "USUARIOS", "TIEMPO PROM.", "THROUGHPUT", "ERROR %", "NIVEL");
        System.out.println("-".repeat(80));

        metricas.stream()
                .sorted(Comparator.comparing(MetricaRendimiento::getNombreEscenario)
                        .thenComparing(MetricaRendimiento::getUsuariosConcurrentes))
                .forEach(metrica -> System.out.printf("%-25s %-10d %-15s %-15s %-12s %-10s%n",
                        metrica.getNombreEscenario(),
                        metrica.getUsuariosConcurrentes(),
                        String.format("%.0f ms", metrica.getTiempoPromedioMs()),
                        String.format("%.1f req/s", metrica.getThroughputReqSeg()),
                        String.format("%.1f%%", metrica.getTasaErrorPorcentaje()),
                        metrica.evaluarNivelRendimiento().getDescripcion()));

        System.out.println("=".repeat(80));
    }

    private void mostrarRecomendacionesPrincipales(AnalizadorMetricas.ComparacionMetricas comparacion) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("💡 RECOMENDACIONES PRINCIPALES");
        System.out.println("=".repeat(80));

        List<String> recomendaciones = generarRecomendaciones(comparacion);

        for (int i = 0; i < recomendaciones.size(); i++) {
            System.out.println((i + 1) + ". " + recomendaciones.get(i));
        }

        System.out.println("\n" + "=".repeat(80));
    }

    private List<String> generarRecomendaciones(AnalizadorMetricas.ComparacionMetricas comparacion) {
        List<String> recomendaciones = new ArrayList<>();

        if (comparacion.getMetricas().isEmpty()) {
            recomendaciones.add("📊 Ejecutar pruebas de rendimiento JMeter para obtener métricas reales");
            recomendaciones.add("🔧 Configurar escenarios de prueba con diferentes cargas de usuarios");
            recomendaciones.add("📈 Establecer umbrales de rendimiento para monitoreo continuo");
            return recomendaciones;
        }

        // Analizar métricas reales
        long metricasCriticas = comparacion.getMetricas().stream()
                .filter(m -> m.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.MALO ||
                        m.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.INACEPTABLE)
                .count();

        double tiempoPromedio = comparacion.getMetricas().stream()
                .mapToDouble(MetricaRendimiento::getTiempoPromedioMs)
                .average().orElse(0.0);

        double throughputPromedio = comparacion.getMetricas().stream()
                .mapToDouble(MetricaRendimiento::getThroughputReqSeg)
                .average().orElse(0.0);

        if (metricasCriticas > 0) {
            recomendaciones.add("⚠️ Optimizar rendimiento: Se detectaron " + metricasCriticas +
                    " escenarios con rendimiento crítico que requieren atención inmediata.");
        }

        if (tiempoPromedio > 2000) {
            recomendaciones.add("🚀 Reducir latencia: El tiempo promedio de " +
                    String.format("%.0f ms", tiempoPromedio) +
                    " excede los umbrales recomendados. Implementar cache y optimizar consultas.");
        }

        if (throughputPromedio < 20) {
            recomendaciones.add("📈 Mejorar throughput: El rendimiento actual de " +
                    String.format("%.1f req/s", throughputPromedio) +
                    " puede mejorarse con auto-scaling y balanceador de carga.");
        }

        if (recomendaciones.isEmpty()) {
            recomendaciones.add("✅ Rendimiento aceptable: El sistema muestra métricas dentro de rangos aceptables. " +
                    "Continuar con monitoreo regular.");
            recomendaciones.add("🔄 Ejecutar pruebas periódicas para mantener el nivel de calidad");
            recomendaciones.add("📊 Configurar alertas automáticas para degradación de rendimiento");
        }

        return recomendaciones;
    }

    /**
     * Método para ejecutar solo análisis sin reportes (para debugging)
     */
    public static void ejecutarSoloAnalisis() {
        try {
            System.out.println("🔍 Ejecutando análisis básico de métricas...");

            EjecutorAnalisisMetricas ejecutor = new EjecutorAnalisisMetricas();
            ConfiguracionAplicacion config = ConfiguracionAplicacion.obtenerInstancia();

            AnalizadorMetricas analizador = new AnalizadorMetricas();
            List<MetricaRendimiento> metricas = ejecutor.procesarArchivosJTL(
                    config.obtenerDirectorioResultados(), analizador);

            if (!metricas.isEmpty()) {
                ejecutor.mostrarResumenEnConsola(metricas);
                System.out.println("✅ Análisis básico completado");
            } else {
                System.out.println("ℹ️ No se encontraron métricas para analizar");
            }

        } catch (Exception e) {
            System.err.println("❌ Error en análisis: " + e.getMessage());
        }
    }
}