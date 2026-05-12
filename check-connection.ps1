Write-Host "=== Diagnóstico Final Lectuaria ===" -ForegroundColor Cyan

# 1. Encontrar pg_hba.conf
Write-Host "`n[1] Buscando pg_hba.conf..." -ForegroundColor Yellow
$pgHba = Get-ChildItem -Path "C:\" -Filter "pg_hba.conf" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
if ($pgHba) {
    Write-Host "✓ Encontrado: $pgHba" -ForegroundColor Green
    Write-Host "Métodos de autenticación:" -ForegroundColor Cyan
    Get-Content $pgHba | Select-String "host.*127.0.0.1|host.*::1" | ForEach-Object { Write-Host "  $_" }
}
else {
    Write-Host "✗ No encontrado" -ForegroundColor Red
}

# 2. Verificar data directory
Write-Host "`n[2] Data directory de PostgreSQL:" -ForegroundColor Yellow
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d postgres -c "SHOW data_directory;" 2>$null

# 3. Probar conexión JDBC
Write-Host "`n[3] Probando conexión JDBC..." -ForegroundColor Yellow
$testUrl = "jdbc:postgresql://localhost:5432/lectuaria"
$testUser = "postgres"
$testPass = "1234"

# Crear archivo de test temporal
$testFile = "$env:TEMP\TestJDBC.java"
@"
import java.sql.*;
public class TestJDBC {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection c = DriverManager.getConnection("$testUrl", "$testUser", "$testPass")) {
            System.out.println("SUCCESS");
        } catch (SQLException e) {
            System.out.println("FAIL: " + e.getMessage());
            System.exit(1);
        }
    }
}
"@ | Out-File $testFile -Encoding UTF8

# Compilar y ejecutar
javac $testFile 2>$null
if ($LASTEXITCODE -eq 0) {
    java -cp "$env:TEMP;C:\Program Files\PostgreSQL\18\lib\postgresql-42.7.3.jar" TestJDBC 2>&1
}
else {
    Write-Host "✗ Fallo al compilar test JDBC" -ForegroundColor Red
}

# 4. Verificar perfil Spring
Write-Host "`n[4] Verificando application-dev.properties:" -ForegroundColor Yellow
$devProps = "src/main/resources/application-dev.properties"
if (Test-Path $devProps) {
    Get-Content $devProps | Select-String "password|url|username" | ForEach-Object { Write-Host "  $_" }
}
else {
    Write-Host "✗ application-dev.properties no existe" -ForegroundColor Red
}