package com.videoservice.manager.job;

import com.videoservice.manager.LoadVideoPort;
import com.videoservice.manager.SaveVideoPort;
import com.videoservice.manager.listener.JobCompletionNotificationListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VideoViewCountSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobCompletionNotificationListener jobCompletionNotificationListener;
    private final LoadVideoPort loadVideoPort;
    private final SaveVideoPort saveVideoPort;

    @Bean
    public Job viewCountSyncJob() {
        return new JobBuilder("viewCountSyncJob", jobRepository)
                .listener(jobCompletionNotificationListener)
                .start(syncViewCountStep())
                .build();
    }

    @Bean
    public Step syncViewCountStep() {
        return new StepBuilder("syncViewCountStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    loadVideoPort.getAllVideoIdsWithViewCount()
                            .forEach(saveVideoPort::syncViewCount);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

}
