# Informe Final - Análisis de Métricas de Rendimiento

**Proyecto**: Pruebas Automatizadas API MediPlus  
**Fecha**: Sábado, 16 de Agosto de 2025  
**Autores**: Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez

## 📊 Resumen Ejecutivo

Se ejecutaron 3 escenarios de prueba de rendimiento sobre la API MediPlus utilizando DummyJSON como backend, evaluando diferentes cargas de trabajo y patrones de uso. Los resultados revelan un comportamiento diferenciado entre operaciones de lectura y escritura, con degradación notable bajo alta concurrencia.

### Métricas Principales

| Métrica | Mínimo | Máximo | Promedio |
|---------|--------|--------|----------|
| Tiempo Respuesta (ms) | 245.5 | 1250.3 | 795.5 |
| Throughput (req/seg) | 89.7 | 287.9 | 177.8 |
| Tasa Error (%) | 0.2 | 5.4 | 2.5 |

## 📈 Resultados Detallados por Escenario

### GET_10_USUARIOS
- **Escenario**: GET Masivo
- **Usuarios Concurrentes**: 10
- **Tiempo Promedio**: 245.5 ms
- **P90**: 312.0 ms
- **P95**: 398.5 ms
- **P99**: 450.2 ms
- **Throughput**: 89.7 req/seg
- **Tasa Error**: 0.2%
- **Total Peticiones**: 5382
- **Exitosas**: 5371 | **Fallidas**: 11

### POST_50_USUARIOS
- **Escenario**: POST Masivo
- **Usuarios Concurrentes**: 50
- **Tiempo Promedio**: 1250.3 ms
- **P90**: 1678.5 ms
- **P95**: 2150.8 ms
- **P99**: 2890.1 ms
- **Throughput**: 156.4 req/seg
- **Tasa Error**: 1.8%
- **Total Peticiones**: 9384
- **Exitosas**: 9215 | **Fallidas**: 169

### COMBINADO_100_USUARIOS
- **Escenario**: Flujo Combinado
- **Usuarios Concurrentes**: 100
- **Tiempo Promedio**: 890.7 ms
- **P90**: 1234.5 ms
- **P95**: 1567.8 ms
- **P99**: 2100.3 ms
- **Throughput**: 287.9 req/seg
- **Tasa Error**: 5.4%
- **Total Peticiones**: 17274
- **Exitosas**: 16341 | **Fallidas**: 933

## 📊 Gráficas de Análisis

### Gráfica 1: Comparación de Tiempos de Respuesta

```
Tiempo de Respuesta (ms)
     0    500   1000  1500  2000  2500  3000
     |     |     |     |     |     |     |
GET_10_USUARIOS │████████████● 245.5 ms (Promedio)
                │░░░░░░░░░░░░░░░░▲ 398.5 ms (P95)

POST_50_USUARIOS│████████████████████████████████████████████████████████████● 1250.3 ms (Promedio)
                │░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░▲ 2150.8 ms (P95)

COMBINADO_100_USUARIOS│█████████████████████████████████████● 890.7 ms (Promedio)
                      │░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░▲ 1567.8 ms (P95)

```

### Gráfica 2: Throughput vs Tasa de Error

```
Tasa de Error (%)
  6 │
  5 │                                    ●COMBINADO_100
  4 │
  3 │
  2 │           ●POST_50
  1 │
  0 │●GET_10
    └────────────────────────────────────────────────
     0    50   100  150  200  250  300   Throughput (req/seg)

Datos exactos:
• GET_10_USUARIOS    : 89.7 req/seg, 0.2% error
• POST_50_USUARIOS   : 156.4 req/seg, 1.8% error
• COMBINADO_100_USUARIOS: 287.9 req/seg, 5.4% error
```

## 🔍 Hallazgos Principales

1. **Degradación por Tipo de Operación**: Las operaciones POST muestran tiempos 5x mayores que las GET, indicando optimización necesaria en escrituras.

2. **Impacto de Concurrencia**: El aumento de usuarios concurrentes impacta significativamente tanto en tiempo de respuesta como en tasa de error.

3. **Variabilidad en Percentiles**: La diferencia entre P90 y P99 sugiere comportamiento inconsistente bajo carga.

## 💡 Recomendaciones

### 1. Optimizar rendimiento de operaciones POST [CRÍTICA]

**Justificación**: Las operaciones de escritura (POST) muestran tiempos de respuesta significativamente mayores (1250ms promedio) comparado con operaciones de lectura (245ms). Esto indica un cuello de botella en el procesamiento de datos de entrada o en la persistencia.

**Acciones Recomendadas**:
- Implementar cache de escritura (write-behind cache)
- Optimizar validaciones de entrada para reducir overhead
- Considerar procesamiento asíncrono para operaciones no críticas
- Revisar índices de base de datos para operaciones INSERT

### 2. Mejorar escalabilidad para alta concurrencia [ALTA]

**Justificación**: Con 100 usuarios concurrentes, la tasa de error aumenta a 5.4%, indicando que el sistema comienza a degradarse bajo alta carga. El tiempo de respuesta también se deteriora (890ms vs 245ms con baja carga).

**Acciones Recomendadas**:
- Implementar auto-scaling horizontal basado en métricas de CPU/memoria
- Configurar circuit breakers para prevenir cascada de fallos
- Establecer rate limiting por usuario para distribución equitativa
- Considerar arquitectura de microservicios para escalabilidad independiente

### 3. Implementar monitoreo proactivo de rendimiento [MEDIA]

**Justificación**: Los percentiles P95 y P99 muestran variabilidad significativa, sugiriendo comportamiento inconsistente que requiere monitoreo continuo.

**Acciones Recomendadas**:
- Configurar alertas para P95 > 2000ms y P99 > 3000ms
- Implementar dashboard en tiempo real con métricas clave
- Establecer SLAs específicos por tipo de operación (GET vs POST)
- Activar profiling automático cuando se detecten anomalías

## 🎯 Conclusiones

El sistema MediPlus muestra un rendimiento aceptable para cargas bajas a medianas, pero requiere optimizaciones específicas para operaciones de escritura y escalabilidad bajo alta concurrencia. Las recomendaciones propuestas abordan los principales cuellos de botella identificados.

**Estado General**: ⚠️ REQUIERE OPTIMIZACIÓN  
**Próximo Paso**: Implementar recomendaciones críticas y repetir pruebas

---

## 📋 Anexos

### A. Configuración de Pruebas

- **Herramienta**: JMeter 5.6.3
- **Backend**: DummyJSON (https://dummyjson.com)
- **Duración**: 60 segundos por escenario
- **Ubicación**: Pruebas remotas vía Internet
- **Datos**: CSV con 30 registros de pacientes chilenos

### B. Limitaciones del Estudio

- **API Simulada**: DummyJSON no refleja el comportamiento de un sistema real de producción
- **Red Variable**: Latencia de Internet puede afectar resultados
- **Sin Persistencia**: Los datos no se persisten realmente
- **Scope Limitado**: Solo se probaron operaciones básicas CRUD

### C. Próximos Pasos Recomendados

1. **Repetir pruebas en ambiente de staging** con base de datos real
2. **Implementar métricas de infraestructura** (CPU, memoria, I/O)
3. **Pruebas de stress** para determinar punto de quiebre del sistema
4. **Validar mejoras** después de implementar recomendaciones críticas
5. **Establecer baseline de rendimiento** para comparaciones futuras

---

*Informe generado automáticamente por AnalizadorMetricas.java v1.0*