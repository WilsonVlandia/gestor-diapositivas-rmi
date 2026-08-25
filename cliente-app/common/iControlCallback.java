package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Callback remoto que implementa cada cliente (control) para que el servidor
 * pueda notificarle eventos de forma asincrona, sin que el cliente tenga que
 * estar preguntando (polling).
 */
public interface iControlCallback extends Remote {

    void diapositivaCambio(int nuevaDiapositiva) throws RemoteException;
}
