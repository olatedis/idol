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

    // nori 분석기 적용(검색 매칭용)
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String title;

    // content는 검색 매칭에만 사용(응답에는 내려주지 않음)
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String content;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime updatedAt;
}
