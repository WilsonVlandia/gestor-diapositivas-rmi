package servidor;

/**
 * Copia inmutable de una ControlSession, pensada solo para refrescar la GUI
 * (panel de conexiones) sin exponer el objeto interno mutable.
 */
public class ControlInfoUI {

    public final String token;
    public final String nombre;

    public ControlInfoUI(String token, String nombre) {
        this.token = token;
        this.nombre = nombre;
    }
}
