package com.xw.recruitment.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {
    boolean existsByTelegramIgnoreCase(String telegram);

    @Query("""
        select a from ApplicationEntity a
        where (:stage = '' or a.stage = :stage)
          and (:query = '' or lower(a.resumeName) like lower(concat('%', :query, '%'))
            or lower(a.jobTitle) like lower(concat('%', :query, '%'))
            or lower(a.telegram) like lower(concat('%', :query, '%'))
            or lower(a.applicationNo) like lower(concat('%', :query, '%')))
          and (:referrer = '' or lower(a.referrer) like lower(concat('%', :referrer, '%')))
          and (:operatingSystem = '' or lower(a.operatingSystem) like lower(concat('%', :operatingSystem, '%')))
          and (:deviceModel = '' or lower(a.deviceModel) like lower(concat('%', :deviceModel, '%')))
          and a.createdAt >= :createdFrom
          and a.createdAt < :createdTo
        """)
    Page<ApplicationEntity> search(
        @Param("stage") String stage,
        @Param("query") String query,
        @Param("referrer") String referrer,
        @Param("operatingSystem") String operatingSystem,
        @Param("deviceModel") String deviceModel,
        @Param("createdFrom") Instant createdFrom,
        @Param("createdTo") Instant createdTo,
        Pageable pageable
    );
}
