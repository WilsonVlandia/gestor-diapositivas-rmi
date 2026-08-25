package cliente;

import javax.swing.SwingUtilities;

public class ClienteMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteFrame frame = new ClienteFrame();
            frame.setVisible(true);
            frame.pedirIpServidorYConectar();
        });
    }
}
