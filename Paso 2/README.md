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

5. Generar reporte de pruebas
```bash
mvn surefire-report:report
```

## Configuración
- **URL Base**: https://dummyjson.com
- **Timeout**: 10 segundos
- **Credenciales de prueba**: kminchelle / 0lelplR
- **Token de autenticación**: Se obtiene dinámicamente via `/auth/login`

## Mapeo de Endpoints

| Funcionalidad MediPlus | Endpoint DummyJSON | Descripción |
|------------------------|-------------------|-------------|
| Gestión Pacientes | /users | CRUD de usuarios como pacientes |
| Gestión Citas | /posts | Posts como citas médicas |
| Reportes | /products | Products como reportes |
| Autenticación | /auth/login | Login con JWT |

## Archivos Principales

### Configuración
- **ConfiguracionBase.java**: Configuración centralizada, manejo de timeouts y autenticación
- **pom.xml**: Dependencias Maven con REST Assured 5.4.0 y JUnit 5

### Modelos de Datos
- **Paciente.java**: Modelo para mapeo con `/users` de DummyJSON
- **Cita.java**: Modelo para mapeo con `/posts` de DummyJSON
- **Reporte.java**: Modelo para mapeo con `/products` de DummyJSON

### Servicios (Separación de Responsabilidades)
- **ServicioPacientes.java**: Operaciones CRUD para pacientes
- **ServicioCitas.java**: Gestión de citas médicas
- **ServicioAutenticacion.java**: Manejo de tokens y seguridad

### Casos de Prueba
- **PruebasPacientesTest.java**: 6 pruebas (2 GET, 2 POST, 1 PUT, 1 DELETE)
- **PruebasCitasTest.java**: 4 pruebas (1 GET, 1 POST, 1 PUT, 1 DELETE)
- **PruebasSeguridadTest.java**: 4+ pruebas (autenticación y casos negativos)

## Estado del Proyecto

### Lección 1 ✅ - Exploración y documentación
- [x] Tabla con 8 endpoints documentados
- [x] Proyecto Java base creado
- [x] README con pasos para ejecutar
- [x] Configuración Maven completa

### Lección 2 ✅ - Validación funcional automatizada
- [x] **12+ pruebas automatizadas implementadas (superando las 6 mínimas):**
    - **4 GET**: obtener pacientes, paciente por ID, citas, reportes
    - **4 POST**: crear paciente, agendar cita, crear reporte, autenticación
    - **2 PUT**: actualizar paciente, actualizar cita
    - **2 DELETE**: eliminar paciente, eliminar cita
- [x] **Validación completa en todas las pruebas:**
    - Status code (200, 201, 404, etc.)
    - Estructura del body JSON
    - Tiempo de respuesta (< 10 segundos)
    - Headers Content-Type
- [x] **4+ pruebas negativas implementadas:**
    - Buscar paciente con ID inexistente (404)
    - Crear paciente con datos inválidos (400)
    - Acceso sin autenticación (401)
    - Datos malformados en requests (400)
- [x] **Arquitectura con principios SOLID:**
    - Modelos de datos encapsulados
    - Servicios con responsabilidad única
    - Configuración base reutilizable
    - Separación clara de responsabilidades

### Próximo: Lección 3 - Seguridad y autenticación
- [ ] 2 pruebas con token válido
- [ ] 2 pruebas con token inválido
- [ ] Documentación de método de seguridad simulado