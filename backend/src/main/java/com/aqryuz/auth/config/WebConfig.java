package com.aqryuz.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root to the admin dashboard
        registry.addRedirectViewController("/", "/index.html");
        registry.addViewController("/admin").setViewName("forward:/index.html");
    }
}
