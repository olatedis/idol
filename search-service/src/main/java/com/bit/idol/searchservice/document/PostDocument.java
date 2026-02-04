package com.bit.idol.searchservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Document(indexName = "board_posts")
@Setting(settingPath = "elastic/post-setting.json")
@Mapping(mappingPath = "elastic/post-mapping.json")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {

    @Id
    private String id; // postId를 String으로 저장

    @Field(type = FieldType.Long)
    private Long postId;

    @Field(type = FieldType.Keyword)
    private String boardType;

    @Field(type = FieldType.Long)
    private Long idolId;

    @Field(type = FieldType.Long)
    private Long groupId;

    // 기본 analyzer 사용(별도 플러그인 불필요)
    @Field(type = FieldType.Text)
    private String title;

    // content는 검색 매칭에만 사용(응답에는 내려주지 않음)
    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
}
