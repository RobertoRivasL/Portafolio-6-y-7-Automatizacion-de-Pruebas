package com.mediplus.pruebas.modelos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Modelo que representa una cita médica en MediPlus
 * Mapea la estructura de Post de DummyJSON a nuestro dominio
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cita {

    private Integer id;

    @JsonProperty("title")
    private String motivo;

    @JsonProperty("body")
    private String descripcion;

    @JsonProperty("userId")
    private Integer pacienteId;

    private List<String> tags;

    // Constructor vacío requerido por Jackson
    public Cita() {}

    // Constructor para crear citas en pruebas
    public Cita(String motivo, String descripcion, Integer pacienteId) {
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.pacienteId = pacienteId;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}