package com.geojit.contractnote.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    // All LocalDateTime fields serialized as "2026-04-22T10:30:45Z" — Z suffix tells
    // the browser this is UTC, preventing the 5.5h IST offset bug.
    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(UTC_FORMATTER));

        return new ObjectMapper()
                .registerModule(module)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
