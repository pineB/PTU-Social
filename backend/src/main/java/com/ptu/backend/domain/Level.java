package com.ptu.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "levels",
        indexes = @Index(name = "idx_level_battle_pass", columnList = "battle_pass_season_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Level {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_pass_season_id", nullable = false)
    private BattlePassSeason battlePassSeason;

    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;

    @Column(name = "required_xp", nullable = false)
    private Integer requiredXp;

    @Column(name = "reward_currency", nullable = false)
    private Integer rewardCurrency;

    @Column(name = "reward_case_id")
    private Long rewardCaseId;

    @Column(name = "reward_name", length = 120)
    private String rewardName;
}
