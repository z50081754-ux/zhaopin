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
        @Param("queriedAddress") boolean queriedAddress
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update WebsiteVisitEntity visit
           set visit.submittedResearch = true
         where visit.systemCode = 'research'
           and visit.visitId = :visitId
        """)
    int markResearchSubmitted(@Param("visitId") String visitId);

    @Query("""
        select visit from WebsiteVisitEntity visit
         where visit.systemCode = :systemCode
           and visit.durationSeconds between :minDurationSeconds and :maxDurationSeconds
           and visit.qualifiedAt >= :qualifiedFrom and visit.qualifiedAt < :qualifiedTo
           and (:submissionFilter = 'all'
             or (:submissionFilter = 'true' and visit.submittedResearch = true)
             or (:submissionFilter = 'false' and visit.submittedResearch = false))
         order by visit.qualifiedAt desc
        """)
    Page<WebsiteVisitEntity> search(
        @Param("systemCode") String systemCode,
        @Param("minDurationSeconds") int minDurationSeconds,
        @Param("maxDurationSeconds") int maxDurationSeconds,
        @Param("qualifiedFrom") Instant qualifiedFrom,
        @Param("qualifiedTo") Instant qualifiedTo,
        @Param("submissionFilter") String submissionFilter,
        Pageable pageable
    );

    @Query("""
        select count(visit), coalesce(avg(visit.durationSeconds), 0),
               coalesce(max(visit.durationSeconds), 0),
               coalesce(sum(case when visit.submittedResearch = true then 1L else 0L end), 0)
          from WebsiteVisitEntity visit
         where visit.systemCode = :systemCode
           and visit.qualifiedAt >= :qualifiedFrom and visit.qualifiedAt < :qualifiedTo
        """)
    Object[] summarize(
        @Param("systemCode") String systemCode,
        @Param("qualifiedFrom") Instant qualifiedFrom,
        @Param("qualifiedTo") Instant qualifiedTo
    );
}
