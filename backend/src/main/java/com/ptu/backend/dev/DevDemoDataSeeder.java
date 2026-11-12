package com.ptu.backend.dev;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * Большой демонстрационный набор: группы, люди, кошельки, инвентарь, кейсы,
 * ПТУ-пасс, задания, расписание, мероприятия и справки. Идемпотентен.
 */
@Slf4j
@Component
@Profile("dev")
@Order(2)
public class DevDemoDataSeeder implements ApplicationRunner {

	private static final String PASSWORD = "demo12345";
	private static final String DEMO_SOURCE = "Демо-набор";
	private static final String[] GROUP_NAMES = {"ИС-21", "ИС-22", "ИС-23", "ПИ-21", "ПИ-22", "ЭК-21", "ЭК-22", "БД-21"};
	private static final int GROUP_SIZE = 6;
	private static final int[] LEADER_RATINGS = {1250, 640, 520, 460, 380, 320, 260, 200};
	private static final String[] STUDENTS = {
			"Петров Алексей", "Соколова Дарья", "Морозов Илья", "Лебедева Полина", "Новиков Артём", "Егорова София",
			"Смирнов Максим", "Ковалёва Анна", "Волков Дмитрий", "Павлова Мария", "Зайцев Никита", "Орлова Виктория",
			"Фёдоров Егор", "Михайлова Алиса", "Белов Кирилл", "Тихонова Ева", "Громов Матвей", "Крылова Варвара",
			"Кузнецов Роман", "Алексеева Ульяна", "Степанов Даниил", "Николаева Ксения", "Сергеев Тимофей", "Андреева Арина",
			"Васильев Марк", "Дмитриева Валерия", "Козлов Арсений", "Фролова Алина", "Ефимов Лев", "Ильина Дарья",
			"Попов Андрей", "Виноградова Екатерина", "Комаров Иван", "Белова Наталья", "Кузьмин Олег", "Гусева Юлия",
			"Тарасов Владислав", "Ершова Анастасия", "Мельников Георгий", "Романова Вероника", "Киселёв Артём", "Сидорова Милана",
			"Чернов Александр", "Литвинова Полина", "Антонов Сергей", "Макарова Дарья", "Данилов Никита", "Пономарёва Алиса"
	};
	private static final String[] PREMIUM_TYPES = {"avatar", "avatar-frame", "profile-background", "badge"};
	private static final String[][] COSMETICS = {
			{"Аватар «Дебют»", "avatar", "Обычный"},
			{"Аватар «Хацкер»", "avatar", "Редкий"},
			{"Аватар «Звезда курса»", "avatar", "Эпический"},
			{"Рамка «Зачётка»", "avatar-frame", "Обычный"},
			{"Рамка «Терминал»", "avatar-frame", "Редкий"},
			{"Рамка «Золото»", "avatar-frame", "Эпический"},
			{"Фон «Аудитория»", "profile-background", "Обычный"},
			{"Фон «Градиент»", "profile-background", "Редкий"},
			{"Фон «Матрица»", "profile-background", "Эпический"},
			{"Значок «Первокурсник»", "badge", "Обычный"},
			{"Значок «Староста»", "badge", "Редкий"},
			{"Значок «Легенда кампуса»", "badge", "Эпический"},
	};

	private static final List<CaseSeed> CASES = List.of(
			new CaseSeed("Кейс первокурсника", "Первый шаг в кампусе: аватары, рамки и значки для новичков.", 100, "#2563eb", "#7c3aed",
					List.of(reward("Аватар «Дебют»", "avatar", "Обычный", 50),
							reward("Рамка «Зачётка»", "avatar-frame", "Обычный", 30),
							reward("Значок «Первокурсник»", "badge", "Обычный", 14),
							reward("Фон «Общежитие»", "profile-background", "Редкий", 5),
							reward("Аватар «Звезда курса»", "avatar", "Эпический", 1))),
			new CaseSeed("Кейс программиста", "Для тех, кто живёт в терминале и дебажит по ночам.", 150, "#0f766e", "#22d3ee",
					List.of(reward("Аватар «Хацкер»", "avatar", "Редкий", 35),
							reward("Рамка «Терминал»", "avatar-frame", "Редкий", 25),
							reward("Значок «Senior»", "badge", "Редкий", 20),
							reward("Рамка «Неон»", "avatar-frame", "Обычный", 14),
							reward("Фон «Матрица»", "profile-background", "Эпический", 4),
							reward("Аватар «Кот-программист»", "avatar", "Эпический", 2))),
			new CaseSeed("Кейс дизайнера", "Градиенты, сетки и пиксели — всё для визуала.", 150, "#db2777", "#f59e0b",
					List.of(reward("Аватар «Палитра»", "avatar", "Обычный", 45),
							reward("Рамка «Неон»", "avatar-frame", "Обычный", 30),
							reward("Фон «Градиент»", "profile-background", "Редкий", 12),
							reward("Значок «Пиксель»", "badge", "Редкий", 10),
							reward("Фон «Космос»", "profile-background", "Эпический", 3))),
			new CaseSeed("Кейс старосты", "Больше ответственности — больше наград.", 200, "#7c3aed", "#facc15",
					List.of(reward("Значок «Староста»", "badge", "Редкий", 40),
							reward("Аватар «Лидер»", "avatar", "Редкий", 30),
							reward("Фон «Аудитория»", "profile-background", "Обычный", 20),
							reward("Рамка «Золото»", "avatar-frame", "Эпический", 5),
							reward("Значок «Легенда кампуса»", "badge", "Эпический", 5))),
			new CaseSeed("Кейс сессии", "Антистресс-набор для зачётной недели.", 250, "#dc2626", "#f97316",
					List.of(reward("Аватар «Отличник»", "avatar", "Редкий", 35),
							reward("Рамка «Пятёрка»", "avatar-frame", "Редкий", 25),
							reward("Значок «Антистресс»", "badge", "Обычный", 25),
							reward("Фон «Зачётная неделя»", "profile-background", "Редкий", 12),
							reward("Аватар «Легенда»", "avatar", "Эпический", 3))),
			new CaseSeed("Кейс выпускника", "Финальный аккорд студенческой жизни.", 300, "#1d4ed8", "#0ea5e9",
					List.of(reward("Аватар «Выпускник»", "avatar", "Редкий", 35),
							reward("Фон «Выпускной»", "profile-background", "Редкий", 25),
							reward("Рамка «Мантия»", "avatar-frame", "Эпический", 5),
							reward("Значок «Диплом»", "badge", "Эпический", 5),
							reward("Аватар «Звезда курса»", "avatar", "Эпический", 2))),
			new CaseSeed("Кейс киберспорта", "Турнирные скины и награды кампусной арены.", 180, "#111827", "#22c55e",
					List.of(reward("Аватар «Геймер»", "avatar", "Обычный", 40),
							reward("Рамка «Кибер»", "avatar-frame", "Редкий", 20),
							reward("Значок «MVP»", "badge", "Редкий", 20),
							reward("Фон «Арена»", "profile-background", "Эпический", 4),
							reward("Аватар «Легенда»", "avatar", "Эпический", 2))));

