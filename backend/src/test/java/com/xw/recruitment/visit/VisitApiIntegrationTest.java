package com.xw.recruitment.visit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:visit-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes",
    "xw.research.trusted-proxies=127.0.0.1"
})
@AutoConfigureMockMvc
class VisitApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired WebsiteVisitRepository visits;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearVisits() {
        visits.deleteAll();
    }

    @Test
    void writesWalletCheckVisitsAndReturnsOnlyTheRequestedProtectedSubsystem() throws Exception {
        mockMvc.perform(post("/api/visits/walletcheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content(walletVisitJson("visit-wallet-api-0001", 15, true)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tracked").value(true));

        mockMvc.perform(get("/api/admin/visits")
                .param("systemCode", "walletcheck"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "walletcheck"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visits[0].system_code").value("walletcheck"))
            .andExpect(jsonPath("$.visits[0].queried_address").value(true));

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "other"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void existingPublicVisitEndpointWritesRecruitmentSystem() throws Exception {
        mockMvc.perform(post("/api/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(walletVisitJson("visit-recruitment-api-0001", 15, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tracked").value(true));

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visits[0].system_code").value("recruitment"))
            .andExpect(jsonPath("$.visits[0].queried_address").value(false));
    }

    @Test
    void walletCheckHeartbeatSetsQueriedAddressWithoutClearingIt() throws Exception {
        String visitId = "visit-wallet-heartbeat-0001";
        mockMvc.perform(post("/api/visits/walletcheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content(walletVisitJson(visitId, 15, false)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/visits/walletcheck/{visitId}/heartbeat", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationSeconds\":16,\"lastPath\":\"/wallet/0x123\",\"queriedAddress\":true}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/visits/walletcheck/{visitId}/heartbeat", visitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationSeconds\":17,\"lastPath\":\"/wallet/0x123\",\"queriedAddress\":false}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "walletcheck"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visits[0].queried_address").value(true));
    }

    @Test
    void acceptsProductionWalletOriginForTrackingPosts() throws Exception {
        mockMvc.perform(options("/api/visits/walletcheck")
                .header(HttpHeaders.ORIGIN, "https://wallet.xw-company.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://wallet.xw-company.com"));

        mockMvc.perform(post("/api/visits/walletcheck")
                .header(HttpHeaders.ORIGIN, "https://wallet.xw-company.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(walletVisitJson("visit-wallet-origin-0001", 15, false)))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://wallet.xw-company.com"))
            .andExpect(jsonPath("$.tracked").value(true));
    }

    @Test
    void removesAddressBearingWalletCheckPathsAtThePublicEndpoint() throws Exception {
        mockMvc.perform(post("/api/visits/walletcheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content(walletVisitJson(
                    "visit-wallet-privacy-0001",
                    15,
                    false,
                    "/analyze?address=0xSecret#fragment",
                    "/WALLET%2F0xSecret?tab=history"
                )))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "walletcheck"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visits[0].entry_path").value("/analyze"))
            .andExpect(jsonPath("$.visits[0].last_path").value("/wallet/:address"));
    }

    @Test
    void qualifiesResearchAtFiveSecondsAndKeepsOtherThresholdsAtFifteen() throws Exception {
        mockMvc.perform(post("/api/visits/research")
                .contentType(MediaType.APPLICATION_JSON)
                .content(researchVisitJson("visit-research-0001", 4)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/visits/research")
                .contentType(MediaType.APPLICATION_JSON)
                .content(researchVisitJson("visit-research-0001", 5)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tracked").value(true));

        mockMvc.perform(post("/api/visits/walletcheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content(walletVisitJson("visit-wallet-still-15", 5, false)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(walletVisitJson("visit-recruitment-still-15", 5, false)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void researchHeartbeatKeepsDurationMonotonicAndSanitizesAllPaths() throws Exception {
        mockMvc.perform(post("/api/visits/research")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                })
                .header("X-Trusted-Country", "TH")
                .contentType(MediaType.APPLICATION_JSON)
                .content(researchVisitJson("visit-research-0002", 5)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/visits/research/visit-research-0002/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationSeconds\":25,\"lastPath\":\"/?secret=removed\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/visits/research/visit-research-0002/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"durationSeconds\":10,\"lastPath\":\"/\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "research"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visits[0].duration_seconds").value(25))
            .andExpect(jsonPath("$.visits[0].submitted_research").value(false))
            .andExpect(jsonPath("$.visits[0].visitor_country").value("TH"))
            .andExpect(jsonPath("$.visits[0].entry_path").value("/"))
            .andExpect(jsonPath("$.visits[0].last_path").value("/"));
    }

    @Test
    void directResearchVisitsIgnoreForgedForwardingAndCountryHeaders() throws Exception {
        mockMvc.perform(post("/api/visits/research")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.77");
                    return request;
                })
                .header("CF-Connecting-IP", "203.0.113.10")
                .header("CF-IPCountry", "TH")
                .header("X-Country-Code", "JP")
                .header("X-Forwarded-For", "203.0.113.11")
                .header("X-Real-IP", "203.0.113.12")
                .contentType(MediaType.APPLICATION_JSON)
                .content(researchVisitJson("visit-research-forged-001", 5)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "research"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visits[0].ip_address").value("198.51.100.77"))
            .andExpect(jsonPath("$.visits[0].visitor_country").value("UNKNOWN"));
    }

    @Test
    void unallowlistedPrivatePeerCannotForwardResearchIdentity() throws Exception {
        mockMvc.perform(post("/api/visits/research")
                .with(request -> { request.setRemoteAddr("10.1.2.3"); return request; })
                .header("X-Real-IP", "203.0.113.13").header("X-Trusted-Country", "TH")
                .contentType(MediaType.APPLICATION_JSON).content(researchVisitJson("visit-research-private-01", 5)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/visits").with(user("admin").roles("ADMIN")).param("systemCode", "research"))
            .andExpect(jsonPath("$.visits[0].ip_address").value("10.1.2.3"))
            .andExpect(jsonPath("$.visits[0].visitor_country").value("UNKNOWN"));
    }

    @Test
    void filtersResearchVisitsAndSummarizesTheCurrentBangkokDay() throws Exception {
        String fiveSecondVisitId = "visit-research-filter-0001";
        String twentySecondVisitId = "visit-research-filter-0002";
        String fiftySecondVisitId = "visit-research-filter-0003";
        qualifyResearch(fiveSecondVisitId, 5);
        qualifyResearch(twentySecondVisitId, 20);
        qualifyResearch(fiftySecondVisitId, 50);
        visits.markResearchSubmitted(twentySecondVisitId);
        visits.markResearchSubmitted(fiftySecondVisitId);

        ZoneId bangkok = ZoneId.of("Asia/Bangkok");
        LocalDate today = LocalDate.now(bangkok);
        Instant todayStart = today.atStartOfDay(bangkok).toInstant();
        Instant tomorrowStart = today.plusDays(1).atStartOfDay(bangkok).toInstant();
        setQualifiedAt(fiveSecondVisitId, todayStart);
        qualifyResearch("visit-research-before-day", 7);
        setQualifiedAt("visit-research-before-day", todayStart.minusSeconds(1));
        qualifyResearch("visit-research-after-day-", 7);
        setQualifiedAt("visit-research-after-day-", tomorrowStart);

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "research")
                .param("minDurationSeconds", "10")
                .param("maxDurationSeconds", "30")
                .param("submittedResearch", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.visits[0].duration_seconds").value(20))
            .andExpect(jsonPath("$.visits[0].operating_system").value("iOS"))
            .andExpect(jsonPath("$.visits[0].operating_system_version").value("18.6.2"));

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "research")
                .param("from", today.toString())
                .param("to", today.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3));

        mockMvc.perform(get("/api/admin/visits")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "walletcheck"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(get("/api/admin/visits/summary")
                .with(user("admin").roles("ADMIN"))
                .param("systemCode", "research"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.todayEffective").value(3))
            .andExpect(jsonPath("$.averageDurationSeconds").value(25))
            .andExpect(jsonPath("$.maxDurationSeconds").value(50))
            .andExpect(jsonPath("$.submittedCount").value(2))
            .andExpect(jsonPath("$.conversionRate").value(66.67));
    }

    @Test
    void adminResearchReportingRepairsDurableSubmissionRaceStateWithoutCrossSystemLeakage() throws Exception {
        String researchId = "visit-research-durable-01";
        qualifyResearch(researchId, 20);
        persistSubmission(researchId);
        String walletId = "visit-wallet-durable--01";
        mockMvc.perform(post("/api/visits/walletcheck").contentType(MediaType.APPLICATION_JSON)
                .content(walletVisitJson(walletId, 15, false))).andExpect(status().isOk());
        persistSubmission(walletId);

        mockMvc.perform(get("/api/admin/visits").with(user("admin").roles("ADMIN")).param("systemCode", "research").param("submittedResearch", "true"))
            .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.visits[0].submitted_research").value(true));
        mockMvc.perform(get("/api/admin/visits").with(user("admin").roles("ADMIN")).param("systemCode", "research").param("submittedResearch", "false"))
            .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/api/admin/visits/summary").with(user("admin").roles("ADMIN")).param("systemCode", "research"))
            .andExpect(jsonPath("$.submittedCount").value(1)).andExpect(jsonPath("$.conversionRate").value(100.0));
        mockMvc.perform(get("/api/admin/visits").with(user("admin").roles("ADMIN")).param("systemCode", "walletcheck").param("submittedResearch", "false"))
            .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.visits[0].submitted_research").value(false));
    }

    private void persistSubmission(String visitId) {
        jdbc.update("insert into research_submissions (submission_number, source, rating, concern, feedback, wallet_network, wallet_ciphertext, wallet_nonce, wallet_hash, ip_hash, request_context_hash, terms_version, consented_at, created_at, visit_id) values (?, 'OPEN_CARD', 5, 'SECURITY', '', 'TRC20', 'cipher', 'nonce', ?, 'ip', 'context', '2026-08-01', current_timestamp, current_timestamp, ?)",
            "SP-" + visitId, "hash-" + visitId, visitId);
    }

    private void qualifyResearch(String visitId, int durationSeconds) throws Exception {
        mockMvc.perform(post("/api/visits/research")
                .contentType(MediaType.APPLICATION_JSON)
                .content(researchVisitJson(visitId, durationSeconds)))
            .andExpect(status().isOk());
    }

    private void setQualifiedAt(String visitId, Instant qualifiedAt) {
        WebsiteVisitEntity visit = visits.findByVisitId(visitId).orElseThrow();
        visit.setQualifiedAt(qualifiedAt);
        visits.saveAndFlush(visit);
    }

    private String researchVisitJson(String visitId, int durationSeconds) {
        return """
            {"visitId":"%s","durationSeconds":%d,"entryPath":"/?campaign=private",
             "lastPath":"/","deviceType":"mobile","deviceModel":"iPhone",
             "operatingSystem":"iOS","operatingSystemVersion":"18.6.2","browserName":"Mobile Safari",
             "screenResolution":"390x844","deviceLanguage":"en-US",
             "deviceTimezone":"Asia/Bangkok","userAgent":"test-agent",
             "detectedWallets":[],"queriedAddress":false}
            """.formatted(visitId, durationSeconds);
    }

    private String walletVisitJson(String visitId, int durationSeconds, boolean queriedAddress) {
        return walletVisitJson(visitId, durationSeconds, queriedAddress, "/wallet/0x123", "/wallet/0x123");
    }

    private String walletVisitJson(
        String visitId,
        int durationSeconds,
        boolean queriedAddress,
        String entryPath,
        String lastPath
    ) {
        return """
            {
              "visitId":"%s",
              "durationSeconds":%d,
              "entryPath":"%s",
              "lastPath":"%s",
              "deviceType":"desktop",
              "deviceModel":"Test PC",
              "operatingSystem":"Test OS",
              "operatingSystemVersion":"1.0",
              "browserName":"Test Browser",
              "screenResolution":"1920x1080",
              "deviceLanguage":"zh-CN",
              "deviceTimezone":"Asia/Shanghai",
              "userAgent":"test-agent",
              "detectedWallets":[],
              "queriedAddress":%b
            }
            """.formatted(visitId, durationSeconds, entryPath, lastPath, queriedAddress);
    }
}
