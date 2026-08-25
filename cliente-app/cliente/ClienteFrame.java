package cliente;

import common.AccionTipo;
import common.ConexionInfo;
import common.ConexionRechazadaException;
import common.PermisoDenegadoException;
import common.iPresentationServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * GUI minima del control remoto: solo botones y estado de conexion, nunca
 * muestra las diapositivas.
 */
public class ClienteFrame extends JFrame {

    private final JTextField campoNombre = new JTextField("Control-" + (int) (Math.random() * 1000), 12);
    private final JTextField campoServidor = new JTextField("127.0.0.1:1802/presentador", 22);
    private final JButton btnConectar = new JButton("Conectar");
    private final JLabel lblEstado = new JLabel("Desconectado");

    private final JButton btnAnterior = new JButton("◀ Atras");
    private final JButton btnSiguiente = new JButton("Adelante ▶");
    private final JSpinner spinnerIrA = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JButton btnIrA = new JButton("Ir a");
    private final JButton btnPantallaCompleta = new JButton("Pantalla completa");
    private final JLabel lblDiapositivaActual = new JLabel("Diapositiva: -");

    private iPresentationServer servidor;
    private String token;
    private int totalDiapositivas = 1;

    public ClienteFrame() {
        super("Control remoto");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel panelConexion = new JPanel();
        panelConexion.add(new JLabel("Nombre:"));
        panelConexion.add(campoNombre);
        panelConexion.add(new JLabel("Servidor:"));
        panelConexion.add(campoServidor);
        panelConexion.add(btnConectar);
        add(panelConexion, BorderLayout.NORTH);

        JPanel panelBotones = new JPanel(new GridLayout(3, 2, 8, 8));
        panelBotones.add(btnAnterior);
        panelBotones.add(btnSiguiente);
        panelBotones.add(spinnerIrA);
        panelBotones.add(btnIrA);
        panelBotones.add(btnPantallaCompleta);
        panelBotones.add(lblDiapositivaActual);
        add(panelBotones, BorderLayout.CENTER);

        JPanel panelEstado = new JPanel();
        panelEstado.add(new JLabel("Estado:"));
        panelEstado.add(lblEstado);
        add(panelEstado, BorderLayout.SOUTH);

        habilitarBotonesAccion(false);

        btnConectar.addActionListener(e -> conectar());
        btnAnterior.addActionListener(e -> enviarAccion(AccionTipo.RETROCEDER, null));
        btnSiguiente.addActionListener(e -> enviarAccion(AccionTipo.AVANZAR, null));
        btnIrA.addActionListener(e -> enviarAccion(AccionTipo.IR_A, (Integer) spinnerIrA.getValue()));
        btnPantallaCompleta.addActionListener(e -> enviarAccion(AccionTipo.PANTALLA_COMPLETA, null));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (servidor != null && token != null) {
                        servidor.desconectar(token);
                    }
                } catch (RemoteException ignored) {
                    // el servidor ya no responde; no hay nada mas que hacer al cerrar.
                }
            }
        });

        setSize(420, 260);
        setLocationRelativeTo(null);
    }

    void conectar() {
        String nombre = campoNombre.getText().trim();
        String urlServidor = "rmi://" + campoServidor.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa un nombre para tu control");
            return;
        }

        btnConectar.setEnabled(false);
        lblEstado.setText("Esperando aprobacion del servidor...");

        // conectar() se queda bloqueada en el servidor hasta que el operador
        // responda al popup de Aceptar/Rechazar, asi que se llama desde un
        // SwingWorker para no congelar la interfaz mientras se espera.
        new SwingWorker<ConexionInfo, Void>() {
            private Exception error;

            @Override
            protected ConexionInfo doInBackground() {
                try {
                    servidor = (iPresentationServer) Naming.lookup(urlServidor);
                    ImpControlCallback callback = new ImpControlCallback(ClienteFrame.this);
                    return servidor.conectar(nombre, callback);
                } catch (MalformedURLException | RemoteException | NotBoundException | ConexionRechazadaException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    lblEstado.setText("Rechazado / error: " + error.getMessage());
                    btnConectar.setEnabled(true);
                    return;
                }
                ConexionInfo info;
                try {
                    info = get();
                } catch (Exception e) {
                    lblEstado.setText("Error: " + e.getMessage());
                    btnConectar.setEnabled(true);
                    return;
                }
                token = info.getToken();
                totalDiapositivas = Math.max(1, info.getTotalDiapositivas());
                spinnerIrA.setModel(new SpinnerNumberModel(info.getDiapositivaActual(), 1, totalDiapositivas, 1));
                lblDiapositivaActual.setText("Diapositiva: " + info.getDiapositivaActual() + "/" + totalDiapositivas);
                lblEstado.setText("Conectado");
                habilitarBotonesAccion(true);
            }
        }.execute();
    }

    private void enviarAccion(AccionTipo tipo, Integer n) {
        if (servidor == null || token == null) {
            return;
        }
        String idempotencyKey = campoNombre.getText().trim() + ":" + tipo + ":" + System.currentTimeMillis();
        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    servidor.solicitarAccion(token, idempotencyKey, tipo, n);
                } catch (RemoteException | PermisoDenegadoException e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(ClienteFrame.this, error.getMessage(),
                            "Accion rechazada", JOptionPane.WARNING_MESSAGE);
                }
            }
        }.execute();
    }

    private void habilitarBotonesAccion(boolean habilitados) {
        btnAnterior.setEnabled(habilitados);
        btnSiguiente.setEnabled(habilitados);
        btnIrA.setEnabled(habilitados);
        btnPantallaCompleta.setEnabled(habilitados);
    }

    // ---- Llamadas desde ImpControlCallback (hilo RMI) ----

    void actualizarDiapositivaActual(int nueva) {
        SwingUtilities.invokeLater(() -> {
            lblDiapositivaActual.setText("Diapositiva: " + nueva + "/" + totalDiapositivas);
            spinnerIrA.setValue(nueva);
        });
    }
}
