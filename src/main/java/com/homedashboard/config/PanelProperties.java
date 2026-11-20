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
        @Valid @NotNull Agenda agenda,
        @Valid @DefaultValue Fitcina fitcina) {

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

    /**
     * Cómo llegar a Fitcina. En el NUC es una llamada entre contenedores del mismo
     * docker-compose ({@code http://fitcina-backend:8080}). Sin email y contraseña la
     * integración queda desactivada y la tarjeta del panel lo dice.
     */
    public record Fitcina(
            @DefaultValue("http://fitcina-backend:8080") URI url,
            @DefaultValue("") String email,
            @DefaultValue("") String password,
            @DefaultValue("2m") Duration cache,
            @DefaultValue("1m") Duration reintento) {

        public boolean configurada() {
            return !email.isBlank() && !password.isBlank();
        }
    }

    /** Un calendario iCal: nombre visible, URL secreta y color en el panel. */
    public record Calendario(
            @NotBlank String nombre,
            @NotNull URI url,
            @DefaultValue("#FF5A36") String color) {
    }
}
