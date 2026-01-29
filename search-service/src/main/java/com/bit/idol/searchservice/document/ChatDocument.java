package com.bit.idol.searchservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;

@Document(indexName = "chat")
@Setting(settingPath = "elastic/chat-setting.json")
@Mapping(mappingPath = "elastic/chat-mapping.json")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long idolId;

    @Field(type = FieldType.Integer)
    private int senderId;

    @Field(type = FieldType.Keyword)
    private String senderNickname;

    // Nori 분석기 적용 (자동 완성 필드는 일단 제거 - 설정 복잡도 낮춤)
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "nori_analyzer"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String content;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;
}
