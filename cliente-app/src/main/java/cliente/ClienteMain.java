package cliente;

import common.RedUtil;

import javax.swing.SwingUtilities;

public class ClienteMain {

    public static void main(String[] args) {
        // El servidor necesita poder llamar de vuelta a este cliente
        // (callback RMI), asi que se anuncia con la IP de esta maquina en la
        // red local en vez de localhost. Se hace aqui para que el .jar
        // funcione solo con "java -jar cliente.jar", sin flags.
        System.setProperty("java.rmi.server.hostname", RedUtil.detectarIpLocal());

        SwingUtilities.invokeLater(() -> {
            ClienteFrame frame = new ClienteFrame();
            frame.setVisible(true);
            frame.pedirIpServidorYConectar();
        });
    }
}
