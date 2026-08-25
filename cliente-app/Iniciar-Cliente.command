#!/bin/bash
# Doble clic (o "./Iniciar-Cliente.command" en terminal) para compilar y
# arrancar el control remoto (cliente). No hace falta tocar nada mas.
set -e
cd "$(dirname "$0")"

echo "== Cliente / Control remoto =="

# --- 1. Localizar un JDK con javac (no solo JRE) ---
if command -v javac >/dev/null 2>&1; then
    JAVAC=javac
    JAVA=java
elif [ -x "$(brew --prefix openjdk 2>/dev/null)/bin/javac" ]; then
    JDK="$(brew --prefix openjdk)"
    JAVAC="$JDK/bin/javac"
    JAVA="$JDK/bin/java"
else
    echo "No se encontro un JDK (compilador). Instala uno, por ejemplo:"
    echo "  brew install openjdk"
    read -p "Presiona Enter para cerrar..."
    exit 1
fi

# --- 2. Detectar la IP de esta maquina en la red local ---
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

# --- 3. Compilar ---
echo "Compilando..."
mkdir -p out
"$JAVAC" -d out $(find common cliente -name "*.java")

# --- 4. Ejecutar ---
echo ""
echo "IP de este cliente en la red: $IP"
echo "En la ventana que se abre, escribe en 'Servidor' la IP de la maquina"
echo "que tiene el presentador (ej: 192.168.1.23:1802/presentador)."
echo ""
"$JAVA" -Djava.rmi.server.hostname="$IP" -cp out cliente.ClienteMain

read -p "El cliente se cerro. Presiona Enter para salir..."
