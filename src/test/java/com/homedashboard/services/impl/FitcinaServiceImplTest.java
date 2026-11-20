package com.homedashboard.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homedashboard.config.PanelProperties;
import com.homedashboard.dtos.EstadoFitcina;
import com.homedashboard.dtos.EstadoRutinaHoy;
import com.homedashboard.dtos.FitcinaHoy;
import com.homedashboard.exceptions.CredencialesFitcinaException;
import com.homedashboard.exceptions.ServicioExternoException;
import com.homedashboard.services.external.FitcinaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FitcinaServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-20T12:30:00Z"); // viernes 13:30 en Madrid
    private static final ObjectMapper JSON = new ObjectMapper();

    private final FitcinaClient cliente = mock(FitcinaClient.class);
    private final TiempoServiceImplTest.MutableClock reloj = new TiempoServiceImplTest.MutableClock(AHORA);
    private FitcinaServiceImpl servicio;

    @BeforeEach
    void preparar() throws Exception {
        servicio = servicio("jorge@casa.test", "secreto");
        when(cliente.rutinas()).thenReturn(json("""
                [
                  {"id": 1, "nombre": "Empuje", "emoji": "💪", "intensidad": "ALTA", "ejercicioCount": 5, "diasSemana": "Lunes,Viernes"},
                  {"id": 2, "nombre": "Pierna", "ejercicioCount": 6, "diasSemana": "Martes,Jueves"},
                  {"id": 3, "nombre": "Core", "ejercicioCount": 3, "diasSemana": "viernes"},
                  {"id": 4, "nombre": "Sin días", "ejercicioCount": 2}
                ]"""));
        when(cliente.sesiones(anyInt())).thenReturn(json("[]"));
        when(cliente.estadisticas(any())).thenReturn(json("{\"rachaDias\": 3, \"estaSemana\": 2}"));
    }

    private FitcinaServiceImpl servicio(String email, String password) {
        PanelProperties properties = new PanelProperties(ZoneId.of("Europe/Madrid"),
                new PanelProperties.Tiempo("Madrid", 40.4, -3.7, URI.create("http://open-meteo.test"), Duration.ofMinutes(10)),
                new PanelProperties.Agenda(Duration.ofMinutes(5), List.of()),
                new PanelProperties.Fitcina(URI.create("http://fitcina.test"), email, password,
                        Duration.ofMinutes(2), Duration.ofMinutes(1)));
        return new FitcinaServiceImpl(cliente, properties, reloj);
    }

    private static JsonNode json(String texto) throws Exception {
        return JSON.readTree(texto);
    }

    @Test
    void devuelveSoloLasRutinasQueTocanHoy() {
        FitcinaHoy hoy = servicio.hoy();

        assertThat(hoy.getEstado()).isEqualTo(EstadoFitcina.OK);
        assertThat(hoy.getFecha().toString()).isEqualTo("2026-11-20");
        assertThat(hoy.getRutinas()).extracting(r -> r.getNombre()).containsExactly("Empuje", "Core");
        assertThat(hoy.getRutinas().get(0).getEjercicios()).isEqualTo(5);
        assertThat(hoy.getRutinas().get(0).getEmoji()).isEqualTo("💪");
        assertThat(hoy.getRutinas()).allMatch(r -> r.getEstado() == EstadoRutinaHoy.PENDIENTE);
        assertThat(hoy.getRachaDias()).isEqualTo(3);
        assertThat(hoy.getSesionesSemana()).isEqualTo(2);
    }

    @Test
    void marcaHechaLaRutinaConSesionCompletadaHoyYEnCursoLaEmpezada() throws Exception {
        when(cliente.sesiones(anyInt())).thenReturn(json("""
                [
                  {"id": 9, "rutinaId": 1, "fechaInicio": "2026-11-20T08:00:00Z", "completada": true},
                  {"id": 8, "rutinaId": 3, "fechaInicio": "2026-11-20T11:00:00Z", "completada": false},
                  {"id": 7, "rutinaId": 3, "fechaInicio": "2026-11-19T11:00:00Z", "completada": true}
                ]"""));

        FitcinaHoy hoy = servicio.hoy();

        assertThat(hoy.getRutinas()).extracting(r -> r.getEstado())
                .containsExactly(EstadoRutinaHoy.HECHA, EstadoRutinaHoy.EN_CURSO);
    }

    @Test
    void elDiaSeCuentaEnLaZonaDeCasa() throws Exception {
        // 23:30Z del jueves ya es viernes en Madrid (00:30): cuenta como hoy
        when(cliente.sesiones(anyInt())).thenReturn(json(
                "[{\"id\": 9, \"rutinaId\": 1, \"fechaInicio\": \"2026-11-19T23:30:00Z\", \"completada\": true}]"));

        assertThat(servicio.hoy().getRutinas().get(0).getEstado()).isEqualTo(EstadoRutinaHoy.HECHA);
    }

    @Test
    void diaDeDescansoNoTraeRutinas() throws Exception {
        reloj.avanzar(Duration.ofDays(1)); // sábado
        FitcinaHoy hoy = servicio.hoy();

        assertThat(hoy.getEstado()).isEqualTo(EstadoFitcina.OK);
        assertThat(hoy.getRutinas()).isEmpty();
    }

    @Test
    void sinCredencialesQuedaSinConfigurarYNoLlamaAFitcina() {
        FitcinaHoy hoy = servicio("", "").hoy();

        assertThat(hoy.getEstado()).isEqualTo(EstadoFitcina.SIN_CONFIGURAR);
        assertThat(hoy.getRutinas()).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(cliente);
    }

    @Test
    void laCacheEvitaPreguntarDeNuevo() {
        servicio.hoy();
        reloj.avanzar(Duration.ofMinutes(1));
        servicio.hoy();
        verify(cliente, times(1)).rutinas();

        reloj.avanzar(Duration.ofMinutes(2));
        servicio.hoy();
        verify(cliente, times(2)).rutinas();
    }

    @Test
    void siFitcinaCaeSeServeLaUltimaCopiaMarcadaComoVieja() {
        FitcinaHoy bueno = servicio.hoy();
        reloj.avanzar(Duration.ofMinutes(10));
        when(cliente.rutinas()).thenThrow(new ServicioExternoException("Fitcina caída", null));

        FitcinaHoy hoy = servicio.hoy();

        assertThat(hoy.getEstado()).isEqualTo(EstadoFitcina.DESACTUALIZADO);
        assertThat(hoy.getMotivo()).isEqualTo("Fitcina caída");
        assertThat(hoy.getRutinas()).isEqualTo(bueno.getRutinas());
        assertThat(hoy.getActualizadoEn()).isEqualTo(bueno.getActualizadoEn());
    }

    @Test
    void sinCopiaYConFitcinaCaidaLaTarjetaDiceNoDisponibleSinLanzar() {
        when(cliente.rutinas()).thenThrow(new ServicioExternoException("Fitcina caída", null));

        FitcinaHoy hoy = servicio.hoy();

        assertThat(hoy.getEstado()).isEqualTo(EstadoFitcina.NO_DISPONIBLE);
        assertThat(hoy.getMotivo()).isEqualTo("Fitcina caída");
        assertThat(hoy.getRutinas()).isEmpty();
    }

    @Test
    void trasUnFalloNoSeReintentaHastaPasadoElPlazo() {
        when(cliente.rutinas()).thenThrow(new CredencialesFitcinaException("Fitcina rechaza el email o la contraseña del panel"));

        servicio.hoy();
        reloj.avanzar(Duration.ofSeconds(30));
        servicio.hoy();
        verify(cliente, times(1)).rutinas();

        reloj.avanzar(Duration.ofSeconds(40));
        servicio.hoy();
        verify(cliente, times(2)).rutinas();
    }

    @Test
    void unaCopiaDeAyerNoSeServiraComoDeHoy() {
        servicio.hoy();
        reloj.avanzar(Duration.ofDays(1));
        when(cliente.rutinas()).thenThrow(new ServicioExternoException("Fitcina caída", null));

        assertThat(servicio.hoy().getEstado()).isEqualTo(EstadoFitcina.NO_DISPONIBLE);
    }

    @Test
    void unaRespuestaIlegibleTambienDegrada() throws Exception {
        when(cliente.rutinas()).thenReturn(null);

        assertThat(servicio.hoy().getEstado()).isEqualTo(EstadoFitcina.NO_DISPONIBLE);
    }

    @ParameterizedTest
    @CsvSource({
            "'Lunes,Miércoles,Viernes', FRIDAY, true",
            "'lunes, miercoles', WEDNESDAY, true",
            "'Sábado', SATURDAY, true",
            "'Lunes', TUESDAY, false",
            "'', MONDAY, false"
    })
    void tocaHoyEntiendeLosDiasDeFitcina(String dias, DayOfWeek dia, boolean esperado) {
        assertThat(FitcinaServiceImpl.tocaHoy(dias, dia)).isEqualTo(esperado);
    }
}
