package com.fitness.aiservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fitness.aiservice.model.Activity;
import org.apache.kafka.common.serialization.Deserializer;
public class ActivityDeserializer implements Deserializer<Activity>{
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()); // handles LocalDateTime

    @Override
    public Activity deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return objectMapper.readValue(data, Activity.class);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing Activity object", e);
        }
    }
}
