package com.homedashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HomeDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeDashboardApplication.class, args);
    }
}