	private final JdbcTemplate jdbc;
	private final PasswordEncoder passwordEncoder;

	public DevDemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
		this.jdbc = jdbc;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		UUID anchor = userIdOrNull("student@ptu.local");
		if (anchor != null && count("select count(*) from inventory_items where owner_student_id = ? and source = ?",
				anchor, DEMO_SOURCE) > 0) {
			log.info("Dev demo seeder: демо-данные уже загружены");
			return;
		}
		String passwordHash = passwordEncoder.encode(PASSWORD);
		UUID semesterId = id("select id from semesters where is_current");
		UUID itFaculty = id("select id from faculties where name = ?", "Факультет информационных технологий");
		UUID economicsFaculty = id("select id from faculties where name = ?", "Факультет экономики");
		UUID isDepartment = id("select id from departments where name = ?", "Кафедра информационных систем");
		UUID economicsDepartment = id("select id from departments where name = ?", "Кафедра экономики");

		UUID teacher = userId("teacher@ptu.local", "Иванов Пётр Сергеевич", "TEACHER", passwordHash);
		UUID teacher2 = userId("teacher2@ptu.local", "Кузнецова Мария Андреевна", "TEACHER", passwordHash);
		UUID organizer = userId("organizer@ptu.local", "Смирнова Анна Викторовна", "ORGANIZER", passwordHash);
		UUID office = firstId("select id from users where role = 'ACADEMIC_OFFICE' order by created_at limit 1", teacher);

		Map<String, UUID> groups = new LinkedHashMap<>();
		for (int index = 0; index < GROUP_NAMES.length; index++) {
			groups.put(GROUP_NAMES[index], groupId(GROUP_NAMES[index], index < 5 ? itFaculty : economicsFaculty));
		}

		Map<String, UUID> disciplines = new LinkedHashMap<>();
		for (String name : List.of("Программирование", "Базы данных", "Веб-разработка", "Математика", "Экономика",
				"Алгоритмы и структуры данных", "Мобильная разработка", "Проектный практикум", "Дизайн интерфейсов",
				"Бухгалтерский учёт", "Маркетинг", "Статистика")) {
			boolean economics = name.equals("Экономика") || name.equals("Бухгалтерский учёт")
					|| name.equals("Маркетинг") || name.equals("Статистика");
			disciplines.put(name, disciplineId(name, economics ? economicsDepartment : isDepartment));
		}

		List<UUID> itDisciplines = List.of("Программирование", "Базы данных", "Веб-разработка",
				"Алгоритмы и структуры данных", "Мобильная разработка", "Дизайн интерфейсов", "Проектный практикум")
				.stream().map(disciplines::get).toList();
		List<UUID> economicsDisciplines = List.of("Экономика", "Бухгалтерский учёт", "Маркетинг", "Статистика")
				.stream().map(disciplines::get).toList();
		for (UUID group : groups.values()) {
			for (UUID discipline : itDisciplines) {
				assign(teacher, discipline, group, semesterId);
			}
			for (UUID discipline : economicsDisciplines) {
				assign(teacher2, discipline, group, semesterId);
			}
		}

		Map<String, Long> caseIds = new LinkedHashMap<>();
		for (CaseSeed seed : CASES) {
			caseIds.put(seed.name(), ensureCase(seed));
		}
		long legacyCase = caseIdByName("Кейс кампуса");
		if (legacyCase > 0) {
			backfillImages(legacyCase);
		}

		UUID seasonId = id("select id from campus_pass_seasons where is_active");
		seedPassCatalog(seasonId, caseIds);

