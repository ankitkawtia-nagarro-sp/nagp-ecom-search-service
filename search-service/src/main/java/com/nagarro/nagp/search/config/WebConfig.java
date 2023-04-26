package com.nagarro.nagp.search.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebMvc
@Data
@Slf4j
public class WebConfig implements WebMvcConfigurer {

	@Value("${allowed-origins}")
	public String corsAllowedOrigins;
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		// TODO Auto-generated method stub
		String []listOrigins = corsAllowedOrigins.split(",");
		log.info("ListOrigins" + Arrays.asList(listOrigins));
		registry.addMapping("/**")
		.allowedOrigins(listOrigins)
		.allowedOriginPatterns(listOrigins);
	}

}
