package com.mediplus.pruebas.servicios;

import com.mediplus.pruebas.modelos.Cita;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Servicio para manejar operaciones de citas médicas
 * Implementa el principio de encapsulación y abstracción
 */
public class ServicioCitas {

    private final RequestSpecification especificacionBase;

    public ServicioCitas(RequestSpecification especificacionBase) {
        this.especificacionBase = especificacionBase;
    }

    /**
     * Obtener todas las citas médicas (GET)
     */
    public Response obtenerTodasCitas() {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/posts")
                .then()
                .extract()
                .response();
    }

    /**
     * Obtener citas médicas con límite (GET con parámetros)
     */
    public Response obtenerCitasConLimite(int limite) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .queryParam("limit", limite)
                .when()
                .get("/posts")
                .then()
                .extract()
                .response();
    }

    /**
     * Obtener cita específica por ID (GET)
     */
    public Response obtenerCitaPorId(int id) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/posts/{id}", id)
                .then()
                .extract()
                .response();
    }

    /**
     * Crear nueva cita médica (POST)
     */
    public Response crearCita(Cita cita) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "title": "%s",
                            "body": "%s",
                            "userId": %d,
                            "tags": ["cita", "medica", "mediplus"]
                        }
                        """,
                        cita.getMotivo(),
                        cita.getDescripcion(),
                        cita.getPacienteId()))
                .when()
                .post("/posts/add")
                .then()
                .extract()
                .response();
    }

    /**
     * Crear cita con especialidad específica
     */
    public Response crearCitaConEspecialidad(String motivo, String descripcion,
                                             int pacienteId, String especialidad) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "title": "%s",
                            "body": "%s",
                            "userId": %d,
                            "tags": ["cita", "%s", "especialidad"]
                        }
                        """, motivo, descripcion, pacienteId, especialidad))
                .when()
                .post("/posts/add")
                .then()
                .extract()
                .response();
    }

    /**
     * Actualizar cita existente (PUT)
     */
    public Response actualizarCita(int id, Cita cita) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "title": "%s",
                            "body": "%s"
                        }
                        """,
                        cita.getMotivo(),
                        cita.getDescripcion()))
                .when()
                .put("/posts/{id}", id)
                .then()
                .extract()
                .response();
    }

    /**
     * Cancelar/eliminar cita (DELETE)
     */
    public Response cancelarCita(int id) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .delete("/posts/{id}", id)
                .then()
                .extract()
                .response();
    }

    /**
     * Buscar citas por palabra clave
     */
    public Response buscarCitasPorTexto(String textoBusqueda) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .queryParam("q", textoBusqueda)
                .when()
                .get("/posts/search")
                .then()
                .extract()
                .response();
    }

    /**
     * Obtener citas de un paciente específico
     */
    public Response obtenerCitasPorPaciente(int pacienteId) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/posts/user/{userId}", pacienteId)
                .then()
                .extract()
                .response();
    }
}