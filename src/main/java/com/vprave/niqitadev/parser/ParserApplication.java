package com.vprave.niqitadev.parser;

import com.vprave.niqitadev.parser.storage.StorageProperties;
import com.vprave.niqitadev.parser.storage.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class ParserApplication {
    @Bean
    CommandLineRunner init(StorageService storageService) {
        return (args) -> storageService.init();
    }

    public static void main(String[] args) {
        SpringApplication.run(ParserApplication.class, args);
    }
}
