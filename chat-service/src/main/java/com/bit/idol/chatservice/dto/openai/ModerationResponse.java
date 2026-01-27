package com.bit.idol.chatservice.dto.openai;

import lombok.Data;
import java.util.List;

@Data
public class ModerationResponse {
    private String id;
    private String model;
    private List<Result> results;

    @Data
    public static class Result {
        private boolean flagged; // 유해 여부 (true면 유해함)
        private Categories categories;
        private CategoryScores category_scores;
    }

    @Data
    public static class Categories {
        private boolean sexual;
        private boolean hate;
        private boolean harassment;
        private boolean self_harm;
        private boolean sexual_minors;
        private boolean hate_threatening;
        private boolean violence_graphic;
        private boolean self_harm_intent;
        private boolean self_harm_instructions;
        private boolean harassment_threatening;
        private boolean violence;
    }
    
    @Data
    public static class CategoryScores {
        // 점수 필드는 필요하면 추가 (double)
    }
}
