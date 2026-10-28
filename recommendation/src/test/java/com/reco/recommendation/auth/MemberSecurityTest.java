package com.reco.recommendation.auth;
import com.reco.recommendation.member.*;
import com.reco.recommendation.domain.Perfume;
import com.reco.recommendation.repository.PerfumeRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.auth.naver.enabled=true","app.auth.naver.client-id=test-client","app.auth.naver.client-secret=test-secret","app.auth.naver.redirect-uri=http://localhost:8080/login/oauth2/code/naver"})
class MemberSecurityTest {
 @Autowired WebApplicationContext context; @Autowired MemberRepository members; @Autowired SavedPerfumeRepository saved; @Autowired PerfumeRepository perfumes; MockMvc mvc;
 @BeforeEach void setup(){mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();}
 DefaultOAuth2User principal(Member m){return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),Map.of("memberId",m.getId()),"memberId");}
 @Test void guestAndCsrf()throws Exception{mvc.perform(get("/api/auth/me")).andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false));mvc.perform(get("/api/member/saved")).andExpect(status().isUnauthorized());mvc.perform(get("/api/auth/csrf")).andExpect(jsonPath("$.token").isNotEmpty());}
 @Test void savesAreIdempotentAndScoped()throws Exception{
  Member a=members.save(new Member(UUID.randomUUID().toString())),b=members.save(new Member(UUID.randomUUID().toString()));Perfume p=perfumes.save(Perfume.builder().name("Test fragrance").brand("Test").category("woody").notes("Cedar").build());String path="/api/member/saved/"+p.getId();
  mvc.perform(put(path).with(oauth2Login().oauth2User(principal(a)))).andExpect(status().isForbidden());
  for(int i=0;i<2;i++)mvc.perform(put(path).with(oauth2Login().oauth2User(principal(a))).with(csrf())).andExpect(status().isNoContent());assertEquals(1,saved.findByMemberIdOrderByIdDesc(a.getId()).size());
  mvc.perform(get("/api/member/saved").with(oauth2Login().oauth2User(principal(b)))).andExpect(content().json("[]"));mvc.perform(delete(path).with(oauth2Login().oauth2User(principal(b))).with(csrf())).andExpect(status().isNoContent());assertTrue(saved.existsByMemberIdAndPerfumeId(a.getId(),p.getId()));
  mvc.perform(put("/api/member/saved/9223372036854775807").with(oauth2Login().oauth2User(principal(a))).with(csrf())).andExpect(status().isNotFound());mvc.perform(post("/api/auth/logout").with(oauth2Login().oauth2User(principal(a)))).andExpect(status().isForbidden());mvc.perform(post("/api/auth/logout").with(oauth2Login().oauth2User(principal(a))).with(csrf())).andExpect(status().isNoContent());
 }
 @Test void stateIsIssuedAndUnsolicitedCallbackFails()throws Exception{mvc.perform(get("/oauth2/authorization/naver")).andExpect(status().is3xxRedirection()).andExpect(header().string("Location",org.hamcrest.Matchers.containsString("state=")));mvc.perform(get("/login/oauth2/code/naver?code=fake&state=untrusted")).andExpect(redirectedUrl("/?login=failed"));}
}
