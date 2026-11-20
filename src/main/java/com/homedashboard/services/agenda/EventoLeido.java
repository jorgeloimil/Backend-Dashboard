package com.homedashboard.services.agenda;

import java.time.Instant;

/** Una ocurrencia de un evento de un calendario iCal, ya con sus repeticiones resueltas. */
public record EventoLeido(String titulo, Instant inicio, Instant fin, boolean todoElDia, String ubicacion) {
}
