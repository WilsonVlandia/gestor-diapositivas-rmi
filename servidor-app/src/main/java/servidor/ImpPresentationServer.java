package servidor;

import common.AccionTipo;
import common.ConexionInfo;
import common.ConexionRechazadaException;
import common.PermisoDenegadoException;
import common.iControlCallback;
import common.iPresentationServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;

/**
 * Implementacion del servicio remoto. Cada llamada remota corre en un hilo
 * propio del pool de RMI, por lo que el estado compartido (sesiones, ultima
 * diapositiva, ventana de idempotencia) se maneja con estructuras
 * concurrentes / bloques sincronizados.
 */
public class ImpPresentationServer extends UnicastRemoteObject implements iPresentationServer {

    private static final long serialVersionUID = 1L;

    /** Tamano de la ventana de idempotencia: solicitudes equivalentes dentro de este margen se descartan. */
    private static final long VENTANA_IDEMPOTENCIA_MS = 1000;

    private final ServidorFrame frame;

    private final Object lockDiapositiva = new Object();
    private int diapositivaActual = 1;

    private final Map<String, ControlSession> sesiones = new ConcurrentHashMap<>();
    private final Map<String, Long> ultimaEjecucionPorAccion = new ConcurrentHashMap<>();

    protected ImpPresentationServer(ServidorFrame frame) throws RemoteException {
        this.frame = frame;
    }

    @Override
    public ConexionInfo conectar(String nombreControl, iControlCallback callback)
            throws RemoteException, ConexionRechazadaException {

        CompletableFuture<Boolean> decision = new CompletableFuture<>();

        // La ventana emergente Aceptar/Rechazar se muestra en el hilo de Swing.
        // El hilo RMI que atiende esta llamada se queda esperando la decision
        // del operador; como RMI usa un thread por llamada, esto no bloquea
        // al resto del servidor ni a otras solicitudes concurrentes.
        SwingUtilities.invokeLater(() ->
                frame.mostrarSolicitudConexion(nombreControl,
                        () -> decision.complete(true),
                        () -> decision.complete(false)));

        boolean aceptada;
        try {
            aceptada = decision.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new ConexionRechazadaException("Error interno esperando la decision del servidor");
        }

        if (!aceptada) {
            frame.agregarLog(nombreControl + " -> conexion rechazada");
            throw new ConexionRechazadaException("El presentador rechazo la conexion");
        }

        String token = UUID.randomUUID().toString();
        ControlSession sesion = new ControlSession(token, nombreControl, callback);
        sesiones.put(token, sesion);

        frame.agregarLog(nombreControl + " -> conexion aceptada");
        frame.actualizarListaConexiones(snapshotSesiones());

        synchronized (lockDiapositiva) {
            return new ConexionInfo(token, frame.getTotalDiapositivas(), diapositivaActual);
        }
    }

    @Override
    public void solicitarAccion(String token, String idempotencyKey, AccionTipo tipo, Integer nDiapositiva)
            throws RemoteException, PermisoDenegadoException {

        ControlSession sesion = sesiones.get(token);
        if (sesion == null) {
            throw new PermisoDenegadoException("Sesion desconocida o expirada, vuelve a conectarte");
        }

        // =================== VENTANA DE IDEMPOTENCIA (1s) ===================
        // Las solicitudes se agrupan por "firma" (tipo de accion + diapositiva
        // destino si aplica), sin importar que control las envio. Map.compute
        // es atomico por clave: si dos hilos RMI llegan casi al mismo tiempo
        // pidiendo la misma accion (doble clic, o dos controles a la vez),
        // solo el primero que entra a compute() "gana" la ventana; el resto
        // se descarta como duplicado durante el siguiente segundo.
        String firma = tipo + (tipo == AccionTipo.IR_A ? ":" + nDiapositiva : "");
        long ahora = System.currentTimeMillis();
        long ganador = ultimaEjecucionPorAccion.compute(firma, (clave, ultimaEjecucion) -> {
            if (ultimaEjecucion != null && ahora - ultimaEjecucion < VENTANA_IDEMPOTENCIA_MS) {
                return ultimaEjecucion; // se conserva el timestamp anterior -> esta solicitud es duplicada
            }
            return ahora; // esta solicitud gana la ventana y queda registrada
        });

        if (ganador != ahora) {
            System.out.println("[idempotencia] duplicado ignorado (key=" + idempotencyKey + ") firma=" + firma
                    + " enviado por " + sesion.nombre);
            return; // se descarta silenciosamente: no es un error para el cliente
        }
        // ===================================================================

        ejecutarAccion(sesion.nombre, tipo, nDiapositiva);
    }

