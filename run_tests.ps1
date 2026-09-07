$javac = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\javac.exe'
$java = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\java.exe'
$jars = (Get-ChildItem 'lib/*.jar' | Select-Object -ExpandProperty FullName) -join ';'
$cp = 'bin;' + $jars

Write-Host 'Compilando tests...'
& $javac -cp $cp -d 'bin' 'src/test/java/PurgaSystemTest.java'

if ($LASTEXITCODE -eq 0) {
    Write-Host 'Ejecutando tests...'
    & $java -cp $cp PurgaSystemTest
} else {
    Write-Host 'Fallo al compilar tests'
}
