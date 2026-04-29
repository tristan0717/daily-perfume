package com.reco.recommendation.repository;

import com.reco.recommendation.domain.Perfume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PerfumeRepository extends JpaRepository<Perfume, Long> {

    /**
     * 기본 검색 쿼리 (기존 유지 및 가독성 개선)
     * 이름, 브랜드, 카테고리, 노트 정보를 모두 검색합니다.
     */
    @Query("""
        SELECT p
        FROM Perfume p
        WHERE
            LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(p.category, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(p.notes, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
        """)
    Page<Perfume> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * [추가] 추천 알고리즘을 위한 쿼리
     * 특정 노트(향)를 포함하고 있는 다른 향수들을 찾아냅니다.
     * COALESCE를 사용하여 null 값으로 인한 쿼리 오류를 방지합니다.
     */
    @Query("""
        SELECT p 
        FROM Perfume p 
        WHERE LOWER(COALESCE(p.notes, '')) LIKE LOWER(CONCAT('%', :note, '%'))
        AND p.id != :currentId
        """)
    List<Perfume> findRecommendedByNote(@Param("note") String note, @Param("currentId") Long currentId, Pageable pageable);

    /**
     * [추가] 브랜드별 더보기 기능
     * 같은 브랜드의 다른 제품들을 보여줄 때 사용합니다.
     */
    List<Perfume> findByBrandAndIdNot(String brand, Long id, Pageable pageable);
}