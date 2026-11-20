package com.homedashboard.exceptions;

/** Un servicio externo (Open-Meteo, un calendario) no responde o responde algo ilegible. */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
