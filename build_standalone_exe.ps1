Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ">>> STARTING STANDALONE KOCKROCH EXE COMPILATION <<<" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$start_time = Get-Date

# 1. Compile Latest Spring Boot Jar
Write-Host "`n[Step 1] Compiling Java Application via Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven compilation failed!"
    exit 1
}

# 2. Cleanup Packaging Directory
Write-Host "`n[Step 2] Preparing staging directories..." -ForegroundColor Yellow
$staging = "target\kockroch-pack"
$zipPayload = "target\payload.zip"

if (Test-Path $staging) {
    Remove-Item -Recurse -Force $staging
}
if (Test-Path $zipPayload) {
    Remove-Item -Force $zipPayload
}

# 3. Create Custom Minimal JRE using jlink
Write-Host "`n[Step 3] Compiling Custom Mini-JRE using jlink..." -ForegroundColor Yellow
$jlink = "C:\Program Files\Java\jdk-22\bin\jlink.exe"
$modules = "java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,jdk.unsupported,jdk.charsets,java.net.http,jdk.accessibility"

& $jlink --add-modules $modules `
        --strip-debug `
        --no-man-pages `
        --no-header-files `
        --compress=2 `
        --output "$staging\jre"

if ($LASTEXITCODE -ne 0) {
    Write-Error "jlink JRE optimization failed!"
    exit 1
}

# 4. Copy JAR and Default Configs
Write-Host "`n[Step 4] Staging JAR and configurations..." -ForegroundColor Yellow
Copy-Item "target\Cyber-Guardian-Java-1.0-SNAPSHOT.jar" -Destination "$staging\app.jar"
Copy-Item "config.json" -Destination "$staging\config.json" -ErrorAction SilentlyContinue

# 5. Compress Everything into a high-compression Zip
Write-Host "`n[Step 5] Compressing JRE and App into single ZIP payload..." -ForegroundColor Yellow
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($staging, $zipPayload, [System.IO.Compression.CompressionLevel]::Optimal, $false)

# 6. Compile C# Native Launcher with Embedded Payload
Write-Host "`n[Step 6] Compiling Native Standalone C# Executable..." -ForegroundColor Yellow
$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$csharpSource = "src\main\csharp\Launcher.cs"
$outputExe = "kockroch.exe"

$resourceArg = "/resource:" + $zipPayload + ",payload.zip"
& $csc "/target:winexe" "/out:$outputExe" "/reference:System.IO.Compression.FileSystem.dll" $resourceArg $csharpSource

if ($LASTEXITCODE -ne 0) {
    Write-Error "C# EXE compilation failed!"
    exit 1
}

# 7. Clean up Temporary Staging Files
Write-Host "`n[Step 7] Cleaning up temporary intermediate files..." -ForegroundColor Yellow
if (Test-Path $staging) {
    Remove-Item -Recurse -Force $staging
}
if (Test-Path $zipPayload) {
    Remove-Item -Force $zipPayload
}

# 8. Finished!
$end_time = Get-Date
$duration = $end_time - $start_time
$exeSize = (Get-Item $outputExe).Length / 1MB

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host ">>> STANDALONE COMPILATION COMPLETED SUCCESSFULLY! <<<" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Output File  : $outputExe" -ForegroundColor White
Write-Host "File Size    : $('{0:N2}' -f $exeSize) MB" -ForegroundColor White
Write-Host "Total Time   : $($duration.ToString('mm\:ss'))" -ForegroundColor White
Write-Host "Status       : Ready for deployment on ANY Windows PC without Java!" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green
