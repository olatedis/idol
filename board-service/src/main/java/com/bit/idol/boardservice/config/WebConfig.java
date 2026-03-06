package com.bit.idol.boardservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.bit.idol.boardservice.interceptor.UserRestrictionInterceptor;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserRestrictionInterceptor userRestrictionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userRestrictionInterceptor)
                .addPathPatterns("/**") // 모든 경로에 대해 인터셉터 적용 (내부에서 GET은 통과시킴)
                .excludePathPatterns("/uploads/**"); // 정적 파일 제외
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 실행 경로 기준 uploads 폴더를 정적으로 서빙
        // 예) 파일 저장: ./uploads/abc.jpg
        // 접근 URL: /uploads/abc.jpg
        Path uploadDir = Paths.get("uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}