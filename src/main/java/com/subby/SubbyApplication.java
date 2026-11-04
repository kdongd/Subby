package com.subby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SubbyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubbyApplication.class, args);
    }
}
