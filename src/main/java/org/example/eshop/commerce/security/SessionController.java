package org.example.eshop.commerce.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class SessionController {
    public record UserView(Long id,String username,String fullName,List<String> roles) {}
    public record SessionView(boolean authenticated,UserView user) {}
    public record CsrfView(String token,String headerName) {}
    public static SessionView sessionView(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CommercePrincipal principal) {
            return new SessionView(true,new UserView(principal.id(),principal.getUsername(),principal.fullName(),principal.roles().stream().sorted().toList()));
        }
        return new SessionView(false,null);
    }
    @GetMapping("/api/session") public SessionView session() { return sessionView(SecurityContextHolder.getContext().getAuthentication()); }
    @GetMapping("/api/csrf") public CsrfView csrf(HttpServletRequest request) {
        var token=(CsrfToken)request.getAttribute(CsrfToken.class.getName());
        return new CsrfView(token.getToken(),token.getHeaderName());
    }
}
