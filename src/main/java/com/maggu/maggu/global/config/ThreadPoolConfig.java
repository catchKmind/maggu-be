package com.maggu.maggu.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "tourApiExecutor")
    public Executor tourApiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // TourAPI 1회 실측 약 0.09s, 보수적으로 0.15s 적용 → 상세조회 1건 = 3회 × 0.15s = 0.45 스레드·초
        // 피크 가정: 동시 접속 100명(핀 클릭 10건/s) + /search/spots 1건/s(상세조회 10건 순차) → 약 9스레드
        // core 10: 피크 + 여유 / max 20: burst 흡수(core의 2배)
        // queue 20: TourAPI 지연 시(L=2s 가정) 최대 대기 약 4초로 제한 → 그 이상은 503으로 실패
        // TODO: 운영에서 스레드 몇 개까지 늘어나는지 확인 필요 (tour-api-executor- 스레드 확인)
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(60);

        // 종료 처리 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(12); // connect 2s + read 10s

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy()); // 거절 정책

        executor.setThreadNamePrefix("tour-api-executor-"); // 로그 추적용 prefix

        return executor;
    }
}
