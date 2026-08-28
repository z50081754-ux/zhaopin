package com.xw.recruitment.research;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.sql.Types;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "research_submissions")
public class ResearchSubmissionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "submission_number", nullable = false, unique = true, length = 40)
    private String submissionNumber;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "rating", nullable = false)
    @JdbcTypeCode(Types.SMALLINT)
    private int rating;

    @Column(name = "concern", nullable = false, length = 30)
    private String concern;

    @Column(name = "feedback", nullable = false, length = 500)
    private String feedback;

    @Column(name = "wallet_network", nullable = false, length = 20)
    private String walletNetwork;

    @Column(name = "wallet_ciphertext", nullable = false, length = 200)
    private String walletCiphertext;

    @Column(name = "wallet_nonce", nullable = false, length = 80)
    private String walletNonce;

    @Column(name = "wallet_hash", nullable = false, unique = true, length = 64)
    private String walletHash;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Column(name = "request_context_hash", nullable = false, length = 64)
    private String requestContextHash;

    @Column(name = "terms_version", nullable = false, length = 40)
    private String termsVersion;

    @Column(name = "consented_at", nullable = false)
    private Instant consentedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "research_submission_scenes", joinColumns = @JoinColumn(name = "submission_id", nullable = false))
    @Column(name = "scene", nullable = false, length = 30)
    private Set<String> scenes = new HashSet<>();

    protected ResearchSubmissionEntity() {}

    ResearchSubmissionEntity(String submissionNumber, String source, int rating, String concern,
            String feedback, String walletCiphertext, String walletNonce, String walletHash,
            String ipHash, String requestContextHash, String termsVersion, Instant consentedAt,
            Instant createdAt, Set<String> scenes) {
        this.submissionNumber = submissionNumber;
        this.source = source;
        this.rating = rating;
        this.concern = concern;
        this.feedback = feedback;
        this.walletNetwork = "TRC20";
        this.walletCiphertext = walletCiphertext;
        this.walletNonce = walletNonce;
        this.walletHash = walletHash;
        this.ipHash = ipHash;
        this.requestContextHash = requestContextHash;
        this.termsVersion = termsVersion;
        this.consentedAt = consentedAt;
        this.createdAt = createdAt;
        this.scenes = new HashSet<>(scenes);
    }

    public Long getId() { return id; }
    public String getSubmissionNumber() { return submissionNumber; }
    public String getSource() { return source; }
    public int getRating() { return rating; }
    public String getConcern() { return concern; }
    public String getFeedback() { return feedback; }
    public String getWalletNetwork() { return walletNetwork; }
    public String getWalletCiphertext() { return walletCiphertext; }
    public String getWalletNonce() { return walletNonce; }
    public String getWalletHash() { return walletHash; }
    public String getIpHash() { return ipHash; }
    public String getRequestContextHash() { return requestContextHash; }
    public String getTermsVersion() { return termsVersion; }
    public Instant getConsentedAt() { return consentedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<String> getScenes() { return scenes; }
}
