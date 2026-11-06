package com.ptu.backend.user.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.dto.CreateAcademicOfficeUserRequest;
import com.ptu.backend.user.dto.ManagedUserView;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public AdminUserService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<ManagedUserView> listAcademicOfficeUsers() {
		return users.findAllByRole(Role.ACADEMIC_OFFICE, Sort.by("fullName")).stream()
				.map(ManagedUserView::from).toList();
	}

	@Transactional
	public ManagedUserView createAcademicOfficeUser(CreateAcademicOfficeUserRequest request) {
		if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw ApiException.badRequest("Пароль не должен превышать 72 байта UTF-8");
		}
		String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
		if (users.existsByEmailIgnoreCase(email)) {
			throw ApiException.conflict("Пользователь с таким email уже существует");
		}

		User user = new User();
		user.setFullName(request.fullName().trim());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(Role.ACADEMIC_OFFICE);
		try {
			return ManagedUserView.from(users.saveAndFlush(user));
		}
		catch (DataIntegrityViolationException exception) {
			throw ApiException.conflict("Пользователь с таким email уже существует");
		}
	}
}
