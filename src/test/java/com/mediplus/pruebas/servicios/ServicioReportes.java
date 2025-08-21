package com.mediplus.pruebas.servicios;

import com.mediplus.pruebas.modelos.Reporte;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio para manejar operaciones de reportes de salud
 * Mapea operaciones de Products de DummyJSON a reportes médicos
 * MEJORADO: Ahora guarda automáticamente todos los reportes en archivos
 * 
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class ServicioReportes {

    private static final DateTimeFormatter FORMATO_TIMESTAMP = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String SEPARADOR = "=".repeat(80);
    
    private final RequestSpecification especificacionBase;
    private final Path directorioReportes;
    private final String timestampSesion;
    private final Map<String, Integer> contadoresOperaciones;

    public ServicioReportes(RequestSpecification especificacionBase) {
        this.especificacionBase = especificacionBase;
        this.timestampSesion = LocalDateTime.now().format(FORMATO_TIMESTAMP);
        this.contadoresOperaciones = new HashMap<>();
        
        // Crear directorio de reportes si no existe
        try {
            this.directorioReportes = Paths.get("evidencias", "reportes-dummyjson");
            Files.createDirectories(directorioReportes);
            
            // Crear archivo de índice de sesión
            crearIndiceSession();
            
        } catch (IOException e) {
            throw new RuntimeException("Error creando directorio de reportes: " + e.getMessage(), e);
        }
    }

    private void crearIndiceSession() throws IOException {
        Path archivoIndice = directorioReportes.resolve("INDICE-SESION-" + timestampSesion + ".md");
        
        try (BufferedWriter writer = Files.newBufferedWriter(archivoIndice)) {
            writer.write("# 📊 Índice de Sesión - Reportes DummyJSON\n\n");
            writer.write("**Iniciada:** " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n");
            writer.write("**Timestamp:** " + timestampSesion + "\n\n");
            writer.write("## 📋 Operaciones Realizadas\n\n");
            writer.write("Este archivo se actualiza automáticamente con cada operación.\n\n");
        }
    }

    private void guardarReporte(String operacion, Response response, Object... parametros) {
        try {
            // Incrementar contador
            contadoresOperaciones.merge(operacion, 1, Integer::sum);
            int numeroOperacion = contadoresOperaciones.get(operacion);
            
            // Nombre del archivo
            String nombreArchivo = String.format("%s_%02d_%s.json", 
                operacion.toLowerCase().replace(" ", "_"), 
                numeroOperacion, 
                timestampSesion);
            
            Path archivoReporte = directorioReportes.resolve(nombreArchivo);
            
            // Guardar response completo en JSON
            try (BufferedWriter writer = Files.newBufferedWriter(archivoReporte)) {
                writer.write("{\n");
                writer.write("  \"operacion\": \"" + operacion + "\",\n");
                writer.write("  \"timestamp\": \"" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\",\n");
                writer.write("  \"parametros\": " + Arrays.toString(parametros) + ",\n");
                writer.write("  \"statusCode\": " + response.getStatusCode() + ",\n");
                writer.write("  \"statusLine\": \"" + response.getStatusLine() + "\",\n");
                writer.write("  \"headers\": {\n");
                
                // Guardar headers importantes
                response.getHeaders().forEach(header -> {
                    try {
                        writer.write("    \"" + header.getName() + "\": \"" + header.getValue() + "\",\n");
                    } catch (IOException e) {
                        // Ignorar errores menores en headers
                    }
                });
                
                writer.write("  },\n");
                writer.write("  \"responseTime\": " + response.getTime() + ",\n");
                writer.write("  \"responseBody\": ");
                
                // Guardar body (si es JSON válido, formatearlo; si no, como string)
                String body = response.getBody().asString();
                if (esJsonValido(body)) {
                    writer.write(body);
                } else {
                    writer.write("\"" + body.replace("\"", "\\\"") + "\"");
                }
                
                writer.write("\n}");
            }
            
            // Actualizar índice de sesión
            actualizarIndiceSession(operacion, nombreArchivo, response.getStatusCode(), response.getTime());
            
            // Log en consola (opcional)
            System.out.println("📁 Reporte guardado: " + nombreArchivo);
            
        } catch (IOException e) {
            System.err.println("⚠️ Error guardando reporte de " + operacion + ": " + e.getMessage());
        }
    }

    private boolean esJsonValido(String json) {
        try {
            // Verificación simple de JSON válido
            return (json.trim().startsWith("{") && json.trim().endsWith("}")) ||
                   (json.trim().startsWith("[") && json.trim().endsWith("]"));
        } catch (Exception e) {
            return false;
        }
    }

    private void actualizarIndiceSession(String operacion, String nombreArchivo, int statusCode, long tiempoRespuesta) {
        try {
            Path archivoIndice = directorioReportes.resolve("INDICE-SESION-" + timestampSesion + ".md");
            
            // Leer contenido existente
            List<String> lineas = Files.readAllLines(archivoIndice);
            
            // Buscar dónde insertar la nueva operación
            boolean encontradoSeccion = false;
            for (int i = 0; i < lineas.size(); i++) {
                if (lineas.get(i).contains("## 📋 Operaciones Realizadas")) {
                    encontradoSeccion = true;
                    // Agregar nueva línea después de encontrar la sección
                    String nuevaLinea = String.format("- `%s` - %s - Status: %d - Tiempo: %dms - [Ver archivo](%s)", 
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        operacion, 
                        statusCode, 
                        tiempoRespuesta, 
                        nombreArchivo);
                    
                    // Insertar después de la línea en blanco que sigue a la sección
                    if (i + 2 < lineas.size()) {
                        lineas.add(i + 3, nuevaLinea);
                    } else {
                        lineas.add(nuevaLinea);
                    }
                    break;
                }
            }
            
            // Si no encontró la sección, agregar al final
            if (!encontradoSeccion) {
                lineas.add("");
                lineas.add("## 📋 Operaciones Realizadas");
                lineas.add("");
                lineas.add(String.format("- `%s` - %s - Status: %d - [Ver archivo](%s)", 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    operacion, 
                    statusCode, 
                    nombreArchivo));
            }
            
            // Escribir de vuelta al archivo
            Files.write(archivoIndice, lineas);
            
        } catch (IOException e) {
            System.err.println("⚠️ Error actualizando índice: " + e.getMessage());
        }
    }

    // ============================================================================
    // MÉTODOS ORIGINALES MEJORADOS CON GUARDADO AUTOMÁTICO
    // ============================================================================

    /**
     * Obtener todos los reportes disponibles (GET)
     */
    public Response obtenerTodosReportes() {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/products")
                .then()
                .extract()
                .response();
        
        guardarReporte("Obtener Todos Reportes", response);
        return response;
    }

    /**
     * Obtener reportes con límite (GET con parámetros)
     */
    public Response obtenerReportesConLimite(int limite) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .queryParam("limit", limite)
                .when()
                .get("/products")
                .then()
                .extract()
                .response();
        
        guardarReporte("Obtener Reportes Con Limite", response, "limite=" + limite);
        return response;
    }

    /**
     * Obtener reporte específico por ID (GET)
     */
    public Response obtenerReportePorId(int id) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/products/{id}", id)
                .then()
                .extract()
                .response();
        
        guardarReporte("Obtener Reporte Por ID", response, "id=" + id);
        return response;
    }

    /**
     * Obtener reportes por categoría/tipo (GET)
     */
    public Response obtenerReportesPorCategoria(String categoria) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/products/category/{category}", categoria)
                .then()
                .extract()
                .response();
        
        guardarReporte("Obtener Reportes Por Categoria", response, "categoria=" + categoria);
        return response;
    }

    /**
     * Obtener todas las categorías disponibles de reportes
     */
    public Response obtenerCategoriasReportes() {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .get("/products/categories")
                .then()
                .extract()
                .response();
        
        guardarReporte("Obtener Categorias Reportes", response);
        return response;
    }

    /**
     * Buscar reportes por palabra clave
     */
    public Response buscarReportes(String textoBusqueda) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .queryParam("q", textoBusqueda)
                .when()
                .get("/products/search")
                .then()
                .extract()
                .response();
        
        guardarReporte("Buscar Reportes", response, "busqueda=" + textoBusqueda);
        return response;
    }

    /**
     * Generar nuevo reporte (POST)
     */
    public Response generarNuevoReporte(Reporte reporte) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "title": "%s",
                            "description": "%s",
                            "category": "%s",
                            "price": %.2f,
                            "brand": "%s"
                        }
                        """,
                        reporte.getTitulo(),
                        reporte.getDescripcion(),
                        reporte.getTipoReporte(),
                        reporte.getCosto(),
                        reporte.getDepartamento()))
                .when()
                .post("/products/add")
                .then()
                .extract()
                .response();
        
        guardarReporte("Generar Nuevo Reporte", response, "titulo=" + reporte.getTitulo());
        return response;
    }

    /**
     * Generar reporte de laboratorio específico
     */
    public Response generarReporteLaboratorio(String nombrePaciente, String tipoExamen) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "title": "Reporte Laboratorio - %s",
                            "description": "Resultados de %s para paciente %s",
                            "category": "laboratory",
                            "price": 0,
                            "brand": "Laboratorio MediPlus"
                        }
                        """, nombrePaciente, tipoExamen, nombrePaciente))
                .when()
                .post("/products/add")
                .then()
                .extract()
                .response();
        
        guardarReporte("Generar Reporte Laboratorio", response, "paciente=" + nombrePaciente, "examen=" + tipoExamen);
        return response;
    }

    /**
     * Generar reporte de radiología
     */
    public Response generarReporteRadiologia(String nombrePaciente, String tipoEstudio) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "title": "Reporte Radiología - %s",
                            "description": "Estudio de %s para paciente %s",
                            "category": "radiology",
                            "price": 0,
                            "brand": "Radiología MediPlus"
                        }
                        """, tipoEstudio, tipoEstudio, nombrePaciente))
                .when()
                .post("/products/add")
                .then()
                .extract()
                .response();
        
        guardarReporte("Generar Reporte Radiologia", response, "paciente=" + nombrePaciente, "estudio=" + tipoEstudio);
        return response;
    }

    /**
     * Actualizar reporte existente (PUT)
     */
    public Response actualizarReporte(int id, Reporte reporte) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "title": "%s",
                            "description": "%s"
                        }
                        """,
                        reporte.getTitulo(),
                        reporte.getDescripcion()))
                .when()
                .put("/products/{id}", id)
                .then()
                .extract()
                .response();
        
        guardarReporte("Actualizar Reporte", response, "id=" + id, "titulo=" + reporte.getTitulo());
        return response;
    }

    /**
     * Eliminar reporte (DELETE)
     */
    public Response eliminarReporte(int id) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .when()
                .delete("/products/{id}", id)
                .then()
                .extract()
                .response();
        
        guardarReporte("Eliminar Reporte", response, "id=" + id);
        return response;
    }

    /**
     * Obtener reportes ordenados por criterio específico
     */
    public Response obtenerReportesOrdenados(String criterio, String orden) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .queryParam("sortBy", criterio)
                .queryParam("order", orden)
                .when()
                .get("/products")
                .then()
                .extract()
                .response();
        
        guardarReporte("Obtener Reportes Ordenados", response, "criterio=" + criterio, "orden=" + orden);
        return response;
    }

    /**
     * Obtener estadísticas de reportes generados
     */
    public Response obtenerEstadisticasReportes() {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .queryParam("limit", 5)
                .queryParam("select", "title,category,rating")
                .when()
                .get("/products")
                .then()
                .extract()
                .response();
        
        guardarReporte("Obtener Estadisticas Reportes", response);
        return response;
    }

    /**
     * Generar reporte mensual de actividad
     */
    public Response generarReporteMensual(String mes, String anio) {
        Response response = RestAssured
                .given()
                .spec(especificacionBase)
                .body(String.format("""
                        {
                            "title": "Reporte Mensual %s/%s",
                            "description": "Estadísticas mensuales de actividad médica",
                            "category": "statistics",
                            "price": 0,
                            "brand": "Sistema MediPlus"
                        }
                        """, mes, anio))
                .when()
                .post("/products/add")
                .then()
                .extract()
                .response();
        
        guardarReporte("Generar Reporte Mensual", response, "periodo=" + mes + "/" + anio);
        return response;
    }

    // ============================================================================
    // MÉTODOS DE UTILIDAD PARA REPORTES
    // ============================================================================

    /**
     * Generar resumen de la sesión actual
     */
    public void generarResumenSesion() {
        try {
            Path archivoResumen = directorioReportes.resolve("RESUMEN-SESION-" + timestampSesion + ".md");
            
            try (BufferedWriter writer = Files.newBufferedWriter(archivoResumen)) {
                writer.write("# 📊 Resumen de Sesión - Reportes DummyJSON\n\n");
                writer.write("**Finalizada:** " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n");
                writer.write("**Timestamp:** " + timestampSesion + "\n\n");
                
                writer.write("## 📈 Estadísticas de Operaciones\n\n");
                writer.write("| Operación | Cantidad |\n");
                writer.write("|-----------|----------|\n");
                
                contadoresOperaciones.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        try {
                            writer.write("| " + entry.getKey() + " | " + entry.getValue() + " |\n");
                        } catch (IOException e) {
                            // Ignorar errores menores
                        }
                    });
                
                writer.write("\n**Total de operaciones:** " + contadoresOperaciones.values().stream().mapToInt(Integer::intValue).sum() + "\n\n");
                
                writer.write("## 📁 Archivos Generados\n\n");
                writer.write("Todos los archivos están disponibles en: `evidencias/reportes-dummyjson/`\n\n");
                
                writer.write("### Archivos de esta sesión:\n");
                Files.list(directorioReportes)
                    .filter(path -> path.getFileName().toString().contains(timestampSesion))
                    .sorted()
                    .forEach(path -> {
                        try {
                            writer.write("- `" + path.getFileName().toString() + "`\n");
                        } catch (IOException e) {
                            // Ignorar errores menores
                        }
                    });
            }
            
            System.out.println("📋 Resumen de sesión generado: " + archivoResumen.getFileName());
            
        } catch (IOException e) {
            System.err.println("⚠️ Error generando resumen de sesión: " + e.getMessage());
        }
    }

    /**
     * Obtener directorio donde se guardan los reportes
     */
    public Path getDirectorioReportes() {
        return directorioReportes;
    }

    /**
     * Obtener timestamp de la sesión actual
     */
    public String getTimestampSesion() {
        return timestampSesion;
    }
}