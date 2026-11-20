package com.homedashboard.cucumber;

import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Comprobaciones comunes sobre la última respuesta. */
public class RespuestaStepDefinitions {

    @Autowired
    private EscenarioContext context;

    @Then("la respuesta es correcta")
    public void respuestaCorrecta() {
        assertThat(context.getUltima().getResponse().getStatus()).isEqualTo(200);
    }

    @Then("la respuesta es un error de validación")
    public void respuestaValidacion() {
        assertThat(context.getUltima().getResponse().getStatus()).isEqualTo(400);
    }
}
