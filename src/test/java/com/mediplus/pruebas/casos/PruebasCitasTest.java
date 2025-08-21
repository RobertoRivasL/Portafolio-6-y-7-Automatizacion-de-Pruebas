package com.mediplus.pruebas.casos;

import com.mediplus.pruebas.configuracion.ConfiguracionBase;
import com.mediplus.pruebas.modelos.Cita;
import com.mediplus.pruebas.servicios.ServicioCitas;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Pruebas funcionales complementarias para el servicio de citas médicas
 * Completa los casos de prueba requeridos para la Lección 2
 */
public class PruebasCitasTest extends ConfiguracionBase {

    private ServicioCitas servicioCitas;

    @BeforeEach
    public void configurarServicio() {
        servicioCitas = new ServicioCitas(especificacionPeticion);
    }

    @Test
    @DisplayName("GET - Obtener todas las citas médicas programadas")
    public void deberiaObtenerTodasCitasExitosamente() {
        Response respuesta = servicioCitas.obtenerTodasCitas();

        // Validar status code
        assertEquals(200, respuesta.getStatusCode(),
                "El status code debería ser 200");

        // Validar tiempo de respuesta
        assertTrue(respuesta.getTime() < TIMEOUT_RESPUESTA,
                "El tiempo de respuesta debería ser menor a " + TIMEOUT_RESPUESTA + "ms");

        // Validar estructura del body
        respuesta.then()
                .body("posts", notNullValue())
                .body("posts.size()", greaterThan(0))
                .body("posts[0].id", notNullValue())
                .body("posts[0].title", notNullValue())
                .body("posts[0].userId", notNullValue());

        int totalCitas = respuesta.jsonPath().getList("posts").size();
        System.out.println("✅ Total de citas obtenidas: " + totalCitas);
    }

    private Integer extraerIdComoInteger(Response respuesta) {
        try {
            Object idObject = respuesta.jsonPath().get("id");
            if (idObject == null) {
                System.out.println("⚠️ El campo 'id' es nulo en la respuesta");
                return null;
            }
            return Integer.valueOf(String.valueOf(idObject));
        } catch (Exception e) {
            System.out.println("⚠️ Error al extraer ID como Integer: " + e.getMessage());
            return null;
        }
    }

    @Test
    @DisplayName("POST - Agendar nueva cita médica especializada")
    public void deberiaAgendarNuevaCitaExitosamente() {
        Cita nuevaCita = new Cita(
                "Consulta Neurología",
                "Evaluación neurológica por dolores de cabeza recurrentes",
                5
        );
        Response respuesta = servicioCitas.crearCita(nuevaCita);

        // Validar status code de creación
        assertEquals(201, respuesta.getStatusCode(),
                "El status code debería ser 201 para creación exitosa");

        // Validar tiempo de respuesta
        assertTrue(respuesta.getTime() < TIMEOUT_RESPUESTA);

        // Validar body de respuesta
        respuesta.then()
                .body("id", notNullValue())
                .body("title", equalTo(nuevaCita.getMotivo()))
                .body("body", equalTo(nuevaCita.getDescripcion()))
                .body("userId", equalTo(nuevaCita.getPacienteId()));

        // ✅ CORRECCIÓN SIMPLE: Una sola línea
        Integer idCitaCreada = extraerIdComoInteger(respuesta);

        System.out.println("✅ Cita agendada con ID: " + idCitaCreada +
                " para paciente: " + nuevaCita.getPacienteId());
    }
}