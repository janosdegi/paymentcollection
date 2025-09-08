package io.paymentcollection.payment.api.config;

import io.paymentcollection.payment.api.error.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * @author degijanos
 * @version 1.0
 * @since 2025. 09. 08.
 */

public class WebConfig {

    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        var bean = new FilterRegistrationBean<>(new TraceIdFilter());
        bean.setOrder(1);
        return bean;
    }

}
