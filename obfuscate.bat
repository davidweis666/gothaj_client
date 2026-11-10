@echo off
setlocal

call gradlew.bat build
if errorlevel 1 (
    echo Gradle build failed.
    exit /b 1
)

if not exist ".\obfuscate\input\lib" mkdir ".\obfuscate\input\lib"

copy ".\build\libs\client.jar" ".\obfuscate\input\client.jar" /Y
copy ".\build\libs\libraries.jar" ".\obfuscate\input\lib\libraries.jar" /Y

cd obfuscate
if exist "..\output.jar" del /q "..\output.jar"
if exist ".\native\output.jar" del /q ".\native\output.jar"
if exist ".\native\output-native.jar" del /q ".\native\output-native.jar"

java -Xms128M -Xmx24576M -Xss1024M -jar ".\runtime\obfuscate.jar" ".\config.txt"
if errorlevel 1 (
    echo ZKM obfuscation failed.
    exit /b 1
)
if not exist "..\output.jar" (
    echo ZKM did not produce ..\output.jar.
    exit /b 1
)

echo Running native obfuscator...
copy /Y "..\output.jar" ".\native\output.jar" >nul
if errorlevel 1 (
    echo Failed to stage the ZKM output for native obfuscation.
    exit /b 1
)

cd native
java -Xms128M -Xmx24576M -Xss1024M -jar obfuscator.jar
if errorlevel 1 (
    echo Native obfuscation failed.
    exit /b 1
)
if not exist "output-native.jar" (
    echo Native obfuscator did not produce output-native.jar.
    exit /b 1
)
cd ..

echo Done! Final native JAR: .\obfuscate\native\output-native.jar

cd ..
endlocal
