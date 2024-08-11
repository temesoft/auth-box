package com.authbox.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
        // This is to avoid errors like "Serializing PageImpl instances as-is is not supported"
)
public class JacksonConfiguration {
}
