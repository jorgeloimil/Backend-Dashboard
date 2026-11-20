package com.homedashboard.cucumber;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

/** Última respuesta del escenario en curso, compartida entre las clases de steps. */
@Component
@ScenarioScope
public class EscenarioContext {

    private MvcResult ultima;

    public MvcResult getUltima() {
        return ultima;
    }

    public void setUltima(MvcResult ultima) {
        this.ultima = ultima;
    }
}
