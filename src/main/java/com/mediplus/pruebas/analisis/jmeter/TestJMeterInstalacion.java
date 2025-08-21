package com.mediplus.pruebas.analisis.jmeter;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * Prueba específica para la instalación de JMeter detectada
 * Diseñado especialmente para: D:\Program Files\Apache Software Foundation\apache-jmeter-5.6.3
 */
public class TestJMeterInstalacion {

    public static void main(String[] args) {
        System.out.println("🧪 PRUEBA DE INSTALACIÓN JMETER ESPECÍFICA");
        System.out.println("=".repeat(50));

        probarInstalacionEspecifica();
    }

    public static void probarInstalacionEspecifica() {
        // Tu instalación específica
        String jmeterHome = System.getenv("JMETER_HOME");
        System.out.println("📍 JMETER_HOME: " + jmeterHome);

        if (jmeterHome == null) {
            System.out.println("❌ JMETER_HOME no configurado");
            return;
        }

        // Verificar estructura de directorios
        verificarEstructuraJMeter(jmeterHome);

        // Probar ejecutable
        String rutaEjecutable = jmeterHome + "\\bin\\jmeter.bat";
        System.out.println("🔍 Probando ejecutable: " + rutaEjecutable);

        if (probarComandoVersion(rutaEjecutable)) {
            System.out.println("✅ JMeter responde correctamente");
            ejecutarPruebaCompleta(rutaEjecutable);
        } else {
            System.out.println("❌ JMeter no responde");
            diagnosticarProblemas(rutaEjecutable);
        }
    }

    private static void verificarEstructuraJMeter(String jmeterHome) {
        System.out.println("📁 Verificando estructura de directorios...");

        String[] directoriosEsperados = {
                "bin", "lib", "extras", "licenses", "printable_docs"
        };

        for (String dir : directoriosEsperados) {
            Path dirPath = Paths.get(jmeterHome, dir);
            if (Files.exists(dirPath)) {
                System.out.println("✅ " + dir + "/ existe");
            } else {
                System.out.println("❌ " + dir + "/ NO existe");
            }
        }

        // Verificar ejecutables específicos
        String[] ejecutables = {"jmeter.bat", "jmeter-server.bat", "jmeter.sh"};
        Path binPath = Paths.get(jmeterHome, "bin");

        for (String ejecutable : ejecutables) {
            Path execPath = binPath.resolve(ejecutable);
            if (Files.exists(execPath)) {
                System.out.println("✅ bin/" + ejecutable + " existe");
            } else {
                System.out.println("❌ bin/" + ejecutable + " NO existe");
            }
        }
    }

