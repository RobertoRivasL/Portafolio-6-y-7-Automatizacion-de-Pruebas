package com.mediplus.pruebas.analisis.jmeter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LanzadorJMeter
 *
 * <p>
 * Propósito: Ejecutar todos los planes JMeter (.jmx) ubicados en la carpeta
 * {@code jmeter_plans_mediplus/} y generar:
 * </p>
 * <ul>
 *   <li>Resultados .jtl en {@code evidencias_jmeter_jtl/}</li>
 *   <li>Un dashboard HTML por plan en {@code evidencias_jmeter_html/&lt;plan&gt;_YYYY-MM-DD_HHMMSS/}</li>
 *   <li>Un <b>dashboard final consolidado</b> en {@code evidencias_jmeter_html/_dashboard_final_YYYY-MM-DD_HHMMSS/index.html}</li>
 *   <li>Logs de JMeter en {@code evidencias_jmeter_logs/}</li>
 * </ul>
 *
 * <p>Ejecuta JMeter vía {@code java -jar ApacheJMeter.jar} (sin ir al /bin).</p>
 * <p>Java 21, principios SOLID y nombres en español.</p>
 */
public final class LanzadorJMeter {

    // ================== CONFIGURACIÓN ==================

    /**
     * Ruta fija opcional al ApacheJMeter.jar.
     * Si la dejas en {@code null}, se usará la variable de entorno {@code JMETER_JAR}.
     * Ejemplo: Paths.get("D:\\Program Files\\Apache Software Foundation\\apache-jmeter-5.6.3\\bin\\ApacheJMeter.jar")
     */
    private static final Path RUTA_JMETER_JAR = null;

    /** Ejecutable de Java (debe estar en PATH). */
    private static final String JAVA_EXE = "java";

    /** Formato para timestamp de carpetas/archivos. */
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    /** Carpeta base: donde se ejecuta el proceso (pom del módulo). */
    private static final Path CARPETA_BASE = Paths.get(System.getProperty("user.dir"));

    /** Carpetas relativas esperadas junto al pom. */
    private static final Path CARPETA_PLANES = CARPETA_BASE.resolve("jmeter_plans_mediplus");
    private static final Path CARPETA_HTML   = CARPETA_BASE.resolve("evidencias_jmeter_html");
    private static final Path CARPETA_JTL    = CARPETA_BASE.resolve("evidencias_jmeter_jtl");
    private static final Path CARPETA_LOGS   = CARPETA_BASE.resolve("evidencias_jmeter_logs");

    private LanzadorJMeter() { }

    /**
     * Punto de entrada.
     *
     * <p><b>Propósito:</b> Descubrir .jmx, ejecutar cada plan con JMeter (dejando .jtl, HTML y log),
     * y construir un dashboard final consolidado.</p>
     *
     * <p><b>Parámetros:</b> no usa {@code args}. Opcionalmente puedes pasar filtros por propiedad de sistema:
     * <pre>-Dexec.args="get,post"</pre>
     * para ejecutar solo planes cuyo nombre contenga esas palabras.</p>
     */
    public static void main(String[] args) {
        // Un único timestamp para toda la corrida (así se puede consolidar fácilmente)
        final String TS = LocalDateTime.now().format(TS_FMT);

        try {
            validarConfiguracionBasica();
            crearCarpetasEvidencias();

            var escenarios = descubrirEscenarios(CARPETA_PLANES, TS);
            escenarios = aplicarFiltroDesdePropiedadExecArgs(escenarios);

            if (escenarios.isEmpty()) {
                System.err.println("No se encontraron archivos .jmx (o filtro dejó 0) en: " + CARPETA_PLANES.toAbsolutePath());
                System.exit(2);
            }

            var ejecutor = new EjecutorJMeter(resolverRutaJMeterJar(), JAVA_EXE);
            System.out.printf("=== Ejecutando %d escenario(s) en %s ===%n",
                    escenarios.size(), CARPETA_PLANES.toAbsolutePath());

            boolean todoOk = true;
            for (EscenarioJMeter esc : escenarios) {
                System.out.printf("%n--- Plan: %s%n", esc.nombrePlan());
                try {
                    ResultadoEjecucion r = ejecutor.ejecutar(esc);
                    if (!r.salidaStd().isBlank()) System.out.print(r.salidaStd());
                    if (!r.errorStd().isBlank())  System.err.print(r.errorStd());

                    if (r.codigoSalida() != 0) {
                        todoOk = false;
                        System.err.printf("⚠️  Falló (exit=%d). Log: %s%n", r.codigoSalida(), esc.rutaLog());
                    } else {
                        System.out.printf("✅ OK  → %s%n", esc.rutaCarpetaHtml().resolve("index.html"));
                    }
                } catch (Exception e) {
                    todoOk = false;
                    System.err.printf("❌ Error en '%s': %s%n", esc.nombrePlan(), e.getMessage());
                    e.printStackTrace(System.err);
                }
            }

            // Dashboard final consolidado
            Path carpetaFinal = CARPETA_HTML.resolve("_dashboard_final_" + TS);
            try {
                generarDashboardFinal(
                        escenarios,
                        carpetaFinal,
                        "MediPlus API - Resumen de Pruebas de Rendimiento",
                        List.of("Antonio B. Arriagada LL.", "Dante Escalona Bustos", "Roberto Rivas Lopez")
                );
                System.out.println("\n📊 Dashboard final: " + carpetaFinal.resolve("index.html"));
            } catch (Exception ex) {
                System.err.println("No fue posible generar el dashboard final: " + ex.getMessage());
            }

            if (!todoOk) System.exit(1);
        } catch (Exception fatal) {
            System.err.println("❌ Error fatal: " + fatal.getMessage());
            fatal.printStackTrace(System.err);
            System.exit(3);
        }
    }

