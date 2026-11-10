@echo off

cd obfuscate

java -Xms128M -Xmx16384M -Xss1024M -jar ".\runtime\obfuscate.jar" ".\config.txt"

cd ..