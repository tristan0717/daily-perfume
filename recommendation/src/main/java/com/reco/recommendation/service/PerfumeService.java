package com.reco.recommendation.service;

import com.reco.recommendation.domain.Perfume;
import com.reco.recommendation.dto.NoteImageDto;
import com.reco.recommendation.dto.PerfumeResponseDto;
import com.reco.recommendation.repository.PerfumeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // 성능 최적화
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;
    private final NoteImageResolver noteImageResolver;

    public PerfumeService(PerfumeRepository perfumeRepository, NoteImageResolver noteImageResolver) {
        this.perfumeRepository = perfumeRepository;
        this.noteImageResolver = noteImageResolver;
    }

    public Page<PerfumeResponseDto> getPerfumes(Pageable pageable) {
        return perfumeRepository.findAll(pageable)
                .map(p -> PerfumeResponseDto.from(p, buildNoteImages(p.getNotes())));
    }

    public Page<PerfumeResponseDto> search(String keyword, Pageable pageable) {
        return perfumeRepository.searchByKeyword(keyword, pageable)
                .map(p -> PerfumeResponseDto.from(p, buildNoteImages(p.getNotes())));
    }

    public PerfumeResponseDto getPerfume(Long id) {
        Perfume p = perfumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfume not found: " + id));
        return PerfumeResponseDto.from(p, buildNoteImages(p.getNotes()));
    }

    /**
     * [추가된 핵심 로직] 유사한 향수 추천
     * 현재 보고 있는 향수의 첫 번째 노트를 기준으로 유사한 향수 5개를 추천합니다.
     */
    public List<PerfumeResponseDto> getRecommendedPerfumes(Long id) {
        Perfume target = perfumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perfume not found: " + id));

        if (target.getNotes() == null || target.getNotes().isBlank()) {
            return List.of();
        }

        // 첫 번째 노트를 대표 키워드로 추출
        String mainNote = target.getNotes().split(",")[0].trim();

        // 같은 노트를 가진 향수 검색 (최대 6개 가져와서 본인 제외)
        Page<Perfume> recommendations = perfumeRepository.searchByKeyword(mainNote, PageRequest.of(0, 6));

        return recommendations.getContent().stream()
                .filter(p -> !p.getId().equals(id)) // 현재 보고 있는 향수는 제외
                .limit(5)
                .map(p -> PerfumeResponseDto.from(p, buildNoteImages(p.getNotes())))
                .collect(Collectors.toList());
    }

    private List<NoteImageDto> buildNoteImages(String notes) {
        if (notes == null || notes.isBlank()) return List.of();

        return Arrays.stream(notes.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .map(note -> new NoteImageDto(note, noteImageResolver.resolveUrl(note)))
                .collect(Collectors.toList()); // 자바 버전에 따라 .toList() 혹은 .collect() 사용
    }
}