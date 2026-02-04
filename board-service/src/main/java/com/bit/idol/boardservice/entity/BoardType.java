package com.bit.idol.boardservice.entity;

/*
OFFICIAL: 아이돌/그룹이 공식 공지/게시물을 올리는 영역 (알림 O)
FAN: 팬들이 글을 쓰는 영역 (알림 X)
- IDOL_*  : idolId 필수, groupId는 null
- GROUP_* : groupId 필수, idolId는 null
*/

public enum BoardType {
    IDOL_OFFICIAL,
    IDOL_FAN,
    GROUP_OFFICIAL,
    GROUP_FAN,
    ADMIN_NOTICE
}
