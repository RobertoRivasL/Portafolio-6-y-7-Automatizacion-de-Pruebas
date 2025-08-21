package com.mediplus.pruebas.analisis.jmeter;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Configurador automático de JMeter para el framework
 * Detecta y configura JMeter en el sistema
 * 
 * @author Antonio B. Arriagada LL., Dante Escalona Bustos, Roberto Rivas Lopez
 */
public class ConfiguradorJMeter {

    private static final String JMETER_HOME_ENV = "JMETER_HOME";
    private static final String[] POSIBLES_RUTAS_JMETER = {
        "D:\\Program Files\\Apache Software Foundation\\apache-jmeter-5.6.3",
        "C:\\Program Files\\Apache Software Foundation\\apache-jmeter-5.6.3",
        "C:\\apache-jmeter-5.6.3",
        "/opt/apache-jmeter-5.6.3",
        "/usr/local/apache-jmeter-5.6.3",
        System.getProperty("user.home") + "/apache-jmeter-5.6.3"
    };

    private Path jmeterHome;
    private Path jmeterBin;
    private boolean jmeterDisponible = false;

    public ConfiguradorJMeter() {
        detectarJMeter();
    }

    /**
     * Detecta automáticamente la instalación de JMeter
     */
    private void detectarJMeter() {
        System.out.println("🔍 Detectando instalación de JMeter...");

        // 1. Verificar variable de entorno JMETER_HOME
        String jmeterHomeEnv = System.getenv(JMETER_HOME_ENV);
        if (jmeterHomeEnv != null && verificarInstalacionJMeter(Paths.get(jmeterHomeEnv))) {
            System.out.println("✅ JMeter encontrado via JMETER_HOME: " + jmeterHomeEnv);
            return;
        }

        // 2. Verificar rutas comunes
        for (String ruta : POSIBLES_RUTAS_JMETER) {
            if (verificarInstalacionJMeter(Paths.get(ruta))) {
                System.out.println("✅ JMeter encontrado en: " + ruta);
                return;
            }
        }

        // 3. Verificar si 'jmeter' está en PATH
        if (verificarJMeterEnPath()) {
            System.out.println("✅ JMeter disponible en PATH del sistema");
            jmeterDisponible = true;
            return;
        }

        System.out.println("⚠️ JMeter no detectado automáticamente");
        System.out.println("💡 Asegúrate de tener configurado JMETER_HOME o JMeter en el PATH");
    }

    /**
     * Verifica si JMeter está disponible en el PATH
     */
    private boolean verificarJMeterEnPath() {
        try {
            String comando = System.getProperty("os.name").toLowerCase().contains("windows") ? 
                "jmeter.bat" : "jmeter";
            
            ProcessBuilder pb = new ProcessBuilder(comando, "--version");
            pb.redirectErrorStream(true);
            Process proceso = pb.start();
            
            int resultado = proceso.waitFor();
            return resultado == 0;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica si existe una instalación válida de JMeter en la ruta especificada
     */
    private boolean verificarInstalacionJMeter(Path rutaJMeter) {
        if (!Files.exists(rutaJMeter)) {
            return false;
        }

        Path binDirectory = rutaJMeter.resolve("bin");
        if (!Files.exists(binDirectory)) {
            return false;
        }

        // Verificar ejecutables según el sistema operativo
        boolean esWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String ejecutableJMeter = esWindows ? "jmeter.bat" : "jmeter";
        
        Path ejecutable = binDirectory.resolve(ejecutableJMeter);
        if (Files.exists(ejecutable)) {
            this.jmeterHome = rutaJMeter;
            this.jmeterBin = binDirectory;
            this.jmeterDisponible = true;
            return true;
        }

        return false;
    }

    /**
     * Configura las variables de entorno para JMeter
     */
    public void configurarEntornoJMeter() {
        if (!jmeterDisponible) {
            System.out.println("❌ No se puede configurar JMeter - no está disponible");
            return;
        }

        System.out.println("🔧 Configurando entorno JMeter...");

        if (jmeterHome != null) {
            // Establecer JMETER_HOME si no está configurado
            if (System.getenv(JMETER_HOME_ENV) == null) {
                System.setProperty("jmeter.home", jmeterHome.toString());
                System.out.println("  📝 Configurado jmeter.home: " + jmeterHome);
            }

            // Configurar PATH si es necesario
            configurarPath();
        }

        // Configurar propiedades adicionales de JMeter
        configurarPropiedadesJMeter();

        System.out.println("✅ Entorno JMeter configurado correctamente");
    }

    /**
     * Configura el PATH para incluir JMeter
     */
    private void configurarPath() {
        if (jmeterBin == null) return;

        String pathActual = System.getenv("PATH");
        String jmeterBinPath = jmeterBin.toString();

        if (pathActual == null || !pathActual.contains(jmeterBinPath)) {
            // En entorno Java, no podemos modificar directamente PATH del sistema
            // Pero podemos usar rutas absolutas para ejecutar JMeter
            System.out.println("  💡 Usando ruta absoluta para ejecutar JMeter: " + jmeterBinPath);
        }
    }

    /**
     * Configura propiedades específicas de JMeter
     */
    private void configurarPropiedadesJMeter() {
        // Configurar propiedades del sistema para JMeter
        System.setProperty("jmeter.save.saveservice.output_format", "csv");
        System.setProperty("jmeter.save.saveservice.response_data", "false");
        System.setProperty("jmeter.save.saveservice.samplerData", "false");
        System.setProperty("jmeter.save.saveservice.requestHeaders", "false");
        System.setProperty("jmeter.save.saveservice.url", "true");
        System.setProperty("jmeter.save.saveservice.thread_counts", "true");

        System.out.println("  ⚙️ Propiedades JMeter configuradas para CSV óptimo");
    }

    /**
     * Obtiene el comando para ejecutar JMeter
     */
    public List<String> obtenerComandoJMeter() {
        if (!jmeterDisponible) {
            throw new IllegalStateException("JMeter no está disponible");
        }

        List<String> comando = new ArrayList<>();

        if (jmeterBin != null) {
            // Usar ruta absoluta
            boolean esWindows = System.getProperty("os.name").toLowerCase().contains("windows");
            String ejecutable = esWindows ? "jmeter.bat" : "jmeter";
            comando.add(jmeterBin.resolve(ejecutable).toString());
        } else {
            // Usar comando del PATH
            boolean esWindows = System.getProperty("os.name").toLowerCase().contains("windows");
            comando.add(esWindows ? "jmeter.bat" : "jmeter");
        }

        return comando;
    }

    /**
     * Crea un archivo de configuración JMeter optimizado
     */
    public void crearConfiguracionOptimizada(Path directorioJMeter) throws IOException {
        System.out.println("📝 Creando configuración JMeter optimizada...");

        Files.createDirectories(directorioJMeter);

        // Crear archivo jmeter.properties personalizado
        Path archivoProperties = directorioJMeter.resolve("jmeter.properties");
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(archivoProperties))) {
            writer.println("# Configuración JMeter optimizada para API MediPlus");
            writer.println("# Generado automáticamente por ConfiguradorJMeter.java");
            writer.println();
            
            writer.println("# Formato de salida CSV");
            writer.println("jmeter.save.saveservice.output_format=csv");
            writer.println("jmeter.save.saveservice.response_data=false");
            writer.println("jmeter.save.saveservice.samplerData=false");
            writer.println("jmeter.save.saveservice.requestHeaders=false");
            writer.println("jmeter.save.saveservice.responseHeaders=false");
            writer.println("jmeter.save.saveservice.assertions=false");
            writer.println("jmeter.save.saveservice.subresults=false");
            writer.println();
            
            writer.println("# Información útil a guardar");
            writer.println("jmeter.save.saveservice.time=true");
            writer.println("jmeter.save.saveservice.label=true");
            writer.println("jmeter.save.saveservice.code=true");
            writer.println("jmeter.save.saveservice.message=true");
            writer.println("jmeter.save.saveservice.url=true");
            writer.println("jmeter.save.saveservice.bytes=true");
            writer.println("jmeter.save.saveservice.thread_counts=true");
            writer.println("jmeter.save.saveservice.latency=true");
            writer.println();
            
            writer.println("# Configuración de rendimiento");
            writer.println("jmeter.save.saveservice.autoflush=true");
            writer.println("summariser.interval=30");
            writer.println();
            
            writer.println("# Configuración de HTTP");
            writer.println("httpclient4.retrycount=1");
            writer.println("httpclient4.request.timeout=30000");
            writer.println("httpclient4.connect.timeout=10000");
        }

        System.out.println("✅ Archivo de configuración creado: " + archivoProperties);
    }

