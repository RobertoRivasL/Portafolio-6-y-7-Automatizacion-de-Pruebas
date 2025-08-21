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
├── jmeter-scripts/
│   ├── pruebas-get-masivo.jmx
│   ├── pruebas-post-masivo.jmx
│   ├── pruebas-combinadas.jmx
│   └── datos-pacientes.csv
├── documentacion/
│   ├── tabla-endpoints.md
│   ├── casos-prueba.md
│   ├── documentacion-seguridad.md
│   └── documentacion-jmeter.md
└── resultados-jmeter/
    ├── resultados-get-10.jtl
    ├── resultados-post-50.jtl
    └── resultados-combinado-100.jtl
```

## Instalación y Ejecución

### Prerrequisitos
- Java 21
- Maven 3.9.10
- JMeter 5.6.3

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

3. Ejecutar pruebas funcionales
```bash
mvn test
```

4. Instalar datos CSV para JMeter
```bash
chmod +x instalar_csv_jmeter.sh
./instalar_csv_jmeter.sh
```

5. Ejecutar pruebas de rendimiento JMeter
```bash
# Interfaz gráfica
jmeter

# Línea de comandos - GET Masivo
jmeter -n -t jmeter-scripts/pruebas-get-masivo.jmx -l resultados-jmeter/resultados-get-10.jtl

# Línea de comandos - POST Masivo
jmeter -n -t jmeter-scripts/pruebas-post-masivo.jmx -l resultados-jmeter/resultados-post-50.jtl

# Línea de comandos - Combinado
jmeter -n -t jmeter-scripts/pruebas-combinadas.jmx -l resultados-jmeter/resultados-combinado-100.jtl
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
    - Login exitoso con credenciales válidas
    - Acceso autorizado a perfil con token válido
- [x] **2 pruebas con token/API key inválido**:
    - Error de autenticación con credenciales inválidas
    - Acceso denegado con token inválido
- [x] **Documentación sobre método simulado de seguridad**:
    - Implementación JWT completa con DummyJSON
    - Códigos de respuesta apropiados (200, 400, 401)
    - Usuario de prueba documentado

### Lección 4 ✅ - Pruebas de rendimiento con JMeter
- [x] **3 escenarios de prueba implementados**:
    - **GET masivo**: Operaciones de lectura intensiva
        - Obtener lista de pacientes con paginación
        - Consultar paciente específico por ID aleatorio
        - Validaciones: status 200, tiempo < 5 segundos
    - **POST masivo**: Operaciones de escritura intensiva
        - Crear nuevos pacientes con datos del CSV
        - Agendar citas médicas con datos aleatorios
        - Validaciones: status 201, contenido JSON correcto
    - **GET+POST combinado**: Flujo realista (70% lectura, 30% escritura)
        - ThroughputController para distribución proporcional
        - Pausas aleatorias simulando comportamiento humano
        - Operaciones mixtas: consultas + creaciones
- [x] **Carga simulada con 3 configuraciones**: 10, 50 y 100 usuarios concurrentes
    - 10 usuarios: Ramp-up 10 segundos (carga baja)
    - 50 usuarios: Ramp-up 15 segundos (carga media)
    - 100 usuarios: Ramp-up 30 segundos (carga alta)
- [x] **Duración mínima por prueba**: 60 segundos configurados en todos los escenarios
- [x] **Archivo .jmx bien organizado**:
    - Variables globales parametrizadas
    - Headers HTTP estándar (Content-Type, Accept)
    - Assertions de validación automática
    - Listeners para análisis (Summary Report, Graph Results)
    - CSV Data Set con 30 pacientes chilenos
    - Timers para pausas realistas

## Configuración JMeter Implementada

### Escenarios de Rendimiento

#### 1. GET Masivo (`pruebas-get-masivo.jmx`)
- **Objetivo**: Evaluar rendimiento en operaciones de lectura intensiva
- **Operaciones**:
    - Obtener lista de pacientes (con limit/skip)
    - Consultar paciente por ID aleatorio (1-30)
- **Validaciones**: Status 200, tiempo máximo 5 segundos
- **Configuraciones**: ThreadGroups para 10, 50, 100 usuarios

