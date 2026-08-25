# Gestor de diapositivas distribuido (Java RMI)

Dos aplicaciones independientes, pensadas para correr en **dos máquinas
distintas** dentro de la misma red local:

```
servidor-app/   se queda en la máquina conectada al proyector/pantalla
                (presenta las diapositivas y acepta controles remotos)
cliente-app/    se copia a la OTRA máquina, la que va a controlar la
                presentación a distancia
```

Cada carpeta es un proyecto **Maven** completo por sí mismo (trae su propia
copia de `common/`, las interfaces remotas compartidas), así que puedes
copiar `cliente-app/` a otra máquina sin llevarte nada de `servidor-app/`, y
viceversa. Cada uno empaqueta un `.jar` ejecutable (`target/servidor.jar` /
`target/cliente.jar`) con el `Main-Class` ya puesto en el manifest.

## Requisito en AMBAS máquinas: JDK + Maven

Cada máquina compila y empaqueta su propio código al arrancar, así que
necesita un JDK (no solo un JRE: hace falta `javac`) y Maven (`mvn`). Si
faltan:

```bash
brew install openjdk maven
```

(los scripts de abajo también detectan automáticamente un OpenJDK/Maven
instalados por Homebrew aunque no estén en el `PATH`).

## Puesta en marcha rápida

### 1. Máquina del presentador (la del proyector)

Deja la carpeta `servidor-app/` en esa máquina y haz doble clic en
[`servidor-app/Iniciar-Servidor.command`](servidor-app/Iniciar-Servidor.command)
(o, desde una terminal en la raíz del repo, `make servidor`).
Compila y arranca solo; al final imprime la IP que hay que usar desde los
clientes. Detalle completo en [`servidor-app/README.md`](servidor-app/README.md).

### 2. Máquina(s) del control remoto

Copia la carpeta `cliente-app/` a la otra máquina (USB, AirDrop, zip por
correo/Drive...) y haz doble clic en
[`cliente-app/Iniciar-Cliente.command`](cliente-app/Iniciar-Cliente.command)
(o `make cliente` si tienes el repo completo en esa máquina). Apenas arranca,
un diálogo te pide la IP del servidor (la que imprimió el paso 1); al
aceptarla, el cliente solicita la conexión de inmediato — no hay que tocar
ningún campo a mano. Detalle completo en [`cliente-app/README.md`](cliente-app/README.md).

### Targets de `make` (opcional, si tienes el repo completo en una máquina)

```bash
make servidor   # compila y arranca servidor-app/
make cliente    # compila y arranca cliente-app/
```

Son un atajo a los mismos `.command`; no reemplazan la necesidad de copiar
`cliente-app/` a la otra máquina para el uso real en dos equipos.

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
servidor-app/
  pom.xml
  src/main/java/common/    interfaces remotas compartidas (duplicadas en cliente-app/)
  src/main/java/servidor/  presentador: ventana de diapositivas, log, panel de conexiones
  diapositivas/             imágenes de la presentación
cliente-app/
  pom.xml
  src/main/java/common/    misma copia de las interfaces remotas
  src/main/java/cliente/   control remoto: GUI de solo botones
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
mvn -DskipTests package
java -Djava.rmi.server.hostname=<TU-IP> -jar target/servidor.jar diapositivas
```

```bash
# dentro de cliente-app/
mvn -DskipTests package
java -Djava.rmi.server.hostname=<TU-IP> -jar target/cliente.jar
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
     `servidor-app/src/main/java/servidor/ImpPresentationServer.java`, método
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