    /**
     * Muestra información de diagnóstico de JMeter
     */
    public void mostrarDiagnostico() {
        System.out.println("\n🔍 DIAGNÓSTICO JMETER:");
        System.out.println("=".repeat(50));
        
        System.out.println("JMETER_HOME (variable): " + System.getenv(JMETER_HOME_ENV));
        System.out.println("JMeter disponible: " + (jmeterDisponible ? "✅ SÍ" : "❌ NO"));
        
        if (jmeterHome != null) {
            System.out.println("Directorio JMeter: " + jmeterHome);
            System.out.println("Directorio bin: " + jmeterBin);
        }
        
        // Verificar versión si está disponible
        if (jmeterDisponible) {
            verificarVersionJMeter();
        }
        
        System.out.println("Sistema operativo: " + System.getProperty("os.name"));
        System.out.println("Arquitectura: " + System.getProperty("os.arch"));
    }

    /**
     * Verifica la versión de JMeter instalada
     */
    private void verificarVersionJMeter() {
        try {
            List<String> comando = obtenerComandoJMeter();
            comando.add("--version");
            
            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.redirectErrorStream(true);
            Process proceso = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    if (linea.contains("Apache JMeter")) {
                        System.out.println("Versión JMeter: " + linea);
                        break;
                    }
                }
            }
            
            proceso.waitFor();
            
        } catch (Exception e) {
            System.out.println("Error verificando versión: " + e.getMessage());
        }
    }

    // Getters
    public boolean isJMeterDisponible() {
        return jmeterDisponible;
    }

    public Path getJMeterHome() {
        return jmeterHome;
    }

    public Path getJMeterBin() {
        return jmeterBin;
    }

    /**
     * Método principal para pruebas
     */
    public static void main(String[] args) {
        try {
            ConfiguradorJMeter configurador = new ConfiguradorJMeter();
            
            configurador.mostrarDiagnostico();
            
            if (configurador.isJMeterDisponible()) {
                configurador.configurarEntornoJMeter();
                configurador.crearConfiguracionOptimizada(Paths.get("jmeter"));
                
                System.out.println("\n✅ JMeter configurado y listo para usar!");
                System.out.println("💡 Comando para ejecutar: " + configurador.obtenerComandoJMeter());
            } else {
                System.out.println("\n❌ JMeter no está disponible.");
                System.out.println("💡 Instala JMeter y configura JMETER_HOME o añádelo al PATH");
            }
            
        } catch (Exception e) {
            System.err.println("Error en configuración: " + e.getMessage());
            e.printStackTrace();
        }
    }
}