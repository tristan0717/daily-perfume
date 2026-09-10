package com.reco.recommendation.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record SearchRequest(
    @NotBlank @Size(max = 1000) String keyword,
    @Size(max = 30) List<@NotBlank @Size(max = 100) String> excludedNotes
) {
    public SearchRequest {
        excludedNotes = excludedNotes == null ? List.of() : List.copyOf(excludedNotes);
    }
}
