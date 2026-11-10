package com.ptu.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.ptu.backend.campus.CampusDtos.CaseImage;
import com.ptu.backend.campus.CampusDtos.InventoryItemView;
import com.ptu.backend.campus.CampusDtos.PassView;
import com.ptu.backend.campus.CampusService;
import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.support.AbstractIntegrationTest;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CampusPassArtworkIT extends AbstractIntegrationTest {

    @Autowired
    private CampusService campusService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void passRewardsAndClaimedInventoryUseAdministratorUploadedArtwork() {
        var faculty = createFaculty("Pass artwork faculty");
        var group = createGroup("Pass artwork group", faculty);
        User student = createUser(uniqueEmail("pass-artwork"), "Password123", Role.STUDENT);
        jdbc.update("""
                insert into student_profiles(user_id, group_id, academic_rating)
                values (?, ?, 20)
                """, student.getId(), group.getId());

        Long caseId = jdbc.queryForObject("select id from cases where active order by id limit 1", Long.class);
        Long caseRewardId = jdbc.queryForObject("""
                select id from campus_case_reward_catalog
                where case_id = ? and cosmetic_type = 'avatar' and rarity = 'Обычный'
                order by id limit 1
                """, Long.class, caseId);
        byte[] artwork = new byte[] { 1, 2, 3, 4 };
        jdbc.update("""
                update campus_case_reward_catalog
                set image_data = ?, image_content_type = 'image/png'
                where id = ?
                """, artwork, caseRewardId);

        UUID passRewardId = jdbc.queryForObject("""
                select id from campus_pass_reward_catalog
                where season_id = (select id from campus_pass_seasons where is_active)
                    and track = 'free' and level = 1
                """, UUID.class);
        jdbc.update("""
                update campus_pass_reward_catalog
                set title = 'Pass avatar', reward_kind = 'cosmetic', cosmetic_type = 'avatar',
                    rarity = 'Обычный', case_id = null
                where id = ?
                """, passRewardId);

        UserPrincipal principal = new UserPrincipal(student.getId(), student.getEmail(), Role.STUDENT);
        PassView pass = campusService.pass(principal);
        assertThat(pass.rewards()).filteredOn(reward -> reward.id().equals(passRewardId.toString()))
                .singleElement()
                .satisfies(reward -> assertThat(reward.imageUrl())
                        .isEqualTo("/campus/cases/" + caseId + "/rewards/" + caseRewardId + "/image"));

        campusService.claimPassReward(principal, passRewardId);
        InventoryItemView inventoryItem = campusService.inventory(principal).stream()
                .filter(item -> item.name().equals("Pass avatar"))
                .findFirst()
                .orElseThrow();
        CaseImage image = campusService.inventoryImage(principal, Long.parseLong(inventoryItem.id()));
        assertThat(image.data()).containsExactly(artwork);
        assertThat(image.contentType()).isEqualTo("image/png");
    }
}
