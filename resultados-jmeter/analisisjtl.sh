set -e

# Configuración por defecto
DIRECTORIO_RESULTADOS="resultados"
DIRECTORIO_REPORTES="reportes"
JAR_FILE="target/analisis-metricas-mediplus.jar"

# Función de ayuda
mostrar_ayuda() {
    echo "Uso: $0 [OPCIONES]"
    echo ""
    echo "Opciones:"
    echo "  -r, --resultados DIR    Directorio con archivos JTL (default: resultados)"
    echo "  -o, --output DIR        Directorio de salida (default: reportes)"
    echo "  -j, --jar FILE          Archivo JAR a ejecutar (default: target/analisis-metricas-mediplus.jar)"
    echo "  -h, --help              Mostrar esta ayuda"
    echo ""
    echo "Ejemplos:"
    echo "  $0                                           # Usar configuración por defecto"
    echo "  $0 -r mis-resultados -o mis-reportes        # Directorios personalizados"
    echo "  $0 --resultados /tmp/jtl --output /tmp/out  # Rutas absolutas"
}

# Procesar argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        -r|--resultados)
            DIRECTORIO_RESULTADOS="$2"
            shift 2
            ;;
        -o|--output)
            DIRECTORIO_REPORTES="$2"
            shift 2
            ;;
        -j|--jar)
            JAR_FILE="$2"
            shift 2
            ;;
        -h|--help)
            mostrar_ayuda
            exit 0
            ;;
        *)
            echo "❌ Opción desconocida: $1"
            mostrar_ayuda
            exit 1
            ;;
    esac
done

echo "🚀 Ejecutando Análisis de Métricas MediPlus"
echo "==========================================="
echo "📂 Directorio de resultados: $DIRECTORIO_RESULTADOS"
echo "📂 Directorio de reportes: $DIRECTORIO_REPORTES"
echo "📦 JAR: $JAR_FILE"
echo ""

# Verificar que existe el JAR
if [[ ! -f "$JAR_FILE" ]]; then
    echo "❌ Error: No se encuentra el archivo JAR: $JAR_FILE"
    echo "💡 Ejecuta: mvn package"
    exit 1
fi

# Verificar que existe el directorio de resultados
if [[ ! -d "$DIRECTORIO_RESULTADOS" ]]; then
    echo "❌ Error: No se encuentra el directorio de resultados: $DIRECTORIO_RESULTADOS"
    exit 1
fi

# Crear directorio de reportes si no existe
mkdir -p "$DIRECTORIO_REPORTES"

# Verificar archivos JTL
jtl_count=$(find "$DIRECTORIO_RESULTADOS" -name "*.jtl" | wc -l)
if [[ $jtl_count -eq 0 ]]; then
    echo "⚠️ Advertencia: No se encontraron archivos .jtl en $DIRECTORIO_RESULTADOS"
    echo "💡 Asegúrate de haber ejecutado las pruebas JMeter primero"
fi

echo "📊 Archivos JTL encontrados: $jtl_count"
echo ""

# Ejecutar análisis
echo "🔄 Iniciando análisis..."
java -jar "$JAR_FILE" \
    -Ddirectorio.resultados="$DIRECTORIO_RESULTADOS" \
    -Ddirectorio.reportes="$DIRECTORIO_REPORTES"

echo ""
echo "✅ Análisis completado!"
echo "📋 Revisa los reportes en: $DIRECTORIO_REPORTES"