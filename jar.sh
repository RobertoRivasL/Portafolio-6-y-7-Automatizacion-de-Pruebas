
set -e  # Salir si cualquier comando falla

echo "🚀 Iniciando construcción del proyecto Análisis de Métricas MediPlus"
echo "======================================================================"

# Verificar Java 21
echo "🔍 Verificando Java 21..."
if ! java -version 2>&1 | grep -q "21"; then
    echo "❌ Error: Se requiere Java 21"
    exit 1
fi
echo "✅ Java 21 encontrado"

# Verificar Maven
echo "🔍 Verificando Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven no está instalado"
    exit 1
fi
echo "✅ Maven encontrado: $(mvn -version | head -1)"

# Limpiar build anterior
echo "🧹 Limpiando builds anteriores..."
mvn clean

# Compilar proyecto
echo "🔨 Compilando proyecto..."
mvn compile

# Ejecutar pruebas
echo "🧪 Ejecutando pruebas unitarias..."
mvn test

# Generar reporte de cobertura
echo "📊 Generando reporte de cobertura..."
mvn jacoco:report

# Compilar y empaquetar
echo "📦 Empaquetando aplicación..."
mvn package -DskipTests

# Generar documentación
echo "📚 Generando documentación..."
mvn javadoc:javadoc

# Verificar estructura de directorios
echo "📁 Verificando estructura de directorios..."
mkdir -p resultados
mkdir -p reportes
mkdir -p logs

echo ""
echo "✅ Construcción completada exitosamente!"
echo "📋 Archivos generados:"
echo "   - JAR ejecutable: target/analisis-metricas-mediplus.jar"
echo "   - Reporte cobertura: target/site/jacoco/index.html"
echo "   - Documentación: target/site/apidocs/index.html"
echo ""
echo "🚀 Para ejecutar: java -jar target/analisis-metricas-mediplus.jar"