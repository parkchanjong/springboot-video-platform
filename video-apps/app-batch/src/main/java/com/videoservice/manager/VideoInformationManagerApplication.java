package com.videoservice.manager;

import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing
@SpringBootApplication
public class VideoInformationManagerApplication {

    private final JobLauncher jobLauncher;
    private final Job videoViewCountSyncJob;

    public VideoInformationManagerApplication(JobLauncher jobLauncher, Job videoViewCountSyncJob) {
        this.jobLauncher = jobLauncher;
        this.videoViewCountSyncJob = videoViewCountSyncJob;
    }

    public static void main(String[] args) {
        SpringApplication.run(VideoInformationManagerApplication.class, args);
    }

    @Bean
    public ApplicationRunner runner() {
        return args -> jobLauncher.run(
                videoViewCountSyncJob,
                new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters()
        );
    }
}