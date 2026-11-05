package com.ptu.backend.academic.web;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ptu.backend.academic.domain.Faculty;
import com.ptu.backend.academic.domain.StudyGroup;
import com.ptu.backend.academic.repo.FacultyRepository;
import com.ptu.backend.academic.repo.StudyGroupRepository;

@RestController
@RequestMapping("/api/academic")
public class AcademicCatalogController {

	private final FacultyRepository faculties;
	private final StudyGroupRepository groups;

	public AcademicCatalogController(FacultyRepository faculties, StudyGroupRepository groups) {
		this.faculties = faculties;
		this.groups = groups;
	}

	@GetMapping("/faculties")
	public List<CatalogItem> faculties() {
		return faculties.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
				.map(faculty -> new CatalogItem(faculty.getId(), faculty.getName()))
				.toList();
	}

	@GetMapping("/faculties/{facultyId}/groups")
	public List<CatalogItem> groups(@PathVariable UUID facultyId) {
		return groups.findAllByFacultyId(facultyId, Sort.by(Sort.Direction.ASC, "name")).stream()
				.map(group -> new CatalogItem(group.getId(), group.getName()))
				.toList();
	}

	public record CatalogItem(UUID id, String name) {
	}
}
