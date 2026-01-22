package com.bit.idol.searchservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Document(indexName = "chat_index") // Elasticsearch 인덱스 이름
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDocument {

    @Id
    private String id; // MongoDB ID와 동일하게 사용

    @Field(type = FieldType.Long)
    private Long idolId; // 채팅방 ID (필터링용)

    @Field(type = FieldType.Integer)
    private int senderId;

    @Field(type = FieldType.Keyword)
    private String senderNickname;

    @Field(type = FieldType.Keyword)
    private String senderRole; // USER or IDOL

    @Field(type = FieldType.Text, analyzer = "nori") // 한글 형태소 분석기 사용
    private String content;

    @Field(type = FieldType.Keyword)
    private String type; // TALK, IMAGE 등

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime createdAt;
}