    private static boolean probarComandoVersion(String rutaEjecutable) {
        try {
            System.out.println("⚡ Ejecutando: " + rutaEjecutable + " --version");

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "\"" + rutaEjecutable + "\"", "--version");
            pb.redirectErrorStream(true);

            Process proceso = pb.start();

            // Capturar toda la salida
            StringBuilder salida = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    salida.append(linea).append("\n");
                    System.out.println("📄 " + linea);
                }
            }

            boolean terminado = proceso.waitFor(30, TimeUnit.SECONDS);
            if (!terminado) {
                proceso.destroyForcibly();
                System.out.println("⏱️ Timeout ejecutando comando");
                return false;
            }

            int codigoSalida = proceso.exitValue();
            System.out.println("🔢 Código de salida: " + codigoSalida);

            return codigoSalida == 0;

        } catch (Exception e) {
            System.err.println("❌ Error ejecutando comando: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void ejecutarPruebaCompleta(String rutaEjecutable) {
        try {
            System.out.println("\n🚀 EJECUTANDO PRUEBA COMPLETA");
            System.out.println("-".repeat(30));

            // Crear directorio de resultados
            Path dirResultados = Paths.get("jmeter-results");
            Files.createDirectories(dirResultados);

            // Crear script JMX simple
            Path scriptJMX = crearScriptJMXSimple(dirResultados);

            // Archivos de salida
            Path archivoJTL = dirResultados.resolve("test-instalacion.jtl");
            Path archivoLog = dirResultados.resolve("test-instalacion.log");

            // Eliminar archivos anteriores si existen
            Files.deleteIfExists(archivoJTL);
            Files.deleteIfExists(archivoLog);

            // Construir comando
            String comando = String.format(
                    "cmd.exe /c \"%s\" -n -t \"%s\" -l \"%s\" -j \"%s\"",
                    rutaEjecutable,
                    scriptJMX.toAbsolutePath(),
                    archivoJTL.toAbsolutePath(),
                    archivoLog.toAbsolutePath()
            );

            System.out.println("🚀 Comando completo: " + comando);

            // Ejecutar
            Process proceso = Runtime.getRuntime().exec(comando);

            // Monitorear salida
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    System.out.println("JMeter: " + linea);
                }
            }

            boolean terminado = proceso.waitFor(60, TimeUnit.SECONDS);
            if (!terminado) {
                proceso.destroyForcibly();
                System.out.println("⏱️ Timeout en ejecución de prueba");
                return;
            }

            int codigoSalida = proceso.exitValue();
            System.out.println("🔢 Código de salida final: " + codigoSalida);

            // Verificar resultados
            verificarResultados(archivoJTL, archivoLog);

        } catch (Exception e) {
            System.err.println("❌ Error en prueba completa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Path crearScriptJMXSimple(Path directorio) throws IOException {
        String contenidoJMX = """
            <?xml version="1.0" encoding="UTF-8"?>
            <jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
              <hashTree>
                <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Test Plan">
                  <elementProp name="TestPlan.arguments" elementType="Arguments">
                    <collectionProp name="Arguments.arguments"/>
                  </elementProp>
                </TestPlan>
                <hashTree>
                  <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Thread Group">
                    <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
                    <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
                      <boolProp name="LoopController.continue_forever">false</boolProp>
                      <intProp name="LoopController.loops">3</intProp>
                    </elementProp>
                    <stringProp name="ThreadGroup.num_threads">2</stringProp>
                    <stringProp name="ThreadGroup.ramp_time">5</stringProp>
                  </ThreadGroup>
                  <hashTree>
                    <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="HTTP Request">
                      <stringProp name="HTTPSampler.domain">httpbin.org</stringProp>
                      <stringProp name="HTTPSampler.protocol">https</stringProp>
                      <stringProp name="HTTPSampler.path">/get</stringProp>
                      <stringProp name="HTTPSampler.method">GET</stringProp>
                      <boolProp name="HTTPSampler.use_keepalive">true</boolProp>
                    </HTTPSamplerProxy>
                    <hashTree/>
                  </hashTree>
                </hashTree>
              </hashTree>
            </jmeterTestPlan>
            """;

        Path scriptPath = directorio.resolve("test-simple.jmx");
        Files.writeString(scriptPath, contenidoJMX);
        System.out.println("✅ Script JMX creado: " + scriptPath);

        return scriptPath;
    }

    private static void verificarResultados(Path archivoJTL, Path archivoLog) throws IOException {
        System.out.println("\n📊 VERIFICANDO RESULTADOS");
        System.out.println("-".repeat(30));

        if (Files.exists(archivoJTL)) {
            long size = Files.size(archivoJTL);
            System.out.println("✅ Archivo JTL generado: " + size + " bytes");

            if (size > 0) {
                System.out.println("📄 Contenido del JTL:");
                Files.lines(archivoJTL).limit(10).forEach(linea -> System.out.println("  " + linea));

                long numeroLineas = Files.lines(archivoJTL).count();
                System.out.println("📊 Total de líneas en JTL: " + numeroLineas);

                if (numeroLineas > 1) {
                    System.out.println("🎉 ¡ÉXITO! JMeter generó datos correctamente");
                } else {
                    System.out.println("⚠️ JTL contiene solo header, no hay datos de prueba");
                }
            } else {
                System.out.println("❌ Archivo JTL está vacío");
            }
        } else {
            System.out.println("❌ Archivo JTL no fue generado");
        }

        if (Files.exists(archivoLog)) {
            System.out.println("✅ Archivo LOG generado");
            System.out.println("📄 Últimas líneas del log:");
            try {
                Files.lines(archivoLog)
                        .skip(Math.max(0, Files.lines(archivoLog).count() - 5))
                        .forEach(linea -> System.out.println("  " + linea));
            } catch (Exception e) {
                System.out.println("⚠️ Error leyendo log: " + e.getMessage());
            }
        } else {
            System.out.println("❌ Archivo LOG no fue generado");
        }
    }

    private static void diagnosticarProblemas(String rutaEjecutable) {
        System.out.println("\n🔧 DIAGNÓSTICO DE PROBLEMAS");
        System.out.println("-".repeat(30));

        // Verificar que el archivo existe
        if (!Files.exists(Paths.get(rutaEjecutable))) {
            System.out.println("❌ El archivo jmeter.bat no existe en la ruta especificada");
            return;
        }

        // Verificar permisos
        Path path = Paths.get(rutaEjecutable);
        System.out.println("📁 Archivo existe: " + Files.exists(path));
        System.out.println("📖 Legible: " + Files.isReadable(path));
        System.out.println("✏️ Escribible: " + Files.isWritable(path));
        System.out.println("🚀 Ejecutable: " + Files.isExecutable(path));

        // Probar comando simple
        try {
            System.out.println("🧪 Probando comando básico...");
            Process proceso = Runtime.getRuntime().exec("cmd.exe /c \"" + rutaEjecutable + "\" -?");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    System.out.println("📄 " + linea);
                    if (linea.toLowerCase().contains("usage") || linea.toLowerCase().contains("options")) {
                        System.out.println("✅ JMeter responde a comandos de ayuda");
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error en comando básico: " + e.getMessage());
        }
    }
}