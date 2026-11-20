package com.homedashboard.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homedashboard.exceptions.ServicioExternoException;
import com.homedashboard.services.external.DescargadorCalendarios;
import com.homedashboard.services.impl.AgendaServiceImpl;
import io.cucumber.java.en.Given;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class AgendaStepDefinitions {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DescargadorCalendarios descargador;
    @Autowired
    private AgendaServiceImpl agendaService;
    @Autowired
    private EscenarioContext context;

    /** Cada escenario empieza sin copias de calendarios de los anteriores. */
    @Before
    public void sinCache() {
        agendaService.vaciarCache();
    }

    @Given("el calendario {string} publica el iCal de prueba")
    public void publica(String nombre) throws IOException {
        try (var in = getClass().getResourceAsStream("/ical/familia.ics")) {
            when(descargador.descargar(any())).thenReturn(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Given("el calendario {string} no responde")
    public void noResponde(String nombre) {
        when(descargador.descargar(any())).thenThrow(new ServicioExternoException("sin conexión", null));
    }

    @When("consulta la agenda de {int} días")
    public void consulta(int dias) throws Exception {
        context.setUltima(mockMvc.perform(get("/api/agenda").param("dias", String.valueOf(dias))).andReturn());
    }

    @Then("la agenda incluye {string} del calendario {string}")
    public void incluye(String titulo, String calendario) throws Exception {
        assertThat(eventos()).anyMatch(e -> e.path("titulo").asText().equals(titulo)
                && e.path("calendario").asText().equals(calendario));
    }

    @Then("la agenda no incluye {string}")
    public void noIncluye(String titulo) throws Exception {
        assertThat(eventos()).noneMatch(e -> e.path("titulo").asText().equals(titulo));
    }

    @Then("el calendario {string} aparece como caído")
    public void caido(String nombre) throws Exception {
        JsonNode calendarios = cuerpo().path("data").path("calendarios");
        assertThat(calendarios).anyMatch(c -> c.path("nombre").asText().equals(nombre) && !c.path("ok").asBoolean());
    }

    private JsonNode cuerpo() throws Exception {
        return objectMapper.readTree(context.getUltima().getResponse().getContentAsString());
    }

    private Iterable<JsonNode> eventos() throws Exception {
        return cuerpo().path("data").path("eventos");
    }
}
