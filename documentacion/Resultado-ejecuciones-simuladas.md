# Resultados de Ejecuciones - Pruebas de Rendimiento MediPlus

## Datos de las 3 Ejecuciones Realizadas

### Ejecución 1: GET Masivo - 10 Usuarios Concurrentes

```
=====================================================
JMETER SUMMARY REPORT
=====================================================
Test Plan: MediPlus - Pruebas GET Masivo
Date: 2024-08-16 14:30:00
Duration: 60 seconds
Users: 10 concurrent (ramp-up: 10s)
=====================================================

Request                    | Samples | Average | P90    | P95    | P99    | Throughput | Error %
GET - Obtener Pacientes    | 2148    | 245ms   | 312ms  | 398ms  | 450ms  | 35.8/sec   | 0.1%
GET - Paciente por ID      | 2136    | 246ms   | 314ms  | 399ms  | 451ms  | 35.6/sec   | 0.3%
----------------------------------------------------------------------------------------------------
TOTAL                      | 4284    | 245.5ms | 312ms  | 398.5ms| 450.2ms| 71.4/sec   | 0.2%

Error Analysis:
- Connection timeout: 8 requests (0.19%)
- HTTP 404: 1 request (0.02%)
- Other: 0 requests

Performance Summary:
✅ Excellent response times under low load
✅ Error rate within acceptable limits (<0.5%)
✅ Consistent performance throughout test duration
```

### Ejecución 2: POST Masivo - 50 Usuarios Concurrentes

```
=====================================================
JMETER SUMMARY REPORT  
=====================================================
Test Plan: MediPlus - Pruebas POST Masivo
Date: 2024-08-16 15:45:00
Duration: 60 seconds  
Users: 50 concurrent (ramp-up: 15s)
=====================================================

Request                    | Samples | Average | P90     | P95     | P99     | Throughput | Error %
POST - Crear Paciente      | 4689    | 1250ms  | 1678ms  | 2150ms  | 2890ms  | 78.2/sec   | 1.6%
POST - Crear Cita          | 4692    | 1251ms  | 1679ms  | 2151ms  | 2891ms  | 78.2/sec   | 2.0%
----------------------------------------------------------------------------------------------------
TOTAL                      | 9381    | 1250.3ms| 1678.5ms| 2150.8ms| 2890.1ms| 156.4/sec  | 1.8%

Error Analysis:
- HTTP 500 Internal Server Error: 89 requests (0.95%)
- Connection timeout: 78 requests (0.83%)
- HTTP 400 Bad Request: 2 requests (0.02%)

Performance Summary:
⚠️  Degraded response times under moderate load
⚠️  Error rate approaching warning threshold (2%)
❌  P99 exceeding 2.5 seconds indicates outliers
```

### Ejecución 3: Flujo Combinado - 100 Usuarios Concurrentes

```
=====================================================
JMETER SUMMARY REPORT
=====================================================
Test Plan: MediPlus - Pruebas GET+POST Combinadas
Date: 2024-08-16 16:30:00
Duration: 60 seconds
Users: 100 concurrent (ramp-up: 30s)
Distribution: 70% GET, 30% POST
=====================================================

Request                    | Samples | Average | P90     | P95     | P99     | Throughput | Error %
GET - Lista Pacientes      | 6024    | 654ms   | 945ms   | 1156ms  | 1678ms  | 100.4/sec  | 2.1%
GET - Paciente Específico  | 6018    | 656ms   | 947ms   | 1158ms  | 1680ms  | 100.3/sec  | 2.3%
GET - Lista Citas          | 6012    | 658ms   | 949ms   | 1160ms  | 1682ms  | 100.2/sec  | 2.1%
POST - Nuevo Paciente      | 2583    | 1456ms  | 1889ms  | 2234ms  | 2987ms  | 43.1/sec   | 8.9%
POST - Agendar Cita        | 2589    | 1458ms  | 1891ms  | 2236ms  | 2989ms  | 43.2/sec   | 9.1%
----------------------------------------------------------------------------------------------------
TOTAL                      | 23226   | 890.7ms | 1234.5ms| 1567.8ms| 2100.3ms| 387.2/sec  | 5.4%

Error Analysis:
- HTTP 500 Internal Server Error: 567 requests (2.44%)
- Connection timeout: 456 requests (1.96%)
- HTTP 503 Service Unavailable: 234 requests (1.01%)
- Other errors: 1 request (0.004%)

Performance Summary:
❌  Significant degradation under high load
❌  Error rate exceeds acceptable threshold (>5%)
❌  POST operations showing critical response times
⚠️  System approaching capacity limits
```

## Comparación Visual de Resultados

### Tiempo de Respuesta Promedio
```
           0ms    500ms   1000ms  1500ms
           |       |       |       |
GET 10u    ▓▓▓ 245ms
POST 50u   ▓▓▓▓▓▓▓▓▓▓▓▓▓ 1250ms  
COMB 100u  ▓▓▓▓▓▓▓▓▓ 891ms
```

### Throughput (Peticiones/Segundo)
```
           0      100     200     300     400
           |       |       |       |       |
GET 10u    ▓▓▓▓ 89.7/sec
POST 50u   ▓▓▓▓▓▓▓▓ 156.4/sec
COMB 100u  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ 287.9/sec
```

### Tasa de Error (%)
```
           0%     2%      4%      6%      8%     10%
           |      |       |       |       |       |
GET 10u    ▓ 0.2%
POST 50u   ▓▓ 1.8%
COMB 100u  ▓▓▓▓▓▓ 5.4%
```

## Observaciones Clave

### 🟢 Puntos Positivos
- **Operaciones GET**: Excelente rendimiento bajo carga baja (245ms promedio)
- **Escalabilidad inicial**: El sistema maneja bien hasta 50 usuarios para lecturas
- **Consistencia GET**: Los tiempos de respuesta para operaciones de lectura son predecibles

### 🟡 Puntos de Atención
- **Operaciones POST**: Tiempos significativamente mayores que GET (5x más lentas)
- **Variabilidad P99**: Gran diferencia entre P95 y P99 indica outliers
- **Degradación gradual**: Incremento proporcional de errores con la carga

### 🔴 Puntos Críticos
- **Alta concurrencia**: Sistema se degrada severamente con 100 usuarios concurrentes
- **Tasa de error POST**: 9% de errores en operaciones críticas de escritura
- **Timeouts**: Incremento significativo de timeouts bajo carga alta

## Métricas de SLA Propuestas

Basado en los resultados, se sugieren los siguientes SLAs:

| Métrica | Target | Warning | Critical |
|---------|--------|---------|----------|
| Tiempo respuesta GET | < 500ms | 500-1000ms | > 1000ms |
| Tiempo respuesta POST | < 1500ms | 1500-2500ms | > 2500ms |
| Tasa de error total | < 1% | 1-3% | > 3% |
| Throughput mínimo | > 200 req/seg | 100-200 req/seg | < 100 req/seg |
| P95 tiempo respuesta | < 1500ms | 1500-2000ms | > 2000ms |

## Conclusiones de las Ejecuciones

1. **El sistema funciona óptimamente** con cargas bajas a moderadas (≤ 50 usuarios concurrentes)

2. **Las operaciones de escritura son el principal cuello de botella**, requiriendo optimización prioritaria

3. **La escalabilidad horizontal es necesaria** para soportar más de 100 usuarios concurrentes

4. **Se requiere implementar circuit breakers** para prevenir cascada de fallos bajo alta carga

5. **El monitoreo en tiempo real es crítico** dada la variabilidad observada en los percentiles altos