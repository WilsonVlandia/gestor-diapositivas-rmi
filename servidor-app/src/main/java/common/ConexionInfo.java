package common;

import java.io.Serializable;

/**
 * Datos que el servidor devuelve a un control cuando su conexion es aceptada:
 * el token de sesion que debe usar en cada llamada futura, y el estado actual
 * de la presentacion para que el cliente pueda mostrarlo.
 */
public class ConexionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String token;
    private final int totalDiapositivas;
    private final int diapositivaActual;

    public ConexionInfo(String token, int totalDiapositivas, int diapositivaActual) {
        this.token = token;
        this.totalDiapositivas = totalDiapositivas;
        this.diapositivaActual = diapositivaActual;
    }

    public String getToken() {
        return token;
    }

    public int getTotalDiapositivas() {
        return totalDiapositivas;
    }

    public int getDiapositivaActual() {
        return diapositivaActual;
    }
}
