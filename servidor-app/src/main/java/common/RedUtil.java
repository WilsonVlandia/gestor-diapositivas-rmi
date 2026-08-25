package common;

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
            // se intenta el respaldo de abajo
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}
