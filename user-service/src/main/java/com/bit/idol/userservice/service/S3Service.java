package com.bit.idol.userservice.service;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadFile(MultipartFile file) {
        // 1. 파일명 중복 방지 (UUID)
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        // 2. 메타데이터 설정
        ObjectMetadata metadata = ObjectMetadata.builder()
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        // 3. 업로드 및 URL 획득
        try (InputStream inputStream = file.getInputStream()) {
            S3Resource resource = s3Template.upload(bucket, fileName, inputStream, metadata);
            log.info("S3 파일 업로드 성공: {}", fileName);
            
            // URL 반환
            return resource.getURL().toString();
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패", e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            // URL에서 파일명 추출 (예: https://bucket.s3.../uuid.jpg -> uuid.jpg)
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            s3Template.deleteObject(bucket, fileName);
            log.info("S3 파일 삭제 성공: {}", fileName);
        } catch (Exception e) {
            log.warn("S3 파일 삭제 실패 (무시): {}", e.getMessage());
        }
    }
}
