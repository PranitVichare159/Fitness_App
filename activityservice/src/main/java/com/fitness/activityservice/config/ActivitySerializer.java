package com.fitness.activityservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fitness.activityservice.model.Activity;
import org.apache.kafka.common.serialization.Serializer;

public class ActivitySerializer implements Serializer<Activity> {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()); // handles LocalDateTime etc.

    @Override
    public byte[] serialize(String topic, Activity data) {
        if (data == null) return null;
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing Activity object", e);
        }
    }
}