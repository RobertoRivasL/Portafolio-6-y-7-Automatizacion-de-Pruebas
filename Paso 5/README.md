# Lección 5: Análisis de Métricas - API REST MediPlus

## 📊 Análisis de Rendimiento y Métricas de Carga

### Autores
- **Antonio B. Arriagada LL.** - anarriag@gmail.com
- **Dante Escalona Bustos** - Jacobo.bustos.22@gmail.com
- **Roberto Rivas Lopez** - umancl@gmail.com

### Objetivo
Analizar las métricas de rendimiento obtenidas de las pruebas de carga realizadas con JMeter sobre la API REST de MediPlus, comparando resultados entre diferentes configuraciones de usuarios concurrentes y proporcionando recomendaciones de mejora.

---

## 🛠️ Tecnologías Utilizadas

- **Java 21** - Lenguaje de programación principal
- **Maven 3.9.10** - Gestión de dependencias y build
- **JMeter 5.6.3** - Herramienta de pruebas de rendimiento
- **JUnit 5** - Framework de pruebas unitarias
- **Jackson** - Procesamiento JSON
- **Apache Commons CSV** - Procesamiento de archivos CSV

---

## 🏗️ Estructura del Proyecto

```
leccion5-analisis-metricas/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/mediplus/analisis/
│   │   │       ├── EjecutorAnalisisMetricas.java
│   │   │       ├── configuracion/
│   │   │       │   └── ConfiguracionAplicacion.java
│   │   │       ├── modelo/
│   │   │       │   └── MetricaRendimiento.java
│   │   │       └── servicio/
│   │   │           ├── AnalizadorMetricas.java
│   │   │           ├── CalculadorEstadisticas.java
│   │   │           ├── GeneradorReportes.java
│   │   │           └── LectorArchivosJTL.java
│   │   └── resources/
│   │       └── aplicacion.properties
│   └── test/
│       ├── java/
│       │   └── com/mediplus/analisis/test/
│       │       └── PruebasAnalizadorMetricas.java
│       └── resources/
│           └── logging.properties
├── resultados/
│   ├── get_masivo_10u.jtl
│   ├── get_masivo_50u.jtl
│   ├── get_masivo_100u.jtl
│   ├── post_masivo_10u.jtl
│   ├── post_masivo_50u.jtl
│   ├── post_masivo_100u.jtl
│   ├── mixto_10u.jtl
│   ├── mixto_50u.jtl
│   └── mixto_100u.jtl
├── reportes/
│   ├── reporte_completo.html
│   ├── metricas_detalladas.csv
│   └── graficas/
│       ├── grafica_tiempo_respuesta.txt
│       └── grafica_throughput.txt
├── jmeter-scripts/
│   ├── get_masivo.jmx
│   ├── post_masivo.jmx
│   └── mixto.jmx
├── pom.xml
└── README.md
```

---

## 🎯 Métricas Analizadas

### Configuraciones de Prueba Ejecutadas

| Escenario | Usuarios Concurrentes | Duración | Ramp-up Time | Iteraciones |
|-----------|----------------------|----------|--------------|-------------|
| GET Masivo | 10, 50, 100 | 60s | 10s | Variable |
| POST Masivo | 10, 50, 100 | 60s | 10s | Variable |
| GET+POST Combinado | 10, 50, 100 | 60s | 15s | Variable |

### Métricas Clave Evaluadas

1. **Tiempo de Respuesta Promedio** (ms)
2. **Percentil 90 (P90)** - 90% de requests bajo este tiempo
3. **Percentil 95 (P95)** - 95% de requests bajo este tiempo
4. **Throughput** (requests/segundo)
5. **Tasa de Error** (%)
6. **Tiempo Mínimo/Máximo** de respuesta

---

## 📈 Resultados Comparativos

### Escenario 1: GET Masivo - Consulta de Pacientes

| Usuarios | Tiempo Promedio (ms) | P90 (ms) | P95 (ms) | Throughput (req/s) | Tasa Error (%) |
|----------|---------------------|----------|----------|-------------------|----------------|
| 10       | 245                 | 380      | 420      | 38.5              | 0.0            |
| 50       | 890                 | 1,250    | 1,450    | 56.2              | 2.1            |
| 100      | 2,150               | 3,200    | 3,800    | 45.8              | 8.7            |

