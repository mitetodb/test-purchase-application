package app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.List;

/**
 * Determines whether a request targets a real MVC controller mapping (not the static-resource fallback).
 * Used from the authentication entry point; failures fail closed (treat as mapped).
 */
@Component
public class MvcHandlerPresenceChecker {

    private final HandlerMappingIntrospector handlerMappingIntrospector;
    private final List<HandlerMapping> handlerMappings;

    public MvcHandlerPresenceChecker(
            HandlerMappingIntrospector handlerMappingIntrospector,
            List<HandlerMapping> handlerMappings) {
        this.handlerMappingIntrospector = handlerMappingIntrospector;
        this.handlerMappings = handlerMappings;
    }

    public boolean hasNonResourceHandler(HttpServletRequest request) {
        RequestPath previousPath = ServletRequestPathUtils.hasParsedRequestPath(request)
                ? ServletRequestPathUtils.getParsedRequestPath(request)
                : null;
        try {
            ServletRequestPathUtils.parseAndCache(request);
            if (matchesController(request, request.getMethod())) {
                return true;
            }
            if (!"GET".equalsIgnoreCase(request.getMethod()) && matchesController(request, "GET")) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return true;
        } finally {
            restoreParsedRequestPath(request, previousPath);
        }
    }

    private boolean matchesController(HttpServletRequest request, String httpMethod) throws Exception {
        if (hasControllerHandlerForRequest(request, httpMethod)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri.length() > 1 && uri.endsWith("/")) {
            HttpServletRequest withoutSlash = new RequestUriOverridingWrapper(
                    request, uri.substring(0, uri.length() - 1));
            RequestPath slashPath = ServletRequestPathUtils.hasParsedRequestPath(withoutSlash)
                    ? ServletRequestPathUtils.getParsedRequestPath(withoutSlash)
                    : null;
            try {
                ServletRequestPathUtils.clearParsedRequestPath(withoutSlash);
                ServletRequestPathUtils.parseAndCache(withoutSlash);
                return hasControllerHandlerForRequest(withoutSlash, httpMethod);
            } finally {
                restoreParsedRequestPath(withoutSlash, slashPath);
            }
        }
        return false;
    }

    private boolean hasControllerHandlerForRequest(HttpServletRequest request, String httpMethod) throws Exception {
        HttpServletRequest effectiveRequest = httpMethod.equalsIgnoreCase(request.getMethod())
                ? request
                : new HttpMethodOverridingRequestWrapper(request, httpMethod);

        for (HandlerMapping mapping : handlerMappings) {
            if (!(mapping instanceof RequestMappingHandlerMapping)) {
                continue;
            }
            HandlerExecutionChain chain = mapping.getHandler(effectiveRequest);
            if (isNonResourceControllerChain(chain)) {
                return true;
            }
        }

        var matchable = handlerMappingIntrospector.getMatchableHandlerMapping(effectiveRequest);
        if (matchable instanceof RequestMappingHandlerMapping requestMapping) {
            HandlerExecutionChain chain = requestMapping.getHandler(effectiveRequest);
            if (isNonResourceControllerChain(chain)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNonResourceControllerChain(HandlerExecutionChain chain) {
        return chain != null
                && chain.getHandler() != null
                && !(chain.getHandler() instanceof ResourceHttpRequestHandler);
    }

    private static void restoreParsedRequestPath(HttpServletRequest request, RequestPath previousPath) {
        if (previousPath != null) {
            ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
        } else {
            ServletRequestPathUtils.clearParsedRequestPath(request);
        }
    }

    private static final class HttpMethodOverridingRequestWrapper extends HttpServletRequestWrapper {

        private final String method;

        HttpMethodOverridingRequestWrapper(HttpServletRequest request, String method) {
            super(request);
            this.method = method;
        }

        @Override
        public String getMethod() {
            return method;
        }
    }

    private static final class RequestUriOverridingWrapper extends HttpServletRequestWrapper {

        private final String requestUri;

        RequestUriOverridingWrapper(HttpServletRequest request, String requestUri) {
            super(request);
            this.requestUri = requestUri;
        }

        @Override
        public String getRequestURI() {
            return requestUri;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(getScheme()).append("://").append(getServerName());
            if (getServerPort() != 80 && getServerPort() != 443) {
                url.append(':').append(getServerPort());
            }
            url.append(requestUri);
            return url;
        }
    }
}
