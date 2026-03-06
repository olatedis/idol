package com.bit.idol.boardservice.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRestrictionInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // GET 방식은 조회이므로 허용 (글 읽기)
        if (HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }

        // 인증 필터를 거쳐 들어온 X-User-Id 추출 (일반 회원 기준)
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            return true; // 비회원 또는 ID가 없는 경우는 통과 (별도의 인증 인터셉터/시큐리티가 차단할 것임)
        }

        try {
            int userId = Integer.parseInt(userIdStr);
            String userCacheKey = "user:info:id::" + userId;
            String userJson = stringRedisTemplate.opsForValue().get(userCacheKey);

            if (userJson != null) {
                JsonNode rootNode = objectMapper.readTree(userJson);
                JsonNode statusNode = rootNode.has("status") ? rootNode.get("status")
                        : (rootNode.isArray() && rootNode.size() > 1 && rootNode.get(1).has("status")
                                ? rootNode.get(1).get("status")
                                : null);

                if (statusNode != null) {
                    String status = statusNode.asText();
                    if ("RESTRICTED".equals(status) || "SUSPENDED".equals(status) || "BANNED".equals(status)) {
                        log.warn("활동이 제한된 유저의 글쓰기 시도 차단: userId={}, status={}, URI={}", userId, status,
                                request.getRequestURI());
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "활동이 제한된 계정입니다.");
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("유저 제재 상태 검증 중 오류: {}", e.getMessage());
        }

        return true;
    }
}
