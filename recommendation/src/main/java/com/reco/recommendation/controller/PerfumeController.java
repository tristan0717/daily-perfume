package com.reco.recommendation.controller;

import com.reco.recommendation.dto.PerfumeResponseDto;
import com.reco.recommendation.service.PerfumeService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/perfumes")
public class PerfumeController {
    private final PerfumeService service;
    public PerfumeController(PerfumeService service) { this.service = service; }
    private Pageable page(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page >= 0, size 1..100 required");
        return PageRequest.of(page, size, Sort.by("id"));
    }
    @GetMapping
    public Page<PerfumeResponseDto> all(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) {
        return service.getPerfumes(page(page, size));
    }
    @GetMapping("/db-search")
    public Page<PerfumeResponseDto> search(@RequestParam String keyword, @RequestParam(defaultValue="0") int page,
                                          @RequestParam(defaultValue="50") int size) {
        if (keyword.isBlank() || keyword.length() > 1000) throw new IllegalArgumentException("Invalid keyword");
        return service.search(keyword, page(page, size));
    }
    @GetMapping("/{id}")
    public PerfumeResponseDto one(@PathVariable Long id) { return service.getPerfume(id); }
    @GetMapping("/{id}/recommendations")
    public List<PerfumeResponseDto> recommendations(@PathVariable Long id) { return service.getRecommendedPerfumes(id); }
}
