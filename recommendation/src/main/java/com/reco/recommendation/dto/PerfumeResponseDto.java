package com.reco.recommendation.dto;

import java.util.List;

public record PerfumeResponseDto(Long id, String name, String brand, String category,
    String notes, String description, String imageUrl, List<NoteImageDto> noteImages,
    List<NoteImageDto> topNotes, List<NoteImageDto> middleNotes, List<NoteImageDto> baseNotes) {
    public PerfumeResponseDto withReason(String reason) {
        return new PerfumeResponseDto(id, name, brand, category, notes, reason, imageUrl, noteImages, topNotes, middleNotes, baseNotes);
    }
}
