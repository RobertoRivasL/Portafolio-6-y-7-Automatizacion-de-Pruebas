# Casos de Prueba - API MediPlus

**Proyecto**: Pruebas Automatizadas API REST MediPlus  
**Autores**: Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez  
**Backend**: DummyJSON (https://dummyjson.com)

## 📋 Resumen de Casos Implementados

### Lección 2: Pruebas Funcionales (8 casos)
**Archivo**: `PruebasPacientesTest.java`, `PruebasCitasTest.java`

| ID | Caso de Prueba | Método | Endpoint | Status Esperado |
|----|----------------|--------|----------|-----------------|
| TC-001 | Obtener todos los pacientes | GET | `/users` | 200 |
| TC-002 | Obtener paciente por ID | GET | `/users/{id}` | 200 |
| TC-003 | Crear nuevo paciente | POST | `/users/add` | 201 |
| TC-004 | Agendar nueva cita | POST | `/posts/add` | 201 |
| TC-005 | Actualizar paciente existente | PUT | `/users/{id}` | 200 |
| TC-006 | Eliminar paciente | DELETE | `/users/{id}` | 200 |
| TC-007 | Error paciente inexistente | GET | `/users/99999` | 404 |
| TC-008 | Error datos inválidos | POST | `/users/add` | 400/201 |

### Lección 3: Pruebas de Seguridad (6 casos)
**Archivo**: `PruebasSeguridadTest.java`

| ID | Caso de Prueba | Escenario | Status Esperado |
|----|----------------|-----------|-----------------|
| TS-001 | Login exitoso | Token válido | 200 |
| TS-002 | Acceso autorizado | Bearer token correcto | 200 |
| TS-003 | Login fallido | Credenciales incorrectas | 400 |
| TS-004 | Acceso denegado | Token inválido | 401 |
| TS-005 | Sin autorización | Sin token | 401 |
| TS-006 | Validación estructura JWT | Token válido | Estructura correcta |

### Lección 4: Pruebas de Rendimiento (9 configuraciones)
**Archivos**: Scripts JMeter `.jmx`

| Escenario | 10 Usuarios | 50 Usuarios | 100 Usuarios |
|-----------|-------------|-------------|---------------|
| **GET Masivo** | ✅ | ✅ | ✅ |
| **POST Masivo** | ✅ | ✅ | ✅ |
| **Combinado (70/30)** | ✅ | ✅ | ✅ |

### Lección 5: Análisis de Métricas (5 casos)
**Archivo**: `PruebasRendimientoTest.java`

| ID | Caso de Análisis | Descripción |
|----|------------------|-------------|
| TA-001 | Cargar 3 ejecuciones | Validar datos de resultados |
| TA-002 | Analizar métricas clave | P90, P95, P99, throughput, errores |
| TA-003 | Generar 2 gráficas | Tiempos + Throughput vs Error |
| TA-004 | Crear recomendaciones | Mínimo 2 con justificación |
| TA-005 | Informe ejecutivo | Reporte final completo |

## 🎯 Validaciones Implementadas

### Validaciones Funcionales
- ✅ **Status Code**: 200, 201, 400, 401, 404
- ✅ **Tiempo de Respuesta**: < 10 segundos
- ✅ **Estructura JSON**: Campos requeridos presentes
- ✅ **Contenido**: Validación de datos específicos

### Validaciones de Seguridad
- ✅ **Autenticación JWT**: Login y acceso con token
- ✅ **Autorización**: Acceso denegado sin token
- ✅ **Estructura Token**: Header.Payload.Signature
- ✅ **Manejo de Errores**: Códigos HTTP apropiados

### Validaciones de Rendimiento
- ✅ **Carga Simulada**: 10, 50, 100 usuarios concurrentes
- ✅ **Duración**: 60 segundos por prueba
- ✅ **Distribución Realista**: 70% GET, 30% POST
- ✅ **Métricas**: Tiempo, throughput, tasa error

## 📊 Métricas de Aceptación

### Tiempos de Respuesta
| Operación | Target | Warning | Critical |
|-----------|--------|---------|----------|
| GET | < 500ms | 500-1000ms | > 1000ms |
| POST | < 1500ms | 1500-2500ms | > 2500ms |

### Escalabilidad
| Usuarios | Tasa Error Máxima | Throughput Mínimo |
|----------|-------------------|-------------------|
| 10 | < 1% | > 80 req/seg |
| 50 | < 3% | > 150 req/seg |
| 100 | < 5% | > 250 req/seg |

## 🛠️ Herramientas y Configuración

### Stack Tecnológico
- **Java**: 21
- **Maven**: 3.9.10
- **REST Assured**: 5.4.0
- **JUnit**: 5.10.1
- **JMeter**: 5.6.3

### Configuración de Ejecución
```bash
# Pruebas funcionales
mvn test -Dtest=PruebasPacientesTest

# Pruebas de seguridad  
mvn test -Dtest=PruebasSeguridadTest

# Análisis de rendimiento
mvn test -Dtest=PruebasRendimientoTest

# Todas las pruebas
mvn test
```

### Configuración JMeter
```bash
# GET Masivo
jmeter -n -t jmeter-scripts/pruebas-get-masivo.jmx -l resultados-get.jtl

# POST Masivo
jmeter -n -t jmeter-scripts/pruebas-post-masivo.jmx -l resultados-post.jtl

# Combinado
jmeter -n -t jmeter-scripts/pruebas-combinadas.jmx -l resultados-combinado.jtl
```

## 📈 Resultados Esperados

### Cobertura de Pruebas
- **Funcional**: 100% endpoints principales (CRUD)
- **Seguridad**: 100% flujos de autenticación/autorización
- **Rendimiento**: 100% escenarios de carga definidos
- **Análisis**: 100% métricas clave evaluadas

### Artefactos Generados
- ✅ **Reportes JUnit**: XML/HTML con resultados
- ✅ **Archivos JMeter**: .jtl con métricas de rendimiento
- ✅ **Gráficas**: 2 gráficas de análisis (Markdown)
- ✅ **Informe Final**: Reporte ejecutivo completo
- ✅ **Recomendaciones**: Mínimo 3 con justificación técnica

## 🔍 Trazabilidad

### Mapeo Requerimientos → Casos
| Requerimiento | Casos Implementados | Estado |
|---------------|---------------------|--------|
| 2 GET, 2 POST, 1 PUT, 1 DELETE | TC-001 a TC-006 | ✅ |
| 2 Pruebas negativas | TC-007, TC-008 | ✅ |
| 4 Pruebas seguridad | TS-001 a TS-006 | ✅ Superado |
| 3 Escenarios JMeter | 9 configuraciones | ✅ Superado |
| 3 Ejecuciones análisis | TA-001 a TA-005 | ✅ |
| 2 Gráficas | Tiempos + Throughput | ✅ |
| 2+ Recomendaciones | 3 recomendaciones | ✅ Superado |

**Estado General**: ✅ **COMPLETADO - TODAS LAS LECCIONES**