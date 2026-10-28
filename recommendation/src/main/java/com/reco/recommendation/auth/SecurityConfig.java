package com.reco.recommendation.auth;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.http.HttpMethod;
import java.net.URI;
@Configuration
public class SecurityConfig {
 @Bean @ConditionalOnProperty(name="app.auth.naver.enabled",havingValue="true")
 ClientRegistrationRepository clientRegistrations(Environment env){
  String clientId=required(env,"app.auth.naver.client-id"),secret=required(env,"app.auth.naver.client-secret");
  String callback=required(env,"app.auth.naver.redirect-uri");URI uri=URI.create(callback);
  if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!"/login/oauth2/code/naver".equals(uri.getPath())||!("https".equals(uri.getScheme())||("http".equals(uri.getScheme())&&"localhost".equals(uri.getHost()))))throw new IllegalStateException("Use an HTTPS callback, or http://localhost for local development");
  return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("naver").clientId(clientId).clientSecret(secret)
   .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
   .redirectUri(callback).authorizationUri("https://nid.naver.com/oauth2.0/authorize").tokenUri("https://nid.naver.com/oauth2.0/token")
   .userInfoUri("https://openapi.naver.com/v1/nid/me").userNameAttributeName("response").clientName("Naver").build());
 }
 private static String required(Environment env,String key){String value=env.getProperty(key);if(value==null||value.isBlank())throw new IllegalStateException("Missing setting: "+key);return value;}
 @Bean SecurityFilterChain security(HttpSecurity http,ObjectProvider<ClientRegistrationRepository> registrations,NaverUserService users)throws Exception{
  http.authorizeHttpRequests(a->a.requestMatchers("/api/member/**").authenticated()
   .requestMatchers(HttpMethod.GET,"/","/index.html","/assets/**","/note-images/**","/favicon.ico","/favicon.svg","/icons.svg","/hero-perfume.jpeg","/api/perfumes/**","/api/auth/**","/oauth2/**","/login/oauth2/**","/error").permitAll().anyRequest().denyAll())
   .requestCache(c->c.requestCache(new NullRequestCache()))
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"login_required\"}");}))
   .logout(l->l.logoutUrl("/api/auth/logout").invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,a)->res.setStatus(204)));
  // Keep Spring Security CSRF and session-fixation protection enabled. All writes require a CSRF token.
  ClientRegistrationRepository repository=registrations.getIfAvailable();
  if(repository!=null)http.oauth2Login(o->o.clientRegistrationRepository(repository).userInfoEndpoint(u->u.userService(users)).defaultSuccessUrl("/",true).failureUrl("/?login=failed"));
  return http.build();
 }
}
