package com.reco.recommendation.service;

import org.springframework.stereotype.Component;

@Component
public class NoteImageResolver {
    private final NoteImageMapLoader manifest;
    public NoteImageResolver(NoteImageMapLoader manifest) { this.manifest = manifest; }
    public String resolveUrl(String note) {
        String url = manifest.get(note);
        return url == null ? "/note-images/default.svg" : url;
    }
}
