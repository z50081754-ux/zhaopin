package com.xw.recruitment.research;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchSubmissionRepository extends JpaRepository<ResearchSubmissionEntity, Long> {
    boolean existsByWalletHash(String walletHash);
    Optional<ResearchSubmissionEntity> findBySubmissionNumber(String submissionNumber);
    Optional<ResearchSubmissionEntity> findByWalletHash(String walletHash);
}
