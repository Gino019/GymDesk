@echo off
title GymDesk - Software de Gestion de Gimnasios
color 0F

echo ===================================================
echo             GYMDESK FITNESS CLUB
echo       Sistema de Gestion Administrativa
echo ===================================================
echo.

cd /d "%~dp0\GymDeskDemo"

if exist "target\GymDesk.jar" (
    echo Iniciando GymDesk...
    java -jar target\GymDesk.jar
) else (
    echo [INFO] Generando el paquete ejecutable por primera vez...
    mvn clean package -DskipTests
    if exist "target\GymDesk.jar" (
        echo Iniciando GymDesk...
        java -jar target\GymDesk.jar
    ) else (
        echo [ERROR] No se pudo compilar el archivo ejecutable GymDesk.jar.
        echo Asegurese de tener instalado Java 17 y Maven.
        pause
    )
)

