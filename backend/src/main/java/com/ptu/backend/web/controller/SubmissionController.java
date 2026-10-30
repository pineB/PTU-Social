package com.ptu.backend.web.controller;

import com.ptu.backend.domain.Submission;
import com.ptu.backend.service.SemesterArchiveService;
import com.ptu.backend.service.SubmissionService;
import com.ptu.backend.web.dto.ReviewSubmissionRequest;
import com.ptu.backend.web.dto.SubmitTaskRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Submission lifecycle and review")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final SemesterArchiveService semesterArchiveService;

    @Operation(summary = "Submit task solution")
    @PostMapping("/tasks/{taskId}/submissions")
    public ResponseEntity<Submission> submitTask(
        @PathVariable Long taskId,
        @RequestParam Long studentId,
        @RequestBody SubmitTaskRequest request
    ) {
        return ResponseEntity.ok(submissionService.submitTask(taskId, studentId, request));
    }

    @Operation(summary = "Get all submissions for a task")
    @GetMapping("/tasks/{taskId}/submissions")
    public ResponseEntity<List<Submission>> getSubmissions(@PathVariable Long taskId) {
        return ResponseEntity.ok(submissionService.getSubmissionsForTask(taskId));
    }

    @Operation(summary = "Review submission and award academic rating")
    @PostMapping("/submissions/{submissionId}/review")
    public ResponseEntity<Submission> reviewSubmission(
        @PathVariable Long submissionId,
        @RequestBody ReviewSubmissionRequest request
    ) {
        return ResponseEntity.ok(submissionService.reviewSubmission(submissionId, request));
    }

    @Operation(summary = "Archive tasks and submissions for a group")
    @PostMapping("/groups/{groupId}/archive")
    public ResponseEntity<Void> archiveSemester(@PathVariable Long groupId) {
        semesterArchiveService.archiveSemester(groupId);
        return ResponseEntity.noContent().build();
    }
}
