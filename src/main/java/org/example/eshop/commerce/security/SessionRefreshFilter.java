package org.example.eshop.commerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.eshop.commerce.common.ApiError;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;

final class SessionRefreshFilter extends OncePerRequestFilter {
    private final UserDirectory users;
    private final ObjectMapper mapper;
    SessionRefreshFilter(UserDirectory users,ObjectMapper mapper) { this.users=users; this.mapper=mapper; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var context=SecurityContextHolder.getContext();
        var authentication=context.getAuthentication();
        if(authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CommercePrincipal principal) {
            var account=users.findById(principal.id()).filter(CommerceUserAccount::enabled);
            if(account.isEmpty()) {
                SecurityContextHolder.clearContext();
                var session=request.getSession(false); if(session!=null) session.invalidate();
                response.setStatus(401); response.setContentType("application/json");
                mapper.writeValue(response.getOutputStream(),new ApiError("AUTHENTICATION_REQUIRED","Your account is no longer available. Sign in again."));
                return;
            }
            var refreshed=account.get().principal(); refreshed.eraseCredentials();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(refreshed,null,refreshed.getAuthorities()));
            new HttpSessionSecurityContextRepository().saveContext(context,request,response);
        }
        chain.doFilter(request,response);
    }
}
