package app.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void prefersXRealIpOverRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.1");

        assertEquals("203.0.113.10", ClientIpResolver.resolve(request));
    }

    @Test
    void ignoresSpoofedXForwardedForWhenRealIpAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.99");

        assertEquals("127.0.0.1", ClientIpResolver.resolve(request));
    }
}
