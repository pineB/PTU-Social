package com.ptu.backend.campus;

import com.ptu.backend.campus.CampusDtos.AdminCaseRewardView;
import com.ptu.backend.campus.CampusDtos.AdminCaseView;
import com.ptu.backend.common.exception.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class CampusCaseAdminService {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final Set<String> COSMETIC_TYPES = Set.of("avatar", "avatar-frame", "profile-background", "badge");
    private static final Set<String> RARITIES = Set.of("Обычный", "Редкий", "Эпический");
    private final JdbcTemplate jdbc;

    public CampusCaseAdminService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<AdminCaseView> listCases() {
        return jdbc.query("""
                select id, name, description, price, active from cases order by id
                """, (rs, row) -> {
            long id = rs.getLong("id");
            return new AdminCaseView(Long.toString(id), rs.getString("name"), rs.getString("description"),
                    rs.getBigDecimal("price"), rs.getBoolean("active"), "/campus/cases/" + id + "/image",
                    rewards(id));
        });
    }

    @Transactional
    public AdminCaseView createCase(String name, String description, BigDecimal price, MultipartFile image) {
        String cleanName = requiredText(name, 120, "Укажите название кейса");
        String cleanDescription = optionalText(description, 2000, "Описание кейса слишком длинное");
        validatePrice(price);
        ImageData imageData = readImage(image);
        Long id = jdbc.queryForObject("""
                insert into cases(name, description, rarity, price, active, image_data, image_content_type)
                values (?, ?, 'COMMON', ?, true, ?, ?) returning id
                """, Long.class, cleanName, cleanDescription, price, imageData.data(), imageData.contentType());
        return getCase(id);
    }

    @Transactional
    public AdminCaseView updateCase(long id, String name, String description, BigDecimal price, MultipartFile image) {
        requireCase(id);
        String cleanName = requiredText(name, 120, "Укажите название кейса");
        String cleanDescription = optionalText(description, 2000, "Описание кейса слишком длинное");
        validatePrice(price);
        ImageData imageData = readImage(image);
        if (imageData.data() == null) {
            jdbc.update("update cases set name = ?, description = ?, price = ? where id = ?",
                    cleanName, cleanDescription, price, id);
        } else {
            jdbc.update("""
                    update cases set name = ?, description = ?, price = ?, image_data = ?, image_content_type = ?
                    where id = ?
                    """, cleanName, cleanDescription, price, imageData.data(), imageData.contentType(), id);
        }
        return getCase(id);
    }

    @Transactional
    public void deactivateCase(long id) {
        requireCase(id);
        if (jdbc.queryForObject("""
                select count(*) from inventory_items where case_id = ?
                """, Integer.class, id) > 0) {
            throw ApiException.badRequest("Нельзя скрыть кейс, пока он есть в инвентарях студентов.");
        }
        if (jdbc.queryForObject("""
                select count(*) from campus_pass_reward_catalog where case_id = ?
                    and season_id = (select id from campus_pass_seasons where is_active)
                """, Integer.class, id) > 0) {
            throw ApiException.badRequest("Сначала замените этот кейс в наградах активного ПТУ-пасса.");
        }
        jdbc.update("update cases set active = false where id = ?", id);
    }

    @Transactional
    public AdminCaseRewardView createReward(long caseId, String name, String cosmeticType, String rarity,
                                            Integer weight, MultipartFile image) {
        requireCase(caseId);
        validateReward(name, cosmeticType, rarity, weight);
        ImageData imageData = readImage(image);
        Long rewardId = jdbc.queryForObject("""
                insert into campus_case_reward_catalog
                    (case_id, reward_name, cosmetic_type, rarity, weight, image_data, image_content_type)
                values (?, ?, ?, ?, ?, ?, ?) returning id
                """, Long.class, caseId, name.trim(), cosmeticType, rarity, weight,
                imageData.data(), imageData.contentType());
        return reward(caseId, rewardId);
    }

    @Transactional
    public AdminCaseRewardView updateReward(long caseId, long rewardId, String name, String cosmeticType,
                                            String rarity, Integer weight, MultipartFile image) {
        requireReward(caseId, rewardId);
        validateReward(name, cosmeticType, rarity, weight);
        ImageData imageData = readImage(image);
        if (imageData.data() == null) {
            jdbc.update("""
                    update campus_case_reward_catalog set reward_name = ?, cosmetic_type = ?, rarity = ?, weight = ?
                    where id = ? and case_id = ?
                    """, name.trim(), cosmeticType, rarity, weight, rewardId, caseId);
        } else {
            jdbc.update("""
                    update campus_case_reward_catalog set reward_name = ?, cosmetic_type = ?, rarity = ?, weight = ?,
                        image_data = ?, image_content_type = ? where id = ? and case_id = ?
                    """, name.trim(), cosmeticType, rarity, weight, imageData.data(), imageData.contentType(),
                    rewardId, caseId);
        }
        return reward(caseId, rewardId);
    }

    @Transactional
    public void deleteReward(long caseId, long rewardId) {
        requireReward(caseId, rewardId);
        jdbc.update("delete from campus_case_reward_catalog where id = ? and case_id = ?", rewardId, caseId);
    }

    private AdminCaseView getCase(long id) {
        return jdbc.query("""
                select id, name, description, price, active from cases where id = ?
                """, (rs, row) -> new AdminCaseView(Long.toString(id), rs.getString("name"),
                rs.getString("description"), rs.getBigDecimal("price"), rs.getBoolean("active"),
                "/campus/cases/" + id + "/image", rewards(id)), id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Кейс не найден"));
    }

    private List<AdminCaseRewardView> rewards(long caseId) {
        List<RewardRow> rows = jdbc.query("""
                select id, reward_name, cosmetic_type, rarity, weight
                from campus_case_reward_catalog where case_id = ? order by id
                """, (rs, row) -> new RewardRow(rs.getLong("id"), rs.getString("reward_name"),
                rs.getString("cosmetic_type"), rs.getString("rarity"), rs.getInt("weight")), caseId);
        long total = rows.stream().mapToLong(RewardRow::weight).sum();
        return rows.stream().map(item -> toView(caseId, item, total)).toList();
    }

    private AdminCaseRewardView reward(long caseId, long rewardId) {
        RewardRow item = jdbc.query("""
                select id, reward_name, cosmetic_type, rarity, weight from campus_case_reward_catalog
                where id = ? and case_id = ?
                """, (rs, row) -> new RewardRow(rs.getLong("id"), rs.getString("reward_name"),
                rs.getString("cosmetic_type"), rs.getString("rarity"), rs.getInt("weight")),
                rewardId, caseId).stream().findFirst().orElseThrow(() -> ApiException.notFound("Награда не найдена"));
        long total = jdbc.queryForObject(
                "select coalesce(sum(weight), 0) from campus_case_reward_catalog where case_id = ?",
                Long.class, caseId);
        return toView(caseId, item, total);
    }

    private AdminCaseRewardView toView(long caseId, RewardRow item, long total) {
        int chance = total == 0 ? 0 : (int) Math.round(item.weight * 100.0 / total);
        return new AdminCaseRewardView(Long.toString(item.id), item.name, item.cosmeticType, item.rarity,
                item.weight, chance, "/campus/cases/" + caseId + "/rewards/" + item.id + "/image");
    }

    private void requireCase(long id) {
        if (jdbc.queryForObject("select count(*) from cases where id = ?", Integer.class, id) == 0) {
            throw ApiException.notFound("Кейс не найден");
        }
    }

    private void requireReward(long caseId, long rewardId) {
        if (jdbc.queryForObject("""
                select count(*) from campus_case_reward_catalog where id = ? and case_id = ?
                """, Integer.class, rewardId, caseId) == 0) {
            throw ApiException.notFound("Награда не найдена");
        }
    }

    private void validateReward(String name, String cosmeticType, String rarity, Integer weight) {
        requiredText(name, 120, "Укажите название награды");
        if (!COSMETIC_TYPES.contains(cosmeticType) || !RARITIES.contains(rarity)
                || weight == null || weight < 1 || weight > 1_000_000) {
            throw ApiException.badRequest("Проверьте тип, редкость и шанс награды");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0 || price.scale() > 2 || price.precision() > 17) {
            throw ApiException.badRequest("Цена кейса должна быть положительной и не превышать 15 цифр");
        }
    }

    private String requiredText(String value, int maxLength, String message) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw ApiException.badRequest(message);
        }
        return value.trim();
    }

    private String optionalText(String value, int maxLength, String message) {
        if (value != null && value.trim().length() > maxLength) {
            throw ApiException.badRequest(message);
        }
        return value == null ? "" : value.trim();
    }

    private ImageData readImage(MultipartFile image) {
        if (image == null || image.isEmpty()) return new ImageData(null, null);
        String contentType = image.getContentType();
        if (image.getSize() > MAX_IMAGE_SIZE || contentType == null || !IMAGE_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Изображение должно быть PNG, JPG или WebP размером до 5 МБ");
        }
        try {
            return new ImageData(image.getBytes(), contentType);
        } catch (IOException exception) {
            throw ApiException.badRequest("Не удалось прочитать загруженное изображение");
        }
    }

    private record RewardRow(long id, String name, String cosmeticType, String rarity, int weight) {
    }

    private record ImageData(byte[] data, String contentType) {
    }
}
