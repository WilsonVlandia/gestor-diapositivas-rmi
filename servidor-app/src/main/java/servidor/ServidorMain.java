package servidor;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ServidorMain {

    private static final int PUERTO = 1802;
    private static final String NOMBRE_SERVICIO = "presentador";

    public static void main(String[] args) {
        String rutaDiapositivas = args.length > 0 ? args[0] : "diapositivas";

        SwingUtilities.invokeLater(() -> {
            ServidorFrame frame = null;
            try {
                frame = new ServidorFrame(new File(rutaDiapositivas));
                ImpPresentationServer server = new ImpPresentationServer(frame);
                frame.setServer(server);

                LocateRegistry.createRegistry(PUERTO);
                Naming.rebind("//127.0.0.1:" + PUERTO + "/" + NOMBRE_SERVICIO, server);

                frame.setVisible(true);
                System.out.println("Servidor RMI listo en el puerto " + PUERTO
                        + " (servicio '" + NOMBRE_SERVICIO + "')");
            } catch (RemoteException | MalformedURLException e) {
                e.printStackTrace();
                String mensaje = "No se pudo iniciar el servidor RMI en el puerto " + PUERTO
                        + ".\nEs probable que ya haya otra instancia del servidor corriendo.\n\n"
                        + e.getMessage();
                JOptionPane.showMessageDialog(null, mensaje, "Error al iniciar el servidor",
                        JOptionPane.ERROR_MESSAGE);
                if (frame != null) {
                    frame.dispose();
                }
                System.exit(1);
            }
        });
    }
}
