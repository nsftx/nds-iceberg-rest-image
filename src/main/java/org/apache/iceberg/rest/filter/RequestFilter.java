package org.apache.iceberg.rest.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RequestFilter implements Filter {

    private static Logger LOG = LoggerFactory.getLogger(RequestFilter.class);

    public static final String ENV_REST_CATALOG_ACCESS_TOKEN = "REST_CATALOG_ACCESS_TOKEN";

    private FilterConfig filterConfig = null;

    private String accessToken;

    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        accessToken = System.getenv(ENV_REST_CATALOG_ACCESS_TOKEN);
        //LOG.info("access token: {}", accessToken);
        if(accessToken == null) {
            throw new RuntimeException("Env. value of REST_CATALOG_ACCESS_TOKEN is null!");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // add cors.
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("Access-Control-Allow-Methods", "*");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");
        httpResponse.setHeader("Access-Control-Allow-Headers", "*");
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null) {
            String[] headerTokens = authHeader.split(" ");
            String token = headerTokens[1];

            boolean isValid = token.equals(accessToken);
            if(isValid) {
                HttpServletRequestWrapper requestWrapper = new HttpServletRequestWrapper(httpRequest);
                HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(httpResponse);
                chain.doFilter(requestWrapper, responseWrapper);
            } else {
                throw new ServletException("Token is not valid!");
            }
        } else {
            throw new ServletException("Authorization header not found!");
        }
    }

    public void destroy() {
        this.filterConfig = null;
    }
}
