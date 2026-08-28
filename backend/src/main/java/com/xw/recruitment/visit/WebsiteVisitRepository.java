package com.xw.recruitment.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.Instant;

public interface WebsiteVisitRepository extends JpaRepository<WebsiteVisitEntity, Long> {
    Optional<WebsiteVisitEntity> findByVisitId(String visitId);
    Page<WebsiteVisitEntity> findAllBySystemCodeAndDurationSecondsGreaterThanEqualOrderByQualifiedAtDesc(
        String systemCode,
        int minDurationSeconds,
        Pageable pageable
    );
    Page<WebsiteVisitEntity> findAllBySystemCodeAndDurationSecondsGreaterThanEqualAndQualifiedAtGreaterThanEqualOrderByQualifiedAtDesc(
        String systemCode,
        int minDurationSeconds,
        Instant qualifiedFrom,
        Pageable pageable
    );
}
