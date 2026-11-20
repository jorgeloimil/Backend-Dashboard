package com.homedashboard.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.homedashboard.config.PanelProperties;
import com.homedashboard.dtos.Tiempo;
import com.homedashboard.dtos.TiempoActual;
import com.homedashboard.dtos.TiempoDia;
import com.homedashboard.dtos.TiempoHora;
import com.homedashboard.exceptions.ServicioExternoException;
import com.homedashboard.services.ITiempoService;
import com.homedashboard.services.external.OpenMeteoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiempo de Open-Meteo con una caché en memoria: la tablet pregunta a menudo y
 * Open-Meteo solo cambia cada cuarto de hora. Si Open-Meteo falla, se sirve lo
 * último que se tenga, por viejo que sea, antes que un panel vacío.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TiempoServiceImpl implements ITiempoService {

    static final int HORAS = 12;
    static final int DIAS = 6;

    private final OpenMeteoClient openMeteo;
    private final PanelProperties properties;
    private final Clock clock;

    private volatile Tiempo cache;

    @Override
    public Tiempo tiempo() {
        Tiempo actual = cache;
        Instant ahora = clock.instant();
        if (actual != null && actual.getActualizadoEn().toInstant().plus(properties.tiempo().cache()).isAfter(ahora)) {
            return actual;
        }
        try {
            Tiempo nuevo = interpretar(openMeteo.prevision(), ahora);
            cache = nuevo;
            return nuevo;
        } catch (ServicioExternoException | IllegalArgumentException | NullPointerException e) {
            if (actual != null) {
                log.warn("Open-Meteo no responde; se sirve el tiempo de {}", actual.getActualizadoEn(), e);
                return actual;
            }
            if (e instanceof ServicioExternoException servicio) {
                throw servicio;
            }
            throw new ServicioExternoException("Open-Meteo devolvió datos que no se entienden", e);
        }
    }

    Tiempo interpretar(JsonNode json, Instant ahora) {
        ZoneId zona = properties.zona();
        JsonNode actual = json.path("current");
        int codigoActual = actual.path("weather_code").asInt();

        TiempoActual tiempoActual = new TiempoActual()
                .temperatura(actual.path("temperature_2m").asDouble())
                .sensacion(actual.path("apparent_temperature").asDouble())
                .humedad(actual.path("relative_humidity_2m").asInt())
                .viento(actual.path("wind_speed_10m").asDouble())
                .codigo(codigoActual)
                .descripcion(CodigosTiempo.descripcion(codigoActual))
                .esDeDia(actual.path("is_day").asInt(1) == 1);

        return new Tiempo()
                .ubicacion(properties.tiempo().ubicacion())
                .actualizadoEn(ahora.atOffset(ZoneOffset.UTC))
                .actual(tiempoActual)
                .horas(horas(json.path("hourly"), ahora, zona))
                .dias(dias(json.path("daily"), ahora, zona));
    }

    /** Las próximas horas, empezando por la hora en curso. */
    private static List<TiempoHora> horas(JsonNode hourly, Instant ahora, ZoneId zona) {
        JsonNode tiempos = hourly.path("time");
        Instant horaEnCurso = ahora.minusSeconds(3600);
        List<TiempoHora> horas = new ArrayList<>();
        for (int i = 0; i < tiempos.size() && horas.size() < HORAS; i++) {
            OffsetDateTime hora = utc(tiempos.get(i).asText(), zona);
            if (hora.toInstant().isAfter(horaEnCurso)) {
                horas.add(new TiempoHora()
                        .hora(hora)
                        .temperatura(hourly.path("temperature_2m").get(i).asDouble())
                        .codigo(hourly.path("weather_code").get(i).asInt())
                        .probPrecipitacion(hourly.path("precipitation_probability").get(i).asInt()));
            }
        }
        return horas;
    }

    /** Hoy (en casa) y los días siguientes. */
    private static List<TiempoDia> dias(JsonNode daily, Instant ahora, ZoneId zona) {
        JsonNode fechas = daily.path("time");
        LocalDate hoy = LocalDate.ofInstant(ahora, zona);
        List<TiempoDia> dias = new ArrayList<>();
        for (int i = 0; i < fechas.size() && dias.size() < DIAS; i++) {
            LocalDate fecha = LocalDate.parse(fechas.get(i).asText());
            if (fecha.isBefore(hoy)) {
                continue;
            }
            int codigo = daily.path("weather_code").get(i).asInt();
            dias.add(new TiempoDia()
                    .fecha(fecha)
                    .maxima(daily.path("temperature_2m_max").get(i).asDouble())
                    .minima(daily.path("temperature_2m_min").get(i).asDouble())
                    .codigo(codigo)
                    .descripcion(CodigosTiempo.descripcion(codigo))
                    .probPrecipitacion(daily.path("precipitation_probability_max").get(i).asInt())
                    .amanecer(utcOpcional(daily.path("sunrise").get(i), zona))
                    .atardecer(utcOpcional(daily.path("sunset").get(i), zona)));
        }
        return dias;
    }

    /** Open-Meteo da horas locales ("2026-11-20T08:00") en la zona pedida: se pasan a UTC. */
    private static OffsetDateTime utc(String horaLocal, ZoneId zona) {
        return LocalDateTime.parse(horaLocal).atZone(zona).toInstant().atOffset(ZoneOffset.UTC);
    }

    private static OffsetDateTime utcOpcional(JsonNode nodo, ZoneId zona) {
        return nodo == null || nodo.isNull() || nodo.asText().isBlank() ? null : utc(nodo.asText(), zona);
    }
}
