package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaz remota expuesta por el servidor de presentaciones via RMI Registry.
 */
public interface iPresentationServer extends Remote {

    /**
     * Solicita conectarse como control remoto. Dispara la ventana emergente
     * Aceptar/Rechazar en el servidor. Si es aceptada, devuelve el token de
     * sesion y el estado actual de la presentacion; si es rechazada, lanza
     * ConexionRechazadaException.
     */
    ConexionInfo conectar(String nombreControl, iControlCallback callback)
            throws RemoteException, ConexionRechazadaException;

    /**
     * Solicita ejecutar una accion sobre la presentacion actual. El servidor
     * valida que la sesion siga activa y aplica la ventana de idempotencia
     * de 1 segundo antes de ejecutarla (ver ImpPresentationServer.solicitarAccion).
     */
    void solicitarAccion(String token, String idempotencyKey, AccionTipo tipo, Integer nDiapositiva)
            throws RemoteException, PermisoDenegadoException;

    void desconectar(String token) throws RemoteException;
}
