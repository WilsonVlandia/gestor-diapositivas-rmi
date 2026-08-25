package common;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Detecta la IP de esta maquina en la red local, para anunciar los objetos
 * remotos RMI con una direccion alcanzable desde la otra maquina en vez de
 * "localhost" (java.rmi.server.hostname). Sin esto, RMI funciona en una sola
 * maquina pero no entre dos.
 */
public final class RedUtil {

    private RedUtil() {
    }

    public static String detectarIpLocal() {
        String ip = detectarViaRutaDeSalida();
        if (ip != null) {
            return ip;
        }
        ip = detectarViaInterfaces();
        if (ip != null) {
            return ip;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * Deja que el sistema operativo elija la interfaz de red real (la que
     * usaria para salir hacia otra maquina), en vez de recorrer a mano la
     * lista de interfaces. No se envia trafico de verdad (UDP "conectado"),
     * pero evita elegir por error un adaptador virtual (VPN, VMware,
     * Hyper-V, hotspot compartido...) que suele existir en Windows y que la
     * otra maquina no puede alcanzar.
     */
    private static String detectarViaRutaDeSalida() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress local = socket.getLocalAddress();
            if (local != null && !local.isAnyLocalAddress() && !local.isLoopbackAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception ignored) {
            // sin ruta de salida (ej. sin red); se intenta el respaldo de abajo
        }
        return null;
    }

    /** Respaldo: recorre las interfaces buscando una IPv4 no loopback. */
    private static String detectarViaInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> direcciones = iface.getInetAddresses();
                while (direcciones.hasMoreElements()) {
                    InetAddress direccion = direcciones.nextElement();
                    if (direccion instanceof Inet4Address && !direccion.isLoopbackAddress()) {
                        return direccion.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
            // sin resultado; el llamador usa el ultimo respaldo
        }
        return null;
    }
}
