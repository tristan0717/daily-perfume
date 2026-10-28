package com.reco.recommendation.auth;
import com.reco.recommendation.member.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NaverUserServiceTest {
 @Test void usesSubjectNotEmail(){var repo=mock(MemberRepository.class);var member=new Member("subject-a");when(repo.findByNaverSubject("subject-a")).thenReturn(Optional.of(member));var user=new NaverUserService(repo).fromAttributes(Map.of("resultcode","00","response",Map.of("id","subject-a","email","ignored@example.test")));assertEquals(member.getId(),user.getName());assertEquals(Set.of("memberId"),user.getAttributes().keySet());verify(repo,never()).saveAndFlush(any());}
 @Test void rejectsInvalidProfile(){var service=new NaverUserService(mock(MemberRepository.class));for(var attributes:List.of(Map.<String,Object>of(),Map.<String,Object>of("resultcode","00","response",Map.of("id"," ")),Map.<String,Object>of("resultcode","99","response",Map.of("id","subject"))))assertThrows(OAuth2AuthenticationException.class,()->service.fromAttributes(attributes));}
}
