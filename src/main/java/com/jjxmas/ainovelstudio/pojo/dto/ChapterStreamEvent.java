package com.jjxmas.ainovelstudio.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterStreamEvent {

    private String type;

    private String content;

    private ChapterResponse chapter;

    private String message;

    public static ChapterStreamEvent queued(String message) {
        return ChapterStreamEvent.builder()
                .type("queued")
                .message(message)
                .build();
    }

    public static ChapterStreamEvent started(String message) {
        return ChapterStreamEvent.builder()
                .type("started")
                .message(message)
                .build();
    }

    public static ChapterStreamEvent postProcessing(String message) {
        return ChapterStreamEvent.builder()
                .type("post_processing")
                .message(message)
                .build();
    }

    public static ChapterStreamEvent chunk(String content) {
        return ChapterStreamEvent.builder()
                .type("chunk")
                .content(content)
                .build();
    }

    public static ChapterStreamEvent done(ChapterResponse chapter) {
        return ChapterStreamEvent.builder()
                .type("done")
                .chapter(chapter)
                .build();
    }

    public static ChapterStreamEvent error(String message) {
        return ChapterStreamEvent.builder()
                .type("error")
                .message(message)
                .build();
    }
}
