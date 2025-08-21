package com.mediplus.pruebas.modelos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modelo que representa un paciente de MediPlus
 * Mapea la estructura de User de DummyJSON a nuestro dominio
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Paciente {

    private Integer id;

    @JsonProperty("firstName")
    private String nombre;

    @JsonProperty("lastName")
    private String apellido;

    private String email;

    @JsonProperty("phone")
    private String telefono;

    @JsonProperty("birthDate")
    private String fechaNacimiento;

    private DireccionPaciente address;

    // Constructor vacío requerido por Jackson
    public Paciente() {}

    // Constructor para crear pacientes en pruebas
    public Paciente(String nombre, String apellido, String email, String telefono) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public DireccionPaciente getAddress() {
        return address;
    }

    public void setAddress(DireccionPaciente address) {
        this.address = address;
    }

    /**
     * Clase anidada para manejar la dirección del paciente
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DireccionPaciente {
        private String address;
        private String city;
        private String postalCode;

        // Getters y Setters
        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
    }
}