package com.jjxmas.ainovelstudio.pojo.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterContext {

    private ProjectProfile projectProfile;

    private ImmutableSetting immutableSetting;

    private StoryPlan storyPlan;

    private CurrentChapter currentChapter;

    private Continuity continuity;

    private CurrentState currentState;

    private ActiveThreads activeThreads;

    private MemoryStack memoryStack;

    private GenerationConstraints generationConstraints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectProfile {

        private Long projectId;

        private String title;

        private String genres;

        private String platformTarget;

        private Integer targetWordCountMin;

        private Integer targetWordCountMax;

        private Integer targetChapterWordCount;

        private String stylePreference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImmutableSetting {

        private String settingSummary;

        private String settingOverview;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoryPlan {

        private String globalOutline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentChapter {

        private Long chapterId;

        private Integer chapterNo;

        private String title;

        private String outline;

        private List<String> scenePlan;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Continuity {

        private Boolean hasPreviousChapter;

        private Integer previousChapterNo;

        private String previousChapterTitle;

        private String previousChapterSummary;

        private List<String> previousKeyEvents;

        private List<String> previousCharacterChanges;

        private List<String> previousLocationChanges;

        private List<String> previousForeshadowChanges;

        private String previousChapterTail;

        private String openingRequirement;

        private List<String> carryForwardRequirements;

        private String chapterTask;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentState {

        private List<String> relevantCharacters;

        private List<String> relevantOrganizations;

        private List<String> relevantLocations;

        private List<String> relevantItems;

        private List<String> relevantRelations;

        private List<String> relevantStateRecords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveThreads {

        private List<String> unresolvedThreads;

        private List<String> activeForeshadowThreads;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryStack {

        private String globalMemory;

        private List<String> highMemories;

        private List<String> middleMemories;

        private List<String> recentSummaries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConstraints {

        private Integer targetChapterWordCount;

        private String stylePreference;

        private String userAdvice;

        private List<String> dataQualityWarnings;
    }
}
