package com.bit.idol.boardservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/board/uploads")
public class UploadController {

    // 업로드 허용 타입(필요하면 늘리세요)
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    // 로컬 저장 디렉토리 (실행 경로 기준 ./uploads)
    private static final Path UPLOAD_DIR = Paths.get("uploads");

    @PostMapping(
            value = "/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadImage(@RequestPart("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "파일이 비어있습니다."));
        }

        // 타입 검사 (브라우저가 보내는 contentType 기반)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미지 파일만 업로드할 수 있습니다."));
        }

        // 파일 크기 제한(예: 5MB) - 필요시 조정
        long maxBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            return ResponseEntity.badRequest().body(Map.of("message", "파일이 너무 큽니다. (최대 5MB)"));
        }

        // 저장 폴더 생성
        if (Files.notExists(UPLOAD_DIR)) {
            Files.createDirectories(UPLOAD_DIR);
        }

        // 확장자 추출(원본 이름 기반)
        String originalName = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(originalName);
        if (ext == null || ext.isBlank()) {
            // content-type 기반 기본 확장자 추정(최소 안전장치)
            ext = contentType.equals("image/png") ? "png"
                    : contentType.equals("image/gif") ? "gif"
                    : contentType.equals("image/webp") ? "webp"
                    : "jpg";
        }

        // 저장 파일명(UUID)
        String savedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = UPLOAD_DIR.resolve(savedName);

        // 저장
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 프론트에서 접근할 URL (정적서빙 /uploads/** 로 연결)
        // 게이트웨이(baseURL http://localhost:8000) 기준으로 접근하게 "경로만" 내려줌
        String urlPath = "/uploads/" + savedName;

        return ResponseEntity.ok(Map.of(
                "url", urlPath
        ));
    }
}