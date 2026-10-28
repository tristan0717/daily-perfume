package com.reco.recommendation.member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SavedPerfumeRepository extends JpaRepository<SavedPerfume,Long> {
 List<SavedPerfume> findByMemberIdOrderByIdDesc(String memberId);
 boolean existsByMemberIdAndPerfumeId(String memberId,Long perfumeId);
 void deleteByMemberIdAndPerfumeId(String memberId,Long perfumeId);
}
