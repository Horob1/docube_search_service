package com.horob1.docube_search_search.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class WebConfig {

    @Bean
    fun corsFilter(): CorsFilter {
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.addAllowedOriginPattern("*")
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")
        config.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}

