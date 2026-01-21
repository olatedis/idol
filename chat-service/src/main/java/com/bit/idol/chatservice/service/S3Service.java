package com.bit.idol.chatservice.service;

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
    private String bucketName;

    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 파일명 중복 방지를 위해 UUID 사용
        String fileName = "chat/" + UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucketName, fileName, inputStream);
            
            // 업로드된 파일의 URL 반환
            String fileUrl = s3Template.download(bucketName, fileName).getURL().toString();
            log.info("S3 파일 업로드 성공: {}", fileUrl);
            return fileUrl;
            
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패", e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }
    }
}
