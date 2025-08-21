## Tabla de Endpoints

| Endpoint DummyJSON | Método | Simulación MediPlus | Parámetros | Respuesta Esperada |
|-------------------|--------|-------------------|------------|-------------------|
| `/users` | GET | Obtener lista de pacientes | Query: `limit`, `skip` | 200 - Lista de pacientes |
| `/users/{id}` | GET | Obtener paciente por ID | Path: `id` | 200 - Datos del paciente |
| `/users/add` | POST | Registrar nuevo paciente | Body: datos del paciente | 201 - Paciente creado |
| `/users/{id}` | PUT | Actualizar paciente | Path: `id`, Body: datos | 200 - Paciente actualizado |
| `/users/{id}` | DELETE | Eliminar paciente | Path: `id` | 200 - Confirmación eliminación |
| `/products` | GET | Obtener lista de medicamentos | Query: `limit`, `skip` | 200 - Lista de medicamentos |
| `/products/{id}` | GET | Obtener medicamento por ID | Path: `id` | 200 - Datos del medicamento |
| `/posts` | GET | Obtener reportes médicos | Query: `limit`, `skip` | 200 - Lista de reportes |
| `/posts/add` | POST | Crear reporte médico | Body: datos del reporte | 201 - Reporte creado |
| `/todos` | GET | Obtener citas médicas | Query: `limit`, `skip` | 200 - Lista de citas |
| `/todos/{id}` | GET | Obtener cita específica | Path: `id` | 200 - Datos de la cita |
| `/todos/add` | POST | Agendar nueva cita | Body: datos de la cita | 201 - Cita creada |
| `/todos/{id}` | PUT | Actualizar cita | Path: `id`, Body: datos | 200 - Cita actualizada |
| `/auth/login` | POST | Autenticación del sistema | Body: credenciales | 200 - Token de acceso |

## URL Base
```
https://dummyjson.com
```

## Estructura de Datos

### Paciente (Simulado con Users)
```json
{
    "id": 1,
    "firstName": "Juan",
    "lastName": "Pérez",
    "email": "juan.perez@email.com",
    "phone": "+56912345678",
    "birthDate": "1985-03-15",
    "address": {
        "address": "Av. Principal 123",
        "city": "Santiago",
        "coordinates": {
            "lat": -33.4489,
            "lng": -70.6693
        }
    }
}
```

### Medicamento (Simulado con Products)
```json
{
    "id": 1,
    "title": "Paracetamol 500mg",
    "description": "Analgésico y antipirético",
    "price": 2500,
    "category": "Analgésicos",
    "brand": "MediPlus Pharma",
    "stock": 150
}
```

### Cita Médica (Simulado con Todos)
```json
{
    "id": 1,
    "todo": "Consulta cardiológica - Dr. González",
    "completed": false,
    "userId": 15,
    "fechaHora": "2024-12-15T10:30:00",
    "especialidad": "Cardiología",
    "motivo": "Control rutinario"
}
```

### Reporte Médico (Simulado con Posts)
```json
{
    "id": 1,
    "title": "Consulta Cardiológica - Paciente 001",
    "body": "Examen cardiovascular completo realizado. Resultados normales.",
    "userId": 1,
    "tags": ["cardiologia", "consulta", "normal"]
}
```

## Autenticación

### Endpoint de Login
```
POST https://dummyjson.com/auth/login
```

### Credenciales de Prueba
```json
{
    "username": "kminchelle",
    "password": "0lelplR"
}
```

### Respuesta de Autenticación
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "id": 15,
    "username": "kminchelle",
    "email": "kminchelle@qq.com",
    "firstName": "Jeanne",
    "lastName": "Halvorson"
}
```