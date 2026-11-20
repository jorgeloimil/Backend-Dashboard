package com.homedashboard.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * Configuración del panel ({@code panel.*} en application.yml). Todo se puede
 * sobrescribir con variables de entorno, que es como lo hará el docker-compose.
 */
@Validated
@ConfigurationProperties(prefix = "panel")
public record PanelProperties(
        @DefaultValue("Europe/Madrid") ZoneId zona,
        @Valid @NotNull Tiempo tiempo,
        @Valid @NotNull Agenda agenda) {

    public record Tiempo(
            @NotBlank String ubicacion,
            double latitud,
            double longitud,
            @NotNull URI url,
            @DefaultValue("10m") Duration cache) {
    }

    public record Agenda(
            @DefaultValue("5m") Duration cache,
            @DefaultValue List<@Valid Calendario> calendarios) {
    }

    /** Un calendario iCal: nombre visible, URL secreta y color en el panel. */
    public record Calendario(
            @NotBlank String nombre,
            @NotNull URI url,
            @DefaultValue("#FF5A36") String color) {
    }
}
