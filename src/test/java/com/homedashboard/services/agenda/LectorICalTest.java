package com.homedashboard.services.agenda;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LectorICalTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private final LectorICal lector = new LectorICal();

    /** Del viernes 20 al jueves 26 de noviembre de 2026 (ambos incluidos). */
    private List<EventoLeido> semana() throws IOException {
        return lector.leer(ics("familia"), LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 27), MADRID);
    }

    @Test
    void leeLosEventosDeLaSemanaEnOrden() throws IOException {
        assertThat(semana()).extracting(EventoLeido::titulo).containsExactly(
                "Viaje de trabajo",       // empezó el 19, sigue el 20
                "Mercadillo",             // hora flotante del domingo 22
                "Dentista",               // lunes 23, 17:15
                "Natación (antes)",       // la del lunes 23 se movió a las 18:30
                "Cumple de la abuela");   // miércoles 25 (la natación de ese día se anuló)
    }

    @Test
    void lasHorasConZonaSePasanAInstantes() throws IOException {
        EventoLeido dentista = buscar(semana(), "Dentista");
        assertThat(dentista.inicio()).isEqualTo(Instant.parse("2026-11-23T16:15:00Z"));
        assertThat(dentista.fin()).isEqualTo(Instant.parse("2026-11-23T17:00:00Z"));
        assertThat(dentista.todoElDia()).isFalse();
        assertThat(dentista.ubicacion()).isEqualTo("Clínica Sonrisas");
    }

    @Test
    void losEventosDeTodoElDiaEmpiezanALaMedianocheDeCasa() throws IOException {
        EventoLeido cumple = buscar(semana(), "Cumple de la abuela");
        assertThat(cumple.todoElDia()).isTrue();
        assertThat(cumple.inicio()).isEqualTo(Instant.parse("2026-11-24T23:00:00Z"));
        assertThat(cumple.fin()).isEqualTo(Instant.parse("2026-11-25T23:00:00Z"));
    }

    @Test
    void lasHorasFlotantesSonDeCasa() throws IOException {
        assertThat(buscar(semana(), "Mercadillo").inicio()).isEqualTo(Instant.parse("2026-11-22T09:00:00Z"));
    }

    @Test
    void lasRepeticionesSalenCadaSemana() throws IOException {
        List<EventoLeido> dosSemanas = lector.leer(ics("familia"), LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 7), MADRID);
        assertThat(dosSemanas).filteredOn(e -> e.titulo().equals("Natación"))
                .extracting(EventoLeido::inicio)
                .containsExactly(Instant.parse("2026-11-30T18:00:00Z"), Instant.parse("2026-12-02T18:00:00Z"));
    }

    @Test
    void unTextoQueNoEsICalFalla() {
        assertThatThrownBy(() -> lector.leer("<html>hola</html>", LocalDate.now(), LocalDate.now().plusDays(1), MADRID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static EventoLeido buscar(List<EventoLeido> eventos, String titulo) {
        return eventos.stream().filter(e -> e.titulo().equals(titulo)).findFirst().orElseThrow();
    }

    static String ics(String nombre) throws IOException {
        try (InputStream in = LectorICalTest.class.getResourceAsStream("/ical/" + nombre + ".ics")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
