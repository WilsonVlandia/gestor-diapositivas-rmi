# Gestor de diapositivas distribuido (Java RMI)

Dos aplicaciones independientes, pensadas para correr en **dos máquinas
distintas** dentro de la misma red local:

```
servidor-app/   se queda en la máquina conectada al proyector/pantalla
                (presenta las diapositivas y acepta controles remotos)
cliente-app/    se copia a la OTRA máquina, la que va a controlar la
                presentación a distancia
```

Cada carpeta es un proyecto completo por sí mismo (trae su propia copia de
`common/`, las interfaces remotas compartidas), así que puedes copiar
`cliente-app/` a otra máquina sin llevarte nada de `servidor-app/`, y
viceversa.

## Requisito en AMBAS máquinas: un JDK (compilador), no solo un JRE

Cada máquina compila su propio código al arrancar, así que necesita
`javac`, no solo `java`. Si `javac -version` falla en una terminal:

```bash
brew install openjdk
```

(los scripts de abajo también detectan automáticamente un OpenJDK instalado
por Homebrew aunque no esté en el `PATH`).

## Puesta en marcha rápida

### 1. Máquina del presentador (la del proyector)

Deja la carpeta `servidor-app/` en esa máquina y haz doble clic en
[`servidor-app/Iniciar-Servidor.command`](servidor-app/Iniciar-Servidor.command).
Compila y arranca solo; al final imprime la IP que hay que usar desde los
clientes. Detalle completo en [`servidor-app/README.md`](servidor-app/README.md).

### 2. Máquina(s) del control remoto

Copia la carpeta `cliente-app/` a la otra máquina (USB, AirDrop, zip por
correo/Drive...) y haz doble clic en
[`cliente-app/Iniciar-Cliente.command`](cliente-app/Iniciar-Cliente.command).
Compila y arranca solo; en la ventana escribe la IP que te dio el servidor
en el campo "Servidor". Detalle completo en [`cliente-app/README.md`](cliente-app/README.md).

Puedes repetir el paso 2 en varias máquinas (o varias veces en la misma)
para simular varios controles conectados a la vez.

### 3. Primera vez: permisos de macOS

- **Gatekeeper**: al hacer doble clic en el `.command` por primera vez,
  macOS dirá que no puede verificar al desarrollador. Clic derecho sobre el
  archivo → **Abrir** → confirmar **Abrir**. Solo hace falta una vez.
- **Firewall**: macOS puede preguntar "¿Permitir que `java` acepte
  conexiones entrantes?" tanto en el servidor como en el cliente — pulsa
  **Permitir** en ambos casos, si no la conexión RMI no se completa.

### 4. Ambas máquinas en la misma red

Este proyecto usa RMI directo entre las dos máquinas (sin servidor
intermedio en internet), así que **ambas deben estar en la misma red local**
(mismo Wi-Fi o LAN). Si cambias de red, vuelve a arrancar el servidor: te
dará la IP nueva a usar en los clientes.

## Cómo funciona (para referencia / si quieres tocar el código)

Sigue el patrón de conexión RMI estándar (`iRMI` / `ImpRMI` /
`Naming.rebind` / `LocateRegistry.createRegistry`):

```
common/     interfaces remotas compartidas (iPresentationServer, iControlCallback, tipos)
            — duplicada dentro de servidor-app/ y cliente-app/
servidor/   (dentro de servidor-app/) presentador: ventana de diapositivas,
            log, panel de controles conectados
cliente/    (dentro de cliente-app/) control remoto: GUI de solo botones
diapositivas/  (dentro de servidor-app/) imágenes de la presentación
```

Los scripts `.command` arrancan la JVM con
`-Djava.rmi.server.hostname=<IP-de-esa-máquina>`: es lo que hace que los
objetos remotos (el servidor y el callback del cliente) queden anunciados
con una IP alcanzable desde la otra máquina en vez de `localhost`, que es la
causa más común de que RMI "funcione en una sola máquina pero no entre dos".
No hace falta arrancar `rmiregistry` a mano: `ServidorMain` crea el
registro con `LocateRegistry.createRegistry(1802)` al arrancar.

