package org.example.eshop.commerce.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.eshop.commerce.port.CommerceActor;
import org.example.eshop.commerce.port.CurrentActorPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

@Component
public class ServletCurrentActor implements CurrentActorPort {
    private static final String COOKIE="FURNITURE_CART";
    private static final String TOKEN_ATTRIBUTE=ServletCurrentActor.class.getName()+".token";
    private static final String CLEARED_ATTRIBUTE=ServletCurrentActor.class.getName()+".cleared";
    private final SecureRandom random=new SecureRandom();
    @Override public CommerceActor currentActor() {
        var attributes=(ServletRequestAttributes)RequestContextHolder.currentRequestAttributes();
        var request=attributes.getRequest();
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        var principal=authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof CommercePrincipal p ? p : null;
        String token=null;
        if(!Boolean.TRUE.equals(request.getAttribute(CLEARED_ATTRIBUTE))) {
            token=(String)request.getAttribute(TOKEN_ATTRIBUTE);
            if(token==null && request.getCookies()!=null) {
                for(var cookie:request.getCookies()) if(COOKIE.equals(cookie.getName()) && valid(cookie.getValue())) { token=cookie.getValue(); break; }
            }
            if(token==null && principal==null) {
                byte[] bytes=new byte[32]; random.nextBytes(bytes);
                token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                writeCookie(request,attributes.getResponse(),token,Duration.ofDays(30));
            }
            if(token!=null) request.setAttribute(TOKEN_ATTRIBUTE,token);
        }
        return new CommerceActor(principal==null ? null : principal.id(),token==null ? null : hash(token),principal==null ? Set.of() : principal.roles());
    }
    @Override public void clearGuestIdentity() {
        var attributes=(ServletRequestAttributes)RequestContextHolder.currentRequestAttributes();
        attributes.getRequest().setAttribute(CLEARED_ATTRIBUTE,true);
        attributes.getRequest().removeAttribute(TOKEN_ATTRIBUTE);
        writeCookie(attributes.getRequest(),attributes.getResponse(),"",Duration.ZERO);
    }
    private static boolean valid(String token) {
        if(token==null || !token.matches("[A-Za-z0-9_-]{43}")) return false;
        try { byte[] bytes=Base64.getUrlDecoder().decode(token); return bytes.length==32 && Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token); }
        catch(IllegalArgumentException exception) { return false; }
    }
    private static byte[] hash(String token) {
        try { return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)); }
        catch(NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable",exception); }
    }
    private static void writeCookie(HttpServletRequest request,HttpServletResponse response,String token,Duration age) {
        if(response==null) throw new IllegalStateException("No response available for cart identity");
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(COOKIE,token).httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/").maxAge(age).build().toString());
    }
}
