package com.homedashboard.services.external;

import com.homedashboard.config.PanelProperties;
import com.homedashboard.exceptions.CredencialesFitcinaException;
import com.homedashboard.exceptions.ServicioExternoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FitcinaClientTest {

    private static final String BASE = "http://fitcina.test";
    private static final String RUTINAS = "{\"success\": true, \"data\": [{\"id\": 1}]}";

    private MockRestServiceServer servidor;
    private FitcinaClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        PanelProperties properties = new PanelProperties(ZoneId.of("Europe/Madrid"),
                new PanelProperties.Tiempo("Madrid", 40.4, -3.7, URI.create("http://open-meteo.test"), Duration.ofMinutes(10)),
                new PanelProperties.Agenda(Duration.ofMinutes(5), List.of()),
                new PanelProperties.Fitcina(URI.create(BASE), "jorge@casa.test", "secreto",
                        Duration.ofMinutes(2), Duration.ofMinutes(1)));
        cliente = new FitcinaClient(builder, properties);
    }

    private void login(String token) {
        servidor.expect(once(), requestTo(BASE + "/api/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"email\":\"jorge@casa.test\",\"password\":\"secreto\"}"))
                .andRespond(withSuccess("{\"success\": true}", MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.SET_COOKIE, "token=" + token + "; Path=/; HttpOnly; SameSite=Lax")
                        .header(HttpHeaders.SET_COOKIE, "refresh_token=otro; Path=/; HttpOnly"));
    }

    @Test
    void entraUnaVezYReutilizaElToken() {
        login("jwt1");
        servidor.expect(once(), requestTo(BASE + "/api/routines"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt1"))
                .andRespond(withSuccess(RUTINAS, MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo(BASE + "/api/sessions?limit=30"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt1"))
                .andRespond(withSuccess("{\"data\": []}", MediaType.APPLICATION_JSON));

        assertThat(cliente.rutinas().get(0).path("id").asInt()).isEqualTo(1);
        assertThat(cliente.sesiones(30)).isEmpty();
        servidor.verify();
    }

    @Test
    void siElTokenCaducaEntraDeNuevoYRepiteUnaVez() {
        login("jwt1");
        servidor.expect(once(), requestTo(BASE + "/api/routines"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        login("jwt2");
        servidor.expect(once(), requestTo(BASE + "/api/routines"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer jwt2"))
                .andRespond(withSuccess(RUTINAS, MediaType.APPLICATION_JSON));

        assertThat(cliente.rutinas()).hasSize(1);
        servidor.verify();
    }

    @Test
    void contrasenaRechazadaEsUnErrorDeCredenciales() {
        servidor.expect(once(), requestTo(BASE + "/api/auth/login"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(cliente::rutinas).isInstanceOf(CredencialesFitcinaException.class);
    }

    @Test
    void fitcinaCaidaEsUnServicioExterno() {
        login("jwt1");
        servidor.expect(once(), requestTo(BASE + "/api/routines"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(cliente::rutinas).isInstanceOf(ServicioExternoException.class);
    }

    @Test
    void sacaElTokenDelSetCookie() {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.add(HttpHeaders.SET_COOKIE, "refresh_token=r; Path=/");
        cabeceras.add(HttpHeaders.SET_COOKIE, "token=abc.def; Path=/; HttpOnly");

        assertThat(FitcinaClient.cookieToken(cabeceras)).isEqualTo("abc.def");
        assertThat(FitcinaClient.cookieToken(new HttpHeaders())).isNull();
    }
}
