# Pruebas Automatizadas API MediPlus

## Autores
- Antonio B. Arriagada LL. (anarriag@gmail.com)
- Dante Escalona Bustos (Jacobo.bustos.22@gmail.com)
- Roberto Rivas Lopez (umancl@gmail.com)

## Descripción
Proyecto de automatización de pruebas para la API REST de MediPlus utilizando DummyJSON como backend de pruebas.

## Tecnologías
- Java 21
- Maven 3.9.10
- REST Assured 5.4.0
- JUnit 5
- JMeter 5.6.3 (para pruebas de rendimiento)

## API de Pruebas
- **Backend**: DummyJSON (https://dummyjson.com)
- **Base URL**: https://dummyjson.com
- **Documentación**: https://dummyjson.com/docs

## Estructura del Proyecto
```
src/
├── test/
│   └── java/
│       └── com/
│           └── mediplus/
│               └── pruebas/
│                   ├── configuracion/
│                   │   └── ConfiguracionBase.java
│                   ├── modelos/
│                   │   ├── Paciente.java
│                   │   ├── Cita.java
│                   │   └── Reporte.java
│                   ├── servicios/
│                   │   ├── ServicioPacientes.java
│                   │   ├── ServicioCitas.java
│                   │   └── ServicioAutenticacion.java
│                   └── casos/
│                       ├── PruebasPacientesTest.java
│                       ├── PruebasCitasTest.java
│                       └── PruebasSeguridadTest.java
│   └── resources/
│       ├── configuracion.properties
│       └── datos-prueba.json
├── documentacion/
│   ├── tabla-endpoints.md
│   ├── casos-prueba.md
│   └── documentacion-seguridad.md
```

## Instalación y Ejecución

### Prerrequisitos
- Java 21
- Maven 3.9.10

### Pasos
1. Clonar el repositorio
```bash
git clone <url-repositorio>
cd pruebas-api-mediplus
```

2. Compilar el proyecto
```bash
mvn clean compile
```

3. Ejecutar todas las pruebas
```bash
mvn test
```

4. Ejecutar pruebas específicas
```bash
mvn test -Dtest=PruebasPacientesTest
mvn test -Dtest=PruebasCitasTest
mvn test -Dtest=PruebasSeguridadTest
```

## Configuración
- **URL Base**: https://dummyjson.com
- **Timeout**: 10 segundos
- **Usuario de prueba**: emilys / emilyspass
- **Archivo de configuración**: `src/test/resources/configuracion.properties`

## Mapeo de Endpoints

| Funcionalidad MediPlus | Endpoint DummyJSON | Descripción |
|------------------------|-------------------|-------------|
| Gestión Pacientes | /users | CRUD de usuarios como pacientes |
| Gestión Citas | /posts | Posts como citas médicas |
| Reportes | /products | Products como reportes |
| Autenticación | /auth/login | Login con JWT |

## Estado del Proyecto

### Lección 1 ✅ - Exploración y documentación
- [x] Tabla con 8 endpoints documentados
- [x] Proyecto Java base creado
- [x] README con pasos para ejecutar
- [x] Configuración Maven completa

### Lección 2 ✅ - Validación funcional automatizada
- [x] 6 pruebas automatizadas implementadas:
    - **2 GET**: obtener todos los pacientes, obtener paciente por ID
    - **2 POST**: crear nuevo paciente, agendar nueva cita
    - **1 PUT**: actualizar datos de paciente existente
    - **1 DELETE**: eliminar paciente
- [x] Validación de status code, body y tiempo de respuesta en todas
- [x] 2 pruebas negativas: ID inexistente, datos inválidos
- [x] Modelos de datos con principios SOLID
- [x] Servicios con separación de responsabilidades

### Lección 3 ✅ - Seguridad y autenticación
- [x] **2 pruebas simulando token/API key correcto**:
  - **Login exitoso con credenciales válidas** (`pruebaLoginExitoso()`): Usuario `emilys` con password `emilyspass` retorna status 200, token JWT válido con estructura header.payload.signature
  - **Acceso autorizado a perfil con token válido** (`pruebaAccesoAutorizado()`): Endpoint `/auth/me` con header `Authorization: Bearer {token}` retorna status 200 y datos completos del usuario
- [x] **2 pruebas con token/API key inválido**:
  - **Error de autenticación con credenciales inválidas** (`pruebaLoginFallido()`): Usuario inexistente/password incorrecto retorna status 400 con mensaje "Invalid credentials"
  - **Acceso denegado con token inválido** (`pruebaAccesoDenegado()`): Token malformado `token.jwt.invalido.12345` en `/auth/me` retorna status 401 con mensaje "Invalid token"
- [x] **Documentación sobre método simulado de seguridad** (`DocumentacionSeguridad.md`):
  - **Autenticación JWT con DummyJSON**: Implementación real de tokens JWT que se validan correctamente
  - **Flujo completo**: POST `/auth/login` → Obtener token → Usar `Authorization: Bearer {token}` → Acceso a recursos protegidos
  - **Casos documentados**: 6 pruebas implementadas con validaciones específicas (estructura JWT, códigos HTTP, tiempo respuesta)
  - **Configuración REST Assured**: Métodos helper para autenticación y especificaciones de request
  - **Usuarios de prueba**: 3 usuarios documentados para testing (emilys, michaelw, sophiab)

## Casos de Prueba Implementados

### Pruebas Funcionales (8 casos)
| ID | Caso de Prueba | Método | Endpoint | Status |
|----|----------------|--------|----------|--------|
| TC-001 | Obtener todos los pacientes | GET | `/users` | 200 |
| TC-002 | Obtener paciente por ID | GET | `/users/{id}` | 200 |
| TC-003 | Crear nuevo paciente | POST | `/users/add` | 201 |
| TC-004 | Agendar nueva cita | POST | `/posts/add` | 201 |
| TC-005 | Actualizar paciente | PUT | `/users/{id}` | 200 |
| TC-006 | Eliminar paciente | DELETE | `/users/{id}` | 200 |
| TC-007 | Error paciente inexistente | GET | `/users/99999` | 404 |
| TC-008 | Error datos inválidos | POST | `/users/add` | 400 |

### Pruebas de Seguridad (6 casos)
| ID | Caso de Prueba | Escenario | Validaciones | Status |
|----|----------------|-----------|--------------|--------|
| TS-001 | Login exitoso | emilys/emilyspass | Status 200, estructura JWT, datos usuario | ✅ |
| TS-002 | Acceso autorizado | Bearer token válido a /auth/me | Status 200, información completa | ✅ |
| TS-003 | Login fallido | Credenciales incorrectas | Status 400, mensaje error | ✅ |
| TS-004 | Acceso denegado | Token inválido a /auth/me | Status 401, "Invalid token" | ✅ |
| TS-005 | Renovación token | Refresh token válido | Status 200, nuevos tokens | ✅ |
| TS-006 | Sin autorización | Request sin header Authorization | Status 401, acceso denegado | ✅ |

## Validaciones Implementadas

### Validaciones de Seguridad
- ✅ **JWT Auténticos**: DummyJSON genera y valida tokens JWT reales con estructura correcta
- ✅ **Bearer Authentication**: Header `Authorization: Bearer {token}` implementado según estándares
- ✅ **Códigos HTTP precisos**: 200 (éxito), 400 (credenciales inválidas), 401 (sin autorización)
- ✅ **Validación estructura JWT**: Verificación de 3 partes (header.payload.signature)
- ✅ **Tiempo de respuesta**: Todas las pruebas bajo 3 segundos (login < 3s, acceso < 2s)
- ✅ **Configuración modular**: Clase base con métodos helper para autenticación
- ✅ **Casos negativos**: Manejo apropiado de errores de autenticación y autorización

## Métricas de Aceptación - Lección 3

### Cumplimiento de Requerimientos
| Requerimiento | Mínimo | Implementado | Estado |
|---------------|--------|--------------|--------|
| Pruebas token correcto | 2 | 2 | ✅ 100% |
| Pruebas token inválido | 2 | 2 | ✅ 100% |
| Documentación seguridad | 1 | 1 completa | ✅ 100% |

**Estado Lección 3**: ✅ **COMPLETADA - TODOS LOS REQUISITOS CUMPLIDOS**

## Próximos Pasos

### Lección 4: Pruebas de rendimiento con JMeter
- [ ] 3 escenarios de prueba (GET masivo, POST masivo, GET+POST combinado)
- [ ] Carga simulada: 10, 50, 100 usuarios concurrentes
- [ ] Duración mínima: 1 minuto por prueba

### Lección 5: Análisis de métricas
- [ ] Comparación entre 3 ejecuciones
- [ ] Análisis métricas clave: tiempo promedio, p90, p95, throughput, tasa error
- [ ] 2 gráficas generadas
- [ ] Al menos 2 recomendaciones justificadas