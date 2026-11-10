package com.ptu.backend.campus;

import com.ptu.backend.campus.CampusDtos.AdminPassRewardView;
import com.ptu.backend.campus.CampusDtos.AdminPassView;
import com.ptu.backend.campus.CampusDtos.PassSeasonSummary;
import com.ptu.backend.campus.CampusDtos.AdminCaseOption;
import com.ptu.backend.campus.CampusDtos.SavePassRewardRequest;
import com.ptu.backend.campus.CampusDtos.SavePassSeasonRequest;
import com.ptu.backend.common.exception.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Service
public class AdminPassService {
    private static final List<String> TRACKS = List.of("free", "premium");
    private static final List<String> KINDS = List.of("case", "cosmetic");
    private static final List<String> COSMETIC_TYPES =
            List.of("avatar", "avatar-frame", "profile-background", "badge");
    private static final List<String> RARITIES = List.of("Обычный", "Редкий", "Эпический");
    private final JdbcTemplate jdbc;

    public AdminPassService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public AdminPassView getConfiguration() {
        SeasonSettings season = activeSeason();
        List<PassSeasonSummary> seasons = jdbc.query("""
                select id, season_name, starts_at, ends_at, premium_pass_price, is_active
                from campus_pass_seasons order by created_at desc, id desc
                """, (rs, row) -> new PassSeasonSummary(rs.getObject("id", UUID.class).toString(),
                rs.getString("season_name"), rs.getTimestamp("starts_at").toInstant().toString(),
                rs.getTimestamp("ends_at").toInstant().toString(), rs.getBigDecimal("premium_pass_price"),
                rs.getBoolean("is_active")));
        List<AdminPassRewardView> rewards = jdbc.query("""
                select id, track, level, title, reward_kind, cosmetic_type, rarity, case_id
                from campus_pass_reward_catalog where season_id = ?
                order by track, level
                """, (rs, row) -> new AdminPassRewardView(rs.getObject("id", UUID.class).toString(),
                rs.getString("track"), rs.getInt("level"), rs.getString("title"),
                rs.getString("reward_kind"), rs.getString("cosmetic_type"), rs.getString("rarity"),
                nullableLong(rs.getObject("case_id", Long.class))), season.id);
        List<AdminCaseOption> cases = jdbc.query("""
                select id, name from cases where active = true order by name, id
                """, (rs, row) -> new AdminCaseOption(Long.toString(rs.getLong("id")), rs.getString("name")));
        return new AdminPassView(season.id.toString(), season.name, season.startsAt, season.endsAt,
                season.premiumPrice, seasons, rewards, cases);
    }

    @Transactional
    public AdminPassView saveSeason(SavePassSeasonRequest request) {
        validateSeason(request);
        Instant startsAt = parseInstant(request.startsAt());
        Instant endsAt = parseInstant(request.endsAt());
        jdbc.update("""
                update campus_pass_seasons
                set season_name = ?, starts_at = ?, ends_at = ?, premium_pass_price = ?
                where is_active
                """, request.seasonName().trim(), Timestamp.from(startsAt), Timestamp.from(endsAt),
                request.premiumPrice());
        return getConfiguration();
    }

    @Transactional
    public AdminPassView createSeason(SavePassSeasonRequest request, boolean copyRewards) {
        validateSeason(request);
        UUID previousSeasonId = activeSeason().id;
        jdbc.update("update campus_pass_seasons set is_active = false where is_active");
        UUID seasonId = jdbc.queryForObject("""
                insert into campus_pass_seasons(season_name, starts_at, ends_at, premium_pass_price, is_active)
                values (?, ?, ?, ?, true) returning id
                """, UUID.class, request.seasonName().trim(), Timestamp.from(parseInstant(request.startsAt())),
                Timestamp.from(parseInstant(request.endsAt())), request.premiumPrice());
        if (copyRewards) {
            jdbc.update("""
                    insert into campus_pass_reward_catalog(
                        season_id, track, level, title, reward_kind, cosmetic_type, rarity, case_id
                    )
                    select ?, r.track, r.level, r.title, r.reward_kind, r.cosmetic_type, r.rarity,
                        case when r.reward_kind = 'case' and exists (
                            select 1 from cases c where c.id = r.case_id and c.active = true
                        ) then r.case_id else (
                            select c.id from cases c where c.active = true order by c.id limit 1
                        ) end
                    from campus_pass_reward_catalog r where r.season_id = ?
                    """, seasonId, previousSeasonId);
        }
        return getConfiguration();
    }

    @Transactional
    public AdminPassRewardView createReward(SavePassRewardRequest request) {
        validateReward(request);
        UUID seasonId = activeSeason().id;
        UUID id = jdbc.queryForObject("""
                insert into campus_pass_reward_catalog(
                    season_id, track, level, title, reward_kind, cosmetic_type, rarity, case_id
                ) values (?, ?, ?, ?, ?, ?, ?, ?) returning id
                """, UUID.class, seasonId, request.track(), request.level(), request.title().trim(), request.rewardKind(),
                request.cosmeticType(), request.rarity(), caseId(request));
        return reward(id);
    }

