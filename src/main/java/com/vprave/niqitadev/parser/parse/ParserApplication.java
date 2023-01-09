package com.vprave.niqitadev.parser.parse;

import com.vprave.niqitadev.parser.storage.FileSystemStorageService;
import com.vprave.niqitadev.parser.storage.StorageProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class ParserApplication {
    @Bean
    CommandLineRunner init(FileSystemStorageService storageService) {
        return (args) -> {
            storageService.deleteAll();
            storageService.init();
        };
    }
    public static void main(String[] args) {
        SpringApplication.run(ParserApplication.class, args);
    }
}