**Observaciones:**
- ✅ Rendimiento óptimo con 10 usuarios
- ⚠️ Degradación notable con 50 usuarios
- ❌ Tiempo inaceptable con 100 usuarios (>2s promedio)

### Escenario 2: POST Masivo - Registro de Pacientes

| Usuarios | Tiempo Promedio (ms) | P90 (ms) | P95 (ms) | Throughput (req/s) | Tasa Error (%) |
|----------|---------------------|----------|----------|-------------------|----------------|
| 10       | 380                 | 520      | 580      | 25.8              | 0.0            |
| 50       | 1,250               | 1,890    | 2,100    | 38.5              | 4.2            |
| 100      | 3,450               | 5,200    | 6,100    | 28.9              | 15.3           |

**Observaciones:**
- ✅ POST operations más costosas que GET (esperado)
- ⚠️ Tasa de error significativa con alta concurrencia
- ❌ Throughput no escala linealmente

### Escenario 3: GET+POST Combinado - Flujo Mixto

| Usuarios | Tiempo Promedio (ms) | P90 (ms) | P95 (ms) | Throughput (req/s) | Tasa Error (%) |
|----------|---------------------|----------|----------|-------------------|----------------|
| 10       | 315                 | 450      | 500      | 31.2              | 0.0            |
| 50       | 1,120               | 1,650    | 1,850    | 44.7              | 3.8            |
4,650    | 34.6              | 12.1           |

**Observaciones:**
- ✅ Comportamiento mixto entre GET y POST puros
- ⚠️ Mejor throughput que POST puro con 50 usuarios
- ❌ Degradación similar en alta concurrencia

---

## 📊 Gráficas de Análisis

### Gráfica 1: Tiempo de Respuesta vs Usuarios Concurrentes

```
Tiempo Promedio de Respuesta (ms)
4000 |                                    ●POST
     |                               ●
3500 |                          ●
     |                     ●
3000 |                ●    ●MIXTO
     |           ●
2500 |      ●
     | ●
2000 |      ●GET
     | ●
1500 |●
     |●
1000 |●
     |●
 500 |●
     |●●●
   0 +--+--+--+--+--+--+--+--+--+--+
     0 10 20 30 40 50 60 70 80 90 100
                Usuarios Concurrentes
```

### Gráfica 2: Throughput vs Usuarios Concurrentes

```
Throughput (requests/segundo)
60 |●GET
   |
55 |     ●GET
   |
50 |           
   |     ●MIXTO
45 |          ●GET
   |               ●MIXTO
40 |●POST
   |          ●POST
35 |               ●MIXTO
   |                    ●POST
30 |●MIXTO
   |
25 |●POST
   |
20 +--+--+--+--+--+--+--+--+--+--+
   0 10 20 30 40 50 60 70 80 90 100
            Usuarios Concurrentes
```

---

## 🔍 Análisis Detallado

### Comportamiento del Sistema

1. **Punto Dulce de Rendimiento**: Entre 10-20 usuarios concurrentes
2. **Degradación Crítica**: A partir de 50 usuarios concurrentes
3. **Umbral de Falla**: 100 usuarios concurrentes con >10% error rate

### Cuellos de Botella Identificados

1. **Base de Datos**: Probable saturación de conexiones
2. **Memoria**: Posible memory leak en operaciones POST
3. **CPU**: Picos durante validaciones de autenticación
4. **Red**: Timeout en conexiones con alta concurrencia

---

## 📋 Recomendaciones de Mejora

### Recomendación 1: Optimización de Base de Datos
**Justificación**: Los tiempos de respuesta P95 > 3000ms indican queries lentos o bloqueos de BD.

**Acciones propuestas:**
- Implementar connection pooling con HikariCP
- Optimizar queries con índices apropiados
- Configurar cache L2 (Redis/Hazelcast)
- Establecer timeout de conexión en 5 segundos máximo

**Impacto esperado**: Reducción 40-60% en tiempo de respuesta

### Recomendación 2: Implementación de Rate Limiting
**Justificación**: Tasa de error del 15.3% en POST con 100 usuarios indica falta de control de carga.

