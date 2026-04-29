package com.reco.recommendation.controller;

import com.reco.recommendation.dto.PerfumeResponseDto;
import com.reco.recommendation.service.PerfumeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfumes")
public class PerfumeController {

    private final PerfumeService perfumeService;

    public PerfumeController(PerfumeService perfumeService) {
        this.perfumeService = perfumeService;
    }

    @GetMapping
    public Page<PerfumeResponseDto> getPerfumes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return perfumeService.getPerfumes(PageRequest.of(page, size));
    }

    // 🔥 여기 딱 한 줄만 /search 에서 /db-search 로 변경했습니다! (Gemini API와 충돌 방지)
    @GetMapping("/db-search")
    public Page<PerfumeResponseDto> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return perfumeService.search(keyword, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public PerfumeResponseDto getPerfume(@PathVariable Long id) {
        return perfumeService.getPerfume(id);
    }

    /**
     * [추가] 특정 향수와 유사한 추천 향수 목록 조회
     * GET /api/perfumes/{id}/recommendations
     */
    @GetMapping("/{id}/recommendations")
    public ResponseEntity<List<PerfumeResponseDto>> getRecommendedPerfumes(@PathVariable Long id) {
        List<PerfumeResponseDto> recommendations = perfumeService.getRecommendedPerfumes(id);
        return ResponseEntity.ok(recommendations);
    }
}