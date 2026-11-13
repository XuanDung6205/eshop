package org.example.eshop.commerce.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class CurrentActorTest {
    @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); SecurityContextHolder.clearContext(); }
    @Test void guestsReceiveOpaqueHttpOnlyCookieAndOnlyItsHashBecomesIdentity() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        var adapter = new ServletCurrentActor();
        var actor = adapter.currentActor();
        String cookie = response.getHeader("Set-Cookie");
        assertThat(cookie).contains("FURNITURE_CART=", "HttpOnly", "SameSite=Lax", "Path=/");
        String token = cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));
        assertThat(Base64.getUrlDecoder().decode(token)).hasSize(32);
        assertThat(actor.userId()).isNull();
        assertThat(actor.guestHash()).containsExactly(MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(adapter.currentActor().guestHash()).containsExactly(actor.guestHash());
    }
    @Test void authenticatedActorUsesDatabaseIdAndClearingCookiePreventsReuse() {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie("FURNITURE_CART", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32])));
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        var principal = new CommercePrincipal(77L, "alice", "Alice", null, Set.of("CUSTOMER"));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        var adapter = new ServletCurrentActor();
        assertThat(adapter.currentActor().userId()).isEqualTo(77L);
        assertThat(adapter.currentActor().guestHash()).hasSize(32);
        adapter.clearGuestIdentity();
        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0", "HttpOnly");
        assertThat(adapter.currentActor().guestHash()).isNull();
    }
    @Test void malformedGuestCookiesAreReplacedRatherThanAccepted() {
        var request = new MockHttpServletRequest(); request.setCookies(new Cookie("FURNITURE_CART", "chosen-by-client"));
        var response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request,response));
        assertThat(new ServletCurrentActor().currentActor().guestHash()).hasSize(32);
        assertThat(response.getHeader("Set-Cookie")).doesNotContain("chosen-by-client");
    }
}
