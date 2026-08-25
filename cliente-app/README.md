# Cliente — Control remoto (esta carpeta va en la OTRA máquina)

Copia esta carpeta completa (`cliente-app/`) a la máquina que va a controlar
la presentación a distancia. No necesita nada de `servidor-app/`: ya trae su
propia copia de `common/`.

## Requisito: un JDK (compilador de Java)

Esta máquina necesita un JDK, no solo un JRE (`java -version` no basta, hace
falta `javac`). Si no lo tienes:

```bash
brew install openjdk
```

## Uso

1. Copia toda la carpeta `cliente-app/` a esta máquina (USB, AirDrop, zip
   por correo/Drive, etc. — como prefieras).
2. Haz doble clic en **`Iniciar-Cliente.command`**.
   - La primera vez, macOS puede avisar "no se puede verificar el
     desarrollador": clic derecho → **Abrir** → **Abrir** otra vez.
   - El script detecta tu JDK, compila el código y abre la ventana del
     control remoto automáticamente.
3. En la ventana que se abre:
   - **Nombre**: como quieres identificarte (ej. `Pepe`).
   - **Servidor**: la IP de la máquina que tiene el presentador, seguida de
     `:1802/presentador`. Por ejemplo `192.168.1.23:1802/presentador`
     (esa IP te la muestra la terminal del servidor al arrancar).
   - Pulsa **Conectar** y espera a que el operador del servidor acepte la
     conexión.
4. Cuando el servidor te dé permiso, los botones (Atrás / Adelante / Ir a /
   Pantalla completa) se habilitan.

## Si no conecta

- Confirma que ambas máquinas están en la **misma red local** (mismo Wi-Fi /
  LAN). Esto no funciona a través de internet sin configurar el router.
- La IP que escribes en "Servidor" debe ser la que muestra la terminal del
  **servidor**, no la de este cliente.
- La primera vez que ejecutas el script, el firewall de macOS puede
  preguntar "¿Permitir que java acepte conexiones entrantes?" — pulsa
  **Permitir** (el servidor necesita poder llamar de vuelta a este cliente).
- Si sigue sin conectar, revisa que no haya un firewall de red (router /
  antivirus) bloqueando el puerto `1802` entre las dos máquinas.

Para el detalle del flujo completo (aceptar conexión, dar permiso, etc.) ver
el `README.md` de la carpeta `servidor-app/` o el de la raíz del proyecto.
