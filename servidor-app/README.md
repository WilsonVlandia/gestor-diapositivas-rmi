# Servidor — Presentador (esta carpeta se queda en la máquina que proyecta)

Esta carpeta (`servidor-app/`) corre en la máquina conectada al proyector /
pantalla. Trae un **`.jar` ya compilado** (`servidor.jar`) y las imágenes en
`diapositivas/`; no hace falta compilar nada para usarlo. (El código fuente
Maven está en `pom.xml` / `src/main/java/`, por si quieres modificarlo —
ver el README de la raíz para regenerar el `.jar`.)

## Requisito: Java (JRE)

```bash
java -version
```

Si no está instalado:

```bash
brew install openjdk
```

## Uso

1. Pon tus imágenes (`.png`/`.jpg`/`.jpeg`/`.gif`, se ordenan
   alfabéticamente) dentro de `diapositivas/`, reemplazando las de ejemplo
   si quieres.
2. En una terminal dentro de esta carpeta:
   ```bash
   java -jar servidor.jar
   ```
   (o `java -jar servidor.jar otra-carpeta` para usar otra carpeta de
   imágenes en vez de `diapositivas/`).
   - macOS puede preguntar "¿Permitir que java acepte conexiones
     entrantes?" — pulsa **Permitir** (si no, ningún cliente podrá
     conectarse).
3. Se abre la ventana del presentador. En la franja amarilla de arriba
   aparece algo como:
   ```
   IP de este servidor para los clientes:  192.168.1.23:1802/presentador
   ```
   Esa es la IP que el cliente pide en su diálogo apenas arranca (ver
   `cliente-app/README.md`) — también queda registrada en el log de
   actividad y en la consola.
4. La ventana trae: diapositiva actual, botones locales, log de actividad y
   panel de controles conectados.

## Flujo de aceptar/rechazar una conexión de un cliente

1. En el cliente, escribe un nombre y pulsa **Conectar**.
2. En la ventana del servidor aparece un popup: *"El control 'X' quiere
   conectarse"* con **Aceptar** / **Rechazar**.
3. Al aceptar, el cliente pasa a "Conectado" y aparece en el panel de la
   derecha. Sus botones de acción quedan disponibles según los permisos que
   maneje el servidor.
4. Las acciones (avanzar, retroceder, ir a diapositiva, pantalla completa)
   se reflejan en la ventana del presentador y se notifican a todos los
   clientes conectados.

## Ambas máquinas en la misma red

Este proyecto usa RMI directo (sin servidor intermedio), así que:

- Las dos máquinas deben estar en la **misma red local** (mismo Wi-Fi/LAN).
  No funciona a través de internet sin abrir puertos en el router.
- Puerto usado: `1802` (registro RMI). El cliente además expone un puerto
  para recibir notificaciones del servidor; si hay un firewall de red
  aparte del de macOS, permite tráfico entre ambas IPs sin restringir
  puertos.
- Si mueves de red (otro Wi-Fi) o reinicia el router y la IP cambia, vuelve
  a arrancar `java -jar servidor.jar`: la franja amarilla mostrará la IP
  nueva.

## Notas de diseño

- El servidor asigna un **token de sesión** por control al aceptarlo; ese
  token identifica al control en `solicitarAccion` y `desconectar`.
- Idempotencia: cada acción del cliente lleva una `idempotencyKey`, pero la
  deduplicación real la hace el servidor por *firma de acción* (tipo +
  diapositiva destino) usando `ConcurrentHashMap.compute(...)`, atómico por
  clave — así se resuelve tanto el doble clic de un mismo control como la
  carrera entre dos controles pidiendo la misma acción a la vez. Ver
  `src/main/java/servidor/ImpPresentationServer.java`, método `solicitarAccion`.
