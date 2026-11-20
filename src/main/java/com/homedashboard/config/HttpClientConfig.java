package com.homedashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Cliente HTTP para los servicios externos (Open-Meteo, calendarios iCal). */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient.Builder restClientBuilderExterno() {
        // Plazos cortos: si un servicio no responde, el panel sigue con lo que tenga guardado
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "home-dashboard/0.1");
    }
}
