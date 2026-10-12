package com.crustcrunch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Serves admin-uploaded images from the uploads volume. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(props.uploads().dir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(props.uploads().publicPath() + "/**")
                .addResourceLocations(location)
                .setCachePeriod(86400);
    }
}
