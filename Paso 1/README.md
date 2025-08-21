# Pruebas Automatizadas API MediPlus - Paso 1

## Autores
- Antonio B. Arriagada LL. (anarriag@gmail.com)
- Dante Escalona Bustos (Jacobo.bustos.22@gmail.com)
- Roberto Rivas Lopez (umancl@gmail.com)

## Descripción
Proyecto de automatización de pruebas para la API REST de MediPlus utilizando DummyJSON como simulador de endpoints.

## Tecnologías
- Java 21
- Maven 3.9.10
- REST Assured 5.4.0
- JUnit 5
- API de Simulación: DummyJSON (https://dummyjson.com)

## Estructura del Proyecto
```
src/
├── test/
│   └── java/
│       └── com/
│           └── mediplus/
│               └── automatizacion/
│                   ├── configuracion/
│                   │   └── ConfiguracionBase.java
│                   ├── entidades/
│                   │   ├── Paciente.java
│                   │   ├── Medicamento.java
│                   │   ├── CitaMedica.java
│                   │   └── ReporteMedico.java
│                   ├── servicios/
│                   │   ├── ServicioPacientes.java
│                   │   ├── ServicioMedicamentos.java
│                   │   ├── ServicioCitas.java
│                   │   └── ServicioReportes.java
│                   └── casos/
│                       ├── PruebasFuncionalesPacientes.java
│                       ├── PruebasFuncionalesMedicamentos.java
│                       ├── PruebasFuncionalesCitas.java
│                       └── PruebasSeguridad.java
```

## Ejecución de Pruebas

### Prerrequisitos
1. Java 21 instalado
2. Maven 3.9.10 instalado
3. Conexión a Internet (para DummyJSON)

### Comandos
```bash
# Compilar proyecto
mvn clean compile

# Ejecutar todas las pruebas
mvn test

# Ejecutar pruebas específicas
mvn test -Dtest=PruebasFuncionalesPacientes

# Generar reporte
mvn surefire-report:report
```

## Configuración
- URL Base API: `https://dummyjson.com`
- Token de pruebas: Se obtiene mediante `/auth/login`
- Timeout: 10 segundos

## Estado del Proyecto
- ✅ Lección 1: Exploración y documentación de la API completada
- ⏳ Próximo: Lección 2 - Validación funcional automatizada