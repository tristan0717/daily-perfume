package com.reco.recommendation.member;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="app_members", uniqueConstraints=@UniqueConstraint(columnNames="naver_subject"))
public class Member {
 @Id @Column(length=36) private String id;
 @Column(name="naver_subject",nullable=false,length=255) private String naverSubject;
 protected Member() {}
 public Member(String subject) { id=UUID.randomUUID().toString(); naverSubject=subject; }
 public String getId(){return id;}
}
