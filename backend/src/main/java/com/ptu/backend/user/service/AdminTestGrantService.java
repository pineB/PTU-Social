package com.ptu.backend.user.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.dto.ManagedUserView;
import com.ptu.backend.user.repo.UserRepository;
import com.ptu.backend.wallet.service.WalletService;

import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminTestGrantService {

	private final UserRepository users;
	private final WalletService wallets;
	private final JdbcTemplate jdbc;

	public AdminTestGrantService(UserRepository users, WalletService wallets, JdbcTemplate jdbc) {
		this.users = users;
		this.wallets = wallets;
		this.jdbc = jdbc;
	}

	@Transactional(readOnly = true)
	public List<ManagedUserView> students() {
		return users.findAllByRole(Role.STUDENT, Sort.by("fullName")).stream()
				.map(ManagedUserView::from).toList();
	}

	@Transactional
	public void grantCurrency(UUID studentId, BigDecimal amount) {
		requireStudent(studentId);
		if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() > 19) {
			throw ApiException.badRequest("Укажите положительную сумму не более чем с двумя знаками после запятой.");
		}
		wallets.credit(studentId, amount, WalletService.TYPE_ADJUSTMENT,
				"admin-test-grant:" + studentId + ":" + UUID.randomUUID());
	}

	@Transactional
	public void grantItem(UUID studentId, Long rewardId) {
		requireStudent(studentId);
		if (rewardId == null || rewardId <= 0) {
			throw ApiException.badRequest("Выберите предмет из каталога наград.");
		}
		int inserted = jdbc.update("""
				insert into inventory_items(
					case_id, owner_student_id, item_name, rarity, quantity, item_kind, cosmetic_type, source,
					image_data, image_content_type
				)
				select c.id, ?, r.reward_name,
					case r.rarity when 'Редкий' then 'RARE' when 'Эпический' then 'EPIC' else 'COMMON' end,
					1, 'cosmetic', r.cosmetic_type, 'Тестовая выдача администратором',
					r.image_data, r.image_content_type
				from campus_case_reward_catalog r
				join cases c on c.id = r.case_id
				where r.id = ? and c.active = true
				""", studentId, rewardId);
		if (inserted == 0) {
			throw ApiException.notFound("Предмет не найден или его кейс отключён.");
		}
	}

	private void requireStudent(UUID studentId) {
		if (studentId == null || users.findById(studentId)
				.filter(user -> user.getRole() == Role.STUDENT)
				.isEmpty()) {
			throw ApiException.notFound("Студент не найден.");
		}
	}
}
