package com.mediplus.pruebas.servicios;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Servicio para manejar autenticación y autorización
 * Implementa el principio de responsabilidad única para seguridad
 */
public class ServicioAutenticacion {

    private final RequestSpecification especificacionBase;

    public ServicioAutenticacion(RequestSpecification especificacionBase) {
        this.especificacionBase = especificacionBase;
    }

    /**
     * Realizar login y obtener token JWT
     */
    public Response realizarLogin(String usuario, String password) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "username": "%s",
                            "password": "%s",
                            "expiresInMins": 30
                        }
                        """, usuario, password))
                .when()
                .post("/auth/login")
                .then()
                .extract()
                .response();
    }

    /**
     * Obtener información del usuario autenticado usando token válido
     */
    public Response obtenerUsuarioAutenticado(String token) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/auth/me")
                .then()
                .extract()
                .response();
    }

    /**
     * Refrescar token de acceso
     */
    public Response refrescarToken(String refreshToken) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "refreshToken": "%s",
                            "expiresInMins": 30
                        }
                        """, refreshToken))
                .when()
                .post("/auth/refresh")
                .then()
                .extract()
                .response();
    }

    /**
     * Acceder a recurso protegido con token
     */
    public Response accederRecursoProtegido(String token, String endpoint) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }

    /**
     * Intentar acceder a recurso protegido sin token
     */
    public Response accederSinAutenticacion(String endpoint) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                // Sin header Authorization
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }

    /**
     * Intentar acceder con token inválido/expirado
     */
    public Response accederConTokenInvalido(String tokenInvalido, String endpoint) {
        return RestAssured
                .given()
                .spec(especificacionBase)
                .header("Authorization", "Bearer " + tokenInvalido)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }
}