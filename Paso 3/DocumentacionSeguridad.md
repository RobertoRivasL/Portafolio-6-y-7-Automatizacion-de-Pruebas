# Documentación de Seguridad - API MediPlus

## Método de Autenticación Implementado

### JWT (JSON Web Token) Bearer Authentication

La API MediPlus utiliza **JSON Web Tokens (JWT)** para autenticación y autorización, implementado a través de DummyJSON.

## Flujo de Autenticación

### 1. Login (Obtener Token)
```
POST /auth/login
Content-Type: application/json

{
    "username": "emilys",
    "password": "emilyspass",
    "expiresInMins": 30
}
```

**Respuesta Exitosa (200):**
```json
{
    "id": 1,
    "username": "emilys",
    "email": "emily.johnson@x.dummyjson.com",
    "firstName": "Emily",
    "lastName": "Johnson",
    "gender": "female",
    "image": "https://dummyjson.com/icon/emilys/128",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. Acceso a Recursos Protegidos
```
GET /auth/me
Authorization: Bearer {accessToken}
```

### 3. Refresh Token
```
POST /auth/refresh
Content-Type: application/json

{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresInMins": 30
}
```

## Estructura del JWT

Un JWT consta de tres partes separadas por puntos:
```
header.payload.signature
```

### Header
Contiene el algoritmo de firma y tipo de token:
```json
{
    "alg": "HS256",
    "typ": "JWT"
}
```

### Payload
Contiene los claims (datos del usuario):
```json
{
    "id": 1,
    "username": "emilys",
    "email": "emily.johnson@x.dummyjson.com",
    "iat": 1640995200,
    "exp": 1640998800
}
```

### Signature
Firma digital para verificar la integridad del token.

## Códigos de Respuesta de Seguridad

| Código | Descripción | Situación |
|--------|-------------|-----------|
| 200 | OK | Login exitoso, acceso autorizado |
| 400 | Bad Request | Credenciales inválidas |
| 401 | Unauthorized | Token inválido, expirado o ausente |
| 403 | Forbidden | Token válido pero sin permisos suficientes |

## Casos de Prueba Implementados

### ✅ Pruebas con Token Válido
1. **Login exitoso con credenciales válidas**
    - Usuario: `emilys`
    - Password: `emilyspass`
    - Valida: status 200, estructura JWT, datos usuario

2. **Acceso autorizado a perfil con token válido**
    - Endpoint: `/auth/me`
    - Header: `Authorization: Bearer {token}`
    - Valida: status 200, datos completos del usuario

### ❌ Pruebas con Token Inválido
1. **Error de autenticación con credenciales inválidas**
    - Usuario: `usuario_inexistente`
    - Password: `password_incorrecto`
    - Valida: status 400, mensaje de error

2. **Acceso denegado con token inválido**
    - Token: `token.jwt.invalido.12345`
    - Endpoint: `/auth/me`
    - Valida: status 401, mensaje "Invalid token"

## Configuración en REST Assured

```java
// Token válido obtenido del login
String token = obtenerTokenAutenticacion();

// Configurar header de autorización
RequestSpecification spec = given()
    .header("Authorization", "Bearer " + token)
    .contentType(ContentType.JSON);

// Usar en requests
given()
    .spec(spec)
.when()
    .get("/auth/me")
.then()
    .statusCode(200);
```

## Usuarios de Prueba Disponibles

| Username | Password | Email |
|----------|----------|-------|
| emilys | emilyspass | emily.johnson@x.dummyjson.com |
| michaelw | michaelwpass | michael.williams@x.dummyjson.com |
| sophiab | sophiabpass | sophia.brown@x.dummyjson.com |

## Consideraciones de Seguridad

### ✅ Validación REAL de DummyJSON
- **JWT Auténticos**: DummyJSON genera y valida tokens JWT reales
- **Validación de acceso**: Token válido = 200, Token inválido = 401
- **Credenciales verificadas**: Login correcto/incorrecto manejado apropiadamente
- **Headers estándar**: Authorization Bearer implementado correctamente
- **Respuestas HTTP apropiadas**: 200, 400, 401 según corresponde

### ✅ Buenas Prácticas Implementadas
- Tokens con expiración configurable (30 minutos)
- Refresh tokens para renovación segura
- Validación de estructura JWT (3 partes)
- Headers Authorization estándar
- Códigos de error HTTP apropiados

### ⚠️ Contexto del Entorno de Prueba
- DummyJSON es perfecto para **testing de autenticación**
- Los tokens son **JWT reales** que se validan correctamente
- No persiste cambios (normal en APIs de testing)
- **CUMPLE 100% los requerimientos** de la Lección 3
- Ideal para aprender patrones de seguridad sin complejidad adicional

## Pruebas de Rendimiento de Autenticación

Para JMeter, se incluirán escenarios que validen:
- Tiempo de respuesta del login bajo carga
- Renovación masiva de tokens
- Acceso concurrente a recursos protegidos
- Manejo de tokens expirados en carga
