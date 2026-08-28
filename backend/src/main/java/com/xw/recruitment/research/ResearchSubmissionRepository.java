package com.xw.recruitment.research;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResearchSubmissionRepository extends JpaRepository<ResearchSubmissionEntity, Long> {
    boolean existsByWalletHash(String walletHash);
    Optional<ResearchSubmissionEntity> findBySubmissionNumber(String submissionNumber);
    Optional<ResearchSubmissionEntity> findByWalletHash(String walletHash);

    @Query(value = """
        select distinct r from ResearchSubmissionEntity r left join r.scenes scene
        where (:number = '' or lower(r.submissionNumber) like lower(concat('%', :number, '%')))
          and (:rating = 0 or r.rating = :rating)
          and (:concern = '' or r.concern = :concern)
          and (:source = '' or r.source = :source)
          and (:scene = '' or scene = :scene)
          and r.createdAt >= :from and r.createdAt < :to
        """, countQuery = """
        select count(distinct r) from ResearchSubmissionEntity r left join r.scenes scene
        where (:number = '' or lower(r.submissionNumber) like lower(concat('%', :number, '%')))
          and (:rating = 0 or r.rating = :rating)
          and (:concern = '' or r.concern = :concern)
          and (:source = '' or r.source = :source)
          and (:scene = '' or scene = :scene)
          and r.createdAt >= :from and r.createdAt < :to
        """)
    Page<ResearchSubmissionEntity> search(
        @Param("number") String number,
        @Param("rating") int rating,
        @Param("concern") String concern,
        @Param("source") String source,
        @Param("scene") String scene,
        @Param("from") Instant from,
        @Param("to") Instant to,
        Pageable pageable);

    @Query("select r.rating, count(r) from ResearchSubmissionEntity r group by r.rating")
    List<Object[]> countByRating();

    @Query("select scene, count(r) from ResearchSubmissionEntity r join r.scenes scene group by scene")
    List<Object[]> countByScene();

    @Query("select r.concern, count(r) from ResearchSubmissionEntity r group by r.concern")
    List<Object[]> countByConcern();

    @Query("select r.source, count(r) from ResearchSubmissionEntity r group by r.source")
    List<Object[]> countBySource();

    @Query("select avg(r.rating) from ResearchSubmissionEntity r")
    Double averageRating();
}
