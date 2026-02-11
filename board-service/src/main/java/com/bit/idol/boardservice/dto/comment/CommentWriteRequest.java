package com.bit.idol.boardservice.dto.comment;

import lombok.Data;

@Data
public class CommentWriteRequest {
    private String content;
    private String nickname; // 추가됨 (컨트롤러에서 주입)
}
