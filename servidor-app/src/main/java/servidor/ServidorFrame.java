package servidor;

import common.AccionTipo;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Ventana del presentador: muestra la diapositiva actual (con modo pantalla
 * completa), tiene sus propios botones de navegacion locales, un log de
 * actividad y el panel de controles conectados.
 */
public class ServidorFrame extends JFrame {

    private final File[] diapositivas;
    private ImpPresentationServer server;

    private final JLabel lblDiapositiva = new JLabel("", SwingConstants.CENTER);
    private final JTextArea areaLog = new JTextArea();
    private final JPanel panelConexiones = new JPanel();
    private final JSpinner spinnerIrA;

    private boolean pantallaCompleta = false;
    private final GraphicsDevice dispositivoGrafico =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

    public ServidorFrame(File carpetaDiapositivas, String ip) {
        super("Presentador - Servidor RMI");
        this.diapositivas = cargarDiapositivas(carpetaDiapositivas);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        lblDiapositiva.setBackground(Color.BLACK);
        lblDiapositiva.setOpaque(true);
        lblDiapositiva.setForeground(Color.WHITE);
        add(lblDiapositiva, BorderLayout.CENTER);

        // --- Aviso con la IP para los clientes (visible sin terminal) ---
        JLabel lblIp = new JLabel("IP de este servidor para los clientes:  " + ip + ":1802/presentador",
                SwingConstants.CENTER);
        lblIp.setOpaque(true);
        lblIp.setBackground(new Color(255, 249, 196));
        lblIp.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(lblIp, BorderLayout.NORTH);

        // --- Controles locales del presentador ---
        JPanel panelControlesLocales = new JPanel();
        JButton btnAnterior = new JButton("◀ Anterior");
        JButton btnSiguiente = new JButton("Siguiente ▶");
        JButton btnPantallaCompleta = new JButton("Pantalla completa");
        spinnerIrA = new JSpinner(new SpinnerNumberModel(1, 1, Math.max(1, diapositivas.length), 1));
        JButton btnIrA = new JButton("Ir a");

        btnAnterior.addActionListener(e -> server.ejecutarAccionLocal(AccionTipo.RETROCEDER, null));
        btnSiguiente.addActionListener(e -> server.ejecutarAccionLocal(AccionTipo.AVANZAR, null));
        btnIrA.addActionListener(e -> server.ejecutarAccionLocal(AccionTipo.IR_A, (Integer) spinnerIrA.getValue()));
        btnPantallaCompleta.addActionListener(e -> alternarPantallaCompleta());

        panelControlesLocales.add(btnAnterior);
        panelControlesLocales.add(btnSiguiente);
        panelControlesLocales.add(spinnerIrA);
        panelControlesLocales.add(btnIrA);
        panelControlesLocales.add(btnPantallaCompleta);

        // --- Log de actividad ---
        areaLog.setEditable(false);
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setPreferredSize(new Dimension(100, 160));
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log de actividad"));

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(panelControlesLocales, BorderLayout.NORTH);
        panelInferior.add(scrollLog, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        // --- Panel de conexiones ---
        panelConexiones.setLayout(new BoxLayout(panelConexiones, BoxLayout.Y_AXIS));
        JScrollPane scrollConexiones = new JScrollPane(panelConexiones);
        scrollConexiones.setPreferredSize(new Dimension(280, 100));
        scrollConexiones.setBorder(BorderFactory.createTitledBorder("Controles conectados"));
        add(scrollConexiones, BorderLayout.EAST);

        setSize(1100, 720);
        setLocationRelativeTo(null);

        mostrarDiapositiva(1);
    }

    void setServer(ImpPresentationServer server) {
        this.server = server;
    }

    private File[] cargarDiapositivas(File carpeta) {
        File[] archivos = carpeta.listFiles((dir, name) -> {
            String n = name.toLowerCase();
            return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".gif");
        });
        if (archivos == null) {
            archivos = new File[0];
        }
        Arrays.sort(archivos);
        return archivos;
    }

