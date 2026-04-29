package com.reco.recommendation.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Perfume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 향수명
    @Column(nullable = false, length = 255)
    private String name;

    // 브랜드명
    @Column(nullable = false, length = 255)
    private String brand;

    // 대표 카테고리 (예: citrus / woody ...)
    @Column(nullable = false, length = 100)
    private String category;

    // 노트 (쉼표로 이어진 문자열)
    // ✅ @Lob 제거 → VARCHAR로 변경 (검색 가능)
    @Column(length = 4000)
    private String notes;

    // Year / tags 같은 설명 문자열
    // description은 검색 대상이 아니니 LOB 유지 가능
    @Column(length = 2000)
    private String description;

    // 이미지 경로: /images/xxx.jpg
    @Column(name = "image_url", length = 500)
    private String imageUrl;
}