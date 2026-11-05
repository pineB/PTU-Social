package com.ptu.backend.support;

import java.util.UUID;

import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.repo.FacultyRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;
import com.ptu.backend.common.security.JwtService;
import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;
import com.ptu.backend.user.repo.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

	static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379)
			.withCommand("redis-server", "--requirepass", "redis");

	static {
		postgres.start();
		redis.start();
	}

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected FacultyRepository facultyRepository;

	@Autowired
	protected StudyGroupRepository studyGroupRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	protected JwtService jwtService;

	protected Faculty createFaculty(String name) {
		return facultyRepository.save(new Faculty(name));
	}

	protected StudyGroup createGroup(String name, Faculty faculty) {
		return studyGroupRepository.save(new StudyGroup(name, faculty.getId()));
	}

	protected User createUser(String email, String password, Role role) {
		User user = new User();
		user.setEmail(email.toLowerCase());
		user.setFullName("User " + UUID.randomUUID());
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(role);
		return userRepository.saveAndFlush(user);
	}

	protected String bearer(User user) {
		return "Bearer " + jwtService.generate(user);
	}

	protected static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@ptu.local";
	}
}
