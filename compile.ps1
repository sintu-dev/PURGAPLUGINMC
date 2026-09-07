$javac = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\javac.exe"
$jarTool = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\jar.exe"

New-Item -ItemType Directory -Force -Path "C:\Users\sintu\Documents\plugins\PurgaPlugin\bin" | Out-Null
New-Item -ItemType Directory -Force -Path "C:\Users\sintu\Documents\plugins\PurgaPlugin\target" | Out-Null

$sources = Get-ChildItem -Path "C:\Users\sintu\Documents\plugins\PurgaPlugin\src\main\java" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName

Write-Host "Compilando archivos Java..."
$jars = (Get-ChildItem "C:\Users\sintu\Documents\plugins\PurgaPlugin\lib\*.jar" | Select-Object -ExpandProperty FullName) -join ";"
& $javac -cp $jars -d "C:\Users\sintu\Documents\plugins\PurgaPlugin\bin" $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "COMPILACION EXITOSA!"
    Copy-Item "C:\Users\sintu\Documents\plugins\PurgaPlugin\src\main\resources\*" "C:\Users\sintu\Documents\plugins\PurgaPlugin\bin" -Recurse -Force
    & $jarTool --create --file "C:\Users\sintu\Documents\plugins\PurgaPlugin\target\PurgaPlugin-1.0.0.jar" -C "C:\Users\sintu\Documents\plugins\PurgaPlugin\bin" .
    Write-Host "JAR creado: C:\Users\sintu\Documents\plugins\PurgaPlugin\target\PurgaPlugin-1.0.0.jar"
} else {
    Write-Host "ERROR EN COMPILACION: $LASTEXITCODE"
}
