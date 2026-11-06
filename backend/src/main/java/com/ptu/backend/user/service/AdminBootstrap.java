package com.ptu.backend.user.service;

import java.nio.charset.StandardCharsets;

import com.ptu.backend.config.AppProperties;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrap implements ApplicationRunner {

	private final AppProperties properties;
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;

	public AdminBootstrap(AppProperties properties, UserRepository users, PasswordEncoder passwordEncoder) {
		this.properties = properties;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		AppProperties.AdminBootstrap bootstrap = properties.adminBootstrap();
		if (bootstrap == null || (!StringUtils.hasText(bootstrap.email())
				&& !StringUtils.hasText(bootstrap.password()))) {
			return;
		}
		if (!StringUtils.hasText(bootstrap.email()) || !StringUtils.hasText(bootstrap.password())
				|| !StringUtils.hasText(bootstrap.fullName())) {
			throw new IllegalStateException("Set ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD together");
		}
		if (bootstrap.password().length() < 12
				|| bootstrap.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD must be 12-72 UTF-8 bytes");
		}

		String email = bootstrap.email().trim().toLowerCase();
		var existing = users.findByEmailIgnoreCase(email);
		if (existing.isPresent()) {
			if (existing.get().getRole() != Role.ADMIN) {
				throw new IllegalStateException("ADMIN_BOOTSTRAP_EMAIL belongs to a non-admin user");
			}
			return;
		}

		User admin = new User();
		admin.setEmail(email);
		admin.setFullName(bootstrap.fullName().trim());
		admin.setPasswordHash(passwordEncoder.encode(bootstrap.password()));
		admin.setRole(Role.ADMIN);
		users.save(admin);
	}
}
