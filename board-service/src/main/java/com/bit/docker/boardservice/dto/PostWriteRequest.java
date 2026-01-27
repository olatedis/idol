package com.bit.docker.boardservice.dto;

import com.bit.docker.boardservice.entity.BoardType;
import lombok.Data;

@Data
public class PostWriteRequest {

    private BoardType boardType;

    // IDOL_* 게시판에서 사용 (GROUP_* 에서는 null)
    private Long idolId;

    // GROUP_* 게시판에서 사용 (IDOL_* 에서는 null)
    private Long groupId;

    private String title;
    private String content;
}
