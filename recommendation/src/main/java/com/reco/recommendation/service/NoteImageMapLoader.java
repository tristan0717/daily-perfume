package com.reco.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class NoteImageMapLoader {

    private final Map<String, String> map = new HashMap<>();

    public NoteImageMapLoader(ObjectMapper objectMapper) {
        try {
            ClassPathResource resource = new ClassPathResource("note-image-map.json");
            if (resource.exists()) {
                Map<String, String> loaded = objectMapper.readValue(
                        resource.getInputStream(),
                        new TypeReference<Map<String, String>>() {}
                );

                // key를 소문자로 정규화
                loaded.forEach((k, v) -> map.put(k.toLowerCase().trim(), v));
            }
        } catch (Exception e) {
            System.out.println("[NoteImageMapLoader] JSON load failed: " + e.getMessage());
        }
    }

    public String get(String note) {
        if (note == null) return null;
        return map.get(note.toLowerCase().trim());
    }
}