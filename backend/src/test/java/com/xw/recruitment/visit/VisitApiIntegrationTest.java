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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:visit-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "xw.storage.directory=./target/test-resumes"
})
@AutoConfigureMockMvc
class VisitApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired WebsiteVisitRepository visits;

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
                .header("CF-IPCountry", "TH")
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

    private String researchVisitJson(String visitId, int durationSeconds) {
        return """
            {"visitId":"%s","durationSeconds":%d,"entryPath":"/?campaign=private",
             "lastPath":"/","deviceType":"mobile","deviceModel":"iPhone",
             "operatingSystem":"iOS","browserName":"Mobile Safari",
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
