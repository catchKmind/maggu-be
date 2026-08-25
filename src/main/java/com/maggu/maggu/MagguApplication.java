package com.maggu.maggu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MagguApplication {

    public static void main(String[] args) {
        SpringApplication.run(MagguApplication.class, args);
    }

}
