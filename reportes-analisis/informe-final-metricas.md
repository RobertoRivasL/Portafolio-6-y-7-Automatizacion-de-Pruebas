# Informe Final - Análisis de Métricas de Rendimiento

**Proyecto**: Pruebas Automatizadas API MediPlus  
**Fecha**: Sun Aug 24 17:55:35 CLT 2025  
**Autores**: Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez  

## 📊 Resumen Ejecutivo

Se ejecutaron 3 escenarios de prueba de rendimiento sobre la API MediPlus utilizando DummyJSON como backend, evaluando diferentes cargas de trabajo y patrones de uso.

### Métricas Principales

| Métrica | Mínimo | Máximo | Promedio |
|---------|--------|--------|---------|
| Tiempo Respuesta (ms) | 245,5 | 1250,3 | 795,5 |
| Throughput (req/seg) | 89,7 | 287,9 | 178,0 |
| Tasa Error (%) | 0,2 | 5,4 | 2,5 |

## 📈 Resultados Detallados por Escenario

### GET_10_USUARIOS
- **Escenario**: GET Masivo
- **Usuarios Concurrentes**: 10
- **Tiempo Promedio**: 245,5 ms
- **P90**: 312,0 ms
- **P95**: 398,5 ms
- **P99**: 450,2 ms
- **Throughput**: 89,7 req/seg
- **Tasa Error**: 0,2%

### POST_50_USUARIOS
- **Escenario**: POST Masivo
- **Usuarios Concurrentes**: 50
- **Tiempo Promedio**: 1250,3 ms
- **P90**: 1678,5 ms
- **P95**: 2150,8 ms
- **P99**: 2890,1 ms
- **Throughput**: 156,4 req/seg
- **Tasa Error**: 1,8%

### COMBINADO_100_USUARIOS
- **Escenario**: Flujo Combinado
- **Usuarios Concurrentes**: 100
- **Tiempo Promedio**: 890,7 ms
- **P90**: 1234,5 ms
- **P95**: 1567,8 ms
- **P99**: 2100,3 ms
- **Throughput**: 287,9 req/seg
- **Tasa Error**: 5,4%

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

El sistema MediPlus muestra un rendimiento aceptable para cargas bajas a medianas, pero requiere optimizaciones específicas para operaciones de escritura y escalabilidad bajo alta concurrencia.

**Estado General**: ⚠️ REQUIERE OPTIMIZACIÓN  
**Próximo Paso**: Implementar recomendaciones críticas y repetir pruebas
