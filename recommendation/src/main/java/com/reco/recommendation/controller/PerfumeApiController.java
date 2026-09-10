package com.reco.recommendation.controller;

import com.reco.recommendation.dto.SearchRequest;
import com.reco.recommendation.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/perfumes")
public class PerfumeApiController {
    private final AiSearchService search;
    private final SearchLimiter limiter;
    public PerfumeApiController(AiSearchService search, SearchLimiter limiter) { this.search = search; this.limiter = limiter; }
    @PostMapping("/search")
    public AiSearchService.SearchResponse search(@Valid @RequestBody SearchRequest request) {
        limiter.acquire();
        return search.search(request);
    }
    @GetMapping("/search")
    public AiSearchService.SearchResponse legacy(@RequestParam String keyword) {
        if (keyword.isBlank() || keyword.length() > 1000) throw new IllegalArgumentException();
        return search(new SearchRequest(keyword, List.of()));
    }
}
