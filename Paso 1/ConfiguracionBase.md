package com.mediplus.pruebas.configuracion;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.BeforeAll;

/**
* Configuración base para todas las pruebas REST Assured
* Implementa principios de abstracción y configuración centralizada
  */
  public class ConfiguracionBase {

  protected static final String URL_BASE = "https://dummyjson.com";
  protected static final int TIMEOUT_RESPUESTA = 10000; // 10 segundos
  protected static final String USUARIO_PRUEBA = "emilys";
  protected static final String PASSWORD_PRUEBA = "emilyspass";

  protected static RequestSpecification especificacionPeticion;
  protected static ResponseSpecification especificacionRespuesta;

  @BeforeAll
  public static void configuracionInicial() {
  configurarRestAssured();
  crearEspecificaciones();
  }

  /**
    * Configuración global de REST Assured
      */
      private static void configurarRestAssured() {
      RestAssured.baseURI = URL_BASE;
      RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
      }

  /**
    * Crear especificaciones reutilizables para peticiones y respuestas
    * Aplica principio de DRY (Don't Repeat Yourself)
      */
      private static void crearEspecificaciones() {
      especificacionPeticion = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .setAccept(ContentType.JSON)
      .log(LogDetail.METHOD)
      .log(LogDetail.URI)
      .build();

      especificacionRespuesta = new ResponseSpecBuilder()
      .expectResponseTime(org.hamcrest.Matchers.lessThan((long) TIMEOUT_RESPUESTA))
      .log(LogDetail.STATUS)
      .log(LogDetail.BODY)
      .build();
      }

  /**
    * Obtener token de autenticación para pruebas que lo requieran
      */
      protected static String obtenerTokenAutenticacion() {
      return RestAssured
      .given()
      .spec(especificacionPeticion)
      .body(String.format("""
      {
      "username": "%s",
      "password": "%s",
      "expiresInMins": 30
      }
      """, USUARIO_PRUEBA, PASSWORD_PRUEBA))
      .when()
      .post("/auth/login")
      .then()
      .statusCode(200)
      .extract()
      .path("accessToken");
      }
      }