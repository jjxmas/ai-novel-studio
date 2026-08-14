package com.jjxmas.ainovelstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
/**
 * AI 小说工作室应用启动类。
 */
public class AiNovelStudioApplication {

    /**
     * 启动 Spring Boot 应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(AiNovelStudioApplication.class, args);
    }

}