    // ================== Descubrimiento/Preparación ==================

    /**
     * Propósito: validar que exista ApacheJMeter.jar y la carpeta de planes .jmx.
     * @throws IllegalStateException si falta algún requisito.
     */
    private static void validarConfiguracionBasica() {
        Path jmeterJar = resolverRutaJMeterJar();
        if (jmeterJar == null || !Files.isRegularFile(jmeterJar)) {
            throw new IllegalStateException("""
                No se encontró ApacheJMeter.jar.
                Define la variable de entorno JMETER_JAR con la ruta completa al jar
                o fija la constante RUTA_JMETER_JAR en esta clase.""");
        }
        if (!Files.isDirectory(CARPETA_PLANES)) {
            throw new IllegalStateException("Falta carpeta con planes .jmx: " + CARPETA_PLANES.toAbsolutePath());
        }
    }

    /**
     * Propósito: crear (si no existen) las carpetas de evidencias.
     */
    private static void crearCarpetasEvidencias() throws IOException {
        Files.createDirectories(CARPETA_HTML);
        Files.createDirectories(CARPETA_JTL);
        Files.createDirectories(CARPETA_LOGS);
    }

    /**
     * Propósito: construir la lista de escenarios a partir de los .jmx, con rutas de salida usando el mismo timestamp.
     * @param carpetaPlanes carpeta donde están los .jmx (p.ej. {@code jmeter_plans_mediplus/})
     * @param timestamp     timestamp de esta corrida, usado para jtl/html/log
     * @return lista de escenarios inmutables listos para ejecutar.
     */
    private static List<EscenarioJMeter> descubrirEscenarios(Path carpetaPlanes, String timestamp) throws IOException {
        try (var stream = Files.list(carpetaPlanes)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jmx"))
                    .sorted()
                    .map(jmx -> {
                        String nombre = quitarExtension(jmx.getFileName().toString());
                        Path jtl  = CARPETA_JTL.resolve(nombre + "_" + timestamp + ".jtl");
                        Path html = CARPETA_HTML.resolve(nombre + "_" + timestamp);
                        Path log  = CARPETA_LOGS.resolve(nombre + "_" + timestamp + ".log");
                        return new EscenarioJMeter(nombre, jmx, jtl, html, log);
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * Propósito: aplicar filtro opcional por nombre (propiedad -Dexec.args="get,post").
     */
    private static List<EscenarioJMeter> aplicarFiltroDesdePropiedadExecArgs(List<EscenarioJMeter> escenarios) {
        String raw = System.getProperty("exec.args", "").trim();
        if (raw.isBlank()) return escenarios;
        List<String> filtros = Arrays.stream(raw.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).map(String::toLowerCase).toList();
        System.out.println("Filtro por nombre de plan: " + filtros);
        return escenarios.stream()
                .filter(e -> filtros.stream().anyMatch(f -> e.nombrePlan().toLowerCase().contains(f)))
                .toList();
    }

    /**
     * Propósito: quitar la extensión de un archivo.
     * @param nombreArchivo p.ej. {@code 01_get.jmx}
     * @return p.ej. {@code 01_get}
     */
    private static String quitarExtension(String nombreArchivo) {
        int i = nombreArchivo.lastIndexOf('.');
        return (i > 0) ? nombreArchivo.substring(0, i) : nombreArchivo;
    }

    /**
     * Propósito: resolver la ruta al jar de JMeter.
     * Prioriza la constante {@link #RUTA_JMETER_JAR} y si está en null usa la variable de entorno {@code JMETER_JAR}.
     */
    private static Path resolverRutaJMeterJar() {
        if (RUTA_JMETER_JAR != null) return RUTA_JMETER_JAR;
        String env = System.getenv("JMETER_JAR");
        return (env != null && !env.isBlank()) ? Paths.get(env) : null;
    }

    // ================== Tipos de dominio ==================

    /** Representa un escenario .jmx con sus rutas de salida. */
    public record EscenarioJMeter(
            String nombrePlan,
            Path rutaJmx,
            Path rutaJtl,
            Path rutaCarpetaHtml,
            Path rutaLog
    ) { }

    /** Resultado del proceso de JMeter. */
    public record ResultadoEjecucion(int codigoSalida, String salidaStd, String errorStd) { }

    // ================== Ejecutor de JMeter ==================

    /**
     * Única responsabilidad: armar y ejecutar
     * <pre>java -jar ApacheJMeter.jar -n -t ... -l ... -e -o ... -j ...</pre>
     * y devolver stdout/stderr/código de salida.
     */
    public static final class EjecutorJMeter {
        private final Path rutaJmeterJar;
        private final String javaExe;

        /**
         * Propósito: crear el ejecutor.
         * @param rutaJmeterJar Ruta al ApacheJMeter.jar (no nula).
         * @param javaExe       Ejecutable de Java (en PATH).
         */
        public EjecutorJMeter(Path rutaJmeterJar, String javaExe) {
            this.rutaJmeterJar = Objects.requireNonNull(rutaJmeterJar);
            this.javaExe = Objects.requireNonNull(javaExe);
        }

        /**
         * Propósito: ejecutar un escenario .jmx en modo no-GUI dejando resultados y reportes.
         * @param esc Escenario con rutas (.jmx, .jtl, carpeta HTML, log).
         * @return Resultado de la ejecución.
         */
        public ResultadoEjecucion ejecutar(EscenarioJMeter esc) throws IOException, InterruptedException {
            // Preparar directorios
            crearPadresDeArchivo(esc.rutaJtl());
            crearPadresDeArchivo(esc.rutaLog());
            if (Files.exists(esc.rutaCarpetaHtml())) {
                // No borramos históricos: como la carpeta lleva timestamp, no debería existir.
                throw new IOException("La carpeta HTML ya existe y debe ser nueva: " + esc.rutaCarpetaHtml());
            }
            Files.createDirectories(esc.rutaCarpetaHtml());

            List<String> cmd = List.of(
                    javaExe, "-jar", rutaJmeterJar.toAbsolutePath().toString(),
                    "-n",
                    "-t", esc.rutaJmx().toAbsolutePath().toString(),
                    "-l", esc.rutaJtl().toAbsolutePath().toString(),
                    "-e",
                    "-o", esc.rutaCarpetaHtml().toAbsolutePath().toString(),
                    "-j", esc.rutaLog().toAbsolutePath().toString()
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(CARPETA_BASE.toFile());

            Process p = pb.start();
            String out = leerTodo(p.getInputStream());
            String err = leerTodo(p.getErrorStream());
            int code = p.waitFor();
            return new ResultadoEjecucion(code, out, err);
        }

        /** Crea carpeta padre del archivo si corresponde. */
        private static void crearPadresDeArchivo(Path archivo) throws IOException {
            Path padre = archivo.getParent();
            if (padre != null) Files.createDirectories(padre);
        }

        /** Lee un InputStream completo a String (UTF-8). */
        private static String leerTodo(java.io.InputStream is) throws IOException {
            try (var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append(System.lineSeparator());
                return sb.toString();
            }
        }
    }

    // ================== Dashboard final (consolidado) ==================

    /** Métricas básicas consolidadas a partir de un .jtl CSV. */
    private record Metricas(long total, long exitos, long fallos,
                            long duracionSeg, double avgMs, double p90, double p95, double throughput) {
        boolean ok() { return total > 0 && fallos == 0; }
    }

    private record Fila(String escenario, String usuarios, Metricas m) { }

    /**
     * Propósito: generar un HTML consolidado con las métricas de todos los .jtl ejecutados.
     * @param escenarios  Escenarios ejecutados (se usarán sus .jtl)
     * @param outDir      Carpeta de salida del dashboard final (se crea).
     * @param titulo      Título del informe.
     * @param autores     Lista de autores a mostrar.
     */
    private static void generarDashboardFinal(List<EscenarioJMeter> escenarios, Path outDir,
                                              String titulo, List<String> autores) throws IOException {
        Files.createDirectories(outDir);
        List<Fila> filas = new ArrayList<>();
        for (EscenarioJMeter esc : escenarios) {
            Metricas m = calcularMetricasDesdeJtl(esc.rutaJtl());
            String usuarios = extraerUsuariosDesdeNombre(esc.nombrePlan()); // si el nombre trae "_10_usuarios", etc.
            filas.add(new Fila(esc.nombrePlan(), usuarios, m));
        }
        String html = renderizarHtmlFinal(titulo, autores, filas);
        Files.writeString(outDir.resolve("index.html"), html, StandardCharsets.UTF_8);
    }

    /** Intenta extraer "N" de un patrón *_N_usuarios* en el nombre del plan. */
    private static String extraerUsuariosDesdeNombre(String nombre) {
        var lower = nombre.toLowerCase(Locale.ROOT);
        int i = lower.indexOf("_usuarios");
        if (i > 0) {
            int j = lower.lastIndexOf('_', i - 1);
            if (j >= 0) {
                String num = lower.substring(j + 1, i).replaceAll("\\D+", "");
                if (!num.isBlank()) return num;
            }
        }
        return "-";
    }

    /** Lee un .jtl (con o sin cabecera) y calcula total, éxitos, p90/p95, avg, duración y throughput. */
    private static Metricas calcularMetricasDesdeJtl(Path jtl) throws IOException {
        if (!Files.isRegularFile(jtl)) return new Metricas(0,0,0,0,0,0,0,0);

        List<String> lines = Files.readAllLines(jtl, StandardCharsets.UTF_8)
                .stream().filter(s -> !s.isBlank()).toList();
        if (lines.isEmpty()) return new Metricas(0,0,0,0,0,0,0,0);

        int start = 0;
        int idxTs, idxElapsed, idxSuccess;

        // Soportar JTL con y sin cabecera (JMeter por defecto puede no imprimir cabecera)
        String first = lines.get(0);
        if (first.toLowerCase(Locale.ROOT).contains("timestamp") || first.toLowerCase(Locale.ROOT).contains("timeStamp".toLowerCase())) {
            String[] hdr = first.split(",", -1);
            idxElapsed = indexOf(hdr, "elapsed");
            idxSuccess = indexOf(hdr, "success");
            idxTs      = Math.max(indexOf(hdr, "timeStamp"), indexOf(hdr, "timestamp"));
            start = 1;
        } else {
            // Layout por defecto (sin cabecera): timeStamp, elapsed, label, responseCode, responseMessage,
            // threadName, dataType, success, failureMessage, bytes, sentBytes, grpThreads, allThreads,
            // URL, Latency, IdleTime, Connect
            idxTs = 0; idxElapsed = 1; idxSuccess = 7;
        }

        if (idxElapsed < 0 || idxSuccess < 0 || idxTs < 0) return new Metricas(0,0,0,0,0,0,0,0);

        List<Integer> elapsed = new ArrayList<>();
        List<Long>    stamps  = new ArrayList<>();
        long total=0, exitos=0;

        for (int i = start; i < lines.size(); i++) {
            String[] c = lines.get(i).split(",", -1);
            try {
                int e = Integer.parseInt(c[idxElapsed].trim());
                boolean ok = "true".equalsIgnoreCase(c[idxSuccess].trim());
                long ts = Long.parseLong(c[idxTs].trim());
                elapsed.add(e);
                stamps.add(ts);
                total++;
                if (ok) exitos++;
            } catch (Exception ignore) { /* línea defectuosa */ }
        }

        long fallos = total - exitos;
        if (total == 0) return new Metricas(0,0,0,0,0,0,0,0);

        long minTs = stamps.stream().mapToLong(Long::longValue).min().orElse(0L);
        long maxTs = stamps.stream().mapToLong(Long::longValue).max().orElse(0L);
        long durSeg = Math.max(1, (maxTs - minTs) / 1000);  // evitar división por cero

        double avg = elapsed.stream().mapToInt(Integer::intValue).average().orElse(0);
        Collections.sort(elapsed);
        double p90 = percentile(elapsed, 0.90);
        double p95 = percentile(elapsed, 0.95);
        double thr = total / (double) durSeg;

        return new Metricas(total, exitos, fallos, durSeg, round(avg), round(p90), round(p95), round(thr));
    }

    private static int indexOf(String[] headers, String name) {
        for (int i=0;i<headers.length;i++) if (headers[i].trim().equalsIgnoreCase(name)) return i;
        return -1;
    }
    private static double percentile(List<Integer> sorted, double q) {
        if (sorted.isEmpty()) return 0;
        double pos = q * (sorted.size() - 1);
        int lo = (int) Math.floor(pos), hi = (int) Math.ceil(pos);
        if (lo == hi) return sorted.get(lo);
        return sorted.get(lo) + (pos - lo) * (sorted.get(hi) - sorted.get(lo));
    }
    private static double round(double x) { return Math.round(x * 100.0) / 100.0; }

    /** HTML minimalista para el dashboard final (similar al de tu captura). */
    private static String renderizarHtmlFinal(String titulo, List<String> autores, List<Fila> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <!doctype html><html lang="es"><head><meta charset="utf-8">
            <title>""").append(titulo).append("""
            </title>
            <style>
            body{font-family:Segoe UI,Arial,sans-serif;background:#0e1621;color:#e6edf3;margin:0}
            .card{background:#1f2937;margin:16px;padding:16px;border-radius:12px}
            table{width:100%;border-collapse:collapse}
            th,td{padding:10px;border-bottom:1px solid #2d3748;text-align:left}
            th{background:#111827;position:sticky;top:0}
            .ok{color:#10b981;font-weight:600}.fail{color:#ef4444;font-weight:700}
            .muted{color:#9ca3af;font-size:0.9em}
            </style></head><body>
            """);
        sb.append("<div class='card'><h2>").append(titulo).append("</h2>")
                .append("<div class='muted'>Generado: ").append(LocalDateTime.now()).append("</div>")
                .append("<div class='muted'>Autores: ").append(String.join(", ", autores)).append("</div></div>");

        sb.append("<div class='card'><table><thead><tr>")
                .append("<th>Escenario</th><th>Usuarios</th><th>Duración (s)</th>")
                .append("<th>Tiempo Prom. (ms)</th><th>P90 (ms)</th><th>P95 (ms)</th>")
                .append("<th>Throughput (req/s)</th><th>Tasa Error (%)</th><th>Estado</th>")
                .append("</tr></thead><tbody>");

        for (Fila f : filas) {
            Metricas m = f.m();
            double errPct = (m.total()==0) ? 100.0 : (100.0 * m.fallos() / m.total());
            boolean ok = m.ok();
            sb.append("<tr><td>").append(f.escenario()).append("</td>")
                    .append("<td>").append(f.usuarios()).append("</td>")
                    .append("<td>").append(m.duracionSeg()).append("</td>")
                    .append("<td>").append(m.avgMs()).append("</td>")
                    .append("<td>").append(m.p90()).append("</td>")
                    .append("<td>").append(m.p95()).append("</td>")
                    .append("<td>").append(m.throughput()).append("</td>")
                    .append("<td>").append(round(errPct)).append("</td>")
                    .append("<td class='").append(ok ? "ok" : "fail").append("'>")
                    .append(ok ? "OK" : "FALLIDO").append("</td></tr>");
        }
        sb.append("</tbody></table></div></body></html>");
        return sb.toString();
    }
}
