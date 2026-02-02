package com.bit.idol.chatservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponseDto {
    private String url;
    private String thumbnailUrl;
    private String type; // IMAGE, VIDEO, FILE
    private String contentType;
    private long size;
}
