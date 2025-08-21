package com.mediplus.pruebas.analisis.jmeter;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/**
 * Prueba simplificada de JMeter para debugging
 */
public class JMeterSimpleTest {

    public static void main(String[] args) {
        System.out.println("🧪 PRUEBA SIMPLE DE JMETER");
        System.out.println("=".repeat(40));

        try {
            // 1. Detectar JMeter
            String jmeterPath = detectarJMeter();
            if (jmeterPath == null) {
                System.out.println("❌ JMeter no encontrado");
                return;
            }

            System.out.println("✅ JMeter encontrado: " + jmeterPath);

            // 2. Crear script JMX simple
            Path scriptJMX = crearScriptSimple();
            System.out.println("✅ Script JMX creado: " + scriptJMX);

            // 3. Preparar archivos de salida
            Path directorioResultados = Paths.get("jmeter-results");
            Files.createDirectories(directorioResultados);

            Path archivoJTL = directorioResultados.resolve("test-simple.jtl");
            Path archivoLog = directorioResultados.resolve("test-simple.log");

            // 4. Ejecutar JMeter
            System.out.println("⚡ Ejecutando JMeter...");
            boolean exitoso = ejecutarJMeterSimple(jmeterPath, scriptJMX, archivoJTL, archivoLog);

            if (exitoso) {
                System.out.println("✅ JMeter ejecutado exitosamente");
                verificarResultados(archivoJTL, archivoLog);
            } else {
                System.out.println("❌ Error ejecutando JMeter");
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String detectarJMeter() {
        String[] rutas = {"jmeter", "/opt/jmeter/bin/jmeter", "C:\\apache-jmeter\\bin\\jmeter.bat"};

        for (String ruta : rutas) {
            try {
                ProcessBuilder pb = new ProcessBuilder(ruta, "--version");
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                Process proceso = pb.start();

                if (proceso.waitFor(5, TimeUnit.SECONDS) && proceso.exitValue() == 0) {
                    return ruta;
                }
            } catch (Exception e) {
                // Continuar con siguiente ruta
            }
        }
        return null;
    }

    private static Path crearScriptSimple() throws IOException {
        String scriptContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <jmeterTestPlan version="1.2" properties="5.0" jmeter="5.6.3">
              <hashTree>
                <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Test Simple">
                  <elementProp name="TestPlan.arguments" elementType="Arguments">
                    <collectionProp name="Arguments.arguments"/>
                  </elementProp>
                </TestPlan>
                <hashTree>
                  <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Thread Group">
                    <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
                    <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
                      <boolProp name="LoopController.continue_forever">false</boolProp>
                      <intProp name="LoopController.loops">2</intProp>
                    </elementProp>
                    <stringProp name="ThreadGroup.num_threads">2</stringProp>
                    <stringProp name="ThreadGroup.ramp_time">2</stringProp>
                  </ThreadGroup>
                  <hashTree>
                    <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="Test Request">
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

        Path scriptPath = Paths.get("jmeter-results", "test-simple.jmx");
        Files.writeString(scriptPath, scriptContent);
        return scriptPath;
    }

    private static boolean ejecutarJMeterSimple(String jmeterPath, Path scriptJMX, Path archivoJTL, Path archivoLog) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    jmeterPath,
                    "-n",
                    "-t", scriptJMX.toAbsolutePath().toString(),
                    "-l", archivoJTL.toAbsolutePath().toString(),
                    "-j", archivoLog.toAbsolutePath().toString()
            );

            pb.redirectErrorStream(true);
            Process proceso = pb.start();

            // Capturar salida
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    System.out.println("JMeter: " + linea);
                }
            }

            boolean terminado = proceso.waitFor(30, TimeUnit.SECONDS);
            if (!terminado) {
                proceso.destroyForcibly();
                return false;
            }

            return proceso.exitValue() == 0;

        } catch (Exception e) {
            System.err.println("Error ejecutando JMeter: " + e.getMessage());
            return false;
        }
    }

    private static void verificarResultados(Path archivoJTL, Path archivoLog) throws IOException {
        if (Files.exists(archivoJTL)) {
            long size = Files.size(archivoJTL);
            System.out.println("📄 Archivo JTL generado: " + size + " bytes");

            if (size > 0) {
                System.out.println("📊 Primeras líneas del JTL:");
                Files.lines(archivoJTL).limit(5).forEach(linea -> System.out.println("  " + linea));
            }
        } else {
            System.out.println("❌ Archivo JTL no generado");
        }

        if (Files.exists(archivoLog)) {
            System.out.println("📋 Log de JMeter disponible: " + archivoLog);
        }
    }
}