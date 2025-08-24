package com.mediplus.pruebas.analisis;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lee el CSV resumen (_summary.csv) producido por el runner JMeter y genera
 * un dashboard HTML con comparativa + observaciones y recomendaciones.
 *
 * Entradas:
 *   evidencias_jmeter_html/_summary.csv
 *
 * Salida:
 *   evidencias_jmeter_html/_resumen_final/index.html
 *
 * Uso:
 *   mvn -q exec:java -Dexec.mainClass=com.mediplus.pruebas.analisis.GeneradorDashboardResumen
 *
 * (Opcional) parámetros:
 *   -DsummaryCsv=evidencias_jmeter_html/_summary.csv
 *   -DoutDir=evidencias_jmeter_html/_resumen_final
 *   -Dwarn.p90.ms=2000 -Dbad.p90.ms=3000
 *   -Dwarn.err.pct=5 -Dbad.err.pct=10
 */
public class GeneradorDashboardResumen {

    // Umbrales (override con -D...)
    private static final double WARN_P90_MS = Double.parseDouble(System.getProperty("warn.p90.ms", "2000"));
    private static final double BAD_P90_MS  = Double.parseDouble(System.getProperty("bad.p90.ms",  "3000"));
    private static final double WARN_ERR_PCT= Double.parseDouble(System.getProperty("warn.err.pct","5"));
    private static final double BAD_ERR_PCT = Double.parseDouble(System.getProperty("bad.err.pct", "10"));

    private static final Path CSV_PATH = Paths.get(System.getProperty("summaryCsv",
            "evidencias_jmeter_html/_summary.csv"));
    private static final Path OUT_DIR  = Paths.get(System.getProperty("outDir",
            "evidencias_jmeter_html/_resumen_final"));

    record Row(
            String plan, Integer users, Integer rampup, Integer duration,
            Long samples, Double avgMs, Double p90Ms, Double errPct, Double tps
    ) {}

    public static void main(String[] args) throws Exception {
        if (!Files.exists(CSV_PATH)) {
            System.err.println("No existe el CSV: " + CSV_PATH.toAbsolutePath());
            System.exit(2);
        }
        List<Row> rows = readSummaryCsv(CSV_PATH);
        if (rows.isEmpty()) {
            System.err.println("CSV sin filas útiles. Nada que procesar.");
            System.exit(3);
        }
        Files.createDirectories(OUT_DIR);

        // Agrupar por plan
        Map<String, List<Row>> porPlan = rows.stream()
                .collect(Collectors.groupingBy(r -> r.plan, LinkedHashMap::new, Collectors.toList()));

        // Observaciones por fila
        LinkedHashMap<Row, List<String>> obsPorFila = new LinkedHashMap<>();
        for (Row r : rows) {
            List<String> obs = new ArrayList<>();
            if (r.errPct != null) {
                if (r.errPct >= BAD_ERR_PCT) obs.add("❌ error% muy alto (" + fmt(r.errPct) + "% ≥ " + BAD_ERR_PCT + "%)");
                else if (r.errPct >= WARN_ERR_PCT) obs.add("⚠ error% elevado (" + fmt(r.errPct) + "% ≥ " + WARN_ERR_PCT + "%)");
                else obs.add("✅ error% bajo (" + fmt(r.errPct) + "%)");
            } else {
                obs.add("⚠ error% N/A");
            }

            if (r.p90Ms != null) {
                if (r.p90Ms >= BAD_P90_MS) obs.add("❌ p90 alto (" + fmtMs(r.p90Ms) + " ≥ " + (int)BAD_P90_MS + "ms)");
                else if (r.p90Ms >= WARN_P90_MS) obs.add("⚠ p90 elevado (" + fmtMs(r.p90Ms) + " ≥ " + (int)WARN_P90_MS + "ms)");
                else obs.add("✅ p90 OK (" + fmtMs(r.p90Ms) + ")");
            } else {
                obs.add("⚠ p90 N/A");
            }

            if (r.tps != null) obs.add("↗ throughput " + fmt(r.tps) + " req/s");
            if (r.samples != null) obs.add("muestras " + r.samples);
            obsPorFila.put(r, obs);
        }

        // Recomendaciones por plan (mirando tendencias por usuarios)
        LinkedHashMap<String, List<String>> recomendacionesPlan = new LinkedHashMap<>();
        for (var e : porPlan.entrySet()) {
            String plan = e.getKey();
            List<Row> lst = e.getValue().stream()
                    .sorted(Comparator.comparing((Row r) -> r.users == null ? 0 : r.users))
                    .toList();
            List<String> recs = new ArrayList<>();

            // Detectar degradación fuerte de p90 al subir carga
            List<Row> conP90 = lst.stream().filter(r -> r.p90Ms != null && r.users != null).toList();
            for (int i = 1; i < conP90.size(); i++) {
                Row prev = conP90.get(i - 1);
                Row cur  = conP90.get(i);
                if (prev.p90Ms != null && cur.p90Ms != null) {
                    double inc = (cur.p90Ms - prev.p90Ms) / prev.p90Ms * 100.0;
                    if (inc >= 50) {
                        recs.add("🔎 P90 sube " + fmt(inc) + "% al pasar de "
                                + prev.users + "→" + cur.users + " usuarios. Revisar cuellos (DB, concurrencia, timeouts).");
                        break;
                    }
                }
            }

            // Error% consistente alto en cualquier carga
            boolean errorAlto = lst.stream().anyMatch(r -> r.errPct != null && r.errPct >= BAD_ERR_PCT);
            if (errorAlto) recs.add("🧪 Error% ≥ " + BAD_ERR_PCT + "% en alguna carga. Validar aserciones/errores funcionales y límites del SUT.");

            // Throughput decreciente con más usuarios (saturación)
            List<Row> conTPS = lst.stream().filter(r -> r.tps != null && r.users != null).toList();
            if (conTPS.size() >= 2) {
                double t0 = conTPS.get(0).tps;
                double tn = conTPS.get(conTPS.size() - 1).tps;
                if (tn < t0 * 0.7) {
                    recs.add("📉 Throughput cae >30% entre la carga mínima y máxima. Posible saturación o lock contention.");
                }
            }

            // Si no hubo hallazgos, deja recomendación genérica
            if (recs.isEmpty()) {
                recs.add("✅ Métricas estables. Aumentar gradualmente duración/usuarios para buscar umbral de saturación.");
            }
            recomendacionesPlan.put(plan, recs);
        }

        String html = renderHtml(rows, obsPorFila, recomendacionesPlan);
        Path out = OUT_DIR.resolve("index.html");
        Files.writeString(out, html, StandardCharsets.UTF_8);
        System.out.println("Dashboard generado: " + out.toAbsolutePath());
    }

