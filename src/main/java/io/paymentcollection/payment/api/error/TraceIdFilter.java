package io.paymentcollection.payment.api.error;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */
public class TraceIdFilter implements Filter {
  public static final String TRACE_ID_KEY = "traceId";
  public static final String[] INBOUND_KEYS = {"X-Trace-Id", "X-Request-Id", "X-Correlation-Id"};

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    String traceId = null;
    for (String h : INBOUND_KEYS) {
      String v = request.getHeader(h);
      if (v != null && !v.isBlank()) {
        traceId = v;
        break;
      }
    }
    if (traceId == null) traceId = UUID.randomUUID().toString().replace("-", "");

    MDC.put(TRACE_ID_KEY, traceId);
    request.setAttribute(TRACE_ID_KEY, traceId);
    response.setHeader("X-Trace-Id", traceId);
    try {
      chain.doFilter(req, res);
    } finally {
      MDC.remove(TRACE_ID_KEY);
    }
  }
}
