package com.xw.recruitment.job;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<JobEntity, Long> {
    List<JobEntity> findAllByStatusOrderByUpdatedAtDesc(String status);
    List<JobEntity> findAllByOrderByUpdatedAtDesc();
    Optional<JobEntity> findBySlugAndStatus(String slug, String status);
    boolean existsBySlug(String slug);
}
