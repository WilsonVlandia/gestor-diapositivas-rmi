#!/bin/bash
# Doble clic (o "./Iniciar-Cliente.command" en terminal) para compilar (con
# Maven) y arrancar el control remoto (cliente). No hace falta tocar nada mas.
set -e
cd "$(dirname "$0")"

echo "== Cliente / Control remoto =="

# --- 1. Localizar un JDK (no solo JRE) ---
if command -v javac >/dev/null 2>&1; then
    JAVA=java
elif [ -x "$(brew --prefix openjdk 2>/dev/null)/bin/javac" ]; then
    JAVA="$(brew --prefix openjdk)/bin/java"
else
    echo "No se encontro un JDK (compilador). Instala uno, por ejemplo:"
    echo "  brew install openjdk"
    read -p "Presiona Enter para cerrar..."
    exit 1
fi

# --- 2. Localizar Maven ---
if command -v mvn >/dev/null 2>&1; then
    MVN=mvn
elif [ -x "$(brew --prefix maven 2>/dev/null)/bin/mvn" ]; then
    MVN="$(brew --prefix maven)/bin/mvn"
else
    echo "No se encontro Maven. Instalalo, por ejemplo:"
    echo "  brew install maven"
    read -p "Presiona Enter para cerrar..."
    exit 1
fi

# --- 3. Detectar la IP de esta maquina en la red local ---
# (el servidor necesita poder llamar de vuelta a este cliente por RMI)
IP=""
for IFACE in en0 en1 en2; do
    CANDIDATE="$(ipconfig getifaddr "$IFACE" 2>/dev/null || true)"
    if [ -n "$CANDIDATE" ]; then
        IP="$CANDIDATE"
        break
    fi
done
if [ -z "$IP" ]; then
    IP="$(ifconfig 2>/dev/null | awk '/inet /{print $2}' | grep -v '^127\.' | head -n1)"
fi
if [ -z "$IP" ]; then
    echo "No se pudo detectar la IP de esta maquina automaticamente."
    read -p "Escribe la IP de esta maquina en la red local: " IP
fi

# --- 4. Compilar y empaquetar el .jar ejecutable ---
echo "Compilando (Maven)..."
"$MVN" -q -DskipTests package

# --- 5. Ejecutar ---
echo ""
echo "IP de este cliente en la red: $IP"
echo "En la ventana que se abre, escribe la IP de la maquina que tiene el"
echo "presentador cuando el dialogo te la pida (ej: 192.168.1.23)."
echo ""
"$JAVA" -Djava.rmi.server.hostname="$IP" -jar target/cliente.jar

read -p "El cliente se cerro. Presiona Enter para salir..."
