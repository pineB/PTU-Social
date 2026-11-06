package com.ptu.backend.repository;

import com.ptu.backend.domain.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("legacyDisciplineRepository")
public interface DisciplineRepository extends JpaRepository<Discipline, Long> {
}
