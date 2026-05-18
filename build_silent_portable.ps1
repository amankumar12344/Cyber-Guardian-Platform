# 1. Compile latest code
mvn clean package -DskipTests

# 2. Cleanup and Create Dist Folder
$dist = "Cyber-Guardian-Silent-Package"
if (Test-Path $dist) { Remove-Item -Recurse -Force $dist }
New-Item -ItemType Directory -Path "$dist\jre"

# 3. Copy JRE and Strip Bloat
# Copy only essential folders
$jreSource = "C:\Program Files\Java\jdk-22"
Copy-Item -Path "$jreSource\bin" -Destination "$dist\jre\" -Recurse
Copy-Item -Path "$jreSource\lib" -Destination "$dist\jre\" -Recurse
Copy-Item -Path "$jreSource\conf" -Destination "$dist\jre\" -Recurse

# Remove large non-functional files
$junk = @(
    "*.pdb", "*.debug", "*.lib", "*.a", "*.h",
    "legal", "man", "include", "lib\src.zip",
    "bin\server\classes.jsa", "bin\server\classes_nocoops.jsa"
)
foreach ($pattern in $junk) {
    Get-ChildItem -Path "$dist\jre" -Include $pattern -Recurse | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}

# 4. Copy App Files
Copy-Item "target/Cyber-Guardian-Java-1.0-SNAPSHOT.jar" -Destination "$dist/app.jar"
Copy-Item "config.json" -Destination "$dist/" -ErrorAction SilentlyContinue

# 5. Create Launch Scripts
$vbs = @"
Set WshShell = CreateObject("WScript.Shell")
WshShell.Run chr(34) & "jre\bin\javaw.exe" & chr(34) & " -jar app.jar", 0
Set WshShell = Nothing
"@
$vbs | Out-File -FilePath "$dist\Launch-Guardian.vbs" -Encoding ASCII
