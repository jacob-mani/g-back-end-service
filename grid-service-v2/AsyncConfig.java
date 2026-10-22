package com.ford.grid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for async task executors and MVC async support.
 * Spring manages the lifecycle — threads are shut down gracefully on application stop/redeploy.
 */
@Configuration
public class AsyncConfig {

    /** Async request timeout for StreamingResponseBody (e.g. /stream-download) — 10 minutes. */
    private static final long STREAM_DOWNLOAD_TIMEOUT_MS = 600_000L;

    /**
     * Sets the default async request timeout for StreamingResponseBody endpoints.
     * Without this Spring uses a 30-second default, which is too short for large Excel exports
     * and causes InterruptedException inside MongoDB cursors.
     */
    @Bean
    public WebMvcConfigurer asyncTimeoutConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                configurer.setDefaultTimeout(STREAM_DOWNLOAD_TIMEOUT_MS);
            }
        };
    }

    /**
     * Executor for PSNL bulk report generation.
     * - corePoolSize: 3 threads always ready
     * - maxPoolSize: 5 threads max under load
     * - queueCapacity: 100 tasks queued (handles burst from concurrent users)
     * - CallerRunsPolicy: if queue is full, the calling thread runs the task (no task is lost)
     * - waitForTasksToCompleteOnShutdown: graceful shutdown
     */
    @Bean(name = "bulkReportExecutor")
    public Executor bulkReportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("bulk-report-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Executor for custom-view launch parallel queries.
     * - corePoolSize: 7 threads (one per collection query)
     * - maxPoolSize: 14 threads under load (concurrent users)
     * - queueCapacity: 50 tasks queued
     */
    @Bean(name = "launchQueryExecutor")
    public Executor launchQueryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(7);
        executor.setMaxPoolSize(14);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("launch-query-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Executor for PSNL program summary parallel queries.
     * - corePoolSize: 6 threads (one per collection)
     * - maxPoolSize: 12 threads under load (concurrent users)
     * - queueCapacity: 100 tasks queued
     */
    @Bean(name = "psnlQueryExecutor")
    public Executor psnlQueryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("psnl-query-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Executor for VFD file content processing (async, fire-and-forget).
     * - corePoolSize: 2 threads always ready
     * - maxPoolSize: 4 threads max under load
     * - queueCapacity: 20 tasks queued
     * - CallerRunsPolicy: if queue is full, the calling thread runs the task (no task is lost)
     */
    @Bean(name = "vfdProcessingExecutor")
    public Executor vfdProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("vfd-processing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
	
	 /**
     * Executor for VERN extract file processing (async background task).
     * - corePoolSize: 2 threads (VERN responses are infrequent)
     * - maxPoolSize: 4 threads under burst
     * - queueCapacity: 50 tasks queued
     * - CallerRunsPolicy: if queue is full, the calling thread runs the task (no task is lost)
     */
    @Bean(name = "vernExtractExecutor")
    public Executor vernExtractExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("vern-extract-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * Executor for parallel LLM document-generation calls inside DocumentTemplateServiceImpl.
     * - corePoolSize: 5 threads always ready (handles multiple brand/template combos concurrently)
     * - maxPoolSize: 10 threads max under peak load
     * - queueCapacity: 25 tasks queued
     * - CallerRunsPolicy: if queue is full, the calling thread runs the task (no task is lost)
     */
    @Bean(name = "documentTemplateExecutor")
    public Executor documentTemplateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("doc-template-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
