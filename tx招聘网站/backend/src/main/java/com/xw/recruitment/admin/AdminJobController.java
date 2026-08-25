package com.xw.recruitment.admin;

import com.xw.recruitment.job.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/jobs")
public class AdminJobController {
    private final JobService service;

    public AdminJobController(JobService service) {
        this.service = service;
    }

    @GetMapping
    public List<JobResponse> list() {
        return service.adminJobs();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse create(@Valid @RequestBody JobRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public JobResponse update(@PathVariable long id, @Valid @RequestBody JobRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable long id) {
        service.delete(id);
        return Map.of("ok", true);
    }
}
