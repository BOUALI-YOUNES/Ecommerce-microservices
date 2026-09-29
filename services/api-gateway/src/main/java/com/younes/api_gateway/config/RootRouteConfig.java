package com.younes.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RootRouteConfig {

    @Bean
    public RouterFunction<ServerResponse> rootRoute() {
        return route(GET("/"), request -> ServerResponse.ok().bodyValue("API Gateway is up"));
    }

}