		List<Student> students = new ArrayList<>();
		List<List<UUID>> groupStudents = new ArrayList<>();
		for (int groupIndex = 0; groupIndex < GROUP_NAMES.length; groupIndex++) {
			UUID groupId = groups.get(GROUP_NAMES[groupIndex]);
			jdbc.update("update student_profiles set is_group_leader = false where group_id = ?", groupId);
			List<UUID> members = new ArrayList<>();
			for (int position = 0; position < GROUP_SIZE; position++) {
				int index = groupIndex * GROUP_SIZE + position;
				String email = index == 0 ? "student@ptu.local" : "student" + (index + 1) + "@ptu.local";
				UUID studentId = userId(email, STUDENTS[index], "STUDENT", passwordHash);
				int rating = position == 0 ? LEADER_RATINGS[groupIndex] : 20 * (2 + ((index * 7) % 28));
				int walletBalance = index == 0 ? 5000 : 300 + 20 * ((index * 11) % 60);
				jdbc.update("""
						insert into student_profiles(user_id, group_id, is_group_leader, academic_rating, boa_points, bua_points)
						values (?, ?, false, ?, ?, ?)
						on conflict (user_id) do update set group_id = excluded.group_id,
							academic_rating = excluded.academic_rating, boa_points = excluded.boa_points,
							bua_points = excluded.bua_points, updated_at = now()
						""", studentId, groupId, rating, rating / 10, rating / 20);
				jdbc.update("""
						insert into wallets(user_id, currency_balance) values (?, ?)
						on conflict (user_id) do update set
							currency_balance = greatest(wallets.currency_balance, excluded.currency_balance),
							updated_at = now()
						""", studentId, walletBalance);
				jdbc.update("""
						insert into campus_pass_accounts(user_id, season_id, premium_pass_owned) values (?, ?, ?)
						on conflict (user_id, season_id) do update set premium_pass_owned = excluded.premium_pass_owned,
							updated_at = now()
						""", studentId, seasonId, index == 0 || index < 3 || index % 4 == 0);
				members.add(studentId);
				students.add(new Student(studentId, email, STUDENTS[index], groupIndex, rating, position == 0));
			}
			jdbc.update("update student_profiles set is_group_leader = true where user_id = ?", members.get(0));
			groupStudents.add(members);
		}

