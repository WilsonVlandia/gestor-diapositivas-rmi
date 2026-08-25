# Cliente — Control remoto (esta carpeta va en la OTRA máquina)

Copia esta carpeta completa (`cliente-app/`) a la máquina que va a controlar
la presentación a distancia. No necesita nada de `servidor-app/`: es un
proyecto Maven autosuficiente, ya trae su propia copia de `common/` (en
`src/main/java/common/`). `mvn package` genera un `.jar` ejecutable en
`target/cliente.jar`.

## Requisito: JDK + Maven

Esta máquina necesita un JDK (no solo un JRE: hace falta `javac`) y Maven
(`mvn`). Si te falta alguno:

```bash
brew install openjdk maven
```

## Uso

1. Copia toda la carpeta `cliente-app/` a esta máquina (USB, AirDrop, zip
   por correo/Drive, etc. — como prefieras).
2. Haz doble clic en **`Iniciar-Cliente.command`** (o corre `make cliente`
   desde la raíz del proyecto).
   - La primera vez, macOS puede avisar "no se puede verificar el
     desarrollador": clic derecho → **Abrir** → **Abrir** otra vez.
   - El script detecta tu JDK/Maven, empaqueta el `.jar` con Maven y abre la
     ventana del control remoto automáticamente.
3. Apenas se abre la ventana, aparece un diálogo pidiendo la **IP del
   servidor** (la que muestra la terminal del presentador al arrancar, ej.
   `192.168.1.23`). Escríbela y pulsa **Aceptar**: el cliente arma solo la
   dirección completa y pide la conexión de inmediato — no hace falta tocar
   el campo "Servidor" a mano.
   - Si cancelas el diálogo, la ventana queda igual que antes: puedes
     escribir la IP tú mismo en el campo "Servidor" y pulsar **Conectar**.
   - Después de pedir la conexión, espera a que el operador del servidor la
     acepte.
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
