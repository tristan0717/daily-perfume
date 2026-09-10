package com.reco.recommendation.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class NoteTranslations {
    private final JdbcTemplate jdbc;
    private Map<String,String> cache = Map.of();
    private long expiresAt;
    public NoteTranslations(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public synchronized String translate(String note) {
        long now = System.nanoTime();
        if (now >= expiresAt) {
            try {
                Map<String,String> loaded = new HashMap<>();
                jdbc.query("SELECT note_eng, note_kor FROM note_mapping", row -> {
                    String eng = row.getString(1), kor = row.getString(2);
                    if (eng != null && kor != null) loaded.put(eng.trim().toLowerCase(Locale.ROOT), kor.trim());
                });
                cache = Map.copyOf(loaded);
            } catch (org.springframework.dao.DataAccessException ignored) {
                // Translations are optional; retain the last successful snapshot.
            }
            expiresAt = now + 300_000_000_000L;
        }
        return cache.getOrDefault(note.toLowerCase(Locale.ROOT), note);
    }
}
