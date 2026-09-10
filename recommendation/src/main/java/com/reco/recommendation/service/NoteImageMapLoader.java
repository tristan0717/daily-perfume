package com.reco.recommendation.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class NoteImageMapLoader {
    private final Properties map = new Properties();
    public NoteImageMapLoader() {
        var resource = new ClassPathResource("note-images.properties");
        if (!resource.exists()) return;
        try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            map.load(reader);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read note image manifest", e);
        }
    }
    public String get(String note) {
        return note == null ? null : map.getProperty(note.trim().toLowerCase(Locale.ROOT));
    }
}
