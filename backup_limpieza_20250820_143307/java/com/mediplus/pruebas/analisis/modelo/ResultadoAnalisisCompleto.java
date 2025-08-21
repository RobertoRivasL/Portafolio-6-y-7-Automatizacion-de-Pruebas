package com.mediplus.pruebas.analisis.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Modelo que representa el resultado completo del análisis
 * Integra todos los componentes del sistema de análisis
 *
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class ResultadoAnalisisCompleto {

    private final LocalDateTime fechaEjecucion;
    private final EstadoEjecucion estadoGeneral;
    private final ResumenFuncional resumenFuncional;
    private final ResumenRendimiento resumenRendimiento;
    private final List<String> recomendaciones;
    private final List<String> archivosGenerados;
    private final String timestampEjecucion;

    private ResultadoAnalisisCompleto(Builder builder) {
        this.fechaEjecucion = builder.fechaEjecucion != null ? builder.fechaEjecucion : LocalDateTime.now();
        this.estadoGeneral = builder.estadoGeneral != null ? builder.estadoGeneral : EstadoEjecucion.EXITOSO;
        this.resumenFuncional = builder.resumenFuncional;
        this.resumenRendimiento = builder.resumenRendimiento;
        this.recomendaciones = Collections.unmodifiableList(new ArrayList<>(builder.recomendaciones));
        this.archivosGenerados = Collections.unmodifiableList(new ArrayList<>(builder.archivosGenerados));
        this.timestampEjecucion = fechaEjecucion.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    }

    /**
     * Genera el reporte ejecutivo completo
     */
    public String generarReporteEjecutivo() {
        StringBuilder reporte = new StringBuilder();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        reporte.append("# 📊 REPORTE EJECUTIVO FINAL - API MEDIPLUS\n\n");
        reporte.append("**Fecha de Análisis:** ").append(fechaEjecucion.format(formato)).append("\n");
        reporte.append("**Estado General:** ").append(estadoGeneral.getDescripcion()).append("\n");
        reporte.append("**Autores:** Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez\n\n");

        // Resumen funcional
        if (resumenFuncional != null) {
            reporte.append("## 🧪 RESUMEN FUNCIONAL\n\n");
            reporte.append("- **Total de Tests:** ").append(resumenFuncional.totalTests).append("\n");
            reporte.append("- **Tests Exitosos:** ").append(resumenFuncional.testsExitosos).append("\n");
            reporte.append("- **Tests Fallidos:** ").append(resumenFuncional.testsFallidos).append("\n");
            reporte.append("- **Tests Omitidos:** ").append(resumenFuncional.testsOmitidos).append("\n");

            double porcentajeExito = resumenFuncional.totalTests > 0 ?
                    (double) resumenFuncional.testsExitosos / resumenFuncional.totalTests * 100 : 0;
            reporte.append("- **Tasa de Éxito:** ").append(String.format("%.1f%%", porcentajeExito)).append("\n\n");

            if (!resumenFuncional.erroresPrincipales.isEmpty()) {
                reporte.append("### ⚠️ Errores Principales:\n");
                resumenFuncional.erroresPrincipales.forEach(error ->
                        reporte.append("- ").append(error).append("\n"));
                reporte.append("\n");
            }
        }

        // Resumen de rendimiento
        if (resumenRendimiento != null) {
            reporte.append("## 📈 RESUMEN DE RENDIMIENTO\n\n");
            reporte.append("- **Total de Escenarios:** ").append(resumenRendimiento.totalEscenarios).append("\n");
            reporte.append("- **Escenarios GET:** ").append(resumenRendimiento.escenariosGET).append("\n");
            reporte.append("- **Escenarios POST:** ").append(resumenRendimiento.escenariosPOST).append("\n");
            reporte.append("- **Escenarios Mixtos:** ").append(resumenRendimiento.escenariosMixtos).append("\n");
            reporte.append("- **Escenarios Críticos:** ").append(resumenRendimiento.escenariosCriticos).append("\n");
            reporte.append("- **Tiempo Promedio:** ").append(String.format("%.0f ms", resumenRendimiento.tiempoPromedioMs)).append("\n");
            reporte.append("- **Throughput Promedio:** ").append(String.format("%.1f req/s", resumenRendimiento.throughputPromedio)).append("\n");
            reporte.append("- **Tasa de Error:** ").append(String.format("%.1f%%", resumenRendimiento.tasaErrorPromedio)).append("\n\n");
        }

        // Recomendaciones
        if (!recomendaciones.isEmpty()) {
            reporte.append("## 💡 RECOMENDACIONES ESTRATÉGICAS\n\n");
            for (int i = 0; i < recomendaciones.size(); i++) {
                reporte.append(i + 1).append(". ").append(recomendaciones.get(i)).append("\n");
            }
            reporte.append("\n");
        }

        // Estado de aptitud para producción
        reporte.append("## 🎯 APTITUD PARA PRODUCCIÓN\n\n");
        if (esAptoParaProduccion()) {
            reporte.append("✅ **RECOMENDACIÓN: API APTA PARA PRODUCCIÓN**\n\n");
            reporte.append("La API MediPlus cumple con los estándares de calidad establecidos. ");
            reporte.append("Se puede proceder con el despliegue siguiendo las recomendaciones de optimización.\n\n");
        } else {
            reporte.append("⚠️ **RECOMENDACIÓN: REQUIERE ATENCIÓN ANTES DE PRODUCCIÓN**\n\n");
            reporte.append("Se han identificado problemas que deben resolverse antes del despliegue. ");
            reporte.append("Revisar las recomendaciones y ejecutar las correcciones necesarias.\n\n");
        }

        // Archivos generados
        if (!archivosGenerados.isEmpty()) {
            reporte.append("## 📁 ARCHIVOS GENERADOS\n\n");
            archivosGenerados.forEach(archivo ->
                    reporte.append("- `").append(archivo).append("`\n"));
            reporte.append("\n");
        }

        reporte.append("## 🚀 PRÓXIMOS PASOS\n\n");
        if (esAptoParaProduccion()) {
            reporte.append("1. 📋 Revisar recomendaciones de optimización\n");
            reporte.append("2. 🚀 Proceder con despliegue a staging\n");
            reporte.append("3. 📊 Configurar monitoring en producción\n");
            reporte.append("4. 🔄 Establecer proceso de CI/CD\n");
        } else {
            reporte.append("1. 🚨 Atender problemas críticos identificados\n");
            reporte.append("2. 🔧 Implementar correcciones sugeridas\n");
            reporte.append("3. 🔄 Re-ejecutar análisis para validar mejoras\n");
            reporte.append("4. 📞 Contactar equipo de arquitectura si necesario\n");
        }

        reporte.append("\n---\n");
        reporte.append("*Reporte generado automáticamente por el Framework de Análisis MediPlus*\n");

        return reporte.toString();
    }

    /**
     * Determina si la API está apta para producción
     */
    public boolean esAptoParaProduccion() {
        if (estadoGeneral == EstadoEjecucion.FALLIDO) {
            return false;
        }

        // Criterios funcionales
        boolean funcionesAceptables = true;
        if (resumenFuncional != null) {
            double tasaExito = resumenFuncional.totalTests > 0 ?
                    (double) resumenFuncional.testsExitosos / resumenFuncional.totalTests * 100 : 0;
            funcionesAceptables = tasaExito >= 85.0; // Mínimo 85% de éxito
        }

        // Criterios de rendimiento
        boolean rendimientoAceptable = true;
        if (resumenRendimiento != null) {
            rendimientoAceptable = resumenRendimiento.escenariosCriticos <= 1 && // Máximo 1 escenario crítico
                    resumenRendimiento.tiempoPromedioMs <= 3000 && // Máximo 3 segundos
                    resumenRendimiento.tasaErrorPromedio <= 10.0; // Máximo 10% error
        }

        return funcionesAceptables && rendimientoAceptable;
    }

    // Getters
    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }
    public EstadoEjecucion getEstadoGeneral() { return estadoGeneral; }
    public ResumenFuncional getResumenFuncional() { return resumenFuncional; }
    public ResumenRendimiento getResumenRendimiento() { return resumenRendimiento; }
    public List<String> getRecomendaciones() { return recomendaciones; }
    public List<String> getArchivosGenerados() { return archivosGenerados; }
    public String getTimestampEjecucion() { return timestampEjecucion; }

    /**
     * Estados posibles de ejecución
     */
    public enum EstadoEjecucion {
        EXITOSO("Exitoso - Análisis completado correctamente"),
        CON_ADVERTENCIAS("Con Advertencias - Completado con observaciones"),
        PARCIAL("Parcial - Completado parcialmente"),
        FALLIDO("Fallido - Error durante la ejecución");

        private final String descripcion;

        EstadoEjecucion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getDescripcion() { return descripcion; }
    }

    /**
     * Resumen de pruebas funcionales
     */
    public static class ResumenFuncional {
        public final int totalTests;
        public final int testsExitosos;
        public final int testsFallidos;
        public final int testsOmitidos;
        public final List<String> erroresPrincipales;

        public ResumenFuncional(int totalTests, int testsExitosos, int testsFallidos,
                                int testsOmitidos, List<String> erroresPrincipales) {
            this.totalTests = totalTests;
            this.testsExitosos = testsExitosos;
            this.testsFallidos = testsFallidos;
            this.testsOmitidos = testsOmitidos;
            this.erroresPrincipales = Collections.unmodifiableList(new ArrayList<>(erroresPrincipales));
        }
    }

    /**
     * Resumen de rendimiento
     */
    public static class ResumenRendimiento {
        public final int totalEscenarios;
        public final int escenariosGET;
        public final int escenariosPOST;
        public final int escenariosMixtos;
        public final int escenariosCriticos;
        public final double tiempoPromedioMs;
        public final double throughputPromedio;
        public final double tasaErrorPromedio;

        public ResumenRendimiento(int totalEscenarios, int escenariosGET, int escenariosPOST,
                                  int escenariosMixtos, int escenariosCriticos, double tiempoPromedioMs,
                                  double throughputPromedio, double tasaErrorPromedio) {
            this.totalEscenarios = totalEscenarios;
            this.escenariosGET = escenariosGET;
            this.escenariosPOST = escenariosPOST;
            this.escenariosMixtos = escenariosMixtos;
            this.escenariosCriticos = escenariosCriticos;
            this.tiempoPromedioMs = tiempoPromedioMs;
            this.throughputPromedio = throughputPromedio;
            this.tasaErrorPromedio = tasaErrorPromedio;
        }
    }

    /**
     * Builder para construcción flexible del resultado
     */
    public static class Builder {
        private LocalDateTime fechaEjecucion;
        private EstadoEjecucion estadoGeneral;
        private ResumenFuncional resumenFuncional;
        private ResumenRendimiento resumenRendimiento;
        private List<String> recomendaciones = new ArrayList<>();
        private List<String> archivosGenerados = new ArrayList<>();

        public Builder fechaEjecucion(LocalDateTime fechaEjecucion) {
            this.fechaEjecucion = fechaEjecucion;
            return this;
        }

        public Builder estadoGeneral(EstadoEjecucion estadoGeneral) {
            this.estadoGeneral = estadoGeneral;
            return this;
        }

        public Builder resumenFuncional(ResumenFuncional resumenFuncional) {
            this.resumenFuncional = resumenFuncional;
            return this;
        }

        public Builder resumenRendimiento(ResumenRendimiento resumenRendimiento) {
            this.resumenRendimiento = resumenRendimiento;
            return this;
        }

        public Builder agregarRecomendacion(String recomendacion) {
            this.recomendaciones.add(recomendacion);
            return this;
        }

        public Builder agregarArchivoGenerado(String archivo) {
            this.archivosGenerados.add(archivo);
            return this;
        }

        public ResultadoAnalisisCompleto build() {
            return new ResultadoAnalisisCompleto(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ResultadoAnalisisCompleto{estado=%s, timestamp=%s, archivos=%d}",
                estadoGeneral, timestampEjecucion, archivosGenerados.size());
    }
}