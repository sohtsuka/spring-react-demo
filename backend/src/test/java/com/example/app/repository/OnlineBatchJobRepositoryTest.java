package com.example.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.app.model.entity.OnlineBatchJob;
import com.example.app.model.enums.BatchJobStatus;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OnlineBatchJobRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    OnlineBatchJobRepository onlineBatchJobRepository;

    @Test
    void insert_and_findById() {
        OnlineBatchJob job = buildJob();
        onlineBatchJobRepository.insert(job);

        OnlineBatchJob found = onlineBatchJobRepository.findById(job.getId()).orElseThrow();
        assertThat(found.getJobName()).isEqualTo("CSV取込");
        assertThat(found.getStatus()).isEqualTo(BatchJobStatus.ACCEPTED);
        assertThat(found.getRecentEvents()).isEqualTo("[\"2026-04-25T10:00:00 ジョブを受け付けました\"]");
    }

    @Test
    void update_changesProgressFields() {
        OnlineBatchJob job = buildJob();
        onlineBatchJobRepository.insert(job);

        job.setStatus(BatchJobStatus.RUNNING);
        job.setProcessedItems(2);
        job.setSuccessCount(2);
        job.setProgressPercent(40);
        job.setCurrentItem("データ3/5");
        job.setRecentEvents("[\"2026-04-25T10:00:02 データ2/5 の処理が完了しました\"]");
        onlineBatchJobRepository.update(job);

        OnlineBatchJob found = onlineBatchJobRepository.findById(job.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(BatchJobStatus.RUNNING);
        assertThat(found.getProcessedItems()).isEqualTo(2);
        assertThat(found.getCurrentItem()).isEqualTo("データ3/5");
    }

    @Test
    void findAll_returnsNewestFirst() {
        OnlineBatchJob first = buildJob();
        first.setJobName("先行ジョブ");
        onlineBatchJobRepository.insert(first);

        OnlineBatchJob second = buildJob();
        second.setJobName("後続ジョブ");
        onlineBatchJobRepository.insert(second);

        List<OnlineBatchJob> jobs = onlineBatchJobRepository.findAll();
        assertThat(jobs).isNotEmpty();
        assertThat(jobs.get(0).getJobName()).isEqualTo("後続ジョブ");
    }

    private OnlineBatchJob buildJob() {
        OnlineBatchJob job = new OnlineBatchJob();
        job.setJobName("CSV取込");
        job.setStatus(BatchJobStatus.ACCEPTED);
        job.setTotalItems(5);
        job.setProcessedItems(0);
        job.setSuccessCount(0);
        job.setFailureCount(0);
        job.setProgressPercent(0);
        job.setFailureAtItem(null);
        job.setProcessingDelayMs(200);
        job.setCurrentItem(null);
        job.setRecentEvents("[\"2026-04-25T10:00:00 ジョブを受け付けました\"]");
        return job;
    }
}
