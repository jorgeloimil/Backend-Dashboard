package com.homedashboard.services.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.homedashboard.config.PanelProperties;
import com.homedashboard.exceptions.ServicioExternoException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Cliente de Open-Meteo (https://open-meteo.com): gratis, sin clave y con datos de
 * AEMET y otros modelos europeos. Devuelve el JSON tal cual; lo interpreta TiempoServiceImpl.
 */
@Component
public class OpenMeteoClient {

    private static final String ACTUAL = "temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m";
    private static final String HORAS = "temperature_2m,precipitation_probability,weather_code";
    private static final String DIAS = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset";

    private final RestClient restClient;
    private final PanelProperties properties;

    public OpenMeteoClient(RestClient.Builder restClientBuilderExterno, PanelProperties properties) {
        this.restClient = restClientBuilderExterno.build();
        this.properties = properties;
    }

    public JsonNode prevision() {
        PanelProperties.Tiempo tiempo = properties.tiempo();
        URI uri = UriComponentsBuilder.fromUri(tiempo.url())
                .queryParam("latitude", tiempo.latitud())
                .queryParam("longitude", tiempo.longitud())
                .queryParam("current", ACTUAL)
                .queryParam("hourly", HORAS)
                .queryParam("daily", DIAS)
                .queryParam("timezone", properties.zona().getId())
                .queryParam("forecast_days", 7)
                .queryParam("wind_speed_unit", "kmh")
                .build()
                .encode()
                .toUri();
        try {
            JsonNode respuesta = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            if (respuesta == null || !respuesta.has("current")) {
                throw new ServicioExternoException("Open-Meteo devolvió una respuesta vacía", null);
            }
            return respuesta;
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo consultar el tiempo en Open-Meteo", e);
        }
    }
}