**Acciones propuestas:**
- Implementar rate limiting por IP (100 req/min)
- Circuit breaker pattern para proteger servicios downstream
- Queue asíncrono para operaciones POST pesadas
- Implementar graceful degradation

**Impacto esperado**: Reducción de errores a <2% bajo cualquier carga

### Recomendación 3: Escalado Horizontal
**Justificación**: Throughput no escala linealmente, sugiere límites de capacidad.

**Acciones propuestas:**
- Load balancer con sticky sessions
- Múltiples instancias de aplicación
- Separación read/write en base de datos
- Implementar health checks

**Impacto esperado**: Soporte para 200+ usuarios concurrentes

---

## 🚀 Instalación y Ejecución

### Prerrequisitos
- Java 21 o superior
- Maven 3.9.10 o superior
- JMeter 5.6.3 (para generar archivos JTL)

### Instalación

1. **Clonar el repositorio:**
```bash
git clone https://github.com/tu-usuario/analisis-metricas-mediplus.git
cd analisis-metricas-mediplus
```

2. **Compilar el proyecto:**
```bash
mvn clean compile
```

3. **Ejecutar pruebas:**
```bash
mvn test
```

4. **Generar JAR ejecutable:**
```bash
mvn clean package
```

### Ejecución

#### Opción 1: Usando Maven
```bash
mvn exec:java -Dexec.mainClass="com.mediplus.analisis.EjecutorAnalisisMetricas"
```

#### Opción 2: Usando JAR ejecutable
```bash
java -jar target/analisis-metricas-mediplus.jar
```

#### Opción 3: Con parámetros personalizados
```bash
java -jar target/analisis-metricas-mediplus.jar \
  --directorio-resultados=mis-resultados \
  --directorio-reportes=mis-reportes
```

---

## 📊 Generación de Datos de Prueba

### Ejecutar Pruebas JMeter

```bash
# Ejecutar escenario GET masivo
jmeter -n -t jmeter-scripts/get_masivo.jmx -l resultados/get_masivo_100u.jtl

# Ejecutar escenario POST masivo  
jmeter -n -t jmeter-scripts/post_masivo.jmx -l resultados/post_masivo_100u.jtl

# Ejecutar escenario mixto
jmeter -n -t jmeter-scripts/mixto.jmx -l resultados/mixto_100u.jtl
```

### Generar Reportes JMeter
```bash
# Generar reportes HTML de JMeter
jmeter -g resultados/get_masivo_100u.jtl -o reportes/jmeter/get_masivo_100u/
```

---

## 🧪 Ejecutar Pruebas del Proyecto

### Pruebas Unitarias
```bash
mvn test
```

### Pruebas con Cobertura
```bash
mvn clean test jacoco:report
```

### Análisis de Código
```bash
mvn clean compile -P analisis-codigo
```

### Generar Documentación
```bash
mvn javadoc:javadoc
```

---

## 📋 Configuración

### Archivo `aplicacion.properties`
```properties
# Directorios
directorio.resultados=resultados
directorio.reportes=reportes

# Configuración de análisis
umbral.error.critico=10.0
umbral.tiempo.critico=2000.0
timeout.lectura.segundos=30

# Configuración de reportes
formato.fecha=dd/MM/yyyy HH:mm
generar.graficas.ascii=true
nivel.log=INFO
```

### Variables de Entorno
```bash
export MEDIPLUS_RESULTADOS_DIR=/ruta/a/resultados
export MEDIPLUS_REPORTES_DIR=/ruta/a/reportes
export MEDIPLUS_LOG_LEVEL=DEBUG
```

---

## 📈 Interpretación de Resultados

### Niveles de Rendimiento

- **✅ Excelente**: < 500ms promedio, 0% errores
- **✅ Bueno**: 500-1000ms promedio, < 2% errores
- **⚠️ Regular**: 1000-2000ms promedio, < 5% errores
- **❌ Malo**: > 2000ms promedio o 5-10% errores
- **❌ Inaceptable**: > 10% errores

### Métricas Críticas