		UUID mainStudent = students.get(0).id();
		seedWalletHistory(mainStudent);
		seedInventory(students, mainStudent, caseIds);
		seedPassClaims(mainStudent, seasonId);
		seedTasks(teacher, teacher2, groups, disciplines, groupStudents);
		seedSchedule(teacher, teacher2, office, groups, disciplines);
		seedEvents(organizer, groups, groupStudents);
		seedCertificates(students, groupStudents);
		seedGroupConfirmations(groups, groupStudents);
		log.info("Dev demo seeder: демо-данные загружены (пароль всех демо-аккаунтов {})", PASSWORD);
	}

	private void seedPassCatalog(UUID seasonId, Map<String, Long> caseIds) {
		List<CaseSeed> cases = CASES;
		for (int level = 1; level <= 50; level++) {
			CaseSeed freeCase = cases.get((level - 1) % cases.size());
			jdbc.update("""
					insert into campus_pass_reward_catalog(season_id, track, level, title, reward_kind,
						cosmetic_type, rarity, case_id)
					values (?, 'free', ?, ?, 'case', null, 'Обычный', ?)
					on conflict (season_id, track, level) do update set title = excluded.title,
						reward_kind = 'case', cosmetic_type = null, rarity = excluded.rarity,
						case_id = excluded.case_id
					""", seasonId, level, freeCase.name(), caseIds.get(freeCase.name()));
			String type = PREMIUM_TYPES[(level - 1) % PREMIUM_TYPES.length];
			String rarity = level <= 9 ? "Обычный" : level <= 29 ? "Редкий" : "Эпический";
			jdbc.update("""
					insert into campus_pass_reward_catalog(season_id, track, level, title, reward_kind,
						cosmetic_type, rarity, case_id)
					values (?, 'premium', ?, ?, 'cosmetic', ?, ?, null)
					on conflict (season_id, track, level) do update set title = excluded.title,
						reward_kind = 'cosmetic', cosmetic_type = excluded.cosmetic_type,
						rarity = excluded.rarity, case_id = null
					""", seasonId, level, premiumTitle(type, rarity), type, rarity);
		}
	}

	private void seedWalletHistory(UUID studentId) {
		UUID walletId = id("select id from wallets where user_id = ?", studentId);
		jdbc.update("""
				insert into transactions(wallet_id, amount, type, idempotency_key)
				values (?, 5000, 'ADJUSTMENT', 'demo:start-balance')
				on conflict (idempotency_key) do nothing
				""", walletId);
	}

	private void seedInventory(List<Student> students, UUID mainStudent, Map<String, Long> caseIds) {
		for (Student student : students) {
			int slots = student.id().equals(mainStudent) ? CASES.size() * 2 : 2;
			for (int index = 0; index < slots; index++) {
				CaseSeed seed = CASES.get(index % CASES.size());
				jdbc.update("""
						insert into inventory_items(case_id, owner_student_id, item_name, rarity, quantity,
							item_kind, source)
						values (?, ?, ?, 'COMMON', 1, 'case', ?)
						""", caseIds.get(seed.name()), student.id(), seed.name(), DEMO_SOURCE);
			}
		}
		for (String[] cosmetic : COSMETICS) {
			Long itemId = giveCosmetic(mainStudent, cosmetic[0], cosmetic[1], cosmetic[2]);
			if (itemId == null) continue;
			switch (cosmetic[1]) {
				case "avatar" -> jdbc.update("update student_profiles set equipped_avatar_item_id = ? where user_id = ?",
						itemId, mainStudent);
				case "avatar-frame" -> jdbc.update("update student_profiles set equipped_frame_item_id = ? where user_id = ?",
						itemId, mainStudent);
				case "profile-background" -> jdbc.update("update student_profiles set equipped_background_item_id = ? where user_id = ?",
						itemId, mainStudent);
				default -> {
				}
			}
		}
	}

	private Long giveCosmetic(UUID studentId, String name, String type, String rarity) {
		byte[] image = cosmeticImage(type, rarity);
		Long caseId = jdbc.query("select case_id from campus_case_reward_catalog where cosmetic_type = ? and rarity = ? "
				+ "and image_data is not null order by id limit 1",
				(rs, row) -> rs.getLong(1), type, rarity).stream().findFirst().orElse(null);
		if (caseId == null) return null;
		return jdbc.queryForObject("""
				insert into inventory_items(case_id, owner_student_id, item_name, rarity, quantity, item_kind,
					cosmetic_type, source, image_data, image_content_type)
				values (?, ?, ?, ?, 1, 'cosmetic', ?, ?, ?, ?) returning id
				""", Long.class, caseId, studentId, name, databaseRarity(rarity), type, DEMO_SOURCE,
				image, image == null ? null : "image/svg+xml");
	}

	private byte[] cosmeticImage(String type, String rarity) {
		return jdbc.query("select image_data from campus_case_reward_catalog where cosmetic_type = ? and rarity = ? "
				+ "and image_data is not null order by id limit 1",
				(rs, row) -> rs.getBytes(1), type, rarity).stream().findFirst().orElse(null);
	}

	private void seedPassClaims(UUID studentId, UUID seasonId) {
		jdbc.update("""
				insert into campus_pass_reward_claims(user_id, reward_id)
				select ?, id from campus_pass_reward_catalog
				where season_id = ? and ((track = 'free' and level <= 12) or (track = 'premium' and level <= 6))
				on conflict (user_id, reward_id) do nothing
				""", studentId, seasonId);
	}

	private void seedTasks(UUID teacher, UUID teacher2, Map<String, UUID> groups, Map<String, UUID> disciplines,
			List<List<UUID>> groupStudents) {
		UUID is21 = groups.get("ИС-21");
		UUID is22 = groups.get("ИС-22");
		UUID is23 = groups.get("ИС-23");
		UUID pi21 = groups.get("ПИ-21");
		UUID pi22 = groups.get("ПИ-22");
		UUID ek21 = groups.get("ЭК-21");

		UUID lab = task(teacher, is21, disciplines.get("Программирование"),
				"Лабораторная работа №1: массивы и списки",
				"Реализуйте динамический массив и связный список, сравните сложность основных операций.",
				LocalDateTime.now().minusDays(14), 20, false);
		UUID sql = task(teacher, is21, disciplines.get("Базы данных"),
				"Практика по SQL: JOIN и агрегаты",
				"Составьте 8 запросов с JOIN и группировкой по учебной базе данных кафедры.",
				LocalDateTime.now().minusDays(6), 25, true);
		UUID landing = task(teacher, is22, disciplines.get("Веб-разработка"),
				"Лендинг на React",
				"Свёрстайте адаптивный лендинг мероприятия с формой регистрации и валидацией.",
				LocalDateTime.now().plusDays(9), 30, true);
		UUID algorithms = task(teacher, pi21, disciplines.get("Алгоритмы и структуры данных"),
				"Сортировки: сравнение алгоритмов",
				"Реализуйте быструю и пирамидальную сортировки, замерьте время на массивах разного размера.",
				LocalDateTime.now().minusDays(9), 20, false);
		UUID coursePlan = task(teacher, is22, disciplines.get("Программирование"),
				"Курсовая: план и постановка задачи",
				"Опишите актуальность, цель, задачи и предполагаемый стек технологий курсового проекта.",
				LocalDateTime.now().plusDays(4), 10, false);
		UUID essay = task(teacher2, ek21, disciplines.get("Экономика"),
				"Эссе: цифровая экономика",
				"Раскройте влияние цифровых платформ на рынок труда. Объём — 2–3 страницы.",
				LocalDateTime.now().minusDays(12), 15, false);
		UUID er = task(teacher, is23, disciplines.get("Базы данных"),
				"Практикум: ER-диаграмма",
				"Спроектируйте ER-диаграмму для системы учёта мероприятий кампуса.",
				LocalDateTime.now().minusDays(3), 20, true);
		UUID mobile = task(teacher, pi22, disciplines.get("Мобильная разработка"),
				"Мобильное приложение: прототип",
				"Соберите кликабельный прототип приложения кампуса в Figma и опишите навигацию.",
				LocalDateTime.now().plusDays(12), 40, true);
		UUID design = task(teacher, groups.get("ПИ-21"), disciplines.get("Дизайн интерфейсов"),
				"Дизайн-система кампуса",
				"Соберите набор компонентов и токенов для будущего портала кампуса.",
				LocalDateTime.now().plusDays(16), 35, true);

		submission(lab, groupStudents.get(0).get(1), "ACCEPTED",
				"Реализация на Java с тестами и таблицей сложности.", 5, 18, "Аккуратно и по делу.", 0);
		submission(lab, groupStudents.get(0).get(2), "ACCEPTED",
				"Готовая лабораторная, приложен отчёт.", 4, 14, "Не хватает анализа памяти.", 1);
		submission(lab, groupStudents.get(0).get(3), "RETURNED",
				"Черновик решения.", null, null, "Добавьте тесты и сравнение с ArrayList.", 0);
		submission(sql, groupStudents.get(0).get(1), "ACCEPTED",
				"Восемь запросов с комментариями.", 5, 20, "Отличная работа с оконными функциями.", 0);
		submission(sql, groupStudents.get(0).get(4), "SUBMITTED",
				"Запросы приложены в файле.", null, null, null, 0);
		submission(landing, groupStudents.get(1).get(1), "SUBMITTED",
				"Ссылка на репозиторий и скриншоты.", null, null, null, 0);
		submission(algorithms, groupStudents.get(3).get(1), "ACCEPTED",
				"Графики и замеры в отчёте.", 4, 16, "Добавьте сравнение на почти отсортированных данных.", 0);
		submission(essay, groupStudents.get(5).get(1), "ACCEPTED",
				"Эссе о платформах и занятости.", 5, 15, "Хорошая аргументация.", 0);
		submission(er, groupStudents.get(2).get(2), "RETURNED",
				"Первая версия диаграммы.", null, null, "Уточните кардинальности связей.", 0);
	}

	private UUID task(UUID teacherId, UUID groupId, UUID disciplineId, String title, String description,
			LocalDateTime dueAt, int maxPoints, boolean allowLate) {
		UUID existing = firstIdOrNull("select id from workspace_tasks where teacher_id = ? and title = ?",
				teacherId, title);
		if (existing != null) return existing;
		return jdbc.queryForObject("""
				insert into workspace_tasks(teacher_id, group_id, discipline_id, title, description, due_at,
					max_points, allow_late_submission, archived)
				values (?, ?, ?, ?, ?, ?, ?, ?, false) returning id
				""", UUID.class, teacherId, groupId, disciplineId, title, description, dueAt, maxPoints, allowLate);
	}

	private void submission(UUID taskId, UUID studentId, String status, String content, Integer grade,
			Integer awarded, String feedback, int revisions) {
		jdbc.update("""
				insert into workspace_submissions(task_id, student_id, submitted_at, status, content, teacher_feedback,
					grade, awarded_rating, revision_count, awarded_at)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				on conflict (task_id, student_id) do nothing
				""", taskId, studentId, LocalDateTime.now().minusDays(2 + revisions).withNano(0), status, content,
				feedback, grade, awarded, revisions, "ACCEPTED".equals(status) ? LocalDateTime.now().minusDays(1) : null);
	}

	private void seedSchedule(UUID teacher, UUID teacher2, UUID office, Map<String, UUID> groups,
			Map<String, UUID> disciplines) {
		record Entry(String weekday, String start, String end, String subject, UUID teacher, String teacherName, String room) {
		}
		List<Entry> week = List.of(
				new Entry("Понедельник", "09:00", "10:30", "Программирование", teacher, "Иванов Пётр Сергеевич", "210"),
				new Entry("Понедельник", "10:45", "12:15", "Математика", teacher2, "Кузнецова Мария Андреевна", "305"),
				new Entry("Понедельник", "12:30", "14:00", "Базы данных", teacher, "Иванов Пётр Сергеевич", "212"),
				new Entry("Вторник", "09:00", "10:30", "Веб-разработка", teacher, "Иванов Пётр Сергеевич", "214"),
				new Entry("Вторник", "12:30", "14:00", "Экономика", teacher2, "Кузнецова Мария Андреевна", "301"),
				new Entry("Среда", "10:45", "12:15", "Алгоритмы и структуры данных", teacher, "Иванов Пётр Сергеевич", "210"),
				new Entry("Среда", "14:15", "15:45", "Дизайн интерфейсов", teacher, "Иванов Пётр Сергеевич", "216"),
				new Entry("Четверг", "09:00", "10:30", "Мобильная разработка", teacher, "Иванов Пётр Сергеевич", "218"),
				new Entry("Четверг", "12:30", "14:00", "Статистика", teacher2, "Кузнецова Мария Андреевна", "305"),
				new Entry("Пятница", "10:45", "12:15", "Проектный практикум", teacher, "Иванов Пётр Сергеевич", "220"));
		for (String groupName : List.of("ИС-21", "ИС-22", "ИС-23", "ПИ-21", "ПИ-22", "ЭК-21")) {
			UUID groupId = groups.get(groupName);
			if (count("select count(*) from campus_schedule_entries where group_id = ?", groupId) > 0) continue;
			for (Entry entry : week) {
				jdbc.update("""
						insert into campus_schedule_entries(group_id, weekday, start_time, end_time, subject, teacher,
							room, created_by, discipline_id, teacher_id)
						values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
						""", groupId, entry.weekday(), entry.start(), entry.end(), entry.subject(),
						entry.teacherName(), entry.room(), office, disciplines.get(entry.subject()), entry.teacher());
			}
		}
		UUID is21 = groups.get("ИС-21");
		if (count("select count(*) from campus_schedule_changes where group_id = ?", is21) == 0) {
			jdbc.update("""
					insert into campus_schedule_changes(group_id, change_type, message)
					values (?, 'CHANGED', 'Базы данных перенесена: понедельник 12:30–14:00, ауд. 212.')
					""", is21);
			jdbc.update("""
					insert into campus_schedule_changes(group_id, change_type, message)
					values (?, 'ADDED', 'Добавлена пара: проектный практикум, пятница 10:45–12:15, ауд. 220.')
					""", is21);
		}
	}

	private void seedEvents(UUID organizer, Map<String, UUID> groups, List<List<UUID>> groupStudents) {
		LocalDateTime now = LocalDateTime.now().withNano(0);
		long hackathon = event(organizer, "Хакатон ПТУ 2026",
				"48 часов кода: команды собирают сервисы для кампуса.", "INDIVIDUAL",
				now.plusDays(10).withHour(10), now.plusDays(12).withHour(18), 120, 0, "БОА", 100,
				"Корпус А, ауд. 210", "IT");
		long excursion = event(organizer, "Экскурсия в IT-компанию",
				"Погружение в рабочие процессы продуктовой команды.", "COLLECTIVE",
				now.plusDays(5).withHour(12), now.plusDays(5).withHour(16), 40, 60, "БОА", 80,
				"Технопарк, ул. Ленина, 10", "Карьера");
		long freshmanDay = event(organizer, "День первокурсника",
				"Квест по кампусу, знакомство с клубами и студенческими объединениями.", "INDIVIDUAL",
				now.minusDays(20).withHour(16), now.minusDays(20).withHour(20), 200, 0, "БУА", 60,
				"Актовый зал", "Студенческая жизнь");
		long cleanup = event(organizer, "Эко-субботник",
				"Уборка кампуса и посадка деревьев у корпуса Б.", "COLLECTIVE",
				now.minusDays(7).withHour(10), now.minusDays(7).withHour(14), 80, 50, "БУА", 40,
				"Корпус Б, внутренний двор", "Волонтёрство");
		long cinema = event(organizer, "Киноклуб: разбор фильма «Социальная сеть»",
				"Смотрим и обсуждаем, как рождались большие цифровые продукты.", "INDIVIDUAL",
				now.plusDays(3).withHour(18), now.plusDays(3).withHour(21), 50, 0, "БОА", 30,
				"Библиотека, медиазал", "Культура");
		long volleyball = event(organizer, "Турнир по волейболу",
				"Межгрупповой турнир к открытию спортивного сезона.", "COLLECTIVE",
				now.plusDays(14).withHour(10), now.plusDays(14).withHour(17), 90, 70, "БУА", 50,
				"Спортзал", "Спорт");
		long jobFair = event(organizer, "Ярмарка вакансий",
				"Работодатели региона: стажировки и первые рабочие места.", "INDIVIDUAL",
				now.plusDays(21).withHour(11), now.plusDays(21).withHour(16), 300, 0, "БОА", 20,
				"Фойе корпуса А", "Карьера");

		for (int index = 0; index < 9; index++) {
			participate(freshmanDay, groupStudents.get(index / 6).get(index % 6), null, 0, false);
		}
		for (int index = 0; index < 6; index++) {
			participate(hackathon, groupStudents.get(0).get(index), null, 0, false);
			participate(jobFair, groupStudents.get(4).get(index), null, 0, false);
		}
		UUID is21Registration = groupRegistration(cleanup, groups.get("ИС-21"), organizer, GROUP_SIZE);
		UUID is22Registration = groupRegistration(cleanup, groups.get("ИС-22"), organizer, GROUP_SIZE);
		for (int index = 0; index < GROUP_SIZE; index++) {
			participate(cleanup, groupStudents.get(0).get(index), is21Registration, 0, true);
			participate(cleanup, groupStudents.get(1).get(index), is22Registration, 0, true);
		}
		UUID excursionRegistration = groupRegistration(excursion, groups.get("ПИ-21"), organizer, GROUP_SIZE);
		for (int index = 0; index < GROUP_SIZE; index++) {
			participate(excursion, groupStudents.get(3).get(index), excursionRegistration, 0, false);
		}

		String[] texts = {
				"Очень тёплая атмосфера, всё понравилось!",
				"Отличная организация и интересная программа.",
				"Было здорово познакомиться с клубами кампуса.",
				"Мероприятие прошло на высоком уровне, спасибо!",
				"Понравился квест и работа команды организаторов.",
				"Хорошо провёл время, обязательно приду ещё.",
				"Программа насыщенная, но всё успели посмотреть.",
				"Классный опыт, узнал много нового о кампусе.",
				"Спасибо за приглашение, было очень полезно.",
				"Организаторы молодцы, всё прошло чётко.",
		};
		for (int index = 0; index < texts.length; index++) {
			review(freshmanDay, groupStudents.get(index / 6).get(index % 6), texts[index],
					4 + (index % 2), "APPROVED", index % 3 == 0, now.minusDays(18).plusHours(index));
		}
		review(freshmanDay, groupStudents.get(2).get(4), "Организация говно, больше не пойду", 2,
				"PENDING_MODERATION", false, now.minusDays(17));
		review(cleanup, groupStudents.get(0).get(1), "Хороший субботник, но инвентаря не хватило всем желающим.",
				3, "PENDING_MODERATION", false, now.minusDays(5));
		review(cleanup, groupStudents.get(1).get(2), "Погода подвела, но сделали много полезного.", 4,
				"APPROVED", true, now.minusDays(5));
		review(cleanup, groupStudents.get(1).get(3), "Слишком рано начали, было холодно и сыро.", 2,
				"REJECTED", false, now.minusDays(4));
		review(cinema, groupStudents.get(3).get(1), "Фильм интересный, обсуждение затянулось.", 4,
				"APPROVED", true, now.minusDays(1));
	}

	private long event(UUID organizerId, String title, String description, String mode, LocalDateTime startsAt,
			LocalDateTime endsAt, int capacity, int requiredPercent, String activityType, int maxPoints,
			String location, String category) {
		Long existing = jdbc.query("select id from activities where title = ? order by id limit 1",
				(rs, row) -> rs.getLong(1), title).stream().findFirst().orElse(null);
		if (existing != null) return existing;
		return jdbc.queryForObject("""
				insert into activities(title, description, type, participation_mode, starts_at, ends_at,
					max_participants, min_participants, required_activation_percent, reward_boa, reward_bua,
					teacher_id, active, location, category, campus_activity_type, max_points)
				values (?, ?, 'COMMUNITY', ?, ?, ?, ?, 0, ?, ?, ?, ?, true, ?, ?, ?, ?) returning id
				""", Long.class, title, description, mode, startsAt, endsAt, capacity, requiredPercent,
				"БОА".equals(activityType) ? BigDecimal.valueOf(maxPoints) : BigDecimal.ZERO,
				"БУА".equals(activityType) ? BigDecimal.valueOf(maxPoints) : BigDecimal.ZERO,
				organizerId, location, category, activityType, maxPoints);
	}

	private UUID groupRegistration(long activityId, UUID groupId, UUID addedBy, int groupSize) {
		jdbc.update("""
				insert into campus_event_group_registrations(activity_id, group_id, added_by, group_size)
				values (?, ?, ?, ?) on conflict (activity_id, group_id) do nothing
				""", activityId, groupId, addedBy, groupSize);
		return id("select id from campus_event_group_registrations where activity_id = ? and group_id = ?",
				activityId, groupId);
	}

	private void participate(long activityId, UUID studentId, UUID registrationId, int points, boolean awarded) {
		jdbc.update("""
				insert into participations(activity_id, student_id, status, points_boa, points_bua,
					participation_confirmed, group_registration_id, awarded_percent, awarded_at)
				values (?, ?, 'CONFIRMED', ?, 0, true, ?, ?, ?)
				on conflict (activity_id, student_id) do nothing
				""", activityId, studentId, awarded ? points : 0, registrationId, awarded ? 100 : null,
				awarded ? LocalDateTime.now().minusDays(1) : null);
	}

	private void review(long activityId, UUID authorId, String text, int rating, String status,
			boolean rewardAwarded, LocalDateTime createdAt) {
		jdbc.update("""
				insert into reviews(activity_id, author_id, text, rating, status, reward_awarded, created_at)
				values (?, ?, ?, ?, ?, ?, ?)
				on conflict (activity_id, author_id) do nothing
				""", activityId, authorId, text, rating, status, rewardAwarded, createdAt);
	}

	private void seedCertificates(List<Student> students, List<List<UUID>> groupStudents) {
		UUID main = students.get(0).id();
		certificate(main, "EDUCATION", "PENDING", null, "Справка об обучении для военкомата", "PICKUP", null, true);
		certificate(main, "EDUCATION", "READY", null, "Справка об обучении для банка", "PICKUP", null, true);
		certificate(main, "EDUCATION", "ISSUED", null, "Справка об обучении", "DIGITAL", null, true);
		certificate(main, "INCOME", "IN_PROGRESS", null, "Справка о стипендии", "DIGITAL", null, false);
		certificate(groupStudents.get(1).get(1), "EDUCATION", "PENDING", null,
				"Справка для оформления визы", "PICKUP", null, false);
		certificate(groupStudents.get(1).get(2), "MILITARY", "READY", null,
				"Справка для военного комиссариата", "PICKUP", null, false);
		certificate(groupStudents.get(2).get(1), "OTHER", "REJECTED", "Справка о посещении мероприятий",
				"Нужна справка о посещении мероприятий кампуса", "PICKUP",
				"Не указан период, за который нужна справка.", false);
		certificate(groupStudents.get(3).get(1), "OTHER", "PENDING", "Справка для работодателя",
				"Подтверждение обучения для стажировки", "DIGITAL", null, false);
	}

	private void certificate(UUID studentId, String type, String status, String customTitle, String description,
			String delivery, String rejectionReason, boolean withDocument) {
		if (count("select count(*) from certificate_requests where student_id = ? and type = ? and status = ?",
				studentId, type, status) > 0) {
			return;
		}
		UUID requestId = jdbc.queryForObject("""
				insert into certificate_requests(student_id, type, status, rejection_reason, custom_title,
					description, delivery_method)
				values (?, ?, ?, ?, ?, ?, ?) returning id
				""", UUID.class, studentId, type, status, rejectionReason, customTitle, description, delivery);
		if (withDocument) {
			jdbc.update("""
					insert into certificate_request_documents(request_id, document_name, document_data)
					values (?, ?, ?)
					""", requestId, "spravka.pdf",
					"%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF"
							.getBytes(StandardCharsets.UTF_8));
		}
	}

	private void seedGroupConfirmations(Map<String, UUID> groups, List<List<UUID>> groupStudents) {
		jdbc.update("""
				insert into campus_group_confirmations(student_id, group_id, status)
				values (?, ?, 'PENDING') on conflict do nothing
				""", groupStudents.get(0).get(1), groups.get("ИС-21"));
		jdbc.update("""
				insert into campus_group_confirmations(student_id, group_id, status)
				values (?, ?, 'PENDING') on conflict do nothing
				""", groupStudents.get(1).get(2), groups.get("ИС-22"));
	}

	private long ensureCase(CaseSeed seed) {
		Long existing = jdbc.query("select id from cases where name = ? order by id limit 1",
				(rs, row) -> rs.getLong(1), seed.name()).stream().findFirst().orElse(null);
		long caseId;
		if (existing == null) {
			caseId = jdbc.queryForObject("""
					insert into cases(name, description, rarity, price, active, image_data, image_content_type)
					values (?, ?, 'COMMON', ?, true, ?, 'image/svg+xml') returning id
					""", Long.class, seed.name(), seed.description(), seed.price(),
					svg(seed.name(), seed.colorA(), seed.colorB()));
		} else {
			caseId = existing;
			jdbc.update("update cases set description = ?, price = ?, active = true, image_data = coalesce(image_data, ?), "
					+ "image_content_type = coalesce(image_content_type, 'image/svg+xml') where id = ?",
					seed.description(), seed.price(), svg(seed.name(), seed.colorA(), seed.colorB()), caseId);
		}
		for (Reward reward : seed.rewards()) {
			if (count("select count(*) from campus_case_reward_catalog where case_id = ? and reward_name = ?",
					caseId, reward.name()) > 0) {
				continue;
			}
			jdbc.update("""
					insert into campus_case_reward_catalog(case_id, reward_name, cosmetic_type, rarity, weight,
						image_data, image_content_type)
					values (?, ?, ?, ?, ?, ?, 'image/svg+xml')
					""", caseId, reward.name(), reward.type(), reward.rarity(), reward.weight(),
					svg(reward.name(), rarityColor(reward.rarity()), "#0f172a"));
		}
		backfillImages(caseId);
		return caseId;
	}

	private void backfillImages(long caseId) {
		List<Object[]> rows = jdbc.query("""
				select id, reward_name, rarity from campus_case_reward_catalog
				where case_id = ? and image_data is null
				""", (rs, row) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3)}, caseId);
		for (Object[] row : rows) {
			jdbc.update("""
					update campus_case_reward_catalog set image_data = ?, image_content_type = 'image/svg+xml'
					where id = ?
					""", svg((String) row[1], rarityColor((String) row[2]), "#0f172a"), row[0]);
		}
		if (count("select count(*) from cases where id = ? and image_data is null", caseId) > 0) {
			String name = jdbc.queryForObject("select name from cases where id = ?", String.class, caseId);
			jdbc.update("update cases set image_data = ?, image_content_type = 'image/svg+xml' where id = ?",
					svg(name, "#1d4ed8", "#0ea5e9"), caseId);
		}
	}

	private long caseIdByName(String name) {
		Long id = jdbc.query("select id from cases where name = ? order by id limit 1",
				(rs, row) -> rs.getLong(1), name).stream().findFirst().orElse(null);
		return id == null ? 0 : id;
	}

	private static Reward reward(String name, String type, String rarity, int weight) {
		return new Reward(name, type, rarity, weight);
	}

	private static String premiumTitle(String type, String rarity) {
		return switch (type) {
			case "avatar" -> switch (rarity) {
				case "Редкий" -> "Аватар «Хацкер»";
				case "Эпический" -> "Аватар «Звезда курса»";
				default -> "Аватар «Дебют»";
			};
			case "avatar-frame" -> switch (rarity) {
				case "Редкий" -> "Рамка «Терминал»";
				case "Эпический" -> "Рамка «Золото»";
				default -> "Рамка «Зачётка»";
			};
			case "profile-background" -> switch (rarity) {
				case "Редкий" -> "Фон «Градиент»";
				case "Эпический" -> "Фон «Матрица»";
				default -> "Фон «Аудитория»";
			};
			default -> switch (rarity) {
				case "Редкий" -> "Значок «Староста»";
				case "Эпический" -> "Значок «Легенда кампуса»";
				default -> "Значок «Первокурсник»";
			};
		};
	}

	private static String rarityColor(String rarity) {
		return switch (rarity) {
			case "Редкий" -> "#2563eb";
			case "Эпический" -> "#7c3aed";
			default -> "#64748b";
		};
	}

	private static String databaseRarity(String rarity) {
		return switch (rarity) {
			case "Редкий" -> "RARE";
			case "Эпический" -> "EPIC";
			default -> "COMMON";
		};
	}

	private static byte[] svg(String label, String colorA, String colorB) {
		String safe = escapeXml(label);
		String markup = """
				<svg xmlns="http://www.w3.org/2000/svg" width="320" height="200" viewBox="0 0 320 200">
					<defs>
						<linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
							<stop offset="0" stop-color="%s"/>
							<stop offset="1" stop-color="%s"/>
						</linearGradient>
					</defs>
					<rect width="320" height="200" rx="24" fill="url(#g)"/>
					<circle cx="286" cy="26" r="54" fill="#ffffff" fill-opacity="0.16"/>
					<circle cx="36" cy="186" r="66" fill="#000000" fill-opacity="0.12"/>
					<text x="22" y="112" font-family="Arial, Helvetica, sans-serif" font-size="20"
						font-weight="700" fill="#ffffff">%s</text>
				</svg>
				""".formatted(colorA, colorB, safe);
		return markup.getBytes(StandardCharsets.UTF_8);
	}

	private static String escapeXml(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&apos;");
	}

	private void assign(UUID teacherId, UUID disciplineId, UUID groupId, UUID semesterId) {
		jdbc.update("""
				insert into teacher_assignments(teacher_id, discipline_id, group_id, semester_id, status)
				values (?, ?, ?, ?, 'ACTIVE') on conflict do nothing
				""", teacherId, disciplineId, groupId, semesterId);
	}

	private UUID userId(String email, String fullName, String role, String passwordHash) {
		jdbc.update("""
				insert into users(email, password_hash, full_name, role) values (?, ?, ?, ?)
				on conflict (email) do nothing
				""", email, passwordHash, fullName, role);
		return id("select id from users where email = ?", email);
	}

	private UUID userIdOrNull(String email) {
		return firstIdOrNull("select id from users where email = ?", email);
	}

	private UUID groupId(String name, UUID facultyId) {
		jdbc.update("insert into study_groups(name, faculty_id) values (?, ?) on conflict (name) do nothing",
				name, facultyId);
		return id("select id from study_groups where name = ?", name);
	}

	private UUID disciplineId(String name, UUID departmentId) {
		jdbc.update("""
				insert into disciplines(name, department_id) values (?, ?)
				on conflict (name, department_id) do nothing
				""", name, departmentId);
		return id("select id from disciplines where name = ? and department_id = ?", name, departmentId);
	}

	private UUID id(String sql, Object... args) {
		return jdbc.queryForObject(sql, UUID.class, args);
	}

	private UUID firstId(String sql, UUID fallback) {
		UUID value = firstIdOrNull(sql);
		return value == null ? fallback : value;
	}

	private UUID firstIdOrNull(String sql, Object... args) {
		return jdbc.query(sql, (rs, row) -> rs.getObject(1, UUID.class), args).stream().findFirst().orElse(null);
	}

	private int count(String sql, Object... args) {
		Integer value = jdbc.queryForObject(sql, Integer.class, args);
		return value == null ? 0 : value;
	}

	private record CaseSeed(String name, String description, int price, String colorA, String colorB,
			List<Reward> rewards) {
	}

	private record Reward(String name, String type, String rarity, int weight) {
	}

	private record Student(UUID id, String email, String name, int groupIndex, int rating, boolean leader) {
	}
}