    public int getTotalDiapositivas() {
        return Math.max(1, diapositivas.length);
    }

    private void alternarPantallaCompleta() {
        pantallaCompleta = !pantallaCompleta;
        dispose();
        setUndecorated(pantallaCompleta);
        if (pantallaCompleta) {
            dispositivoGrafico.setFullScreenWindow(this);
        } else {
            dispositivoGrafico.setFullScreenWindow(null);
            setSize(1100, 720);
            setLocationRelativeTo(null);
        }
        setVisible(true);
        mostrarDiapositivaActualDeNuevo();
    }

    private int ultimaDiapositivaMostrada = 1;

    private void mostrarDiapositivaActualDeNuevo() {
        mostrarDiapositiva(ultimaDiapositivaMostrada);
    }

    /** Llamado (desde el hilo RMI, via solicitarAccion PANTALLA_COMPLETA) para alternar el modo. */
    public void alternarPantallaCompletaDesdeControl() {
        SwingUtilities.invokeLater(this::alternarPantallaCompleta);
    }

    /** Llamado desde ImpPresentationServer (hilo RMI) para reflejar la diapositiva actual. */
    public void actualizarDiapositiva(int numero) {
        SwingUtilities.invokeLater(() -> mostrarDiapositiva(numero));
    }

    private void mostrarDiapositiva(int numero) {
        ultimaDiapositivaMostrada = numero;
        if (diapositivas.length == 0) {
            lblDiapositiva.setText("No hay diapositivas en la carpeta 'diapositivas'");
            return;
        }
        int idx = Math.max(1, Math.min(numero, diapositivas.length)) - 1;
        ImageIcon icono = new ImageIcon(diapositivas[idx].getPath());
        int ancho = getContentPane().getWidth() > 0 ? getContentPane().getWidth() : 1000;
        int alto = lblDiapositiva.getHeight() > 0 ? lblDiapositiva.getHeight() : 600;
        Image escalada = icono.getImage().getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
        lblDiapositiva.setIcon(new ImageIcon(escalada));
        lblDiapositiva.setText(null);
        spinnerIrA.setValue(idx + 1);
        setTitle("Presentador - Diapositiva " + (idx + 1) + "/" + diapositivas.length);
    }

    /** Llamado desde ImpPresentationServer para agregar una linea al log de actividad. */
    public void agregarLog(String texto) {
        String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            areaLog.append("[" + hora + "] " + texto + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    /** Ventana emergente de Aceptar/Rechazar para una nueva solicitud de conexion. */
    public void mostrarSolicitudConexion(String nombreControl, Runnable aceptar, Runnable rechazar) {
        JDialog dialogo = new JDialog(this, "Nueva conexion", false);
        dialogo.setLayout(new BorderLayout(10, 10));
        dialogo.add(new JLabel("  El control \"" + nombreControl + "\" quiere conectarse.  ", SwingConstants.CENTER),
                BorderLayout.CENTER);

        JPanel botones = new JPanel();
        JButton btnAceptar = new JButton("Aceptar");
        JButton btnRechazar = new JButton("Rechazar");
        btnAceptar.addActionListener(e -> {
            aceptar.run();
            dialogo.dispose();
        });
        btnRechazar.addActionListener(e -> {
            rechazar.run();
            dialogo.dispose();
        });
        botones.add(btnAceptar);
        botones.add(btnRechazar);
        dialogo.add(botones, BorderLayout.SOUTH);

        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setAlwaysOnTop(true);
        dialogo.setVisible(true);
    }

    /** Refresca el panel de controles conectados. */
    public void actualizarListaConexiones(List<ControlInfoUI> conexiones) {
        SwingUtilities.invokeLater(() -> {
            panelConexiones.removeAll();
            for (ControlInfoUI info : conexiones) {
                panelConexiones.add(crearFilaControl(info));
            }
            panelConexiones.revalidate();
            panelConexiones.repaint();
        });
    }

    private JPanel crearFilaControl(ControlInfoUI info) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila.add(new JLabel(info.nombre));
        return fila;
    }
}
