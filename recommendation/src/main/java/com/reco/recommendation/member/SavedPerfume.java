package com.reco.recommendation.member;
import jakarta.persistence.*;
@Entity @Table(name="member_saved_perfumes",uniqueConstraints=@UniqueConstraint(columnNames={"member_id","perfume_id"}))
public class SavedPerfume {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="member_id",nullable=false,length=36) private String memberId;
 @Column(name="perfume_id",nullable=false) private Long perfumeId;
 protected SavedPerfume(){}
 public SavedPerfume(String memberId,Long perfumeId){this.memberId=memberId;this.perfumeId=perfumeId;}
 public Long getPerfumeId(){return perfumeId;}
}
