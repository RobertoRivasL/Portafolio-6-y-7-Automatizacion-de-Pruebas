package com.mediplus.pruebas.modelos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Modelo que representa un reporte de salud en MediPlus
 * Mapea la estructura de Product de DummyJSON a nuestro dominio de reportes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Reporte {

    private Integer id;

    @JsonProperty("title")
    private String titulo;

    @JsonProperty("description")
    private String descripcion;

    @JsonProperty("category")
    private String tipoReporte;

    @JsonProperty("price")
    private Double costo;

    @JsonProperty("rating")
    private Double puntuacion;

    @JsonProperty("stock")
    private Integer disponibilidad;

    @JsonProperty("tags")
    private List<String> etiquetas;

    @JsonProperty("brand")
    private String departamento;

    // Campos específicos para reportes de salud
    private String fechaGeneracion;
    private String medicoResponsable;
    private String pacienteId;
    private String estado; // "generado", "enviado", "revisado"

    // Constructor vacío requerido por Jackson
    public Reporte() {}

    // Constructor para crear reportes en pruebas
    public Reporte(String titulo, String descripcion, String tipoReporte, String departamento) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.tipoReporte = tipoReporte;
        this.departamento = departamento;
        this.costo = 0.0; // Los reportes de salud son gratuitos
        this.estado = "generado";
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(String tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public Double getCosto() {
        return costo;
    }

    public void setCosto(Double costo) {
        this.costo = costo;
    }

    public Double getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(Double puntuacion) {
        this.puntuacion = puntuacion;
    }

    public Integer getDisponibilidad() {
        return disponibilidad;
    }

    public void setDisponibilidad(Integer disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public void setEtiquetas(List<String> etiquetas) {
        this.etiquetas = etiquetas;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(String fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getMedicoResponsable() {
        return medicoResponsable;
    }

    public void setMedicoResponsable(String medicoResponsable) {
        this.medicoResponsable = medicoResponsable;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Método helper para verificar si el reporte está completado
     */
    public boolean estaCompletado() {
        return "revisado".equals(this.estado);
    }

    /**
     * Método helper para obtener el tipo de reporte formateado
     */
    public String getTipoReporteFormateado() {
        if (tipoReporte == null) return "General";

        switch (tipoReporte.toLowerCase()) {
            case "laboratory":
                return "Laboratorio";
            case "radiology":
                return "Radiología";
            case "cardiology":
                return "Cardiología";
            case "general":
                return "Medicina General";
            default:
                return tipoReporte;
        }
    }
}