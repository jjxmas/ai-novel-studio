package com.jjxmas.ainovelstudio.pojo.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterFactExtraction {

    private List<EventFact> events;

    private List<StateChangeFact> stateChanges;

    private List<RelationChangeFact> relationChanges;

    private List<ForeshadowChangeFact> foreshadowChanges;

    private List<UnresolvedThreadFact> unresolvedThreads;

    private List<String> issues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventFact {

        private String eventType;

        private String name;

        private String description;

        private String locationText;

        private String eventTimeText;

        private Integer importance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateChangeFact {

        private String entityType;

        private String entityName;

        private String stateType;

        private Map<String, Object> oldValue;

        private Map<String, Object> newValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationChangeFact {

        private String sourceType;

        private String sourceName;

        private String targetType;

        private String targetName;

        private String relationType;

        private String changeType;

        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForeshadowChangeFact {

        private String threadKey;

        private String threadTitle;

        private String threadType;

        private String changeType;

        private String setupText;

        private String progressText;

        private String payoffHint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnresolvedThreadFact {

        private String threadKey;

        private String threadTitle;

        private String threadType;

        private String description;

        private String urgency;

        private Integer targetChapterNo;
    }
}
