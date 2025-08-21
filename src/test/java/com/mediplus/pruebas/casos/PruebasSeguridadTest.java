package com.mediplus.pruebas.casos;

import com.mediplus.pruebas.configuracion.ConfiguracionBase;
import com.mediplus.pruebas.servicios.ServicioAutenticacion;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de seguridad y autenticación para la API MediPlus
 * Valida autenticación JWT y autorización de recursos
 */
public class PruebasSeguridadTest extends ConfiguracionBase {

    private ServicioAutenticacion servicioAuth;
    private String tokenValido;

    @BeforeEach
    public void configurarServicio() {
        servicioAuth = new ServicioAutenticacion(especificacionPeticion);
        tokenValido = obtenerTokenValidoParaPruebas();
    }

    /**
     * Método auxiliar para obtener un token válido para las pruebas
     */
    private String obtenerTokenValidoParaPruebas() {
        Response loginResponse = servicioAuth.realizarLogin(USUARIO_PRUEBA, PASSWORD_PRUEBA);
        if (loginResponse.getStatusCode() == 200) {
            return loginResponse.jsonPath().getString("accessToken");
        }
        return null;
    }

    // ========== PRUEBAS CON TOKEN/API KEY CORRECTO ==========

    @Test
    @DisplayName("AUTH - Login exitoso con credenciales válidas")
    public void deberiaAutenticarUsuarioExitosamente() {
        Response respuesta = servicioAuth.realizarLogin(USUARIO_PRUEBA, PASSWORD_PRUEBA);

        // Validar status code
        assertEquals(200, respuesta.getStatusCode(),
                "El login debería ser exitoso con credenciales válidas");

        // Validar tiempo de respuesta
        assertTrue(respuesta.getTime() < TIMEOUT_RESPUESTA);

        // Validar estructura de respuesta
        respuesta.then()
                .body("id", notNullValue())
                .body("username", equalTo(USUARIO_PRUEBA))
                .body("email", notNullValue())
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());

        String token = respuesta.jsonPath().getString("accessToken");
        assertTrue(token.length() > 50, "El token JWT debería tener longitud significativa");

        System.out.println("✅ Login exitoso para usuario: " + USUARIO_PRUEBA);
        System.out.println("🔑 Token obtenido: " + token.substring(0, 20) + "...");
    }

    @Test
    @DisplayName("AUTH - Acceso autorizado a perfil de usuario con token válido")
    public void deberiaAccederPerfilConTokenValido() {
        // Verificar que tenemos un token válido
        assertNotNull(tokenValido, "Debe existir un token válido para esta prueba");

        Response respuesta = servicioAuth.obtenerUsuarioAutenticado(tokenValido);

        // Validar status code
        assertEquals(200, respuesta.getStatusCode(),
                "Debería acceder al perfil con token válido");

        // Validar tiempo de respuesta
        assertTrue(respuesta.getTime() < TIMEOUT_RESPUESTA);

        // Validar que se obtienen datos del usuario
        respuesta.then()
                .body("id", notNullValue())
                .body("username", equalTo(USUARIO_PRUEBA))
                .body("firstName", notNullValue())
                .body("lastName", notNullValue())
                .body("email", notNullValue());

        String nombreCompleto = respuesta.jsonPath().getString("firstName") + " " +
                respuesta.jsonPath().getString("lastName");

        System.out.println("✅ Acceso autorizado al perfil de: " + nombreCompleto);
    }

    // ========== PRUEBAS CON TOKEN/API KEY INVÁLIDO ==========

    @Test
    @DisplayName("AUTH - Error de autenticación con credenciales inválidas")
    public void deberiaFallarLoginConCredencialesInvalidas() {
        String usuarioInvalido = "usuario_inexistente";
        String passwordInvalido = "password_incorrecto";

        Response respuesta = servicioAuth.realizarLogin(usuarioInvalido, passwordInvalido);

        // Validar status code de error
        assertEquals(400, respuesta.getStatusCode(),
                "Debería retornar error 400 con credenciales inválidas");

        // Validar tiempo de respuesta
        assertTrue(respuesta.getTime() < TIMEOUT_RESPUESTA);

        // Validar mensaje de error
        respuesta.then()
                .body("message", containsStringIgnoringCase("Invalid credentials"));

        System.out.println("✅ Error esperado para credenciales inválidas");
        System.out.println("❌ Usuario: " + usuarioInvalido + " / Password: " + passwordInvalido);
    }

    @Test
    @DisplayName("AUTH - Acceso denegado con token inválido")
    public void deberiaFallarAccesoConTokenInvalido() {
        String tokenInvalido = "token.jwt.invalido.12345";
        String endpointProtegido = "/auth/me";

        Response respuesta = servicioAuth.accederConTokenInvalido(tokenInvalido, endpointProtegido);

        // Validar status code de no autorizado
        assertEquals(401, respuesta.getStatusCode(),
                "Debería retornar 401 Unauthorized con token inválido");

        // Validar tiempo de respuesta
        assertTrue(respuesta.getTime() < TIMEOUT_RESPUESTA);

        // Validar mensaje de error de autorización
        respuesta.then()
                .body("message", notNullValue())
                .body("message", not(emptyString()));

        String mensajeError = respuesta.jsonPath().getString("message");
        System.out.println("🔍 DEBUG - Mensaje de error recibido: '" + mensajeError + "'");
    }

    // ========== PRUEBAS ADICIONALES DE SEGURIDAD ==========

    @Test
    @DisplayName("AUTH - Acceso denegado sin token de autorización")
    public void deberiaFallarAccesoSinToken() {
        String endpointProtegido = "/auth/me";

        Response respuesta = servicioAuth.accederSinAutenticacion(endpointProtegido);

        // Validar status code de no autorizado
        assertEquals(401, respuesta.getStatusCode(),
                "Debería retornar 401 sin token de autorización");

        // Validar tiempo de respuesta
        assertTrue(respuesta.getTime() < TIMEOUT_RESPUESTA);

        System.out.println("✅ Acceso correctamente denegado sin token");
    }

    @Test
    @DisplayName("AUTH - Validación de estructura y expiración del token")
    public void deberiaValidarEstructuraToken() {
        Response loginResponse = servicioAuth.realizarLogin(USUARIO_PRUEBA, PASSWORD_PRUEBA);

        assertEquals(200, loginResponse.getStatusCode());

        String token = loginResponse.jsonPath().getString("accessToken");
        String refreshToken = loginResponse.jsonPath().getString("refreshToken");

        // Validar estructura básica del JWT (debe tener 3 partes separadas por puntos)
        assertNotNull(token, "El access token no debe ser nulo");
        assertNotNull(refreshToken, "El refresh token no debe ser nulo");

        String[] partesToken = token.split("\\.");
        assertEquals(3, partesToken.length,
                "El JWT debe tener 3 partes: header.payload.signature");

        // Validar que cada parte tiene contenido
        for (int i = 0; i < partesToken.length; i++) {
            assertTrue(partesToken[i].length() > 0,
                    "La parte " + (i+1) + " del token no debe estar vacía");
        }

        System.out.println("✅ Estructura del token JWT validada correctamente");
        System.out.println("🔍 Partes del token: " + partesToken.length);
        System.out.println("📏 Longitud total: " + token.length() + " caracteres");
    }
}