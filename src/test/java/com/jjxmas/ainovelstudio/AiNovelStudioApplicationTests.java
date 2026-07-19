package com.jjxmas.ainovelstudio;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.initialization-fail-timeout=-1"
})
class AiNovelStudioApplicationTests {

    @Test
    void contextLoads() {
    }

}
