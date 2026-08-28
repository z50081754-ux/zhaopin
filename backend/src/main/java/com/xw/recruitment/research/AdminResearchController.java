package com.xw.recruitment.research;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/research")
public class AdminResearchController {
    private final ResearchSubmissionService service;

    public AdminResearchController(ResearchSubmissionService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResearchSubmissionService.ResearchSummary summary() {
        return service.researchSummary();
    }

    @GetMapping("/submissions")
    public ResearchSubmissionService.ResearchListResponse submissions(
            @RequestParam(defaultValue = "") String number,
            @RequestParam(defaultValue = "0") int rating,
            @RequestParam(defaultValue = "") String concern,
            @RequestParam(defaultValue = "") String source,
            @RequestParam(defaultValue = "") String scene,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.searchResearch(filters(number, rating, concern, source, scene, from, to),
            page, size);
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<ResearchSubmissionService.ResearchDetail> submission(
            @PathVariable long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.researchDetail(id));
    }

    @PostMapping("/submissions/lookup")
    public ResponseEntity<ResearchSubmissionService.ResearchDetail> lookup(
            @RequestBody WalletLookupRequest request) {
        String walletAddress = request == null ? null : request.walletAddress();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.lookupResearchWallet(walletAddress));
    }

    @GetMapping("/submissions/export")
    public ResponseEntity<String> export(
            @RequestParam(defaultValue = "") String number,
            @RequestParam(defaultValue = "0") int rating,
            @RequestParam(defaultValue = "") String concern,
            @RequestParam(defaultValue = "") String source,
            @RequestParam(defaultValue = "") String scene,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=web3-wallet-research.csv")
            .body(service.exportCsv(filters(number, rating, concern, source, scene, from, to)));
    }

    @DeleteMapping("/submissions/{id}")
    public Map<String, Boolean> delete(@PathVariable long id) {
        service.deleteResearch(id);
        return Map.of("ok", true);
    }

    @DeleteMapping("/submissions/batch")
    public Map<String, Object> deleteBatch(@RequestBody BatchDeleteRequest request) {
        List<Long> ids = request == null ? null : request.ids();
        return Map.of("ok", true, "deleted", service.deleteResearchBatch(ids));
    }

    @GetMapping("/campaign")
    public ResearchSubmissionService.AdminCampaign campaign() {
        return service.adminCampaign();
    }

    @PutMapping("/campaign")
    public ResearchSubmissionService.AdminCampaign updateCampaign(
            @RequestBody CampaignStatusRequest request) {
        return service.updateCampaign(request == null ? null : request.status());
    }

    private ResearchSubmissionService.ResearchFilters filters(String number, int rating,
            String concern, String source, String scene, String from, String to) {
        return new ResearchSubmissionService.ResearchFilters(number, rating, concern, source,
            scene, from, to);
    }

    public record WalletLookupRequest(String walletAddress) {}
    public record BatchDeleteRequest(List<Long> ids) {}
    public record CampaignStatusRequest(String status) {}
}
