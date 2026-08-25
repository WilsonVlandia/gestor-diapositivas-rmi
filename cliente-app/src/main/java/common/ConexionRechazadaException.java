package common;

/**
 * Se lanza cuando el operador del servidor rechaza la solicitud de conexion
 * de un control en la ventana emergente Aceptar/Rechazar.
 */
public class ConexionRechazadaException extends Exception {

    private static final long serialVersionUID = 1L;

    public ConexionRechazadaException(String motivo) {
        super(motivo);
    }
}
