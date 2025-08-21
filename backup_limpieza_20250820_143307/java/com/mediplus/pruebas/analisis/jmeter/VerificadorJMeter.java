package com.mediplus.pruebas.analisis.jmeter;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Verificador específico para la instalación de JMeter del usuario
 * Realiza pruebas de conectividad y configuración
 * 
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class VerificadorJMeter {

    private static final String JMETER_HOME = "D:\\Program Files\\Apache Software Foundation\\apache-jmeter-5.6.3";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void main(String[] args) {
        VerificadorJMeter verificador = new VerificadorJMeter();
        verificador.ejecutarVerificacionCompleta();
    }

    public void ejecutarVerificacionCompleta() {
        mostrarBanner();
        
        try {
            // 1. Verificar instalación
            verificarInstalacion();
            
            // 2. Configurar entorno
            configurarEntorno();
            
            // 3. Crear scripts de prueba
            crearScriptsPrueba();
            
            // 4. Ejecutar prueba rápida
            ejecutarPruebaRapida();
            
            // 5. Validar resultados
            validarResultados();
            
            System.out.println("\n✅ ¡Verificación de JMeter completada exitosamente!");
            
        } catch (Exception e) {
            System.err.println("❌ Error durante la verificación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarBanner() {
        System.out.println("=".repeat(80));
        System.out.println("🔍 VERIFICADOR JMETER - FRAMEWORK API MEDIPLUS");
        System.out.println("📊 Validación de instalación y configuración JMeter 5.6.3");
        System.out.println("👥 Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez");
        System.out.println("🕒 " + LocalDateTime.now().format(FORMATO));
        System.out.println("=".repeat(80));
    }

    private void verificarInstalacion() throws IOException {
        System.out.println("\n🔍 PASO 1: Verificando instalación JMeter...");
        
        Path jmeterHome = Paths.get(JMETER_HOME);
        Path jmeterBin = jmeterHome.resolve("bin");
        Path jmeterExe = jmeterBin.resolve("jmeter.bat");
        
        // Verificar directorio principal
        if (!Files.exists(jmeterHome)) {
            throw new IOException("❌ Directorio JMeter no encontrado: " + JMETER_HOME);
        }
        System.out.println("✅ Directorio JMeter encontrado: " + jmeterHome);
        
        // Verificar directorio bin
        if (!Files.exists(jmeterBin)) {
            throw new IOException("❌ Directorio bin no encontrado: " + jmeterBin);
        }
        System.out.println("✅ Directorio bin encontrado: " + jmeterBin);
        
        // Verificar ejecutable
        if (!Files.exists(jmeterExe)) {
            throw new IOException("❌ Ejecutable jmeter.bat no encontrado: " + jmeterExe);
        }
        System.out.println("✅ Ejecutable JMeter encontrado: " + jmeterExe);
        
        // Verificar otras librerías importantes
        verificarLibrerias(jmeterHome);
    }

    private void verificarLibrerias(Path jmeterHome) {
        System.out.println("\n  📚 Verificando librerías JMeter...");
        
        String[] directoriosImportantes = {"lib", "lib/ext", "lib/junit"};
        
        for (String dir : directoriosImportantes) {
            Path dirPath = jmeterHome.resolve(dir);
            if (Files.exists(dirPath)) {
                try {
                    long count = Files.list(dirPath).count();
                    System.out.println("    ✅ " + dir + ": " + count + " archivos");
                } catch (IOException e) {
                    System.out.println("    ⚠️ " + dir + ": Error leyendo directorio");
                }
            } else {
                System.out.println("    ❌ " + dir + ": No encontrado");
            }
        }
    }

    private void configurarEntorno() {
        System.out.println("\n🔧 PASO 2: Configurando entorno...");
        
        // Configurar variables de sistema
        System.setProperty("jmeter.home", JMETER_HOME);
        System.setProperty("java.awt.headless", "true");
        
        // Crear directorios necesarios
        try {
            Files.createDirectories(Paths.get("jmeter"));
            Files.createDirectories(Paths.get("resultados"));
            Files.createDirectories(Paths.get("temp"));
            System.out.println("✅ Directorios de trabajo creados");
        } catch (IOException e) {
            System.err.println("⚠️ Error creando directorios: " + e.getMessage());
        }
        
        System.out.println("✅ Variables de entorno configuradas");
        System.out.println("  jmeter.home = " + System.getProperty("jmeter.home"));
    }

    private void crearScriptsPrueba() throws IOException {
        System.out.println("\n📝 PASO 3: Creando scripts de prueba...");
        
        // Crear script de verificación simple
        crearScriptVerificacion();
        
        System.out.println("✅ Scripts de prueba creados");
    }

    private void crearScriptVerificacion() throws IOException {
        Path archivoScript = Paths.get("jmeter", "verificacion.jmx");
        
        String contenidoScript = """
            <?xml version="1.0" encoding="UTF-8"?>
            <jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
              <hashTree>
                <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Verificación JMeter" enabled="true">
                  <stringProp name="TestPlan.comments">Script de verificación para instalación JMeter</stringProp>
                  <boolProp name="TestPlan.functional_mode">false</boolProp>
                  <boolProp name="TestPlan.tearDown_on_shutdown">true</boolProp>
                  <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
                  <elementProp name="TestPlan.arguments" elementType="Arguments" guiclass="ArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
                    <collectionProp name="Arguments.arguments"/>
                  </elementProp>
                </TestPlan>
                <hashTree>
                  <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Grupo Verificación" enabled="true">
                    <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
                    <elementProp name="ThreadGroup.main_controller" elementType="LoopController" guiclass="LoopControllerGui" testclass="LoopController" testname="Loop Controller" enabled="true">
                      <boolProp name="LoopController.continue_forever">false</boolProp>
                      <intProp name="LoopController.loops">5</intProp>
                    </elementProp>
                    <stringProp name="ThreadGroup.num_threads">2</stringProp>
                    <stringProp name="ThreadGroup.ramp_time">2</stringProp>
                    <boolProp name="ThreadGroup.scheduler">false</boolProp>
                    <stringProp name="ThreadGroup.duration"></stringProp>
                    <stringProp name="ThreadGroup.delay"></stringProp>
                  </ThreadGroup>
                  <hashTree>
                    <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="GET Verificación" enabled="true">
                      <elementProp name="HTTPsampler.Arguments" elementType="Arguments" guiclass="HTTPArgumentsPanel" testclass="Arguments" testname="User Defined Variables" enabled="true">
                        <collectionProp name="Arguments.arguments"/>
                      </elementProp>
                      <stringProp name="HTTPSampler.domain">httpbin.org</stringProp>
                      <stringProp name="HTTPSampler.port"></stringProp>
                      <stringProp name="HTTPSampler.protocol">https</stringProp>
                      <stringProp name="HTTPSampler.contentEncoding"></stringProp>
                      <stringProp name="HTTPSampler.path">/get</stringProp>
                      <stringProp name="HTTPSampler.method">GET</stringProp>
                      <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
                      <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
                      <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                      <boolProp name="HTTPSampler.DO_MULTIPART_POST">false</boolProp>
                      <stringProp name="HTTPSampler.embedded_url_re"></stringProp>
                      <stringProp name="HTTPSampler.connect_timeout">5000</stringProp>
                      <stringProp name="HTTPSampler.response_timeout">10000</stringProp>
                    </HTTPSamplerProxy>
                    <hashTree/>
                  </hashTree>
                </hashTree>
              </hashTree>
            </jmeterTestPlan>
            """;
        
        Files.writeString(archivoScript, contenidoScript);
        System.out.println("  📄 Script de verificación creado: " + archivoScript);
    }

    private void ejecutarPruebaRapida() throws IOException, InterruptedException {
        System.out.println("\n🧪 PASO 4: Ejecutando prueba rápida...");
        
        Path scriptVerificacion = Paths.get("jmeter", "verificacion.jmx");
        Path archivoResultados = Paths.get("resultados", "verificacion.jtl");
        
        // Configurar comando JMeter
        ConfiguradorJMeter configurador = new ConfiguradorJMeter();
        
        if (!configurador.isJMeterDisponible()) {
            System.out.println("❌ JMeter no disponible para prueba");
            return;
        }
        
        List<String> comando = configurador.obtenerComandoJMeter();
        comando.add("-n");
        comando.add("-t");
        comando.add(scriptVerificacion.toString());
        comando.add("-l");
        comando.add(archivoResultados.toString());
        comando.add("-j");
        comando.add("temp/jmeter_verificacion.log");
        
        System.out.println("  🔧 Ejecutando comando: " + String.join(" ", comando.subList(0, 3)) + "...");
        
        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectErrorStream(true);
        
        // Configurar directorio de trabajo
        if (configurador.getJMeterHome() != null) {
            pb.directory(configurador.getJMeterHome().toFile());
        }
        
        Process proceso = pb.start();
        
        // Capturar salida en tiempo real
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.contains("Created the tree successfully")) {
                    System.out.println("    ✅ Plan de pruebas cargado");
                } else if (linea.contains("Starting ThreadGroup")) {
                    System.out.println("    🚀 Iniciando prueba...");
                } else if (linea.contains("summary +")) {
                    System.out.println("    📊 " + extraerResumen(linea));
                } else if (linea.contains("ERROR")) {
                    System.out.println("    ❌ " + linea);
                } else if (linea.contains("Tidying up")) {
                    System.out.println("    🏁 Finalizando prueba...");
                }
            }
        }
        
        int codigoSalida = proceso.waitFor();
        
        if (codigoSalida == 0) {
            System.out.println("✅ Prueba rápida ejecutada exitosamente");
        } else {
            System.out.println("❌ Prueba rápida falló (código: " + codigoSalida + ")");
        }
    }

    private String extraerResumen(String linea) {
        // Extraer información relevante de la línea de resumen
        if (linea.contains("summary +")) {
            String[] partes = linea.split("\\s+");
            for (int i = 0; i < partes.length - 1; i++) {
                if (partes[i].equals("in") && partes[i + 1].contains("s")) {
                    // Encontrar tiempo y throughput
                    String tiempo = partes[i + 1];
                    String throughput = "N/A";
                    for (int j = i; j < partes.length - 1; j++) {
                        if (partes[j].equals("=") && partes[j + 1].contains("/s")) {
                            throughput = partes[j + 1];
                            break;
                        }
                    }
                    return "Tiempo: " + tiempo + ", Throughput: " + throughput;
                }
            }
        }
        return "Ejecutando...";
    }

    private void validarResultados() throws IOException {
        System.out.println("\n📊 PASO 5: Validando resultados...");
        
        Path archivoResultados = Paths.get("resultados", "verificacion.jtl");
        
        if (!Files.exists(archivoResultados)) {
            System.out.println("❌ Archivo de resultados no encontrado");
            return;
        }
        
        // Leer y analizar resultados
        List<String> lineas = Files.readAllLines(archivoResultados);
        
        if (lineas.size() <= 1) {
            System.out.println("❌ Archivo de resultados vacío");
            return;
        }
        
        // Analizar estadísticas básicas
        int totalMuestras = lineas.size() - 1; // Excluir header
        int exitosas = 0;
        long tiempoTotal = 0;
        
        for (int i = 1; i < lineas.size(); i++) {
            String[] campos = lineas.get(i).split(",");
            if (campos.length >= 8) {
                // Campo 1: tiempo de respuesta, Campo 7: success
                try {
                    long tiempoRespuesta = Long.parseLong(campos[1]);
                    boolean exitoso = "true".equals(campos[7]);
                    
                    tiempoTotal += tiempoRespuesta;
                    if (exitoso) exitosas++;
                } catch (NumberFormatException e) {
                    // Ignorar líneas malformadas
                }
            }
        }
        
        double tiempoPromedio = totalMuestras > 0 ? (double) tiempoTotal / totalMuestras : 0;
        double tasaExito = totalMuestras > 0 ? (double) exitosas / totalMuestras * 100 : 0;
        
        System.out.println("✅ Archivo de resultados validado:");
        System.out.println("  📋 Total muestras: " + totalMuestras);
        System.out.println("  ✅ Muestras exitosas: " + exitosas);
        System.out.println("  📊 Tasa de éxito: " + String.format("%.1f%%", tasaExito));
        System.out.println("  ⏱️ Tiempo promedio: " + String.format("%.0f ms", tiempoPromedio));
        
        // Validar calidad de los resultados
        if (tasaExito >= 90.0) {
            System.out.println("✅ Calidad de resultados: EXCELENTE");
        } else if (tasaExito >= 70.0) {
            System.out.println("🟡 Calidad de resultados: ACEPTABLE");
        } else {
            System.out.println("❌ Calidad de resultados: PROBLEMÁTICA");
        }
        
        // Mostrar ubicación del archivo para inspección manual
        System.out.println("📄 Archivo completo: " + archivoResultados.toAbsolutePath());
    }
}