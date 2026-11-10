package com.ptu.backend.campus;

import com.ptu.backend.campus.CampusDtos.AdminPassRewardView;
import com.ptu.backend.campus.CampusDtos.AdminPassView;
import com.ptu.backend.campus.CampusDtos.SavePassRewardRequest;
import com.ptu.backend.campus.CampusDtos.SavePassSeasonRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/campus/pass")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPassController {
    private final AdminPassService service;

    public AdminPassController(AdminPassService service) {
        this.service = service;
    }

    @GetMapping
    public AdminPassView getConfiguration() {
        return service.getConfiguration();
    }

    @PutMapping("/season")
    public AdminPassView saveSeason(@Valid @RequestBody SaveSeasonRequest request) {
        return service.saveSeason(new SavePassSeasonRequest(request.seasonName(), request.startsAt(),
                request.endsAt(), request.premiumPrice()));
    }

    @PostMapping("/seasons")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPassView createSeason(@Valid @RequestBody CreateSeasonRequest request) {
        return service.createSeason(new SavePassSeasonRequest(request.seasonName(), request.startsAt(),
                request.endsAt(), request.premiumPrice()), request.copyRewards());
    }

    @PostMapping("/rewards")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPassRewardView createReward(@Valid @RequestBody SaveRewardRequest request) {
        return service.createReward(toRequest(request));
    }

    @PutMapping("/rewards/{rewardId}")
    public AdminPassRewardView updateReward(@PathVariable UUID rewardId,
                                             @Valid @RequestBody SaveRewardRequest request) {
        return service.updateReward(rewardId, toRequest(request));
    }

    @DeleteMapping("/rewards/{rewardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReward(@PathVariable UUID rewardId) {
        service.deleteReward(rewardId);
    }

    private SavePassRewardRequest toRequest(SaveRewardRequest request) {
        return new SavePassRewardRequest(request.track(), request.level(), request.title(), request.rewardKind(),
                request.cosmeticType(), request.rarity(), request.caseId());
    }

    public record SaveSeasonRequest(@NotBlank @Size(max = 120) String seasonName, @NotBlank String startsAt,
                                    @NotBlank String endsAt, @NotNull @Min(1) BigDecimal premiumPrice) {
    }

    public record CreateSeasonRequest(@NotBlank @Size(max = 120) String seasonName, @NotBlank String startsAt,
                                      @NotBlank String endsAt, @NotNull @Min(1) BigDecimal premiumPrice,
                                      boolean copyRewards) {
    }

    public record SaveRewardRequest(@NotBlank String track, @NotNull @Min(1) @Max(50) Integer level,
                                    @NotBlank @Size(max = 120) String title, @NotBlank String rewardKind,
                                    String cosmeticType, @NotBlank String rarity, String caseId) {
    }
}
