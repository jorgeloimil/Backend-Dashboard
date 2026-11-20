package com.homedashboard.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.homedashboard.config.PanelProperties;
import com.homedashboard.dtos.EstadoFitcina;
import com.homedashboard.dtos.EstadoRutinaHoy;
import com.homedashboard.dtos.FitcinaHoy;
import com.homedashboard.dtos.RutinaHoy;
import com.homedashboard.exceptions.ServicioExternoException;
import com.homedashboard.commons.utils.Textos;
import com.homedashboard.services.IFitcinaService;
import com.homedashboard.services.external.FitcinaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

/**
 * Cruza las rutinas de Fitcina (con sus días de la semana) con las sesiones de hoy.
 * La tarjeta del panel no puede romperse porque Fitcina esté caída: este servicio no
 * lanza nunca, y degrada a la última copia buena (DESACTUALIZADO) o a NO_DISPONIBLE.
 * Tras un fallo no se vuelve a llamar a Fitcina hasta pasado {@code reintento}, para
 * no martillearla (ni quemar su límite de intentos de login si la contraseña está mal).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FitcinaServiceImpl implements IFitcinaService {

    private static final int SESIONES_A_LEER = 30;

    private final FitcinaClient fitcina;
    private final PanelProperties properties;
    private final Clock clock;

    private volatile FitcinaHoy cache;
    private volatile Instant ultimoFallo;
    private volatile String motivoFallo;

    @Override
    public FitcinaHoy hoy() {
        PanelProperties.Fitcina config = properties.fitcina();
        Instant ahora = clock.instant();
        LocalDate hoy = LocalDate.ofInstant(ahora, properties.zona());

        if (!config.configurada()) {
            return vacio(EstadoFitcina.SIN_CONFIGURAR, "Faltan FITCINA_EMAIL y FITCINA_PASSWORD en el panel", hoy);
        }
        FitcinaHoy actual = cache;
        // La copia solo vale si es de hoy: pasada la medianoche "lo que toca" ya es otro
        if (actual != null && actual.getEstado() == EstadoFitcina.OK && actual.getFecha().equals(hoy)
                && actual.getActualizadoEn().toInstant().plus(config.cache()).isAfter(ahora)) {
            return actual;
        }
        if (ultimoFallo != null && ultimoFallo.plus(config.reintento()).isAfter(ahora)) {
            return degradado(actual, motivoFallo, hoy);
        }
        try {
            FitcinaHoy nuevo = leer(ahora, hoy);
            cache = nuevo;
            ultimoFallo = null;
            return nuevo;
        } catch (ServicioExternoException | IllegalArgumentException | NullPointerException e) {
            ultimoFallo = ahora;
            motivoFallo = e instanceof ServicioExternoException
                    ? e.getMessage() : "Fitcina devolvió datos que no se entienden";
            log.warn("Fitcina no disponible: {}", motivoFallo, e);
            return degradado(actual, motivoFallo, hoy);
        }
    }

    private FitcinaHoy leer(Instant ahora, LocalDate hoy) {
        ZoneId zona = properties.zona();
        JsonNode rutinas = fitcina.rutinas();
        JsonNode sesiones = fitcina.sesiones(SESIONES_A_LEER);
        JsonNode stats = fitcina.estadisticas(zona);

        Set<Long> hechas = new HashSet<>();
        Set<Long> enCurso = new HashSet<>();
        for (JsonNode sesion : sesiones) {
            if (!esDeHoy(sesion.path("fechaInicio").asText(null), hoy, zona)) {
                continue;
            }
            (sesion.path("completada").asBoolean(false) ? hechas : enCurso).add(sesion.path("rutinaId").asLong());
        }

        List<RutinaHoy> deHoy = new ArrayList<>();
        for (JsonNode rutina : rutinas) {
            if (!tocaHoy(rutina.path("diasSemana").asText(""), hoy.getDayOfWeek())) {
                continue;
            }
            long id = rutina.path("id").asLong();
            EstadoRutinaHoy estado = hechas.contains(id) ? EstadoRutinaHoy.HECHA
                    : enCurso.contains(id) ? EstadoRutinaHoy.EN_CURSO : EstadoRutinaHoy.PENDIENTE;
            deHoy.add(new RutinaHoy()
                    .id(id)
                    .nombre(rutina.path("nombre").asText())
                    .emoji(textoOpcional(rutina.path("emoji")))
                    .intensidad(textoOpcional(rutina.path("intensidad")))
                    .ejercicios(rutina.path("ejercicioCount").asInt(rutina.path("ejercicios").size()))
                    .estado(estado));
        }

        return new FitcinaHoy()
                .estado(EstadoFitcina.OK)
                .fecha(hoy)
                .actualizadoEn(ahora.atOffset(ZoneOffset.UTC))
                .rutinas(deHoy)
                .rachaDias(stats.path("rachaDias").asInt(0))
                .sesionesSemana(stats.path("estaSemana").asInt(0));
    }

    /** "Lunes,Miércoles,Viernes": sin distinguir mayúsculas ni tildes ("Miercoles" también vale). */
    static boolean tocaHoy(String diasSemana, DayOfWeek dia) {
        String hoy = Textos.clave(dia.getDisplayName(java.time.format.TextStyle.FULL, Locale.of("es", "ES")));
        return Arrays.stream(diasSemana.split(","))
                .map(Textos::clave)
                .anyMatch(hoy::equals);
    }

    /** Las sesiones llegan en UTC con la Z; "hoy" es el día de casa. */
    private static boolean esDeHoy(String fechaInicio, LocalDate hoy, ZoneId zona) {
        if (fechaInicio == null) {
            return false;
        }
        try {
            return OffsetDateTime.parse(fechaInicio).atZoneSameInstant(zona).toLocalDate().equals(hoy);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String textoOpcional(JsonNode nodo) {
        return nodo.isMissingNode() || nodo.isNull() || nodo.asText().isBlank() ? null : nodo.asText();
    }

    /** Con copia de hoy se sirve marcada como vieja; sin ella, la tarjeta queda con el aviso. */
    private static FitcinaHoy degradado(FitcinaHoy copia, String motivo, LocalDate hoy) {
        if (copia != null && copia.getFecha().equals(hoy)) {
            return new FitcinaHoy()
                    .estado(EstadoFitcina.DESACTUALIZADO)
                    .motivo(motivo)
                    .fecha(copia.getFecha())
                    .actualizadoEn(copia.getActualizadoEn())
                    .rutinas(copia.getRutinas())
                    .rachaDias(copia.getRachaDias())
                    .sesionesSemana(copia.getSesionesSemana());
        }
        return vacio(EstadoFitcina.NO_DISPONIBLE, motivo, hoy);
    }

    private static FitcinaHoy vacio(EstadoFitcina estado, String motivo, LocalDate hoy) {
        return new FitcinaHoy().estado(estado).motivo(motivo).fecha(hoy).rutinas(List.of());
    }
}
