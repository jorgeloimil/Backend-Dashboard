package com.homedashboard.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.homedashboard.repository.ArticuloCompraRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class CompraStepDefinitions {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ArticuloCompraRepository repository;
    @Autowired
    private EscenarioContext context;

    @Given("la lista de la compra está vacía")
    public void listaVacia() {
        repository.deleteAll();
    }

    @When("apunta {string} con cantidad {string}")
    public void apunta(String nombre, String cantidad) throws Exception {
        ObjectNode cuerpo = objectMapper.createObjectNode().put("nombre", nombre);
        if (!cantidad.isEmpty()) {
            cuerpo.put("cantidad", cantidad);
        }
        context.setUltima(mockMvc.perform(post("/api/compra/articulos").contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo.toString())).andReturn());
    }

    @When("marca {string} como comprado")
    public void marcaComprado(String nombre) throws Exception {
        context.setUltima(mockMvc.perform(patch("/api/compra/articulos/" + articulo(nombre).path("id").asLong())
                .contentType(MediaType.APPLICATION_JSON).content("{\"comprado\":true}")).andReturn());
    }

    @When("quita los comprados")
    public void quitaComprados() throws Exception {
        context.setUltima(mockMvc.perform(delete("/api/compra/articulos/comprados")).andReturn());
    }

    @Then("la lista tiene {string} en el pasillo {string}")
    public void enElPasillo(String nombre, String pasillo) throws Exception {
        assertThat(articulo(nombre).path("categoria").asText()).isEqualTo(pasillo);
    }

    @Then("la lista tiene {int} artículo(s)")
    public void tieneArticulos(int cantidad) throws Exception {
        assertThat(lista()).hasSize(cantidad);
    }

    @Then("el artículo {string} tiene cantidad {string}")
    public void tieneCantidad(String nombre, String cantidad) throws Exception {
        assertThat(articulo(nombre).path("cantidad").asText()).isEqualTo(cantidad);
    }

    @Then("el artículo {string} está pendiente")
    public void estaPendiente(String nombre) throws Exception {
        assertThat(articulo(nombre).path("comprado").asBoolean()).isFalse();
    }

    @Then("la lista está en el orden {string}")
    public void enOrden(String esperado) throws Exception {
        List<String> nombres = new ArrayList<>();
        lista().forEach(a -> nombres.add(a.path("nombre").asText()));
        assertThat(String.join(", ", nombres)).isEqualTo(esperado);
    }

    private List<JsonNode> lista() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/compra/articulos")).andReturn();
        List<JsonNode> articulos = new ArrayList<>();
        objectMapper.readTree(resultado.getResponse().getContentAsString()).path("data").forEach(articulos::add);
        return articulos;
    }

    private JsonNode articulo(String nombre) throws Exception {
        return lista().stream().filter(a -> a.path("nombre").asText().equalsIgnoreCase(nombre)).findFirst()
                .orElseThrow(() -> new AssertionError("No está en la lista: " + nombre));
    }
}
