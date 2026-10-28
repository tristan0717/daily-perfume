package com.reco.recommendation.auth;
import com.reco.recommendation.member.*;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class NaverUserService implements OAuth2UserService<OAuth2UserRequest,OAuth2User> {
 private final MemberRepository members;
 private final DefaultOAuth2UserService delegate=new DefaultOAuth2UserService();
 public NaverUserService(MemberRepository members){this.members=members;}
 @Override @Transactional
 public OAuth2User loadUser(OAuth2UserRequest request){
  if(!"naver".equals(request.getClientRegistration().getRegistrationId()))throw invalid();
  return fromAttributes(delegate.loadUser(request).getAttributes());
 }
 OAuth2User fromAttributes(Map<String,Object> attributes){
  if(!"00".equals(attributes.get("resultcode")) || !(attributes.get("response") instanceof Map<?,?> response) || !(response.get("id") instanceof String subject) || subject.isBlank() || subject.length()>255)throw invalid();
  Member member=members.findByNaverSubject(subject).orElseGet(()->members.saveAndFlush(new Member(subject)));
  // Only the internal ID enters our session. Email, name and provider tokens are not exposed to the browser.
  return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),Map.of("memberId",member.getId()),"memberId");
 }
 private OAuth2AuthenticationException invalid(){return new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),"Invalid Naver profile");}
}
