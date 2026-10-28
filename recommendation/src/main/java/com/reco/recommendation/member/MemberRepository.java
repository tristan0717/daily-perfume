package com.reco.recommendation.member;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface MemberRepository extends JpaRepository<Member,String> {
 Optional<Member> findByNaverSubject(String subject);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from Member m where m.id = :id")
 Optional<Member> lockById(String id);
}