    @Override
    public void desconectar(String token) throws RemoteException {
        ControlSession sesion = sesiones.remove(token);
        if (sesion != null) {
            frame.agregarLog(sesion.nombre + " -> se desconecto");
            frame.actualizarListaConexiones(snapshotSesiones());
        }
    }

    /**
     * Acciones disparadas por los botones locales del servidor. No pasan por
     * la ventana de idempotencia remota, ya que no vienen de una llamada RMI.
     */
    public void ejecutarAccionLocal(AccionTipo tipo, Integer nDiapositiva) {
        ejecutarAccion("Servidor (local)", tipo, nDiapositiva);
    }

    private void ejecutarAccion(String origen, AccionTipo tipo, Integer nDiapositiva) {
        if (tipo == AccionTipo.PANTALLA_COMPLETA) {
            frame.alternarPantallaCompletaDesdeControl();
            frame.agregarLog(origen + " -> alterno pantalla completa");
            return;
        }

        int nueva;
        synchronized (lockDiapositiva) {
            int total = frame.getTotalDiapositivas();
            switch (tipo) {
                case AVANZAR:
                    nueva = Math.min(diapositivaActual + 1, total);
                    break;
                case RETROCEDER:
                    nueva = Math.max(diapositivaActual - 1, 1);
                    break;
                case IR_A:
                default:
                    int destino = (nDiapositiva == null) ? diapositivaActual : nDiapositiva;
                    nueva = Math.max(1, Math.min(destino, total));
                    break;
            }
            diapositivaActual = nueva;
        }

        frame.agregarLog(origen + " -> " + describir(tipo, nueva));
        frame.actualizarDiapositiva(nueva);
        notificarCambioATodos(nueva);
    }

    private void notificarCambioATodos(int nueva) {
        for (ControlSession sesion : sesiones.values()) {
            notificar(sesion, cb -> cb.diapositivaCambio(nueva));
        }
    }

    private interface LlamadaCallback {
        void invocar(iControlCallback callback) throws RemoteException;
    }

    private void notificar(ControlSession sesion, LlamadaCallback llamada) {
        try {
            llamada.invocar(sesion.callback);
        } catch (RemoteException e) {
            frame.agregarLog(sesion.nombre + " -> se perdio la conexion (callback fallido)");
            sesiones.remove(sesion.token);
            frame.actualizarListaConexiones(snapshotSesiones());
        }
    }

    private String describir(AccionTipo tipo, Integer n) {
        switch (tipo) {
            case AVANZAR:
                return n == null ? "avanzar" : "avanzo a la diapositiva " + n;
            case RETROCEDER:
                return n == null ? "retroceder" : "retrocedio a la diapositiva " + n;
            case IR_A:
                return n == null ? "ir a una diapositiva" : "fue a la diapositiva " + n;
            case PANTALLA_COMPLETA:
                return "cambio de pantalla completa";
            default:
                return "accion desconocida";
        }
    }

    private List<ControlInfoUI> snapshotSesiones() {
        List<ControlInfoUI> lista = new ArrayList<>();
        for (ControlSession s : sesiones.values()) {
            lista.add(new ControlInfoUI(s.token, s.nombre));
        }
        return lista;
    }
}
