package com.mediplus.pruebas.servicios;

import com.mediplus.pruebas.modelos.Paciente;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Servicio para manejar operaciones CRUD de pacientes
 * Implementa el principio de separación de intereses
 */
public class ServicioPacientes {

    private final RequestSpecification especificacionBase;

    public ServicioPacientes(RequestSpecification especificacionBase) {
        this.especificacionBase = especificacionBase;
    }

    /**
     * Obtener lista de pacientes (GET)
     */
    public Response obtenerTodosPacientes() {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/users")
                .then()
                .extract()
                .response();
    }

    /**
     * Obtener lista de pacientes con límite (GET con parámetros)
     */
    public Response obtenerPacientesConLimite(int limite) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .queryParam("limit", limite)
                .when()
                .get("/users")
                .then()
                .extract()
                .response();
    }

    /**
     * Obtener paciente por ID (GET)
     */
    public Response obtenerPacientePorId(int id) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/users/{id}", id)
                .then()
                .extract()
                .response();
    }

    /**
     * Crear nuevo paciente (POST)
     */
    public Response crearPaciente(Paciente paciente) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "firstName": "%s",
                            "lastName": "%s",
                            "email": "%s",
                            "phone": "%s"
                        }
                        """,
                        paciente.getNombre(),
                        paciente.getApellido(),
                        paciente.getEmail(),
                        paciente.getTelefono()))
                .when()
                .post("/users/add")
                .then()
                .extract()
                .response();
    }

    /**
     * Actualizar paciente existente (PUT)
     */
    public Response actualizarPaciente(int id, Paciente paciente) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "firstName": "%s",
                            "lastName": "%s",
                            "email": "%s"
                        }
                        """,
                        paciente.getNombre(),
                        paciente.getApellido(),
                        paciente.getEmail()))
                .when()
                .put("/users/{id}", id)
                .then()
                .extract()
                .response();
    }

    /**
     * Eliminar paciente (DELETE)
     */
    public Response eliminarPaciente(int id) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .delete("/users/{id}", id)
                .then()
                .extract()
                .response();
    }
}