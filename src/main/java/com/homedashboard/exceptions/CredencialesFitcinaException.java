package com.homedashboard.exceptions;

/** Fitcina rechazó el email o la contraseña configurados. */
public class CredencialesFitcinaException extends ServicioExternoException {

    public CredencialesFitcinaException(String mensaje) {
        super(mensaje, null);
    }
}
