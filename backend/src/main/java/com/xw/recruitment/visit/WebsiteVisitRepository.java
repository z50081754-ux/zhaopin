package com.xw.recruitment.visit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebsiteVisitRepository extends JpaRepository<WebsiteVisitEntity, Long> {
    Optional<WebsiteVisitEntity> findByVisitId(String visitId);
    Optional<WebsiteVisitEntity> findFirstByIpAddressAndDeviceTimezone(String ipAddress, String deviceTimezone);
    Page<WebsiteVisitEntity> findAllByOrderByQualifiedAtDesc(Pageable pageable);
}