#### 2. POST Masivo (`pruebas-post-masivo.jmx`)
- **Objetivo**: Evaluar rendimiento en operaciones de escritura intensiva
- **Operaciones**:
    - Crear pacientes con datos del CSV
    - Agendar citas con especialidades aleatorias
- **Validaciones**: Status 201, estructura JSON válida
- **Datos**: CSV con 30 pacientes chilenos (nombres, emails +56)

#### 3. GET+POST Combinado (`pruebas-combinadas.jmx`)
- **Objetivo**: Simular uso realista del sistema
- **Distribución**: 70% operaciones GET, 30% operaciones POST
- **Controladores**: ThroughputController para distribución exacta
- **Timers**: UniformRandomTimer (2-5 segundos) simulando "tiempo de pensamiento"

### Datos de Prueba
**Archivo**: `datos-pacientes.csv` (30 registros)
```csv
nombre,apellido,email,telefono
Carlos,González,carlos.gonzalez@mediplus.test,+56987654321
María,Rodríguez,maria.rodriguez@mediplus.test,+56976543210
...
```

### Listeners Configurados
- **Summary Report**: Estadísticas generales
- **View Results Tree**: Detalles de requests/responses
- **Aggregate Report**: Métricas agregadas
- **Graph Results**: Visualización gráfica

## Métricas de Aceptación - Lección 4

### Cumplimiento de Requerimientos
| Requerimiento | Mínimo | Implementado | Estado |
|---------------|--------|--------------|--------|
| Escenarios de prueba | 3 | 3 (GET, POST, Combinado) | ✅ 100% |
| Configuraciones de carga | 3 | 9 (3 escenarios × 3 cargas) | ✅ 300% |
| Duración por prueba | 60 seg | 60 seg | ✅ 100% |
| Archivo .jmx organizado | 1 | 3 archivos profesionales | ✅ 300% |

### Matriz de Configuraciones Implementadas
| Escenario | 10 Usuarios | 50 Usuarios | 100 Usuarios |
|-----------|-------------|-------------|---------------|
| **GET Masivo** | ✅ Implementado | ✅ Implementado | ✅ Implementado |
| **POST Masivo** | ✅ Implementado | ✅ Implementado | ✅ Implementado |
| **Combinado 70/30** | ✅ Implementado | ✅ Implementado | ✅ Implementado |

**Total**: 9 configuraciones de prueba disponibles

## Ejecución de Pruebas de Rendimiento

### Preparación
```bash
# 1. Instalar CSV de datos
./instalar_csv_jmeter.sh

# 2. Verificar JMeter
jmeter -v

# 3. Verificar estructura
ls jmeter-scripts/
```

### Ejecución Individual
```bash
# GET Masivo - 10 usuarios
jmeter -n -t jmeter-scripts/pruebas-get-masivo.jmx \
       -l resultados-jmeter/get-10-usuarios.jtl

# POST Masivo - 50 usuarios  
jmeter -n -t jmeter-scripts/pruebas-post-masivo.jmx \
       -l resultados-jmeter/post-50-usuarios.jtl

# Combinado - 100 usuarios
jmeter -n -t jmeter-scripts/pruebas-combinadas.jmx \
       -l resultados-jmeter/combinado-100-usuarios.jtl
```

### Interpretación de Resultados
Los archivos `.jtl` generados contienen métricas clave:
- **Tiempo de respuesta** (promedio, P90, P95, P99)
- **Throughput** (peticiones por segundo)
- **Tasa de error** (porcentaje de fallos)
- **Latencia** (tiempo hasta primer byte)

**Estado Lección 4**: ✅ **COMPLETADA - TODOS LOS REQUISITOS CUMPLIDOS**

## Próximos Pasos

### Lección 5: Análisis de métricas
- [ ] Comparación entre al menos 3 ejecuciones
- [ ] Análisis de métricas clave: tiempo promedio, p90, p95, throughput, tasa de error
- [ ] 2 gráficas generadas
- [ ] Al menos 2 recomendaciones de mejora justificadas