    @Transactional
    public AdminPassRewardView updateReward(UUID rewardId, SavePassRewardRequest request) {
        validateReward(request);
        if (jdbc.update("""
                update campus_pass_reward_catalog
                set track = ?, level = ?, title = ?, reward_kind = ?, cosmetic_type = ?, rarity = ?, case_id = ?
                where id = ? and season_id = (select id from campus_pass_seasons where is_active)
                """, request.track(), request.level(), request.title().trim(), request.rewardKind(),
                request.cosmeticType(), request.rarity(), caseId(request), rewardId) == 0) {
            throw ApiException.notFound("Награда активного сезона не найдена.");
        }
        return reward(rewardId);
    }

    @Transactional
    public void deleteReward(UUID rewardId) {
        if (jdbc.queryForObject("select count(*) from campus_pass_reward_claims where reward_id = ?",
                Integer.class, rewardId) > 0) {
            throw ApiException.conflict("Эта награда уже получена студентами и не может быть удалена.");
        }
        if (jdbc.update("""
                delete from campus_pass_reward_catalog
                where id = ? and season_id = (select id from campus_pass_seasons where is_active)
                """, rewardId) == 0) {
            throw ApiException.notFound("Награда активного сезона не найдена.");
        }
    }

    private void validateReward(SavePassRewardRequest request) {
        if (!TRACKS.contains(request.track()) || request.level() == null || request.level() < 1
                || request.level() > 50 || request.title() == null || request.title().isBlank()
                || request.title().trim().length() > 120 || !KINDS.contains(request.rewardKind())
                || !RARITIES.contains(request.rarity())) {
            throw ApiException.badRequest("Проверьте дорожку, уровень и параметры награды.");
        }
        if ("cosmetic".equals(request.rewardKind())
                && !COSMETIC_TYPES.contains(request.cosmeticType())) {
            throw ApiException.badRequest("Для косметической награды выберите тип предмета.");
        }
        if ("case".equals(request.rewardKind()) && request.cosmeticType() != null
                && !request.cosmeticType().isBlank()) {
            throw ApiException.badRequest("Для кейса не указывается тип косметики.");
        }
        if ("case".equals(request.rewardKind())) {
            long caseId = caseId(request);
            if (jdbc.queryForObject("select count(*) from cases where id = ? and active = true",
                    Integer.class, caseId) == 0) {
                throw ApiException.badRequest("Выберите активный кейс для награды ПТУ-пасса.");
            }
        } else if (request.caseId() != null && !request.caseId().isBlank()) {
            throw ApiException.badRequest("Для косметической награды кейс не выбирается.");
        }
    }

    private void validateSeason(SavePassSeasonRequest request) {
        if (request == null || request.seasonName() == null || request.startsAt() == null
                || request.endsAt() == null || request.startsAt().isBlank() || request.endsAt().isBlank()) {
            throw ApiException.badRequest("Заполните название, даты и стоимость сезона.");
        }
        String name = request.seasonName().trim();
        if (name.isEmpty() || name.length() > 120 || request.premiumPrice() == null
                || request.premiumPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Проверьте название сезона и стоимость премиальной дорожки.");
        }
        Instant startsAt = parseInstant(request.startsAt());
        Instant endsAt = parseInstant(request.endsAt());
        if (!endsAt.isAfter(startsAt)) {
            throw ApiException.badRequest("Дата завершения сезона должна быть позже даты начала.");
        }
    }

    private SeasonSettings activeSeason() {
        return jdbc.query("""
                select id, season_name, starts_at, ends_at, premium_pass_price
                from campus_pass_seasons where is_active
                """, (rs, row) -> new SeasonSettings(rs.getObject("id", UUID.class),
                rs.getString("season_name"), rs.getTimestamp("starts_at").toInstant().toString(),
                rs.getTimestamp("ends_at").toInstant().toString(), rs.getBigDecimal("premium_pass_price")))
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("Активный сезон не настроен."));
    }

    private AdminPassRewardView reward(UUID rewardId) {
        return jdbc.query("""
                select id, track, level, title, reward_kind, cosmetic_type, rarity, case_id
                from campus_pass_reward_catalog where id = ?
                """, (rs, row) -> new AdminPassRewardView(rs.getObject("id", UUID.class).toString(),
                rs.getString("track"), rs.getInt("level"), rs.getString("title"),
                rs.getString("reward_kind"), rs.getString("cosmetic_type"), rs.getString("rarity"),
                nullableLong(rs.getObject("case_id", Long.class))),
                rewardId).stream().findFirst().orElseThrow(() -> ApiException.notFound("Награда не найдена."));
    }

    private Long caseId(SavePassRewardRequest request) {
        if (!"case".equals(request.rewardKind()) || request.caseId() == null || request.caseId().isBlank()) {
            if ("case".equals(request.rewardKind())) {
                throw ApiException.badRequest("Выберите кейс для награды ПТУ-пасса.");
            }
            return null;
        }
        try {
            return Long.parseLong(request.caseId());
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("Выбранный кейс указан некорректно.");
        }
    }

    private String nullableLong(Long value) {
        return value == null ? null : Long.toString(value);
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw ApiException.badRequest("Укажите корректные даты сезона.");
        }
    }

    private record SeasonSettings(UUID id, String name, String startsAt, String endsAt, BigDecimal premiumPrice) {
    }
}
