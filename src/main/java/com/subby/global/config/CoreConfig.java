package com.subby.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class CoreConfig {

    /**
     * 날짜 계산은 모두 한국 시간 기준이다. 서버(Render)는 UTC로 돌기 때문에 명시하지 않으면 오전 9시 전에 하루가 어긋난다.
     */
    @Bean
    public Clock clock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.zone()));
    }
}
