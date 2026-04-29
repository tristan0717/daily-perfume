package com.reco.recommendation.dto;

import java.util.List;

public record PerfumeResponse(
        Long id,
        String name,
        String brand,
        String category,
        String notes,
        String imageUrl,
        String description,
        List<NoteImageDto> noteImages
) {}