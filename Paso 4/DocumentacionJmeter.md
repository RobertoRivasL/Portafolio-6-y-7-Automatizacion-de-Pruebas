# Documentación JMeter - Pruebas de Rendimiento MediPlus

## Configuración de Pruebas

### Herramientas Utilizadas
- **JMeter**: 5.6.3
- **API Objetivo**: DummyJSON (https://dummyjson.com)
- **Duración por prueba**: 60 segundos mínimo
- **Configuraciones de carga**: 10, 50, 100 usuarios concurrentes

## Escenarios de Prueba Implementados

### 1. GET Masivo (`pruebas-get-masivo.jmx`)
**Objetivo**: Evaluar rendimiento en operaciones de lectura intensiva

**Operaciones incluidas**:
- Obtener lista de pacientes con paginación
- Consultar paciente específico por ID (aleatorio 1-30)
- Validaciones de respuesta (status 200, tiempo < 5 segundos)

**Configuraciones**:
- **10 usuarios**: Ramp-up 10 segundos
- **50 usuarios**: Ramp-up 15 segundos
- **100 usuarios**: Ramp-up 30 segundos

**Métricas a evaluar**:
- Tiempo de respuesta promedio
- Throughput (peticiones/segundo)
- Percentiles 90, 95, 99
- Tasa de error

### 2. POST Masivo (`pruebas-post-masivo.jmx`)
**Objetivo**: Evaluar rendimiento en operaciones de escritura intensiva

**Operaciones incluidas**:
- Crear nuevos pacientes con datos del CSV
- Agendar citas médicas
- Validaciones de creación (status 201, contenido JSON)

**Datos de prueba**:
- Archivo CSV con 30 pacientes chilenos
- Datos aleatorios generados (edad, género, especialidades)
- Teléfonos formato +569XXXXXXXX

**Validaciones**:
- Status code 201 para creaciones
- Estructura JSON correcta en respuesta
- Tiempo máximo 10 segundos por operación

### 3. Flujo Combinado (`pruebas-combinadas.jmx`)
**Objetivo**: Simular uso realista del sistema (70% lecturas, 30% escrituras)

**Distribución de operaciones**:
- **70% Lecturas (GET)**:
    - Lista de pacientes con parámetros
    - Paciente específico por ID
    - Lista de citas médicas
- **30% Escrituras (POST)**:
    - Nuevo paciente
    - Agendar cita médica

**Controladores de throughput**:
- ThroughputController para simular distribución realista
- Pausas aleatorias (2-5 segundos) simulando "tiempo de pensamiento"

## Configuración Técnica

### Variables Globales
```
URL_BASE = https://dummyjson.com
DURACION_SEGUNDOS = 60
PORCENTAJE_LECTURAS = 70
```

### Headers HTTP Estándar
```
Content-Type: application/json
Accept: application/json
```

### Assertions Configuradas
- **Response Code**: 200 (GET), 201 (POST)
- **Response Time**: < 5 segundos (GET), < 10 segundos (POST)
- **Content Validation**: Verificación de campos JSON requeridos

## Listeners y Reportes

### Listeners Incluidos
1. **Summary Report**: Resumen estadístico general
2. **View Results Tree**: Detalles de requests/responses individuales
3. **Aggregate Report**: Métricas agregadas por operación
4. **Graph Results**: Visualización gráfica de rendimiento

### Archivos de Salida
- `resultados_get_10_usuarios.jtl`
- `resultados_post_10_usuarios.jtl`
- `resultados_combinado_10_usuarios.jtl`

## Ejecución de Pruebas

### Línea de Comandos
```bash
# GET Masivo - 10 usuarios
jmeter -n -t pruebas-get-masivo.jmx -l resultados_get_10.jtl

# POST Masivo - 50 usuarios (modificar ThreadGroup)
jmeter -n -t pruebas-post-masivo.jmx -l resultados_post_50.jtl

# Combinado - 100 usuarios
jmeter -n -t pruebas-combinadas.jmx -l resultados_combinado_100.jtl
```

### Interfaz Gráfica
1. Abrir JMeter GUI
2. File → Open → Seleccionar script .jmx
3. Habilitar ThreadGroup deseado (10/50/100 usuarios)
4. Run → Start (Ctrl+R)

## Matriz de Escenarios

| Escenario | 10 Usuarios | 50 Usuarios | 100 Usuarios |
|-----------|-------------|-------------|---------------|
| **GET Masivo** | ✅ Habilitado | ✅ Configurado | ✅ Configurado |
| **POST Masivo** | ✅ Habilitado | ✅ Configurado | ✅ Configurado |
| **Combinado** | ✅ Habilitado | ✅ Configurado | ✅ Configurado |

**Total**: 9 configuraciones de prueba (3 escenarios × 3 cargas)

## Métricas Clave a Analizar

### Tiempos de Respuesta
- **Promedio**: Tiempo medio de todas las peticiones
- **Mediana**: Percentil 50
- **P90**: 90% de peticiones por debajo de este tiempo
- **P95**: 95% de peticiones por debajo de este tiempo
- **P99**: 99% de peticiones por debajo de este tiempo

### Throughput
- **Peticiones/segundo**: Capacidad de procesamiento
- **KB/segundo**: Ancho de banda utilizado

### Errores
- **Tasa de error**: Porcentaje de peticiones fallidas
- **Tipos de error**: 4xx, 5xx, timeouts

### Recursos del Sistema
- **Tiempo de conexión**: Latencia de red
- **Tiempo de latencia**: Tiempo hasta primer byte

## Consideraciones Especiales

### DummyJSON Limitations
- API simulada, no refleja rendimiento de servidor real
- Útil para validar patron de carga y estructura de pruebas
- Los resultados son indicativos para testing, no benchmarking absoluto

### Ambiente de Pruebas
- **Red**: Internet pública (variable)
- **CDN**: DummyJSON puede usar CDN
- **Rate Limiting**: Posibles limitaciones no documentadas

### Buenas Prácticas Implementadas
- Ramp-up gradual para evitar shock al sistema
- Pausas realistas entre operaciones
- Validaciones exhaustivas de respuesta
- Datos de prueba variados y realistas
- Archivos de resultados para análisis posterior