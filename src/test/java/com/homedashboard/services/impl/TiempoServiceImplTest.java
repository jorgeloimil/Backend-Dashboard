package com.homedashboard.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homedashboard.config.PanelProperties;
import com.homedashboard.dtos.Tiempo;
import com.homedashboard.exceptions.ServicioExternoException;
import com.homedashboard.services.external.OpenMeteoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TiempoServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-20T12:30:00Z"); // 13:30 en Madrid

    private final OpenMeteoClient openMeteo = mock(OpenMeteoClient.class);
    private final MutableClock reloj = new MutableClock(AHORA);
    private TiempoServiceImpl servicio;

    @BeforeEach
    void preparar() throws IOException {
        PanelProperties properties = new PanelProperties(ZoneId.of("Europe/Madrid"),
                new PanelProperties.Tiempo("Madrid", 40.4, -3.7, URI.create("http://open-meteo.test"), Duration.ofMinutes(10)),
                new PanelProperties.Agenda(Duration.ofMinutes(5), List.of()),
                new PanelProperties.Fitcina(URI.create("http://fitcina.test"), "", "", Duration.ofMinutes(2), Duration.ofMinutes(1)));
        servicio = new TiempoServiceImpl(openMeteo, properties, reloj);
        JsonNode madrid = new ObjectMapper().readTree(getClass().getResourceAsStream("/open-meteo/madrid.json"));
        when(openMeteo.prevision()).thenReturn(madrid);
    }

    @Test
    void interpretaLaRespuestaDeOpenMeteo() {
        Tiempo tiempo = servicio.tiempo();

        assertThat(tiempo.getUbicacion()).isEqualTo("Madrid");
        assertThat(tiempo.getActual().getTemperatura()).isEqualTo(11.4);
        assertThat(tiempo.getActual().getDescripcion()).isEqualTo("Parcialmente nublado");
        assertThat(tiempo.getActual().getEsDeDia()).isTrue();
        // Empieza por la hora en curso (13:00 Madrid = 12:00Z), no por las que ya pasaron
        assertThat(tiempo.getHoras()).extracting(h -> h.getHora().toInstant())
                .containsExactly(Instant.parse("2026-11-20T12:00:00Z"), Instant.parse("2026-11-20T13:00:00Z"),
                        Instant.parse("2026-11-20T14:00:00Z"));
        // Hoy y mañana; el 19 ya pasó
        assertThat(tiempo.getDias()).extracting(d -> d.getFecha().toString()).containsExactly("2026-11-20", "2026-11-21");
        assertThat(tiempo.getDias().get(1).getDescripcion()).isEqualTo("Lluvia débil");
        assertThat(tiempo.getDias().get(0).getAmanecer().toInstant()).isEqualTo(Instant.parse("2026-11-20T06:57:00Z"));
    }

    @Test
    void laCacheEvitaPreguntarDeNuevo() {
        servicio.tiempo();
        reloj.avanzar(Duration.ofMinutes(9));
        servicio.tiempo();
        verify(openMeteo, times(1)).prevision();

        reloj.avanzar(Duration.ofMinutes(2));
        servicio.tiempo();
        verify(openMeteo, times(2)).prevision();
    }

    @Test
    void siOpenMeteoFallaSeUsaLaUltimaCopia() {
        Tiempo bueno = servicio.tiempo();
        reloj.avanzar(Duration.ofHours(1));
        when(openMeteo.prevision()).thenThrow(new ServicioExternoException("caído", null));

        assertThat(servicio.tiempo()).isSameAs(bueno);
    }

    @Test
    void sinCopiaElErrorSube() {
        when(openMeteo.prevision()).thenThrow(new ServicioExternoException("caído", null));
        assertThatThrownBy(servicio::tiempo).isInstanceOf(ServicioExternoException.class);
    }

    /** Reloj que se puede adelantar. */
    static class MutableClock extends Clock {
        private Instant ahora;

        MutableClock(Instant ahora) {
            this.ahora = ahora;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