### Compilar/ejecutar a mano (sin el `.command`, opcional)

```bash
# dentro de servidor-app/
JDK="$(brew --prefix openjdk)"
mkdir -p out
"$JDK/bin/javac" -d out $(find common servidor -name "*.java")
"$JDK/bin/java" -Djava.rmi.server.hostname=<TU-IP> -cp out servidor.ServidorMain diapositivas
```

```bash
# dentro de cliente-app/
JDK="$(brew --prefix openjdk)"
mkdir -p out
"$JDK/bin/javac" -d out $(find common cliente -name "*.java")
"$JDK/bin/java" -Djava.rmi.server.hostname=<TU-IP> -cp out cliente.ClienteMain
```

## Flujo de aceptar/rechazar una conexión

1. En el cliente, escribe un nombre (ej. `Pepe`) y pulsa **Conectar**. El
   estado pasa a `Esperando aprobacion del servidor...`.
2. En la ventana del **servidor** aparece un popup: *"El control 'Pepe'
   quiere conectarse."* con botones **Aceptar** / **Rechazar**.
3. Si el operador pulsa **Rechazar**: el cliente muestra
   `Rechazado / error: El presentador rechazo la conexion` y el botón
   Conectar vuelve a habilitarse.
4. Si pulsa **Aceptar**: el cliente pasa a `Conectado`, y en el servidor
   aparece una fila nueva en "Controles conectados" con el nombre `Pepe`.
   El log del servidor registra `Pepe -> conexion aceptada`.

## Controlar la presentación

1. Al pulsar **Adelante** en el cliente, el servidor:
   - valida la sesión (token) del control,
   - aplica la ventana de idempotencia de 1 segundo (ver
     `servidor-app/servidor/ImpPresentationServer.java`, método
     `solicitarAccion`, bloques `VALIDACION DE PERMISO` y
     `VENTANA DE IDEMPOTENCIA (1s)`),
   - avanza la diapositiva, actualiza la ventana del presentador y notifica
     a *todos* los controles conectados el nuevo número de diapositiva.
2. El log del servidor muestra: `Pepe -> avanzo a la diapositiva 2`.
3. Si `Pepe` y `Ana` pulsan "Adelante" casi al mismo tiempo, solo la primera
   solicitud que llega al servidor avanza la diapositiva; la segunda se
   descarta como duplicada dentro de esa ventana de 1 segundo (se ve en la
   consola del servidor como `[idempotencia] duplicado ignorado ...`, sin
   generar error visible en el cliente).

## Notas de diseño

- El servidor asigna un **token de sesión** por control al aceptarlo; ese
  token identifica al control en `solicitarAccion` y `desconectar`.
- Idempotencia: cada acción del cliente lleva una `idempotencyKey`
  (`nombre:tipoAccion:timestamp`), pero la deduplicación real la hace el
  servidor por *firma de acción* (tipo + diapositiva destino) usando
  `ConcurrentHashMap.compute(...)`, que es atómico por clave — así se
  resuelve tanto el doble clic de un mismo control como la carrera entre
  dos controles distintos pidiendo la misma acción a la vez.
- Las imágenes de `servidor-app/diapositivas/` son PNGs de ejemplo;
  reemplázalas por las tuyas (`.png`/`.jpg`/`.jpeg`/`.gif`, se ordenan
  alfabéticamente).

## Problemas comunes al conectar entre dos máquinas

- **El cliente se queda en "Esperando aprobacion..." y nunca conecta**:
  revisa que ambas máquinas estén en la misma red y que el firewall de
  macOS haya permitido a `java` aceptar conexiones en ambos lados.
- **`NotBoundException` o "Connection refused"**: la IP que escribiste en
  "Servidor" no es la del presentador, o el servidor no está corriendo
  todavía. Copia exactamente la línea que imprime la terminal del servidor.
- **Conecta pero nunca recibe el cambio de diapositiva**: normalmente es el
  firewall bloqueando la conexión de *vuelta* del servidor hacia el
  cliente (el callback); confirma que el cliente también aceptó el aviso
  de firewall de macOS.
