package com.reco.recommendation.dto;

import com.reco.recommendation.domain.Perfume;
import java.util.List;

/**
 * 향수 정보를 반환하는 DTO (Data Transfer Object)
 * record 타입을 사용하여 불변성을 유지하고 보일러플레이트 코드를 줄였습니다.
 */
public record PerfumeResponseDto(
        Long id,
        String name,
        String brand,
        String category,
        String notes,
        String description,
        String imageUrl,
        List<NoteImageDto> noteImages
) {
    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     *
     * @param p          향수 엔티티
     * @param noteImages 노트 이미지 리스트 (NoteImageResolver를 통해 매핑된 결과)
     * @return 변환된 PerfumeResponseDto
     */
    public static PerfumeResponseDto from(Perfume p, List<NoteImageDto> noteImages) {
        return new PerfumeResponseDto(
                p.getId(),
                p.getName(),
                p.getBrand(),
                p.getCategory(),
                p.getNotes() != null ? p.getNotes() : "", // null 방어 로직 추가
                p.getDescription(),
                p.getImageUrl(),
                noteImages
        );
    }
}