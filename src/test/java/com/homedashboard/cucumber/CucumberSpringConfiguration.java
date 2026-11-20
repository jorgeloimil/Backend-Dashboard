package com.homedashboard.cucumber;

import com.homedashboard.config.ClockConfig;
import com.homedashboard.services.external.DescargadorCalendarios;
import com.homedashboard.services.external.FitcinaClient;
import com.homedashboard.services.external.OpenMeteoClient;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Ata Cucumber al contexto de Spring. Los servicios externos se simulan y la hora está fija. */
@CucumberContextConfiguration
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CucumberSpringConfiguration.RelojFijo.class)
public class CucumberSpringConfiguration {

    @MockitoBean
    private OpenMeteoClient openMeteoClient;

    @MockitoBean
    private FitcinaClient fitcinaClient;

    @MockitoBean
    private DescargadorCalendarios descargadorCalendarios;

    /** Viernes 20 de noviembre de 2026, 13:00 en Madrid. */
    static class RelojFijo {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
