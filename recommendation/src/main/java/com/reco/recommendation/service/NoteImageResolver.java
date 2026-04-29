package com.reco.recommendation.service;

import com.reco.recommendation.repository.NoteImageRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class NoteImageResolver {

    private final NoteImageRepository noteImageRepository;
    private final NoteImageMapLoader noteImageMapLoader;

    public NoteImageResolver(NoteImageRepository noteImageRepository,
                             NoteImageMapLoader noteImageMapLoader) {
        this.noteImageRepository = noteImageRepository;
        this.noteImageMapLoader = noteImageMapLoader;
    }

    public String resolveUrl(String note) {
        if (note == null || note.isBlank()) return "/note-images/default.jpg";

        // 1) DB 우선
        var db = noteImageRepository.findByNoteIgnoreCase(note);
        if (db.isPresent() && db.get().getImageUrl() != null && !db.get().getImageUrl().isBlank()) {
            return db.get().getImageUrl();
        }

        // 2) JSON 매핑
        String fromJson = noteImageMapLoader.get(note);
        if (fromJson != null && !fromJson.isBlank()) return fromJson;

        // 3) slug fallback (기본은 webp로 고정)
        String slug = toSlug(note);
        return "/note-images/" + slug + ".webp";
    }

    private String toSlug(String note) {
        String normalized = Normalizer.normalize(note, Normalizer.Form.NFKD);
        normalized = normalized.replaceAll("[^a-zA-Z0-9\\s]", " ");
        normalized = normalized.trim().replaceAll("\\s+", "_").toLowerCase();
        return normalized;
    }
}