package common;

/**
 * Se lanza cuando un control intenta ejecutar una accion con un token de
 * sesion desconocido o expirado (por ejemplo, si el servidor se reinicio).
 */
public class PermisoDenegadoException extends Exception {

    private static final long serialVersionUID = 1L;

    public PermisoDenegadoException(String motivo) {
        super(motivo);
    }
}
