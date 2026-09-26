package com.younes.order.config;

import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignTracingConfig {

    @Bean
    public RequestInterceptor tracePropagationInterceptor(Tracing tracing) {
        Propagation.Setter<RequestTemplate, String> setter = RequestTemplate::header;
        TraceContext.Injector<RequestTemplate> injector = tracing.propagation().injector(setter);
        return template -> {
            TraceContext context = tracing.currentTraceContext().get();
            if (context != null) {
                injector.inject(context, template);
            }
        };
    }

}