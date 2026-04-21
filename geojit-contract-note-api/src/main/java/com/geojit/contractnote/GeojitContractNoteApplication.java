package com.geojit.contractnote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.geojit.contractnote.config.AppProperties;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class GeojitContractNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeojitContractNoteApplication.class, args);
    }
}
