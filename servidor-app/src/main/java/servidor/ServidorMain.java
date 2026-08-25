package servidor;

import common.RedUtil;

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

        // Anuncia los objetos remotos con la IP de esta maquina en la red
        // local (en vez de localhost), para que la otra maquina pueda
        // conectarse. Se hace aqui, antes de exportar nada por RMI, para que
        // el .jar funcione solo con "java -jar servidor.jar", sin flags.
        String ip = RedUtil.detectarIpLocal();
        System.setProperty("java.rmi.server.hostname", ip);

        SwingUtilities.invokeLater(() -> {
            ServidorFrame frame = null;
            try {
                frame = new ServidorFrame(new File(rutaDiapositivas), ip);
                ImpPresentationServer server = new ImpPresentationServer(frame);
                frame.setServer(server);

                LocateRegistry.createRegistry(PUERTO);
                Naming.rebind("//127.0.0.1:" + PUERTO + "/" + NOMBRE_SERVICIO, server);

                frame.setVisible(true);
                frame.agregarLog("IP de este servidor en la red: " + ip
                        + "  ->  en cada cliente usa: " + ip + ":" + PUERTO + "/" + NOMBRE_SERVICIO);
                System.out.println("Servidor RMI listo en el puerto " + PUERTO
                        + " (servicio '" + NOMBRE_SERVICIO + "'), IP: " + ip);
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
