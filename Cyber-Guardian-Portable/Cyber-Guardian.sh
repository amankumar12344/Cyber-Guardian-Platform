#!/bin/bash

# Get the directory where the script is located
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

echo "🛡️ Starting Cyber Guardian (Linux Mode)..."

# Ensure the runtime is executable
chmod +x ./runtime_linux/bin/java

# Run the application using bundled linux runtime
./runtime_linux/bin/java -jar "app/Cyber-Guardian.jar" > error_log.txt 2>&1
