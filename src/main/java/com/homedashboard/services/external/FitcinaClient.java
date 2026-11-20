package com.homedashboard.services.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.homedashboard.config.PanelProperties;
import com.homedashboard.exceptions.CredencialesFitcinaException;
import com.homedashboard.exceptions.ServicioExternoException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.ZoneId;
import java.util.Map;

/**
 * Cliente de la API de Fitcina. Fitcina autentica con JWT de 15 minutos y no tiene
 * claves de servicio: aquí se entra con el email y la contraseña de Jorge (variables
 * de entorno), se guarda el token en memoria y, si Fitcina contesta 401, se entra de
 * nuevo y se repite la petición una sola vez. Solo hace lecturas.
 */
@Component
public class FitcinaClient {

    private final RestClient restClient;
    private final PanelProperties.Fitcina config;

    private volatile String token;

    public FitcinaClient(RestClient.Builder restClientBuilderExterno, PanelProperties properties) {
        this.config = properties.fitcina();
        this.restClient = restClientBuilderExterno.baseUrl(config.url().toString()).build();
    }

    /** Todas las rutinas del usuario (campo {@code data} de la respuesta). */
    public JsonNode rutinas() {
        return get("/api/routines", Map.of());
    }

    /** Sesiones más recientes primero. */
    public JsonNode sesiones(int limite) {
        return get("/api/sessions?limit={limite}", Map.of("limite", limite));
    }

    /** Racha y sesiones de la semana, calculadas por Fitcina en la zona dada. */
    public JsonNode estadisticas(ZoneId zona) {
        return get("/api/sessions/stats?zona={zona}", Map.of("zona", zona.getId()));
    }

    private JsonNode get(String ruta, Map<String, ?> variables) {
        try {
            return pedir(ruta, variables);
        } catch (HttpClientErrorException.Unauthorized caducado) {
            // Token caducado o anulado: entrar otra vez y repetir una vez
            token = null;
            try {
                return pedir(ruta, variables);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw new CredencialesFitcinaException("Fitcina rechaza la sesión del panel");
            } catch (RestClientException e) {
                throw caida(e);
            }
        } catch (RestClientException e) {
            throw caida(e);
        }
    }

    private JsonNode pedir(String ruta, Map<String, ?> variables) {
        JsonNode respuesta = restClient.get()
                .uri(ruta, variables)
                .headers(h -> h.setBearerAuth(tokenVigente()))
                .retrieve()
                .body(JsonNode.class);
        if (respuesta == null || !respuesta.has("data")) {
            throw new ServicioExternoException("Fitcina devolvió una respuesta vacía", null);
        }
        return respuesta.get("data");
    }

    private String tokenVigente() {
        String actual = token;
        return actual != null ? actual : entrar();
    }

    private synchronized String entrar() {
        if (token != null) {
            return token;
        }
        try {
            ResponseEntity<Void> respuesta = restClient.post()
                    .uri("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", config.email(), "password", config.password()))
                    .retrieve()
                    .toBodilessEntity();
            String nuevo = cookieToken(respuesta.getHeaders());
            if (nuevo == null) {
                throw new ServicioExternoException("Fitcina no devolvió la cookie de sesión", null);
            }
            token = nuevo;
            return nuevo;
        } catch (HttpClientErrorException e) {
            HttpStatusCode estado = e.getStatusCode();
            if (estado.value() == 401 || estado.value() == 400) {
                throw new CredencialesFitcinaException("Fitcina rechaza el email o la contraseña del panel");
            }
            throw new ServicioExternoException("Fitcina respondió " + estado.value() + " al entrar", e);
        } catch (RestClientException e) {
            throw caida(e);
        }
    }

    /** El JWT va en la cookie HttpOnly {@code token} del Set-Cookie del login. */
    static String cookieToken(HttpHeaders cabeceras) {
        for (String cookie : cabeceras.getOrDefault(HttpHeaders.SET_COOKIE, java.util.List.of())) {
            if (cookie.startsWith("token=")) {
                int fin = cookie.indexOf(';');
                String valor = fin < 0 ? cookie.substring(6) : cookie.substring(6, fin);
                return valor.isBlank() ? null : valor;
            }
        }
        return null;
    }

    private static ServicioExternoException caida(RestClientException e) {
        return new ServicioExternoException("No se pudo consultar Fitcina", e);
    }
}
