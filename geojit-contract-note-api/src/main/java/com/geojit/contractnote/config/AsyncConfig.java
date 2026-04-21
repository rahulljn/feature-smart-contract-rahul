package com.geojit.contractnote.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Production-grade async thread pool.
 * Replaces Spring's default SimpleAsyncTaskExecutor (which spins a new thread per task).
 *
 * Pool sizing rationale (adjust for your instance size):
 *   - corePoolSize=5    : always-on threads (audit logging, email callbacks)
 *   - maxPoolSize=20    : burst capacity for pipeline event storms
 *   - queueCapacity=500 : absorbs bursts without rejecting
 *   - keepAliveSeconds=60: idle threads above core are reclaimed after 60 s
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // MDC propagation — inherit caller's MDC (traceId, userId) in async threads
        executor.setTaskDecorator(runnable -> {
            // Wrap to propagate MDC context
            org.slf4j.MDC.getCopyOfContextMap();
            java.util.Map<String, String> callerCtx = org.slf4j.MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (callerCtx != null) org.slf4j.MDC.setContextMap(callerCtx);
                    runnable.run();
                } finally {
                    org.slf4j.MDC.clear();
                }
            };
        });
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
            log.error("Uncaught async exception in method={} params={}", method.getName(), params, ex);
    }
}
