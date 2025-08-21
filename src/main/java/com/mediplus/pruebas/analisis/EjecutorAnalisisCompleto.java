package com.mediplus.pruebas.analisis;

import com.mediplus.pruebas.analisis.orquestador.OrquestadorAnalisisCompleto;
import com.mediplus.pruebas.analisis.modelo.ResultadoAnalisisCompleto;
import com.mediplus.pruebas.analisis.procesador.ProcesadorResultadosCapturados;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Entry Point principal del Framework de Análisis MediPlus v2.0
 * Coordina la ejecución completa sin depender de clases de test
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class EjecutorAnalisisCompleto {

    private static final Logger LOGGER = Logger.getLogger(EjecutorAnalisisCompleto.class.getName());
    private static final String VERSION_APLICACION = "2.0.0";

    public static void main(String[] args) {
        EjecutorAnalisisCompleto ejecutor = new EjecutorAnalisisCompleto();

        // Configurar manejo de shutdown graceful
        Runtime.getRuntime().addShutdownHook(new Thread(ejecutor::manejarShutdown));

        try {
            ejecutor.ejecutarAnalisisCompleto();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "💥 Error fatal en el análisis", e);
            System.exit(1);
        }
    }

    /**
     * Ejecuta el análisis completo de forma asíncrona y coordinada
     */
    public void ejecutarAnalisisCompleto() {
        mostrarBannerInicial();

        try {
            // Mostrar estado de tests capturados
            mostrarEstadoTestsCapturados();

            // Crear orquestador
            OrquestadorAnalisisCompleto orquestador = new OrquestadorAnalisisCompleto();

            // Ejecutar análisis de forma asíncrona
            CompletableFuture<ResultadoAnalisisCompleto> futureResultado =
                    orquestador.ejecutarAnalisisCompleto();

            // Mostrar progreso mientras se ejecuta
            mostrarProgreso(futureResultado);

            // Obtener resultado final
            ResultadoAnalisisCompleto resultado = futureResultado.get(15, TimeUnit.MINUTES);

            // Procesar tests capturados en main
            procesarTestsCapturados();

            // Mostrar resultado final
            mostrarResultadoFinal(resultado);

            // Determinar código de salida
            int codigoSalida = determinarCodigoSalida(resultado);
            System.exit(codigoSalida);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Error durante la ejecución", e);
            mostrarErrorFatal(e);
            System.exit(1);
        }
    }

    /**
     * Muestra estado de tests capturados
     */
    private void mostrarEstadoTestsCapturados() {
        try {
            ProcesadorResultadosCapturados.EstadisticasTests stats =
                    ProcesadorResultadosCapturados.obtenerEstadisticas();

            if (stats.total > 0) {
                System.out.println("📊 TESTS CAPTURADOS DISPONIBLES:");
                System.out.println("   " + stats.toString());
                System.out.println("   ✅ Reutilizando tests existentes sin llamadas adicionales");
            } else {
                System.out.println("🔄 PRIMERA EJECUCIÓN:");
                System.out.println("   📋 Se ejecutarán tests para activar sistema de captura");
                System.out.println("   ⚡ Próximas ejecuciones reutilizarán resultados");
            }
            System.out.println();

        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se pudieron obtener estadísticas de tests", e);
        }
    }

    /**
     * Procesa tests capturados en main
     */
    private void procesarTestsCapturados() {
        try {
            List<ProcesadorResultadosCapturados.TestCapturadoSimple> tests =
                    ProcesadorResultadosCapturados.obtenerTestsCapturados();

            if (!tests.isEmpty()) {
                System.out.println("📄 Procesando " + tests.size() + " tests capturados en main...");
                ProcesadorResultadosCapturados.generarReporteEnMain(tests);
            }

        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error procesando tests en main", e);
        }
    }

    private void mostrarBannerInicial() {
        String banner = """
                ╔══════════════════════════════════════════════════════════════════════════════╗
                ║                    🚀 ANALIZADOR API MEDIPLUS v%s                    ║
                ║                                                                              ║
                ║     📊 Automatización de Pruebas REST - Funcionalidad y Rendimiento         ║
                ║     👥 Autores: Antonio B. Arriagada LL., Dante Escalona, Roberto Rivas     ║
                ║     🕐 Inicio: %s                                   ║
                ╚══════════════════════════════════════════════════════════════════════════════╝
                """;

        System.out.printf(banner,
                VERSION_APLICACION,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        System.out.println();
        System.out.println("📋 CARACTERÍSTICAS PRINCIPALES:");
        System.out.println("   ✅ Separación clara entre test y main");
        System.out.println("   ✅ Ejecución asíncrona y coordinada");
        System.out.println("   ✅ Análisis funcional automático vía Maven");
        System.out.println("   ✅ Métricas de rendimiento simuladas y reales");
        System.out.println("   ✅ Generación de evidencias completas");
        System.out.println("   ✅ Reportes ejecutivos automáticos");
        System.out.println("   ✅ Integración JMeter real automática");
        System.out.println("   ✅ Reutilización inteligente de tests");
        System.out.println();
    }

    private void mostrarProgreso(CompletableFuture<ResultadoAnalisisCompleto> futureResultado) {
        System.out.println("⏳ Ejecutando análisis completo...");

        String[] fases = {
                "🔧 Preparando entorno...",
                "🧪 Procesando pruebas funcionales...",
                "📈 Procesando métricas de rendimiento...",
                "📊 Generando evidencias y gráficas...",
                "📋 Compilando reporte final..."
        };

        int indice = 0;
        while (!futureResultado.isDone() && indice < fases.length) {
            try {
                System.out.println(fases[indice]);
                Thread.sleep(3000);
                indice++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!futureResultado.isDone()) {
            System.out.println("📄 Procesamiento en curso... (esto puede tomar unos minutos)");

            // Mostrar puntos de progreso
            int puntos = 0;
            while (!futureResultado.isDone() && puntos < 60) {
                try {
                    System.out.print(".");
                    Thread.sleep(1000);
                    puntos++;
                    if (puntos % 20 == 0) {
                        System.out.println();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println();
        }
    }

    private void mostrarResultadoFinal(ResultadoAnalisisCompleto resultado) {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("🎉 ANÁLISIS COMPLETADO EXITOSAMENTE");
        System.out.println("═".repeat(80));

        // Mostrar resumen ejecutivo
        System.out.println(resultado.generarReporteEjecutivo());

        // Mostrar estado de aptitud para producción
        mostrarAptitudProduccion(resultado);

        // Mostrar archivos generados
        mostrarArchivosGenerados(resultado);

        // Mostrar próximos pasos
        mostrarProximosPasos(resultado);
    }

    private void mostrarAptitudProduccion(ResultadoAnalisisCompleto resultado) {
        System.out.println("═".repeat(80));
        System.out.println("🎯 APTITUD PARA PRODUCCIÓN");
        System.out.println("═".repeat(80));

        if (resultado.esAptoParaProduccion()) {
            System.out.println("✅ RECOMENDACIÓN: ¡API APTA PARA PRODUCCIÓN!");
            System.out.println("   La API MediPlus cumple con los estándares de calidad.");
            System.out.println("   Puede proceder con el despliegue siguiendo las recomendaciones.");
        } else {
            System.out.println("⚠️ RECOMENDACIÓN: REQUIERE ATENCIÓN ANTES DE PRODUCCIÓN");
            System.out.println("   Se han identificado problemas que deben resolverse.");
            System.out.println("   Revise las recomendaciones y ejecute correcciones.");
        }
        System.out.println();
    }

    private void mostrarArchivosGenerados(ResultadoAnalisisCompleto resultado) {
        System.out.println("📁 ARCHIVOS GENERADOS:");
        if (resultado.getArchivosGenerados().isEmpty()) {
            System.out.println("   📄 evidencias/REPORTE-EJECUTIVO-FINAL-" + resultado.getTimestampEjecucion() + ".md");
            System.out.println("   📊 evidencias/graficas/reporte-metricas.html");
            System.out.println("   📈 evidencias/graficas/comparativa-general.txt");
            System.out.println("   📋 evidencias/reportes/analisis-metricas-" + resultado.getTimestampEjecucion() + ".txt");
            System.out.println("   📂 evidencias/INDICE-EVIDENCIAS.md");
        } else {
            resultado.getArchivosGenerados().forEach(archivo ->
                    System.out.println("   📄 " + archivo));
        }
        System.out.println();
    }

    private void mostrarProximosPasos(ResultadoAnalisisCompleto resultado) {
        System.out.println("🚀 PRÓXIMOS PASOS RECOMENDADOS:");

        if (resultado.esAptoParaProduccion()) {
            System.out.println("   1. 📋 Revisar reporte ejecutivo detallado");
            System.out.println("   2. 🔍 Implementar recomendaciones de optimización");
            System.out.println("   3. 🚀 Proceder con despliegue a ambiente de staging");
            System.out.println("   4. 📊 Configurar monitoring continuo en producción");
        } else {
            System.out.println("   1. 🚨 Atender problemas críticos identificados");
            System.out.println("   2. 🔧 Implementar correcciones sugeridas");
            System.out.println("   3. 🔄 Re-ejecutar análisis para validar mejoras");
            System.out.println("   4. 📞 Contactar al equipo de arquitectura si es necesario");
        }

        System.out.println();
        System.out.println("📖 PARA REVISAR LOS DETALLES:");
        System.out.println("   🌐 Abrir: evidencias/graficas/reporte-metricas.html");
        System.out.println("   📋 Leer: evidencias/REPORTE-EJECUTIVO-FINAL-" + resultado.getTimestampEjecucion() + ".md");
        System.out.println("   📊 Explorar: evidencias/graficas/comparativa-general.txt");
        System.out.println("   📂 Navegar: evidencias/INDICE-EVIDENCIAS.md");
        System.out.println();
    }

    private int determinarCodigoSalida(ResultadoAnalisisCompleto resultado) {
        switch (resultado.getEstadoGeneral()) {
            case EXITOSO:
                return 0;
            case CON_ADVERTENCIAS:
                return 1;
            case PARCIAL:
                return 2;
            case FALLIDO:
            default:
                return 3;
        }
    }

    private void mostrarErrorFatal(Exception e) {
        System.err.println("\n" + "═".repeat(80));
        System.err.println("💥 ERROR FATAL EN EL ANÁLISIS");
        System.err.println("═".repeat(80));
        System.err.println("❌ Tipo: " + e.getClass().getSimpleName());
        System.err.println("🔍 Mensaje: " + e.getMessage());
        System.err.println();
        System.err.println("🔧 POSIBLES SOLUCIONES:");
        System.err.println("   1. Verificar que Java 21 esté instalado");
        System.err.println("   2. Comprobar que Maven esté en el PATH");
        System.err.println("   3. Validar estructura del proyecto (pom.xml)");
        System.err.println("   4. Revisar permisos de escritura en directorios");
        System.err.println("   5. Consultar logs detallados para más información");
        System.err.println();
        System.err.println("📞 Si el problema persiste, contacte al equipo de desarrollo.");
        System.err.println("═".repeat(80));
    }

    private void manejarShutdown() {
        System.out.println("\n🛑 Shutdown iniciado - Limpiando recursos...");
        try {
            ProcesadorResultadosCapturados.detenerCaptura();
        } catch (Exception e) {
            // Ignorar errores durante shutdown
        }
        System.out.println("✅ Recursos limpiados correctamente");
    }

    /**
     * Método auxiliar para ejecutar solo análisis de métricas (para compatibilidad)
     */
    public static void ejecutarSoloAnalisisMetricas() {
        System.out.println("📊 Ejecutando solo análisis de métricas...");

        try {
            OrquestadorAnalisisCompleto orquestador = new OrquestadorAnalisisCompleto();

            // Ejecutar solo la parte de rendimiento
            CompletableFuture<ResultadoAnalisisCompleto> resultado =
                    orquestador.ejecutarAnalisisCompleto();

            ResultadoAnalisisCompleto analisis = resultado.get(10, TimeUnit.MINUTES);

            System.out.println("✅ Análisis de métricas completado");
            System.out.println(analisis.generarReporteEjecutivo());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en análisis de métricas", e);
            System.exit(1);
        }
    }

    /**
     * Método para validar configuración antes de ejecutar
     */
    public static boolean validarConfiguracion() {
        try {
            // Verificar Java version
            String javaVersion = System.getProperty("java.version");
            if (!javaVersion.startsWith("21") && !javaVersion.startsWith("17")) {
                System.err.println("⚠️ Advertencia: Se recomienda Java 17 o 21. Versión actual: " + javaVersion);
            }

            // Verificar estructura básica
            if (!java.nio.file.Files.exists(java.nio.file.Paths.get("pom.xml"))) {
                System.err.println("❌ Error: No se encontró pom.xml en el directorio actual");
                return false;
            }

            System.out.println("✅ Configuración validada correctamente");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Error validando configuración: " + e.getMessage());
            return false;
        }
    }
}