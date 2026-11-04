package com.ptu.backend.auth.service;

import com.ptu.backend.auth.dto.LoginRequest;
import com.ptu.backend.auth.dto.TokenResponse;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.security.JwtService;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		String email = RegistrationService.normalizeEmail(request.email());
		User user = users.findByEmailIgnoreCase(email)
				.orElseThrow(() -> ApiException.unauthorized("Неверный email или пароль"));
		if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized("Неверный email или пароль");
		}
		return new TokenResponse(jwtService.generate(user), "Bearer", jwtService.ttlSeconds(), user.getId(),
				user.getRole());
	}
}
