# Servidor — Presentador (esta carpeta se queda en la máquina que proyecta)

Esta carpeta (`servidor-app/`) corre en la máquina conectada al proyector /
pantalla. Es autosuficiente: trae su propia copia de `common/` y las
imágenes en `diapositivas/`.

## Requisito: un JDK (compilador de Java)

Esta máquina necesita un JDK, no solo un JRE (`java -version` no basta, hace
falta `javac`). Si no lo tienes:

```bash
brew install openjdk
```

## Uso

1. Pon tus imágenes (`.png`/`.jpg`/`.jpeg`/`.gif`, se ordenan
   alfabéticamente) dentro de `diapositivas/`, reemplazando las de ejemplo
   si quieres.
2. Haz doble clic en **`Iniciar-Servidor.command`** (o corre `make servidor`
   desde la raíz del proyecto).
   - La primera vez, macOS puede avisar "no se puede verificar el
     desarrollador": clic derecho → **Abrir** → **Abrir** otra vez.
   - macOS puede preguntar "¿Permitir que java acepte conexiones
     entrantes?" — pulsa **Permitir** (si no, ningún cliente podrá
     conectarse).
   - El script detecta tu JDK y tu IP de red, compila el código y abre la
     ventana del presentador.
3. La terminal imprime algo como:
   ```
   IP de este servidor en la red: 192.168.1.23
   En cada cliente, usa como 'Servidor':  192.168.1.23:1802/presentador
   ```
   Anota esa IP: el cliente la pide en un diálogo apenas arranca (ver
   `cliente-app/README.md`).
4. Se abre la ventana del presentador (diapositiva actual, botones locales,
   log de actividad, panel de controles conectados).

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
  a arrancar `Iniciar-Servidor.command`: te mostrará la IP nueva.

## Notas de diseño

- El servidor asigna un **token de sesión** por control al aceptarlo; ese
  token identifica al control en `solicitarAccion` y `desconectar`.
- Idempotencia: cada acción del cliente lleva una `idempotencyKey`, pero la
  deduplicación real la hace el servidor por *firma de acción* (tipo +
  diapositiva destino) usando `ConcurrentHashMap.compute(...)`, atómico por
  clave — así se resuelve tanto el doble clic de un mismo control como la
  carrera entre dos controles pidiendo la misma acción a la vez. Ver
  `servidor/ImpPresentationServer.java`, método `solicitarAccion`.
