package com.mediplus.pruebas.analisis.test;

import com.mediplus.pruebas.analisis.modelo.MetricaRendimiento;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.PrintWriter;

/**
 * Pruebas básicas sin JUnit - Solo para verificar que las clases funcionan
 * Versión sin dependencias externas problemáticas
 * 
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class PruebasBasicas {

    public static void main(String[] args) {
        PruebasBasicas pruebas = new PruebasBasicas();

        try {
            System.out.println("🚀 Iniciando pruebas básicas del sistema MediPlus...");
            System.out.println("=".repeat(60));
            
            pruebas.probarCreacionMetrica();
            pruebas.probarNivelesRendimiento();
            pruebas.probarValidaciones();
            
            System.out.println("=".repeat(60));
            System.out.println("✅ Todas las pruebas pasaron exitosamente!");
            
            // Generar reporte
            pruebas.generarReporte();

        } catch (Exception e) {
            System.err.println("❌ Error en las pruebas: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void probarCreacionMetrica() {
        System.out.println("🧪 Probando creación de métrica...");

        // Given
        String escenario = "GET Masivo";
        int usuarios = 50;
        double tiempo = 850.5;

        // When
        MetricaRendimiento metrica = new MetricaRendimiento.Builder()
                .nombreEscenario(escenario)
                .usuariosConcurrentes(usuarios)
                .tiempoPromedioMs(tiempo)
                .percentil90Ms(1200.0)
                .percentil95Ms(1400.0)
                .throughputReqSeg(45.2)
                .tasaErrorPorcentaje(2.1)
                .tiempoMinimoMs(120.0)
                .tiempoMaximoMs(2500.0)
                .duracionPruebaSegundos(60)
                .build();

        // Then
        verificar(metrica != null, "La métrica no debe ser nula");
        verificar(escenario.equals(metrica.getNombreEscenario()), "Escenario incorrecto");
        verificar(usuarios == metrica.getUsuariosConcurrentes(), "Usuarios incorrectos");
        verificar(Math.abs(tiempo - metrica.getTiempoPromedioMs()) < 0.1, "Tiempo promedio incorrecto");
        verificar(metrica.getThroughputReqSeg() > 0, "Throughput debe ser positivo");

        System.out.println("  ✅ Creación de métrica: OK");
    }

    public void probarNivelesRendimiento() {
        System.out.println("🧪 Probando niveles de rendimiento...");

        // EXCELENTE
        MetricaRendimiento excelente = crearMetricaConTiempo(300.0, 0.0);
        verificar(excelente.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.EXCELENTE,
                "Métrica con 300ms debe ser EXCELENTE");

        // BUENO
        MetricaRendimiento bueno = crearMetricaConTiempo(750.0, 1.0);
        verificar(bueno.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.BUENO,
                "Métrica con 750ms debe ser BUENO");

        // REGULAR
        MetricaRendimiento regular = crearMetricaConTiempo(1500.0, 3.0);
        verificar(regular.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.REGULAR,
                "Métrica con 1500ms debe ser REGULAR");

        // MALO
        MetricaRendimiento malo = crearMetricaConTiempo(2500.0, 5.0);
        verificar(malo.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.MALO,
                "Métrica con 2500ms debe ser MALO");

        // INACEPTABLE
        MetricaRendimiento inaceptable = crearMetricaConTiempo(1000.0, 15.0);
        verificar(inaceptable.evaluarNivelRendimiento() == MetricaRendimiento.NivelRendimiento.INACEPTABLE,
                "Métrica con 15% error debe ser INACEPTABLE");

        System.out.println("  ✅ Niveles de rendimiento: OK");
    }

    public void probarValidaciones() {
        System.out.println("🧪 Probando validaciones...");

        // Debe fallar con nombre nulo
        try {
            new MetricaRendimiento.Builder()
                    .nombreEscenario(null)
                    .usuariosConcurrentes(10)
                    .build();
            throw new RuntimeException("Debería haber fallado con nombre nulo");
        } catch (NullPointerException e) {
            System.out.println("  ✅ Validación nombre nulo: OK");
        }

        // Debe fallar con usuarios negativos
        try {
            new MetricaRendimiento.Builder()
                    .nombreEscenario("Test")
                    .usuariosConcurrentes(-5)
                    .build();
            throw new RuntimeException("Debería haber fallado con usuarios negativos");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✅ Validación usuarios negativos: OK");
        }

        // Debe fallar con tasa de error > 100%
        try {
            new MetricaRendimiento.Builder()
                    .nombreEscenario("Test")
                    .usuariosConcurrentes(10)
                    .tasaErrorPorcentaje(150.0)
                    .build();
            throw new RuntimeException("Debería haber fallado con tasa error > 100%");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✅ Validación tasa error > 100%: OK");
        }

        System.out.println("  ✅ Validaciones: OK");
    }

    private MetricaRendimiento crearMetricaConTiempo(double tiempo, double error) {
        return new MetricaRendimiento.Builder()
                .nombreEscenario("Test")
                .usuariosConcurrentes(10)
                .tiempoPromedioMs(tiempo)
                .percentil90Ms(tiempo * 1.2)
                .percentil95Ms(tiempo * 1.4)
                .throughputReqSeg(50.0)
                .tasaErrorPorcentaje(error)
                .tiempoMinimoMs(100.0)
                .tiempoMaximoMs(tiempo * 2)
                .duracionPruebaSegundos(60)
                .fechaEjecucion(LocalDateTime.now())
                .build();
    }

    private void verificar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new RuntimeException("❌ Verificación falló: " + mensaje);
        }
    }

    private void generarReporte() {
        try {
            // Crear directorio de reportes si no existe
            var directorioReportes = Paths.get("reportes");
            Files.createDirectories(directorioReportes);

            // Generar reporte con timestamp
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            var archivoReporte = directorioReportes.resolve("reporte-pruebas-" + timestamp + ".txt");

            // Escribir reporte
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(archivoReporte))) {
                writer.println("=".repeat(60));
                writer.println("📊 REPORTE DE PRUEBAS BÁSICAS - API MEDIPLUS");
                writer.println("🕐 Fecha: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                writer.println("👥 Autores: Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez");
                writer.println("=".repeat(60));
                writer.println();
                writer.println("✅ Todas las pruebas ejecutadas correctamente:");
                writer.println("  - Creación de métricas");
                writer.println("  - Evaluación de niveles de rendimiento");
                writer.println("  - Validaciones de parámetros");
                writer.println();
                writer.println("🎯 Estado: EXITOSO");
                writer.println("📍 Ubicación: " + archivoReporte.toAbsolutePath());
                writer.println();
                writer.println("🔧 Detalles Técnicos:");
                writer.println("  - Framework: Pruebas básicas sin JUnit");
                writer.println("  - Patrón: Builder para construcción de métricas");
                writer.println("  - Validaciones: NullPointerException, IllegalArgumentException");
                writer.println("  - Niveles: EXCELENTE, BUENO, REGULAR, MALO, INACEPTABLE");
                writer.println();
                writer.println("📈 Próximos Pasos:");
                writer.println("  1. Implementar pruebas REST Assured");
                writer.println("  2. Configurar scripts JMeter");
                writer.println("  3. Integrar con pipeline CI/CD");
                writer.println("  4. Completar documentación técnica");
            }

            System.out.println("📄 Reporte guardado en: " + archivoReporte.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("⚠️ No se pudo guardar el reporte: " + e.getMessage());
        }
    }
}