package cliente;

import common.iControlCallback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Callback remoto que el cliente expone al servidor para recibir
 * notificaciones asincronas de cambio de diapositiva.
 */
public class ImpControlCallback extends UnicastRemoteObject implements iControlCallback {

    private static final long serialVersionUID = 1L;

    private final ClienteFrame frame;

    protected ImpControlCallback(ClienteFrame frame) throws RemoteException {
        this.frame = frame;
    }

    @Override
    public void diapositivaCambio(int nuevaDiapositiva) throws RemoteException {
        frame.actualizarDiapositivaActual(nuevaDiapositiva);
    }
}