    private static String fmt(double d) {
        return (Math.round(d * 100.0) / 100.0) + "";
    }
    private static String fmtMs(double d) {
        return ((int)Math.round(d)) + " ms";
    }

    // CSV robusto: soporta ',' o ';' y comillas básicas.
    private static List<Row> readSummaryCsv(Path p) throws IOException {
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8)
                .stream().filter(s -> !s.trim().isEmpty()).toList();
        if (lines.isEmpty()) return List.of();
        String header = lines.get(0);
        String sep = header.contains(";") ? ";" : ",";

        String[] cols = splitCsv(header, sep);
        Map<String,Integer> idx = new HashMap<>();
        for (int i=0;i<cols.length;i++) idx.put(cols[i].trim().toLowerCase(Locale.ROOT), i);

        List<Row> out = new ArrayList<>();
        for (int ln=1; ln<lines.size(); ln++) {
            String[] c = splitCsv(lines.get(ln), sep);
            out.add(new Row(
                    getS(idx, c, "plan"),
                    getI(idx, c, "users"),
                    getI(idx, c, "rampup"),
                    getI(idx, c, "duration"),
                    getL(idx, c, "samples"),
                    getD(idx, c, "avg_ms"),
                    getD(idx, c, "p90_ms"),
                    getD(idx, c, "error_pct"),
                    getD(idx, c, "throughput")
            ));
        }
        return out;
    }

    private static String[] splitCsv(String line, String sep) {
        // split muy simple que respeta comillas dobles
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i=0;i<line.length();i++) {
            char ch = line.charAt(i);
            if (ch=='"') { inQ = !inQ; continue; }
            if (!inQ && line.startsWith(sep, i)) {
                out.add(cur.toString().trim());
                cur.setLength(0);
                i += sep.length()-1;
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private static String getS(Map<String,Integer> idx, String[] c, String key) {
        Integer i = idx.get(key); if (i==null || i>=c.length) return null;
        String v = c[i].trim();
        return v.isEmpty()? null : v;
    }
    private static Integer getI(Map<String,Integer> idx, String[] c, String key) {
        String s = getS(idx,c,key); if (s==null) return null;
        try { return Integer.parseInt(s.replaceAll("[^0-9-]", "")); } catch(Exception e){ return null; }
    }
    private static Long getL(Map<String,Integer> idx, String[] c, String key) {
        String s = getS(idx,c,key); if (s==null) return null;
        try { return Long.parseLong(s.replaceAll("[^0-9-]", "")); } catch(Exception e){ return null; }
    }
    private static Double getD(Map<String,Integer> idx, String[] c, String key) {
        String s = getS(idx,c,key); if (s==null) return null;
        try {
            // soporta 1,23 o 1.23
            s = s.replace("%","").replace(" ", "");
            s = s.replace(",", ".");
            return Double.parseDouble(s);
        } catch(Exception e){ return null; }
    }

    private static String badge(Double v, Double warn, Double bad, boolean lowerIsBetter, String unit) {
        if (v == null) return "<span class='badge gray'>N/A</span>";
        boolean warnCond = lowerIsBetter ? v >= warn : v <= warn;
        boolean badCond  = lowerIsBetter ? v >= bad  : v <= bad;
        String cls = badCond ? "red" : (warnCond ? "yellow" : "green");
        return "<span class='badge "+cls+"'>" + fmt(v) + (unit==null?"":(" "+unit)) + "</span>";
    }

    private static String renderHtml(List<Row> rows,
                                     Map<Row, List<String>> obsPorFila,
                                     Map<String, List<String>> recomendacionesPlan) {

        // Ordenar tabla: por plan y usuarios
        List<Row> sorted = rows.stream()
                .sorted(Comparator.comparing((Row r) -> r.plan == null ? "" : r.plan)
                        .thenComparing(r -> r.users == null ? 0 : r.users))
                .toList();

        StringBuilder sb = new StringBuilder(64_000);
        sb.append("""
<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>Resumen JMeter – Comparativa y Recomendaciones</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Helvetica,Arial,sans-serif;margin:20px;color:#222}
h1{margin:0 0 6px}
h2{margin-top:28px}
small{color:#666}
table{border-collapse:collapse;width:100%;margin-top:12px}
th,td{border-bottom:1px solid #eee;padding:8px 10px;text-align:left;font-size:14px}
tr:hover{background:#fafafa}
.badge{display:inline-block;padding:2px 8px;border-radius:14px;font-size:12px;font-weight:600}
.badge.green{background:#E8F5E9;color:#1B5E20}
.badge.yellow{background:#FFF8E1;color:#8D6E00}
.badge.red{background:#FFEBEE;color:#B71C1C}
.badge.gray{background:#ECEFF1;color:#37474F}
.note{margin:6px 0 0;color:#444;font-size:12.5px}
.card{border:1px solid #eee;border-radius:12px;padding:14px 16px;margin:12px 0;background:#fff;box-shadow:0 1px 2px rgba(0,0,0,.03)}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:12px}
.kpi{font-size:13px;color:#555}
footer{margin-top:34px;color:#777;font-size:12px}
</style>
</head>
<body>
<h1>Resumen JMeter</h1>
<small>Generado automáticamente a partir de <code>_summary.csv</code>. Umbrales: p90 warn=""" + (int)WARN_P90_MS + "ms, bad=" + (int)BAD_P90_MS +
                "ms; error% warn=" + WARN_ERR_PCT + ", bad=" + BAD_ERR_PCT + ".</small>\n");

        // Tabla comparativa
        sb.append("<h2>Comparativa por escenario y carga</h2>\n<table>\n<thead><tr>")
                .append("<th>Plan</th><th>Usuarios</th><th>Duración</th><th>Samples</th>")
                .append("<th>AVG</th><th>P90</th><th>Error%</th><th>TPS</th><th>Observaciones</th>")
                .append("</tr></thead><tbody>\n");

        for (Row r : sorted) {
            String avgB = badge(r.avgMs, WARN_P90_MS*0.6, BAD_P90_MS*0.8, true, "ms"); // heurística
            String p90B = badge(r.p90Ms, WARN_P90_MS, BAD_P90_MS, true, "ms");
            String errB = badge(r.errPct, WARN_ERR_PCT, BAD_ERR_PCT, true, "%");
            String tpsB = (r.tps == null) ? "<span class='badge gray'>N/A</span>" : "<span class='badge green'>" + fmt(r.tps) + "</span>";
            String obs = String.join(" · ", obsPorFila.getOrDefault(r, List.of()));
            sb.append("<tr>")
                    .append("<td>").append(escape(r.plan)).append("</td>")
                    .append("<td>").append(r.users == null ? "N/A" : r.users).append("</td>")
                    .append("<td>").append(r.duration == null ? "N/A" : r.duration + "s").append("</td>")
                    .append("<td>").append(r.samples == null ? "N/A" : r.samples).append("</td>")
                    .append("<td>").append(avgB).append("</td>")
                    .append("<td>").append(p90B).append("</td>")
                    .append("<td>").append(errB).append("</td>")
                    .append("<td>").append(tpsB).append("</td>")
                    .append("<td class='note'>").append(escape(obs)).append("</td>")
                    .append("</tr>\n");
        }
        sb.append("</tbody></table>\n");

        // Recomendaciones por plan
        sb.append("<h2>Observaciones y recomendaciones por escenario</h2>\n<div class='grid'>\n");
        for (var e : recomendacionesPlan.entrySet()) {
            sb.append("<div class='card'><b>").append(escape(e.getKey())).append("</b><ul>");
            for (String r : e.getValue()) sb.append("<li class='kpi'>").append(escape(r)).append("</li>");
            sb.append("</ul></div>\n");
        }
        sb.append("</div>\n");

        sb.append("""
<footer>
Generado por <code>GeneradorDashboardResumen</code>. Puedes ajustar umbrales con -Dwarn.p90.ms, -Dbad.p90.ms, -Dwarn.err.pct, -Dbad.err.pct.
</footer>
</body>
</html>""");

        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