1. **P95 > 3000ms**: Indica problemas serios de rendimiento
2. **Tasa de error > 10%**: Sistema no apto para producción
3. **Throughput decreciente**: Problemas de escalabilidad
4. **Tiempo máximo > 10x promedio**: Posibles outliers o timeouts

---

## 🎯 Conclusiones

1. **La API de MediPlus tiene un rendimiento aceptable hasta 20 usuarios concurrentes**
2. **Se requiere optimización urgente para soportar cargas de producción**
3. **Las operaciones POST son 3x más costosas que las GET**
4. **El sistema no está preparado para escalar horizontalmente sin modificaciones**

### Próximos Pasos
1. Implementar las recomendaciones priorizadas
2. Ejecutar nuevas pruebas de regresión
3. Establecer monitoreo continuo en producción
4. Definir SLAs basados en estos resultados

---

## 🤝 Contribución

### Equipo de Desarrollo
Para contribuir al proyecto:

1. Fork del repositorio
2. Crear rama de feature: `git checkout -b feature/nueva-funcionalidad`
3. Commit cambios: `git commit -m 'Agregar nueva funcionalidad'`
4. Push a la rama: `git push origin feature/nueva-funcionalidad`
5. Crear Pull Request

### Estándares de Código
- Seguir principios SOLID
- Cobertura de pruebas > 80%
- Documentación Javadoc completa
- Nombres en español para clases y métodos de negocio

---

## 📞 Contacto del Equipo

Para consultas sobre este análisis:
- **Líder Técnico**: Antonio B. Arriagada LL. (anarriag@gmail.com)
- **Especialista Performance**: Dante Escalona Bustos (Jacobo.bustos.22@gmail.com)
- **Analista QA**: Roberto Rivas Lopez (umancl@gmail.com)

---

## 📄 Licencia

Este proyecto es parte del curso de Automatización de Pruebas y está desarrollado con fines educativos.

---

*Documento generado el: `date +"%d/%m/%Y %H:%M"`*  
*Versión: 1.0*  
*Estado: Revisión Final*4,650    | 34.6              | 12.1           |

**Observaciones:**
- ✅ Comportamiento mixto entre GET y POST puros
- ⚠️ Mejor throughput que POST puro con 50 usuarios
- ❌ Degradación similar en alta concurrencia

---

## 📊 Gráficas de Análisis

### Gráfica 1: Tiempo de Respuesta vs Usuarios Concurrentes

```
Tiempo Promedio de Respuesta (ms)
4000 |                                    ●POST
     |                               ●
3500 |                          ●
     |                     ●
3000 |                ●    ●MIXTO
     |           ●
2500 |      ●
     | ●
2000 |      ●GET
     | ●
1500 |●
     |●
1000 |●
     |●
 500 |●
     |●●●
   0 +--+--+--+--+--+--+--+--+--+--+
     0 10 20 30 40 50 60 70 80 90 100
                Usuarios Concurrentes
```

### Gráfica 2: Throughput vs Usuarios Concurrentes

```
Throughput (requests/segundo)
60 |●GET
   |
55 |     ●GET
   |
50 |           
   |     ●MIXTO
45 |          ●GET
   |               ●MIXTO
40 |●POST
   |          ●POST
35 |               ●MIXTO
   |                    ●POST
30 |●MIXTO
   |
25 |●POST
   |
20 +--+--+--+--+--+--+--+--+--+--+
   0 10 20 30 40 50 60 70 80 90 100
            Usuarios Concurrentes
```

---

## 🔍 Análisis Detallado

### Comportamiento del Sistema

1. **Punto Dulce de Rendimiento**: Entre 10-20 usuarios concurrentes
2. **Degradación Crítica**: A partir de 50 usuarios concurrentes
3. **Umbral de Falla**: 100 usuarios concurrentes con >10% error rate

### Cuellos de Botella Identificados

1. **Base de Datos**: Probable saturación de conexiones
2. **Memoria**: Posible memory leak en operaciones POST
3. **CPU**: Picos durante validaciones de autenticación
4. **Red**: Timeout en conexiones con alta concurrencia

---

## 📋 Recomendaciones de Mejora

### Recomendación 1: Optimización de Base de Datos
**Justificación**: Los tiempos de respuesta P95 > 3000ms indican queries lentos o bloqueos de BD.

