package com.crustcrunch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CrustCrunchApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrustCrunchApplication.class, args);
    }
}
