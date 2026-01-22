package com.bit.docker.boardservice.dto;

import com.bit.docker.boardservice.entity.BoardType;
import lombok.Data;

@Data
public class PostWriteRequest {
    private BoardType boardType;
    private long idolId; // boardType=IDOL일 경우
    private long groupId; // boardType=GROUP일 경우
    private String title;
    private String content;
}
