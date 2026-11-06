package com.ptu.backend.user.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ptu.backend.user.domain.Role;
import com.ptu.backend.user.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByIdAndActiveTrue(UUID id);

	java.util.List<User> findAllByRole(Role role, Sort sort);
}
