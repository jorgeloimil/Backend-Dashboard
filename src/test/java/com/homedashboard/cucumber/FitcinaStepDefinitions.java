package com.homedashboard.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homedashboard.exceptions.ServicioExternoException;
import com.homedashboard.services.external.FitcinaClient;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import com.homedashboard.services.impl.FitcinaServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class FitcinaStepDefinitions {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FitcinaClient fitcina;
    @Autowired
    private FitcinaServiceImpl fitcinaService;
    @Autowired
    private EscenarioContext context;

    /** Cada escenario empieza sin copia ni fallos de los anteriores. */
    @Before
    public void limpio() {
        reset(fitcina);
        ReflectionTestUtils.setField(fitcinaService, "cache", null);
        ReflectionTestUtils.setField(fitcinaService, "ultimoFallo", null);
    }

    @Given("Fitcina tiene la rutina {string} los viernes")
    public void conRutina(String nombre) throws Exception {
        when(fitcina.rutinas()).thenReturn(objectMapper.readTree(
                "[{\"id\": 1, \"nombre\": \"" + nombre + "\", \"ejercicioCount\": 5, \"diasSemana\": \"Viernes\"}]"));
        when(fitcina.sesiones(anyInt())).thenReturn(objectMapper.readTree("[]"));
        when(fitcina.estadisticas(any())).thenReturn(objectMapper.readTree("{\"rachaDias\": 1, \"estaSemana\": 1}"));
    }

    @Given("Fitcina no responde")
    public void noResponde() {
        when(fitcina.rutinas()).thenThrow(new ServicioExternoException("No se pudo consultar Fitcina", null));
    }

    @When("consulta lo que toca hoy en Fitcina")
    public void consulta() throws Exception {
        context.setUltima(mockMvc.perform(get("/api/fitcina/hoy")).andReturn());
    }

    @Then("el estado de Fitcina es {string}")
    public void estado(String estado) throws Exception {
        assertThat(datos().path("estado").asText()).isEqualTo(estado);
    }

    @Then("hoy toca la rutina {string}")
    public void tocaRutina(String nombre) throws Exception {
        assertThat(datos().path("rutinas")).anyMatch(r -> r.path("nombre").asText().equals(nombre));
    }

    private JsonNode datos() throws Exception {
        return objectMapper.readTree(context.getUltima().getResponse().getContentAsString()).path("data");
    }
}
