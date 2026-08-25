package servidor;

import common.iControlCallback;

/**
 * Estado interno que el servidor guarda por cada control conectado.
 * No es Remote: es solo un registro en memoria del servidor.
 */
class ControlSession {

    final String token;
    final String nombre;
    final iControlCallback callback;

    ControlSession(String token, String nombre, iControlCallback callback) {
        this.token = token;
        this.nombre = nombre;
        this.callback = callback;
    }
}
