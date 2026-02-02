package com.bit.idol.chatservice.service;

import com.bit.idol.chatservice.dto.FileUploadResponseDto;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    public FileUploadResponseDto uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uuid = UUID.randomUUID().toString();
        String fileName = "chat/" + uuid + extension;
        String contentType = file.getContentType();
        String type = "FILE";
        String thumbnailUrl = null;

        try {
            // 1. 원본 파일 업로드
            try (InputStream inputStream = file.getInputStream()) {
                s3Template.upload(bucketName, fileName, inputStream);
            }
            String fileUrl = s3Template.download(bucketName, fileName).getURL().toString();

            // 2. 이미지인 경우 썸네일 생성 및 업로드
            if (contentType != null && contentType.startsWith("image")) {
                type = "IMAGE";
                try {
                    ByteArrayOutputStream thumbnailOut = new ByteArrayOutputStream();
                    Thumbnails.of(file.getInputStream())
                            .size(300, 300) // 썸네일 크기
                            .outputQuality(0.8) // 압축률
                            .toOutputStream(thumbnailOut);
                    
                    String thumbnailFileName = "chat/thumb/" + uuid + extension;
                    try (InputStream thumbnailIn = new ByteArrayInputStream(thumbnailOut.toByteArray())) {
                        s3Template.upload(bucketName, thumbnailFileName, thumbnailIn);
                    }
                    thumbnailUrl = s3Template.download(bucketName, thumbnailFileName).getURL().toString();
                    
                } catch (Exception e) {
                    log.warn("썸네일 생성 실패 (원본만 사용): {}", e.getMessage());
                    thumbnailUrl = fileUrl; // 실패 시 원본 URL 사용
                }
            } else if (contentType != null && contentType.startsWith("video")) {
                type = "VIDEO";
                // 동영상 썸네일은 ffmpeg 등이 필요하므로 여기서는 생략 (클라이언트가 생성해서 올리거나 별도 처리 필요)
            } else if (contentType != null && contentType.startsWith("audio")) {
                type = "VOICE";
            }

            log.info("S3 파일 업로드 성공: url={}, thumb={}", fileUrl, thumbnailUrl);
            
            return FileUploadResponseDto.builder()
                    .url(fileUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .type(type)
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();
            
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패", e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }
    }
    
    // 기존 메서드 호환성 유지 (필요 시)
    public void deleteFile(String fileUrl) {
        // URL에서 키 추출 로직 필요 (생략)
    }
}
