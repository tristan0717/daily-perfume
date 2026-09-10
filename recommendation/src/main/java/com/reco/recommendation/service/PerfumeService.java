package com.reco.recommendation.service;

import com.reco.recommendation.domain.Perfume;
import com.reco.recommendation.dto.*;
import com.reco.recommendation.repository.PerfumeRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class PerfumeService {
    private final PerfumeRepository repository;
    private final NoteImageResolver images;
    private final NoteTranslations translations;
    public PerfumeService(PerfumeRepository repository, NoteImageResolver images, NoteTranslations translations) {
        this.repository = repository;
        this.images = images;
        this.translations = translations;
    }
    private static final tools.jackson.databind.json.JsonMapper JSON = tools.jackson.databind.json.JsonMapper.builder().build();
    private List<NoteImageDto> noteDtos(List<String> names) {
        return names.stream().map(n -> new NoteImageDto(n, translations.translate(n), images.resolveUrl(n))).toList();
    }
    public PerfumeResponseDto toDto(Perfume p) {
        var groups = noteGroups(p.getNotes());
        return new PerfumeResponseDto(p.getId(), p.getName(), p.getBrand(), p.getCategory(),
            String.join(", ", splitNotes(p.getNotes())), p.getDescription(), p.getImageUrl(),
            noteDtos(splitNotes(p.getNotes())), noteDtos(groups.getOrDefault("top", List.of())),
            noteDtos(groups.getOrDefault("middle", List.of())), noteDtos(groups.getOrDefault("base", List.of())));
    }
    private static List<String> strings(tools.jackson.databind.JsonNode array) {
        List<String> result = new ArrayList<>();
        if (array.isArray()) for (var item : array) {
            if (item.isTextual() && !item.asText().isBlank()) result.add(item.asText().trim());
        }
        return result.stream().distinct().toList();
    }
    public static Map<String,List<String>> noteGroups(String notes) {
        if (notes == null || !notes.trim().startsWith("{")) return Map.of();
        try {
            var node = JSON.readTree(notes);
            return Map.of("top", strings(node.path("top")), "middle", strings(node.path("middle")), "base", strings(node.path("base")));
        } catch (RuntimeException e) { return Map.of(); }
    }
    public static List<String> splitNotes(String notes) {
        if (notes == null || notes.isBlank() || "null".equalsIgnoreCase(notes.trim())) return List.of();
        if (notes.trim().startsWith("{")) {
            var groups = noteGroups(notes);
            return java.util.stream.Stream.of("top", "middle", "base").flatMap(k -> groups.getOrDefault(k, List.of()).stream()).distinct().toList();
        }
        if (notes.trim().startsWith("[")) {
            try { return strings(JSON.readTree(notes)); } catch (RuntimeException e) { return List.of(); }
        }
        return Arrays.stream(notes.split(",")).map(String::trim).filter(n -> !n.isBlank()).distinct().toList();
    }
    public Page<PerfumeResponseDto> getPerfumes(Pageable pageable) { return repository.findAll(pageable).map(this::toDto); }
    public Page<PerfumeResponseDto> search(String keyword, Pageable pageable) { return repository.searchByKeyword(keyword, pageable).map(this::toDto); }
    public PerfumeResponseDto getPerfume(Long id) { return toDto(find(id)); }
    private Perfume find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Perfume not found"));
    }
    public List<PerfumeResponseDto> getRecommendedPerfumes(Long id) {
        var target = find(id);
        var notes = splitNotes(target.getNotes());
        if (notes.isEmpty()) return List.of();
        return repository.findRecommendedByNote(notes.get(0), id, PageRequest.of(0, 5, Sort.by("id")))
            .stream().map(this::toDto).toList();
    }
}
