package com.homedashboard.services.external;

import com.homedashboard.exceptions.ServicioExternoException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;

/**
 * Descarga calendarios iCal. Las URL las pone quien instala el panel (variables de
 * entorno), no quien lo usa, así que no hace falta guarda contra SSRF.
 */
@Component
public class DescargadorCalendarios {

    /** Un calendario familiar ocupa unos KB; 5 MB es margen de sobra. */
    private static final int TAMANO_MAXIMO = 5 * 1024 * 1024;

    private final RestClient restClient;

    public DescargadorCalendarios(RestClient.Builder restClientBuilderExterno) {
        this.restClient = restClientBuilderExterno.build();
    }

    public String descargar(URI url) {
        // webcal:// es https:// con otro nombre (lo que da iCloud al compartir)
        URI real = "webcal".equalsIgnoreCase(url.getScheme())
                ? URI.create("https" + url.toString().substring("webcal".length()))
                : url;
        try {
            String ics = restClient.get()
                    .uri(real)
                    .accept(MediaType.parseMediaType("text/calendar"), MediaType.ALL)
                    .retrieve()
                    .body(String.class);
            if (ics == null || !ics.contains("BEGIN:VCALENDAR")) {
                throw new ServicioExternoException("La URL no devuelve un calendario iCal", null);
            }
            if (ics.length() > TAMANO_MAXIMO) {
                throw new ServicioExternoException("El calendario pesa demasiado", null);
            }
            return ics;
        } catch (RestClientException e) {
            throw new ServicioExternoException("No se pudo descargar el calendario", e);
        }
    }
}
