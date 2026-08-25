# Cliente — Control remoto (esta carpeta va en la OTRA máquina)

Copia esta carpeta completa (`cliente-app/`) a la máquina que va a controlar
la presentación a distancia. No necesita nada de `servidor-app/`: trae un
**`.jar` ya compilado** (`cliente.jar`), no hace falta compilar nada para
usarlo. (El código fuente Maven está en `pom.xml` / `src/main/java/`, por si
quieres modificarlo — ver el README de la raíz para regenerar el `.jar`.)

## Requisito: Java (JRE)

```bash
java -version
```

Si no está instalado:

```bash
brew install openjdk
```

## Uso

1. Copia toda la carpeta `cliente-app/` a esta máquina (USB, AirDrop, zip
   por correo/Drive, etc. — como prefieras).
2. En una terminal dentro de esta carpeta:
   ```bash
   java -jar cliente.jar
   ```
3. Apenas se abre la ventana, aparece un diálogo pidiendo la **IP del
   servidor** (la de la franja amarilla de la ventana del presentador, ej.
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
- La IP que escribes en el diálogo debe ser la de la franja amarilla del
  **servidor**, no la de este cliente.
- La primera vez que ejecutas `java -jar cliente.jar`, el firewall de macOS
  puede preguntar "¿Permitir que java acepte conexiones entrantes?" — pulsa
  **Permitir** (el servidor necesita poder llamar de vuelta a este cliente).
- Si sigue sin conectar, revisa que no haya un firewall de red (router /
  antivirus) bloqueando el puerto `1802` entre las dos máquinas.

Para el detalle del flujo completo (aceptar conexión, dar permiso, etc.) ver
el `README.md` de la carpeta `servidor-app/` o el de la raíz del proyecto.
