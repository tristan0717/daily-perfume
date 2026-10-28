package com.reco.recommendation.auth;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 @Value("${app.auth.naver.enabled:false}") private boolean enabled;
 @GetMapping("/me") public Map<String,Object> me(Authentication auth){boolean signedIn=auth!=null&&auth.isAuthenticated()&&!(auth instanceof AnonymousAuthenticationToken);return Map.of("authenticated",signedIn,"naverEnabled",enabled,"memberId",signedIn?auth.getName():"");}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("headerName",token.getHeaderName(),"token",token.getToken());}
}
