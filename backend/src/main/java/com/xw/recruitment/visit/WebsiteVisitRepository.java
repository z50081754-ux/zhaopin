package com.xw.recruitment.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.Instant;

public interface WebsiteVisitRepository extends JpaRepository<WebsiteVisitEntity, Long> {
    Optional<WebsiteVisitEntity> findByVisitId(String visitId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update WebsiteVisitEntity visit
           set visit.durationSeconds = case
                   when visit.durationSeconds < :durationSeconds then :durationSeconds
                   else visit.durationSeconds
               end,
               visit.lastPath = case
                   when visit.lastSeenAt <= :lastSeenAt then :lastPath
                   else visit.lastPath
               end,
               visit.lastSeenAt = case
                   when visit.lastSeenAt < :lastSeenAt then :lastSeenAt
                   else visit.lastSeenAt
               end,
               visit.queriedAddress = case
                   when visit.queriedAddress = true or :queriedAddress = true then true
                   else false
               end,
               visit.submittedResearch = case
                   when visit.submittedResearch = true or :submittedResearch = true then true
                   else false
               end
         where visit.systemCode = :systemCode
           and visit.visitId = :visitId
        """)
    int mergeVisitState(
        @Param("systemCode") String systemCode,
        @Param("visitId") String visitId,
        @Param("durationSeconds") int durationSeconds,
        @Param("lastPath") String lastPath,
        @Param("lastSeenAt") Instant lastSeenAt,
        @Param("queriedAddress") boolean queriedAddress,
        @Param("submittedResearch") boolean submittedResearch
    );

    default int mergeVisitState(
        String systemCode,
        String visitId,
        int durationSeconds,
        String lastPath,
        Instant lastSeenAt,
        boolean queriedAddress
    ) {
        return mergeVisitState(
            systemCode, visitId, durationSeconds, lastPath, lastSeenAt, queriedAddress, false);
    }

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
