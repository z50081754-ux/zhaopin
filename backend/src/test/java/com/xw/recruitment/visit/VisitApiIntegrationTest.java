package com.xw.recruitment.visit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

    private String walletVisitJson(String visitId, int durationSeconds, boolean queriedAddress) {
        return """
            {
              "visitId":"%s",
              "durationSeconds":%d,
              "entryPath":"/wallet/0x123",
              "lastPath":"/wallet/0x123",
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
            """.formatted(visitId, durationSeconds, queriedAddress);
    }
}