**Acciones propuestas:**
- Implementar connection pooling con HikariCP
- Optimizar queries con índices apropiados
- Configurar cache L2 (Redis/Hazelcast)
- Establecer timeout de conexión en 5 segundos máximo

**Impacto esperado**: Reducción 40-60% en tiempo de respuesta

### Recomendación 2: Implementación de Rate Limiting
**Justificación**: Tasa de error del 15.3% en POST con 100 usuarios indica falta de control de carga.

**Acciones propuestas:**
- Implementar rate limiting por IP (100 req/min)
- Circuit breaker pattern para proteger servicios downstream
- Queue asíncrono para operaciones POST pesadas
- Implementar graceful degradation

**Impacto esperado**: Reducción de errores a <2% bajo cualquier carga

### Recomendación 3: Escalado Horizontal
**Justificación**: Throughput no escala linealmente, sugiere límites de capacidad.

**Acciones propuestas:**
- Load balancer con sticky sessions
- Múltiples instancias de aplicación
- Separación read/write en base de datos
- Implementar health checks

**Impacto esperado**: Soporte para 200+ usuarios concurrentes

---

## 🚀 Ejecución de Pruebas

### Prerrequisitos
- JMeter 5.6.3 instalado
- Java 21 configurado
- Scripts JMeter en `/jmeter-scripts/`

### Comandos de Ejecución

```bash
# Ejecutar escenario GET masivo
jmeter -n -t escenarios/get_masivo.jmx -l resultados/get_masivo_100u.jtl

# Ejecutar escenario POST masivo  
jmeter -n -t escenarios/post_masivo.jmx -l resultados/post_masivo_100u.jtl

# Ejecutar escenario mixto
jmeter -n -t escenarios/mixto.jmx -l resultados/mixto_100u.jtl

# Generar reportes HTML
jmeter -g resultados/get_masivo_100u.jtl -o reportes/get_masivo_100u/
```

### Análisis de Resultados

```bash
# Análisis con herramientas custom
mvn test -Dtest=AnalizadorMetricas
mvn exec:java -Dexec.mainClass="com.mediplus.analisis.GeneradorReportes"
```

---

## 📁 Estructura de Archivos

```
leccion5-analisis-metricas/
├── README.md
├── resultados/
│   ├── get_masivo_10u.jtl
│   ├── get_masivo_50u.jtl
│   ├── get_masivo_100u.jtl
│   ├── post_masivo_10u.jtl
│   ├── post_masivo_50u.jtl
│   ├── post_masivo_100u.jtl
│   ├── mixto_10u.jtl
│   ├── mixto_50u.jtl
│   └── mixto_100u.jtl
├── reportes/
│   ├── comparativo_final.html
│   ├── graficas/
│   └── csv_procesados/
├── scripts-analisis/
│   ├── AnalizadorMetricas.java
│   ├── GeneradorGraficas.java
│   └── ComparadorResultados.java
└── documentacion/
    ├── metricas_detalladas.xlsx
    ├── conclusiones.md
    └── recomendaciones_tecnicas.md
```

---

## 🎯 Conclusiones

1. **La API de MediPlus tiene un rendimiento aceptable hasta 20 usuarios concurrentes**
2. **Se requiere optimización urgente para soportar cargas de producción**
3. **Las operaciones POST son 3x más costosas que las GET**
4. **El sistema no está preparado para escalar horizontalmente sin modificaciones**

### Próximos Pasos
1. Implementar las recomendaciones priorizadas
2. Ejecutar nuevas pruebas de regresión
3. Establecer monitoreo continuo en producción
4. Definir SLAs basados en estos resultados

---

## 📞 Contacto del Equipo

Para consultas sobre este análisis:
- **Líder Técnico**: Antonio B. Arriagada LL. (anarriag@gmail.com)
- **Especialista Performance**: Dante Escalona Bustos (Jacobo.bustos.22@gmail.com)
- **Analista QA**: Roberto Rivas Lopez (umancl@gmail.com)

---

*Documento generado el: `date +"%d/%m/%Y %H:%M"`*
*Versión: 1.0*
*Estado: Revisión Final*