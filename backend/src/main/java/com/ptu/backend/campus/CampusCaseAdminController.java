package com.ptu.backend.campus;

import com.ptu.backend.campus.CampusDtos.AdminCaseRewardView;
import com.ptu.backend.campus.CampusDtos.AdminCaseView;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/cases")
@PreAuthorize("hasRole('ADMIN')")
public class CampusCaseAdminController {
    private final CampusCaseAdminService service;

    public CampusCaseAdminController(CampusCaseAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminCaseView> listCases() {
        return service.listCases();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCaseView createCase(@RequestParam String name, @RequestParam(required = false) String description,
                                    @RequestParam BigDecimal price,
                                    @RequestParam(required = false) MultipartFile image) {
        return service.createCase(name, description, price, image);
    }

    @PutMapping(path = "/{caseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCaseView updateCase(@PathVariable long caseId, @RequestParam String name,
                                    @RequestParam(required = false) String description, @RequestParam BigDecimal price,
                                    @RequestParam(required = false) MultipartFile image) {
        return service.updateCase(caseId, name, description, price, image);
    }

    @DeleteMapping("/{caseId}")
    public void deactivateCase(@PathVariable long caseId) {
        service.deactivateCase(caseId);
    }

    @PostMapping(path = "/{caseId}/rewards", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCaseRewardView createReward(@PathVariable long caseId, @RequestParam String name,
                                            @RequestParam String cosmeticType, @RequestParam String rarity,
                                            @RequestParam Integer weight,
                                            @RequestParam(required = false) MultipartFile image) {
        return service.createReward(caseId, name, cosmeticType, rarity, weight, image);
    }

    @PutMapping(path = "/{caseId}/rewards/{rewardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminCaseRewardView updateReward(@PathVariable long caseId, @PathVariable long rewardId,
                                            @RequestParam String name, @RequestParam String cosmeticType,
                                            @RequestParam String rarity, @RequestParam Integer weight,
                                            @RequestParam(required = false) MultipartFile image) {
        return service.updateReward(caseId, rewardId, name, cosmeticType, rarity, weight, image);
    }

    @DeleteMapping("/{caseId}/rewards/{rewardId}")
    public void deleteReward(@PathVariable long caseId, @PathVariable long rewardId) {
        service.deleteReward(caseId, rewardId);
    }
}
