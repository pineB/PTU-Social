package com.ptu.backend.campus;

import com.ptu.backend.campus.CampusDtos.*;
import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.common.security.UserPrincipal;
import com.ptu.backend.api.dto.ReviewCreateRequest;
import com.ptu.backend.auth.service.RegistrationApprovalService;
import com.ptu.backend.review.ReviewService;
import com.ptu.backend.user.domain.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CampusService {
    private static final String CASE_OPEN = "CASE_OPEN";
    private static final String PREMIUM_PASS = "PASS_PREMIUM";
    private static final String COMMON_RARITY = "Обычный";
    private final JdbcTemplate jdbc;
    private final ReviewService reviewService;
    private final RegistrationApprovalService registrationApproval;
    private final SecureRandom secureRandom = new SecureRandom();

    public CampusService(JdbcTemplate jdbc, ReviewService reviewService,
                         RegistrationApprovalService registrationApproval) {
        this.jdbc = jdbc;
        this.reviewService = reviewService;
        this.registrationApproval = registrationApproval;
    }

    @Transactional(readOnly = true)
    public List<EventView> events(UserPrincipal principal) {
        List<Long> ids = jdbc.query("select id from activities where active = true order by starts_at, id",
                (rs, row) -> rs.getLong(1));
        return ids.stream().map(this::eventView).toList();
    }

    @Transactional
    public EventView createEvent(UserPrincipal principal, CreateEventRequest request) {
        requireRole(principal, Role.TEACHER, Role.ORGANIZER);
        validateEvent(request);
        LocalDateTime start = parseDateTime(request.date(), "date");
        LocalDateTime end = parseDateTime(request.endsAt(), "endsAt");
        Long id = jdbc.queryForObject("""
                insert into activities(title, description, type, participation_mode, starts_at, ends_at,
                    max_participants, min_participants, required_activation_percent, reward_boa, reward_bua,
                    teacher_id, active, location, category, campus_activity_type, max_points)
                values (?, ?, 'COMMUNITY', ?, ?, ?, ?, 0, ?, ?, ?, ?, true, ?, ?, ?, ?)
                returning id
                """, Long.class, request.title().trim(),
                request.description() == null ? "" : request.description().trim(), formatMode(request.format()),
                start, end, request.capacity(), request.groupMinPercent() == null ? 0 : request.groupMinPercent(),
                request.activityType().equals("БОА") ? BigDecimal.valueOf(request.maxPoints()) : BigDecimal.ZERO,
                request.activityType().equals("БУА") ? BigDecimal.valueOf(request.maxPoints()) : BigDecimal.ZERO,
                principal.id(), request.location().trim(), request.category().trim(), request.activityType(),
                request.maxPoints());
        return eventView(id);
    }

    @Transactional
    public void joinEvent(UserPrincipal principal, long eventId) {
        requireRole(principal, Role.STUDENT);
        requireStudentProfile(principal.id());
        EventRow event = eventForUpdate(eventId);
        if (!event.active || !"INDIVIDUAL".equals(event.format)) {
            throw ApiException.badRequest("Запись доступна только на активные индивидуальные мероприятия");
        }
        if (!event.startsAt.isAfter(LocalDateTime.now())) {
            throw ApiException.conflict("Регистрация на мероприятие завершена");
        }
        if (count("select count(*) from participations where activity_id = ?", eventId) >= event.capacity) {
            throw ApiException.conflict("Достигнут лимит участников");
        }
        int inserted = jdbc.update("""
                insert into participations(activity_id, student_id, status, points_boa, points_bua, participation_confirmed)
                values (?, ?, 'CONFIRMED', 0, 0, true)
                on conflict (activity_id, student_id) do nothing
                """, eventId, principal.id());
        if (inserted == 0) {
            throw ApiException.conflict("Вы уже записаны на это мероприятие");
        }
    }

    @Transactional
    public void registerGroup(UserPrincipal principal, long eventId, GroupRegistrationRequest request) {
        if (request == null || request.groupId() == null) {
            throw ApiException.badRequest("Необходимо указать группу");
        }
        UUID groupId = parseUuid(request.groupId(), "groupId");
        EventRow event = eventForUpdate(eventId);
        if (!event.active || !"GROUP".equals(event.format)) {
            throw ApiException.badRequest("Мероприятие не поддерживает групповую регистрацию");
        }
        if (!event.startsAt.isAfter(LocalDateTime.now())) {
            throw ApiException.conflict("Регистрация на мероприятие завершена");
        }
        requireGroupRegistrationPermission(principal, groupId);
        List<UUID> members = jdbc.query("select user_id from student_profiles where group_id = ? order by user_id",
                (rs, row) -> rs.getObject(1, UUID.class), groupId);
        if (members.isEmpty()) {
            throw ApiException.badRequest("В группе нет студентов");
        }
        if (count("select count(*) from participations where activity_id = ?", eventId) + members.size()
                > event.capacity) {
            throw ApiException.conflict("Для всей группы недостаточно мест");
        }
        UUID registrationId = jdbc.queryForObject("""
                insert into campus_event_group_registrations(activity_id, group_id, added_by, group_size)
                values (?, ?, ?, ?) returning id
                """, UUID.class, eventId, groupId, principal.id(), members.size());
        for (UUID studentId : members) {
            jdbc.update("""
                    insert into participations(activity_id, student_id, status, points_boa, points_bua,
                        group_registration_id, participation_confirmed)
                    values (?, ?, 'REGISTERED', 0, 0, ?, false)
                    """, eventId, studentId, registrationId);
        }
    }

    @Transactional
    public void confirmGroupParticipation(UserPrincipal principal, long eventId, UUID registrationId) {
        requireRole(principal, Role.STUDENT);
        int changed = jdbc.update("""
                update participations set participation_confirmed = true, status = 'CONFIRMED', updated_at = now()
                where activity_id = ? and group_registration_id = ? and student_id = ?
                """, eventId, registrationId, principal.id());
        if (changed == 0) {
            throw ApiException.notFound("Запись на это мероприятие не найдена");
        }
    }

    @Transactional
    public void awardParticipant(UserPrincipal principal, long eventId, long participantId, AwardRequest request) {
        if (request == null || request.activityPercent() == null
                || request.activityPercent() < 0 || request.activityPercent() > 100) {
            throw ApiException.badRequest("Процент активности должен быть от 0 до 100");
        }
        ParticipationRow participation = jdbc.query("""
                select p.id, p.student_id, p.group_registration_id, p.participation_confirmed,
                       p.awarded_at, p.activity_id
                from participations p where p.id = ? and p.activity_id = ? for update
                """, (rs, row) -> new ParticipationRow(rs.getLong("id"),
                rs.getObject("student_id", UUID.class), rs.getObject("group_registration_id", UUID.class),
                rs.getBoolean("participation_confirmed"), rs.getTimestamp("awarded_at") != null,
                rs.getLong("activity_id")), participantId, eventId).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Участник мероприятия не найден"));
        EventRow event = eventForUpdate(eventId);
        if (event.endsAt.isAfter(LocalDateTime.now())) {
            throw ApiException.conflict("Начислять баллы можно после завершения мероприятия");
        }
        UUID groupId = jdbc.queryForObject("""
                select coalesce(r.group_id, sp.group_id) from student_profiles sp
                left join campus_event_group_registrations r on r.id = ?
                where sp.user_id = ?
                """, UUID.class, participation.groupRegistrationId, participation.studentId);
        requireEventManager(principal, event, groupId);
        if (participation.awarded) {
            throw ApiException.conflict("Баллы за это мероприятие уже начислены");
        }
        if (participation.groupRegistrationId != null) {
            int confirmed = count("select count(*) from participations where group_registration_id = ? "
                    + "and participation_confirmed = true", participation.groupRegistrationId);
            int groupSize = jdbc.queryForObject(
                    "select group_size from campus_event_group_registrations where id = ?",
                    Integer.class, participation.groupRegistrationId);
            int required = CampusRules.requiredGroupParticipation(groupSize, event.groupMinPercent);
            if (confirmed < required) {
                throw ApiException.badRequest("Группа не достигла порога подтверждённого участия");
            }
        }
        int points = BigDecimal.valueOf(event.maxPoints)
                .multiply(BigDecimal.valueOf(request.activityPercent()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValueExact();
        BigDecimal amount = BigDecimal.valueOf(points);
        if (points > 0) {
            UUID budgetOwnerId = principal.role() == Role.TEACHER ? principal.id() : event.creatorId;
            int budgetUpdated = jdbc.update("""
                    update teacher_budgets
                    set spent_budget = spent_budget + ?, updated_at = now()
                    where teacher_id = ? and total_budget - spent_budget >= ?
                    """, amount, budgetOwnerId, amount);
            if (budgetUpdated == 0) {
                throw ApiException.badRequest("Бюджет создателя мероприятия отсутствует или исчерпан");
            }
        }
        String pointsColumn = "БОА".equals(event.activityType) ? "boa_points" : "bua_points";
        String participationColumn = "БОА".equals(event.activityType) ? "points_boa" : "points_bua";
        jdbc.update("update student_profiles set " + pointsColumn + " = " + pointsColumn
                + " + ?, updated_at = now() where user_id = ?", points, participation.studentId);
        jdbc.update("update participations set " + participationColumn
                + " = ?, awarded_percent = ?, awarded_at = now(), status = 'ATTENDED', updated_at = now() where id = ?",
                amount, request.activityPercent(), participantId);
    }

    @Transactional
    public void createEventReview(UserPrincipal principal, long eventId, ReviewRequest request) {
        requireRole(principal, Role.STUDENT);
        validateReview(request);
        ReviewEligibility eligibility = jdbc.query("""
                select a.ends_at, a.participation_mode, a.required_activation_percent,
                    p.group_registration_id, p.participation_confirmed
                from activities a join participations p on p.activity_id = a.id
                where a.id = ? and p.student_id = ?
                """, (rs, row) -> new ReviewEligibility(rs.getTimestamp("ends_at").toLocalDateTime(),
                rs.getString("participation_mode"), rs.getInt("required_activation_percent"),
                rs.getObject("group_registration_id", UUID.class), rs.getBoolean("participation_confirmed")),
                eventId, principal.id()).stream().findFirst()
                .orElseThrow(() -> ApiException.forbidden("Оставить отзыв могут только участники мероприятия"));
        if (eligibility.endsAt.isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("Отзыв можно оставить после завершения мероприятия");
        }
        if (!eligibility.participationConfirmed) {
            throw ApiException.forbidden("Сначала подтвердите участие в мероприятии");
        }
        if ("COLLECTIVE".equals(eligibility.participationMode)) {
            int groupSize = jdbc.queryForObject(
                    "select group_size from campus_event_group_registrations where id = ?",
                    Integer.class, eligibility.groupRegistrationId);
            int confirmed = count("select count(*) from participations where group_registration_id = ? "
                    + "and participation_confirmed = true", eligibility.groupRegistrationId);
            if (confirmed < CampusRules.requiredGroupParticipation(groupSize, eligibility.requiredPercent)) {
                throw ApiException.badRequest("Группа не достигла порога участия для отзыва");
            }
        }
        reviewService.createReview(new ReviewCreateRequest(eventId, principal.id(), request.rating(),
                request.comment().trim()));
    }

    @Transactional
    public void moderateEventReview(UserPrincipal principal, long eventId, long reviewId, ReviewDecision decision) {
        if (decision == null || decision.approved() == null) {
            throw ApiException.badRequest("Решение модерации обязательно");
        }
        EventRow event = eventForUpdate(eventId);
        UUID groupId = jdbc.query("""
                select sp.group_id from reviews r join student_profiles sp on sp.user_id = r.author_id
                where r.id = ? and r.activity_id = ?
                """, (rs, row) -> rs.getObject(1, UUID.class), reviewId, eventId).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Отзыв не найден"));
        try {
            requireEventManager(principal, event, groupId);
        } catch (ApiException ex) {
            throw ApiException.forbidden("Нет прав на модерацию отзыва");
        }
        int changed = jdbc.update("""
                update reviews set status = ?, moderated_by = ?, updated_at = now()
                where id = ? and activity_id = ? and status = 'PENDING_MODERATION'
                """, decision.approved() ? "APPROVED" : "REJECTED", principal.id(), reviewId, eventId);
        if (changed == 0) {
            throw ApiException.conflict("Отзыв не найден или уже рассмотрен");
        }
    }

    @Transactional(readOnly = true)
    public List<GroupView> groups(UserPrincipal principal) {
        List<GroupRow> groups = switch (principal.role()) {
            case STUDENT -> jdbc.query("""
                    select g.id, g.name, (select count(*) from student_profiles sp where sp.group_id = g.id) size,
                        lead.user_id leader_id
                    from study_groups g join student_profiles sp on sp.group_id = g.id
                    left join student_profiles lead on lead.group_id = g.id and lead.is_group_leader = true
                    where sp.user_id = ?
                    """, groupMapper(), principal.id());
            case TEACHER -> jdbc.query("""
                    select distinct g.id, g.name,
                        (select count(*) from student_profiles sp where sp.group_id = g.id) size, lead.user_id leader_id
                    from study_groups g join teacher_assignments ta on ta.group_id = g.id
                    join semesters s on s.id = ta.semester_id and s.is_current = true
                    left join student_profiles lead on lead.group_id = g.id and lead.is_group_leader = true
                    where ta.teacher_id = ? and ta.status = 'ACTIVE' order by g.name
                    """, groupMapper(), principal.id());
            case ORGANIZER, ACADEMIC_OFFICE -> jdbc.query("""
                    select g.id, g.name, (select count(*) from student_profiles sp where sp.group_id = g.id) size,
                        lead.user_id leader_id
                    from study_groups g left join student_profiles lead
                        on lead.group_id = g.id and lead.is_group_leader = true order by g.name
                    """, groupMapper());
            case ADMIN -> throw ApiException.forbidden("Нет прав на просмотр кампусных групп");
        };
        return groups.stream().map(g -> new GroupView(g.id.toString(), g.name, g.size,
                g.leaderId == null ? null : g.leaderId.toString())).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupStudentView> students(UserPrincipal principal, UUID groupId) {
        requireGroupManager(principal, groupId, true);
        return jdbc.query("""
                select u.id, u.full_name, u.email::text, g.name
                from student_profiles sp join users u on u.id = sp.user_id
                join study_groups g on g.id = sp.group_id where sp.group_id = ? order by u.full_name
                """, (rs, row) -> new GroupStudentView(rs.getObject(1, UUID.class).toString(),
                rs.getString(2), rs.getString(3), rs.getString(4)), groupId);
    }

    @Transactional(readOnly = true)
    public List<GroupConfirmationView> confirmations(UserPrincipal principal, UUID groupId) {
        requireGroupManager(principal, groupId, true);
        List<GroupConfirmationView> views = new java.util.ArrayList<>(confirmationsForGroup(groupId));
        views.addAll(registrationConfirmations(groupId));
        views.sort((left, right) -> right.submittedAt().compareTo(left.submittedAt()));
        return views;
    }

    @Transactional
    public GroupConfirmationView requestGroupConfirmation(UserPrincipal principal) {
        requireRole(principal, Role.STUDENT);
        UUID groupId = jdbc.query("select group_id from student_profiles where user_id = ?",
                (rs, row) -> rs.getObject(1, UUID.class), principal.id()).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Профиль студента не найден"));
        if (count("select count(*) from campus_group_confirmations where student_id = ? and group_id = ? "
                + "and status = 'PENDING'", principal.id(), groupId) == 0) {
            jdbc.update("""
                    insert into campus_group_confirmations(student_id, group_id, status)
                    values (?, ?, 'PENDING')
                    on conflict (student_id, group_id) where status = 'PENDING' do nothing
                    """, principal.id(), groupId);
        }
        return jdbc.query("""
                select c.id, u.full_name, u.email::text, g.name, c.submitted_at, c.status
                from campus_group_confirmations c join users u on u.id = c.student_id
                join study_groups g on g.id = c.group_id where c.student_id = ? and c.group_id = ?
                and c.status = 'PENDING' order by c.submitted_at desc limit 1
                """, confirmationMapper(), principal.id(), groupId).get(0);
    }

    @Transactional(readOnly = true)
    public GroupConfirmationView myGroupConfirmation(UserPrincipal principal) {
        requireRole(principal, Role.STUDENT);
        return jdbc.query("""
                select c.id, u.full_name, u.email::text, g.name, c.submitted_at, c.status
                from campus_group_confirmations c join users u on u.id = c.student_id
                join study_groups g on g.id = c.group_id where c.student_id = ?
                order by c.submitted_at desc, c.id desc limit 1
                """, confirmationMapper(), principal.id()).stream().findFirst().orElse(null);
    }

    @Transactional
    public void decideGroupConfirmation(UserPrincipal principal, UUID confirmationId,
                                        GroupConfirmationDecision decision) {
        if (decision == null || decision.approve() == null) {
            throw ApiException.badRequest("Решение обязательно");
        }
        ConfirmationRow confirmation = jdbc.query("""
                select student_id, group_id from campus_group_confirmations
                where id = ? and status = 'PENDING' for update
                """, (rs, row) -> new ConfirmationRow(rs.getObject(1, UUID.class),
                rs.getObject(2, UUID.class)), confirmationId).stream().findFirst()
                .orElse(null);
        if (confirmation == null) {
            decideRegistrationRequest(principal, confirmationId, decision.approve());
            return;
        }
        requireGroupManager(principal, confirmation.groupId, true);
        if (decision.approve()) {
            jdbc.update("""
                    update student_profiles set
                        is_group_leader = case when group_id = ? then is_group_leader else false end,
                        group_id = ?, updated_at = now()
                    where user_id = ?
                    """, confirmation.groupId, confirmation.groupId, confirmation.studentId);
        }
        jdbc.update("""
                update campus_group_confirmations set status = ?, reviewed_by = ?, reviewed_at = now()
                where id = ?
                """, decision.approve() ? "APPROVED" : "REJECTED", principal.id(), confirmationId);
    }

    @Transactional
    public void assignGroupLeader(UserPrincipal principal, UUID groupId, String studentId) {
        requireGroupManager(principal, groupId, false);
        UUID leaderId = parseUuid(studentId, "studentId");
        if (count("select count(*) from student_profiles where user_id = ? and group_id = ?",
                leaderId, groupId) == 0) {
            throw ApiException.badRequest("Староста должен состоять в выбранной группе");
        }
        jdbc.update("update student_profiles set is_group_leader = false where group_id = ?", groupId);
        jdbc.update("update student_profiles set is_group_leader = true, updated_at = now() "
                + "where user_id = ? and group_id = ?", leaderId, groupId);
    }

    @Transactional(readOnly = true)
    public CatalogView catalog(UserPrincipal principal) {
        List<GroupView> groups = groups(principal);
        CaseRow campusCase = jdbc.query("""
                select id, price from cases where active = true and name = 'Кейс кампуса' order by id limit 1
                """, (rs, row) -> new CaseRow(rs.getLong(1), rs.getBigDecimal(2))).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Каталог кейсов не настроен"));
        List<RewardCatalogRow> rewards = jdbc.query("""
                select reward_name, cosmetic_type, rarity, weight from campus_case_reward_catalog
                where case_id = ? order by id
                """, (rs, row) -> new RewardCatalogRow(rs.getString(1), rs.getString(2),
                rs.getString(3), rs.getInt(4)), campusCase.id);
        int total = rewards.stream().mapToInt(RewardCatalogRow::weight).sum();
        List<CampusCaseRewardView> rewardViews = rewards.stream()
                .map(r -> new CampusCaseRewardView(r.name, r.cosmeticType, r.rarity,
                        total == 0 ? 0 : (int) Math.round(r.weight * 100.0 / total)))
                .toList();
        return new CatalogView(groups, campusCase.price, rewardViews);
    }

    @Transactional
    public WalletView wallet(UserPrincipal principal) {
        ensureWallet(principal.id());
        WalletRow wallet = lockWallet(principal.id(), false);
        List<WalletTransactionView> transactions = jdbc.query("""
                select id, type, amount, created_at from transactions
                where wallet_id = ? order by created_at desc, id desc
                """, (rs, row) -> {
            String type = rs.getString("type");
            BigDecimal amount = rs.getBigDecimal("amount");
            boolean debit = CASE_OPEN.equals(type) || PREMIUM_PASS.equals(type);
            String title = switch (type) {
                case CASE_OPEN -> "Открытие кейса";
                case PREMIUM_PASS -> "Премиум-пропуск";
                case "REVIEW_REWARD" -> "Награда за отзыв";
                case "MONTHLY_CONVERSION" -> "Конвертация валюты";
                case "ADJUSTMENT" -> "Корректировка баланса";
                default -> "Операция по кошельку";
            };
            String detail = switch (type) {
                case CASE_OPEN -> "Кейс кампуса";
                case PREMIUM_PASS -> "Покупка премиум-дорожки";
                case "REVIEW_REWARD" -> "Одобренный отзыв";
                case "MONTHLY_CONVERSION" -> "Месячная конвертация";
                case "ADJUSTMENT" -> "Операция учебной части";
                default -> "Изменение баланса жетонов";
            };
            return new WalletTransactionView(rs.getObject("id", UUID.class).toString(),
                    title, detail, debit ? amount.negate() : amount,
                    rs.getTimestamp("created_at").toInstant().toString());
        }, wallet.id);
        return new WalletView(wallet.balance, transactions);
    }

    @Transactional(readOnly = true)
    public List<ScheduleView> schedule(UserPrincipal principal) {
        return jdbc.query("""
                select s.id, s.weekday, s.start_time, s.end_time, s.group_id, g.name, s.subject,
                    s.teacher_id, coalesce(u.full_name, s.teacher) teacher_name, s.room
                from campus_schedule_entries s join study_groups g on g.id = s.group_id
                left join users u on u.id = s.teacher_id
                where s.group_id in (
                    select sp.group_id from student_profiles sp where sp.user_id = ?
                    union
                    select ta.group_id from teacher_assignments ta join semesters sem
                        on sem.id = ta.semester_id and sem.is_current = true
                    where ta.teacher_id = ? and ta.status = 'ACTIVE'
                    union
                    select id from study_groups where ? in ('ORGANIZER', 'ACADEMIC_OFFICE')
                )
                order by s.weekday, s.start_time, s.id
                """, scheduleMapper(), principal.id(), principal.id(), principal.role().name());
    }

    @Transactional(readOnly = true)
    public List<ScheduleChangeView> scheduleChanges(UserPrincipal principal) {
        requireRole(principal, Role.STUDENT);
        return jdbc.query("""
                select c.id, c.change_type, c.message, g.name, c.created_at
                from campus_schedule_changes c
                join study_groups g on g.id = c.group_id
                join student_profiles sp on sp.group_id = c.group_id
                where sp.user_id = ?
                order by c.created_at desc, c.id desc
                limit 50
                """, (rs, row) -> new ScheduleChangeView(
                rs.getObject("id", UUID.class).toString(), rs.getString("change_type"),
                rs.getString("message"), rs.getString("name"),
                rs.getTimestamp("created_at").toInstant().toString()), principal.id());
    }

    @Transactional
    public ScheduleView createSchedule(UserPrincipal principal, ScheduleRequest request) {
        requireRole(principal, Role.ACADEMIC_OFFICE);
        validateSchedule(request);
        UUID groupId = parseUuid(request.groupId(), "groupId");
        String teacherName = activeTeacherName(request.teacherId());
        ensureTeacherHasNoScheduleOverlap(null, request, parseUuid(request.teacherId(), "teacherId"));
        UUID id = jdbc.queryForObject("""
                insert into campus_schedule_entries(group_id, weekday, start_time, end_time, subject, teacher,
                    teacher_id, room, created_by)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?) returning id
                """, UUID.class, groupId, request.weekday().trim(), request.startTime().trim(),
                request.endTime().trim(), request.subject().trim(), teacherName, parseUuid(request.teacherId(), "teacherId"),
                request.room().trim(), principal.id());
        ScheduleView created = scheduleEntry(id, false);
        recordScheduleChange(groupId, id, "ADDED", "Добавлено занятие: " + describeSchedule(created));
        return created;
    }

    @Transactional
    public ScheduleView updateSchedule(UserPrincipal principal, UUID scheduleId, ScheduleRequest request) {
        requireRole(principal, Role.ACADEMIC_OFFICE);
        validateSchedule(request);
        ScheduleView previous = scheduleEntry(scheduleId, true);
        UUID groupId = parseUuid(request.groupId(), "groupId");
        UUID teacherId = parseUuid(request.teacherId(), "teacherId");
        String teacherName = activeTeacherName(request.teacherId());
        ensureTeacherHasNoScheduleOverlap(scheduleId, request, teacherId);
        jdbc.update("""
                update campus_schedule_entries
                set group_id = ?, weekday = ?, start_time = ?, end_time = ?, subject = ?, teacher = ?,
                    teacher_id = ?, room = ?
                where id = ?
                """, groupId, request.weekday().trim(), request.startTime().trim(), request.endTime().trim(),
                request.subject().trim(), teacherName, teacherId, request.room().trim(), scheduleId);
        ScheduleView updated = scheduleEntry(scheduleId, false);
        String message = "Изменение расписания. Было: " + describeSchedule(previous)
                + ". Стало: " + describeSchedule(updated);
        recordScheduleChange(UUID.fromString(previous.groupId()), scheduleId, "CHANGED", message);
        if (!previous.groupId().equals(groupId)) {
            recordScheduleChange(groupId, scheduleId, "CHANGED", message);
        }
        return updated;
    }

    @Transactional
    public void deleteSchedule(UserPrincipal principal, UUID scheduleId) {
        requireRole(principal, Role.ACADEMIC_OFFICE);
        ScheduleView previous = scheduleEntry(scheduleId, true);
        jdbc.update("delete from campus_schedule_entries where id = ?", scheduleId);
        recordScheduleChange(UUID.fromString(previous.groupId()), scheduleId, "CANCELLED",
                "Занятие отменено: " + describeSchedule(previous));
    }

    @Transactional
    public void updateScheduleTimeSlot(UserPrincipal principal, ScheduleTimeChangeRequest request) {
        requireRole(principal, Role.ACADEMIC_OFFICE);
        if (request == null || !validTimeRange(request.previousStartTime(), request.previousEndTime())
                || !validTimeRange(request.startTime(), request.endTime())) {
            throw ApiException.badRequest("Укажите корректные интервалы времени");
        }
        List<ScheduleView> entries = jdbc.query("""
                select s.id, s.weekday, s.start_time, s.end_time, s.group_id, g.name, s.subject,
                    s.teacher_id, coalesce(u.full_name, s.teacher) teacher_name, s.room
                from campus_schedule_entries s
                join study_groups g on g.id = s.group_id
                left join users u on u.id = s.teacher_id
                where s.start_time = ? and s.end_time = ?
                order by s.teacher_id, s.weekday, s.id
                for update of s
                """, scheduleMapper(), request.previousStartTime(), request.previousEndTime());
        if (entries.isEmpty()) {
            throw ApiException.notFound("В это время нет пар для изменения");
        }
        List<UUID> entryIds = entries.stream().map(entry -> UUID.fromString(entry.id())).toList();
        for (UUID teacherId : entries.stream().map(ScheduleView::teacherId).filter(id -> id != null)
                .distinct().sorted().map(UUID::fromString).toList()) {
            jdbc.query("select id from users where id = ? for update",
                    (rs, row) -> rs.getObject(1, UUID.class), teacherId);
        }
        String excludedIds = String.join(",", java.util.Collections.nCopies(entryIds.size(), "?"));
        for (ScheduleView entry : entries) {
            if (entry.teacherId() == null) {
                throw ApiException.conflict("Сначала назначьте преподавателя каждой паре в этом временном слоте");
            }
            List<Object> parameters = new java.util.ArrayList<>(List.of(UUID.fromString(entry.teacherId()),
                    entry.weekday(), request.endTime(), request.startTime()));
            parameters.addAll(entryIds);
            Integer conflicts = jdbc.queryForObject("""
                    select count(*) from campus_schedule_entries
                    where teacher_id = ? and weekday = ? and start_time < ? and end_time > ?
                        and id not in (
                    """ + excludedIds + ")", Integer.class, parameters.toArray());
            if (conflicts != null && conflicts > 0) {
                throw ApiException.conflict("У преподавателя уже есть занятие в выбранном времени");
            }
        }
        for (ScheduleView entry : entries) {
            UUID id = UUID.fromString(entry.id());
            jdbc.update("update campus_schedule_entries set start_time = ?, end_time = ? where id = ?",
                    request.startTime(), request.endTime(), id);
            ScheduleView updated = scheduleEntry(id, false);
            recordScheduleChange(UUID.fromString(entry.groupId()), id, "CHANGED",
                    "Изменено время пары. Было: " + describeSchedule(entry)
                            + ". Стало: " + describeSchedule(updated));
        }
    }

    @Transactional
    public PassView pass(UserPrincipal principal) {
        requireRole(principal, Role.STUDENT);
        requireStudentProfile(principal.id());
        int experience = Math.max(0, academicRating(principal.id()));
        boolean premium = premiumOwned(principal.id());
        BigDecimal price = jdbc.queryForObject(
                "select premium_pass_price from campus_runtime_catalog_config where id = 1", BigDecimal.class);
        int level = CampusRules.passLevel(experience);
        List<PassRewardView> rewards = jdbc.query("""
                select r.id, r.track, r.level, r.title, r.reward_kind,
                    (c.id is not null) claimed
                from campus_pass_reward_catalog r
                left join campus_pass_reward_claims c on c.reward_id = r.id and c.user_id = ?
                order by r.level, r.track
                """, (rs, row) -> new PassRewardView(rs.getObject("id", UUID.class).toString(),
                rs.getString("track"), rs.getInt("level"), rs.getString("title"),
                rs.getString("reward_kind"), rs.getBoolean("claimed")), principal.id());
        return new PassView(experience, premium, price, rewards);
    }

    @Transactional
    public void purchasePremiumPass(UserPrincipal principal) {
        requireRole(principal, Role.STUDENT);
        requireStudentProfile(principal.id());
        ensureWallet(principal.id());
        jdbc.update("""
                insert into campus_pass_accounts(user_id, premium_pass_owned) values (?, false)
                on conflict (user_id) do nothing
                """, principal.id());
        Boolean owned = jdbc.queryForObject(
                "select premium_pass_owned from campus_pass_accounts where user_id = ? for update",
                Boolean.class, principal.id());
        if (Boolean.TRUE.equals(owned)) {
            throw ApiException.conflict("Премиум-пропуск уже приобретён");
        }
        BigDecimal price = jdbc.queryForObject(
                "select premium_pass_price from campus_runtime_catalog_config where id = 1", BigDecimal.class);
        debit(principal.id(), price, PREMIUM_PASS);
        jdbc.update("update campus_pass_accounts set premium_pass_owned = true, updated_at = now() where user_id = ?",
                principal.id());
    }

    @Transactional
    public void claimPassReward(UserPrincipal principal, UUID rewardId) {
        requireRole(principal, Role.STUDENT);
        requireStudentProfile(principal.id());
        PassRewardClaimRow reward = jdbc.query("""
                select id, track, level, title, reward_kind, cosmetic_type, rarity
                from campus_pass_reward_catalog where id = ? for update
                """, (rs, row) -> new PassRewardClaimRow(rs.getObject("id", UUID.class),
                rs.getString("track"), rs.getInt("level"), rs.getString("title"),
                rs.getString("reward_kind"), rs.getString("cosmetic_type"), rs.getString("rarity")), rewardId)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("Награда не найдена"));
        if ("premium".equals(reward.track) && !premiumOwned(principal.id())) {
            throw ApiException.forbidden("Для получения награды нужен премиум-пропуск");
        }
        if (CampusRules.passLevel(Math.max(0, academicRating(principal.id()))) < reward.level) {
            throw ApiException.badRequest("Уровень награды ещё не достигнут");
        }
        jdbc.update("insert into campus_pass_reward_claims(user_id, reward_id) values (?, ?)",
                principal.id(), reward.id);
        insertInventory(principal.id(), reward.title,
                reward.kind, reward.cosmeticType, reward.rarity, "Боевой пропуск");
    }

    @Transactional(readOnly = true)
    public List<InventoryItemView> inventory(UserPrincipal principal) {
        requireRole(principal, Role.STUDENT);
        requireStudentProfile(principal.id());
        return jdbc.query("""
                select id, item_name, item_kind, cosmetic_type, rarity, source, acquired_at
                from inventory_items where owner_student_id = ? order by acquired_at desc, id desc
                """, inventoryMapper(), principal.id());
    }

    @Transactional
    public InventoryItemView openCase(UserPrincipal principal, long inventoryItemId) {
        requireRole(principal, Role.STUDENT);
        requireStudentProfile(principal.id());
        ensureWallet(principal.id());
        CaseInventoryRow caseItem = jdbc.query("""
                select id, case_id, quantity from inventory_items
                where id = ? and owner_student_id = ? and item_kind = 'case' for update
                """, (rs, row) -> new CaseInventoryRow(rs.getLong("id"), rs.getLong("case_id"),
                rs.getInt("quantity")),
                inventoryItemId, principal.id()).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Кейс не найден в инвентаре"));
        CaseRow campusCase = jdbc.query("""
                select id, price from cases where id = ? and active = true for update
                """, (rs, row) -> new CaseRow(rs.getLong(1), rs.getBigDecimal(2)), caseItem.caseId).stream()
                .findFirst().orElseThrow(() -> ApiException.notFound("Кейс не найден"));
        if (campusCase.price.signum() <= 0) {
            throw ApiException.badRequest("Цена кейса настроена некорректно");
        }
        List<RewardCatalogRow> rewards = jdbc.query("""
                select reward_name, cosmetic_type, rarity, weight from campus_case_reward_catalog
                where case_id = ? order by id
                """, (rs, row) -> new RewardCatalogRow(rs.getString(1), rs.getString(2),
                rs.getString(3), rs.getInt(4)), caseItem.caseId);
        RewardCatalogRow reward = chooseReward(rewards);
        debit(principal.id(), campusCase.price, CASE_OPEN);
        if (caseItem.quantity > 1) {
            jdbc.update("update inventory_items set quantity = quantity - 1 where id = ? and owner_student_id = ?",
                    inventoryItemId, principal.id());
        } else {
            jdbc.update("delete from inventory_items where id = ? and owner_student_id = ?",
                    inventoryItemId, principal.id());
        }
        return insertInventory(principal.id(), reward.name, "cosmetic", reward.cosmeticType,
                reward.rarity, "Кейс кампуса");
    }

    private EventView eventView(long eventId) {
        EventRow event = jdbc.query("""
                select a.id, a.title, a.description, a.starts_at, a.ends_at, a.location, a.category,
                    a.max_participants, a.participation_mode, a.required_activation_percent,
                    a.campus_activity_type, a.max_points, a.teacher_id, u.full_name, a.active
                from activities a join users u on u.id = a.teacher_id where a.id = ?
                """, eventMapper(), eventId).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Мероприятие не найдено"));
        List<EventGroupRegistrationView> registrations = jdbc.query("""
                select r.id, g.name, r.group_id, r.group_size, u.full_name, r.requested_at
                from campus_event_group_registrations r join study_groups g on g.id = r.group_id
                join users u on u.id = r.added_by where r.activity_id = ? order by r.requested_at, r.id
                """, (rs, row) -> new EventGroupRegistrationView(
                rs.getObject("id", UUID.class).toString(), rs.getString("name"),
                rs.getInt("group_size"),
                rs.getString("full_name"), rs.getTimestamp("requested_at").toInstant().toString()), eventId);
        List<EventParticipantView> participants = jdbc.query("""
                select p.id, u.id student_id, u.full_name, g.name, p.created_at, p.participation_confirmed,
                    p.group_registration_id, case when ? = 'БОА' then p.points_boa else p.points_bua end points,
                    p.awarded_percent, p.awarded_at
                from participations p join users u on u.id = p.student_id
                join student_profiles sp on sp.user_id = p.student_id
                left join campus_event_group_registrations r on r.id = p.group_registration_id
                join study_groups g on g.id = coalesce(r.group_id, sp.group_id)
                where p.activity_id = ? order by p.created_at, p.id
                """, (rs, row) -> new EventParticipantView(Long.toString(rs.getLong("id")),
                rs.getObject("student_id", UUID.class).toString(), rs.getString("full_name"), rs.getString("name"),
                rs.getTimestamp("created_at").toInstant().toString(), rs.getBoolean("participation_confirmed"),
                rs.getObject("group_registration_id", UUID.class) == null ? null
                        : rs.getObject("group_registration_id", UUID.class).toString(),
                rs.getTimestamp("awarded_at") == null
                        ? null : rs.getBigDecimal("points"),
                (Integer) rs.getObject("awarded_percent"),
                rs.getTimestamp("awarded_at") == null ? null : rs.getTimestamp("awarded_at").toInstant().toString()),
                event.activityType, eventId);
        List<EventReviewView> reviews = jdbc.query("""
                select r.id, u.id student_id, u.full_name, r.rating, r.text, r.status, r.created_at
                from reviews r join users u on u.id = r.author_id where r.activity_id = ?
                order by r.created_at, r.id
                """, (rs, row) -> new EventReviewView(Long.toString(rs.getLong("id")),
                        rs.getObject("student_id", UUID.class).toString(), rs.getString("full_name"), rs.getInt("rating"), rs.getString("text"),
                switch (rs.getString("status")) {
                    case "APPROVED" -> "approved";
                    case "REJECTED" -> "rejected";
                    default -> "pending";
                }, rs.getTimestamp("created_at").toInstant().toString()), eventId);
        return new EventView(Long.toString(event.id), event.title, event.description == null ? "" : event.description,
                event.startsAt.toString(),
                event.endsAt.toString(), event.location, event.category, event.capacity, event.format,
                "GROUP".equals(event.format) ? event.groupMinPercent : null, event.activityType, event.maxPoints,
                event.creatorId.toString(), event.creatorName,
                registrations, participants, reviews);
    }

    private EventRow eventForUpdate(long eventId) {
        return jdbc.query("""
                select a.id, a.teacher_id, a.title, a.description, a.location, a.category, a.starts_at, a.ends_at, a.max_participants,
                    a.participation_mode, a.required_activation_percent, a.campus_activity_type,
                    a.max_points, a.active, a.reward_boa, a.reward_bua, u.full_name
                from activities a join users u on u.id = a.teacher_id where a.id = ? for update
                """, eventMapper(), eventId).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Мероприятие не найдено"));
    }

    private void requireEventManager(UserPrincipal principal, EventRow event, UUID groupId) {
        if (event.creatorId.equals(principal.id()) || principal.role() == Role.ORGANIZER) {
            return;
        }
        if (principal.role() != Role.TEACHER) {
            throw ApiException.forbidden("Нет прав на управление мероприятием");
        }
        requireGroupAccess(principal, groupId);
    }

    private void requireGroupRegistrationPermission(UserPrincipal principal, UUID groupId) {
        if (principal.role() == Role.STUDENT) {
            Boolean leader = jdbc.query("""
                    select is_group_leader from student_profiles where user_id = ? and group_id = ?
                    """, (rs, row) -> rs.getBoolean(1), principal.id(), groupId).stream().findFirst().orElse(false);
            if (!leader) {
                throw ApiException.forbidden("Группу может зарегистрировать только её староста");
            }
            return;
        }
        if (principal.role() == Role.ORGANIZER) {
            return;
        }
        if (principal.role() != Role.TEACHER) {
            throw ApiException.forbidden("Нет прав на регистрацию группы");
        }
        requireGroupAccess(principal, groupId);
    }

    private void requireGroupAccess(UserPrincipal principal, UUID groupId) {
        if (principal.role() == Role.ORGANIZER || principal.role() == Role.ACADEMIC_OFFICE) {
            return;
        }
        if (principal.role() == Role.STUDENT) {
            if (count("select count(*) from student_profiles where user_id = ? and group_id = ?",
                    principal.id(), groupId) > 0) {
                return;
            }
            throw ApiException.forbidden("Нет доступа к этой группе");
        }
        if (principal.role() == Role.TEACHER && count("""
                select count(*) from teacher_assignments ta join semesters s on s.id = ta.semester_id
                where ta.teacher_id = ? and ta.group_id = ? and ta.status = 'ACTIVE' and s.is_current = true
                """, principal.id(), groupId) > 0) {
            return;
        }
        throw ApiException.forbidden("Нет доступа к этой группе");
    }

    private void requireGroupManager(UserPrincipal principal, UUID groupId, boolean allowGroupLeader) {
        if (principal.role() == Role.STUDENT) {
            boolean leader = allowGroupLeader && jdbc.query("""
                    select is_group_leader from student_profiles where user_id = ? and group_id = ?
                    """, (rs, row) -> rs.getBoolean(1), principal.id(), groupId).stream()
                    .findFirst().orElse(false);
            if (leader) {
                return;
            }
            throw ApiException.forbidden("Управлять группой могут только назначенный староста или сотрудники");
        }
        requireGroupAccess(principal, groupId);
    }

    private void decideRegistrationRequest(UserPrincipal principal, UUID requestId, boolean approve) {
        boolean exists = !jdbc.query("select 1 from registration_requests where id = ?",
                (rs, row) -> rs.getInt(1), requestId).isEmpty();
        if (!exists) {
            throw ApiException.notFound("Заявка не найдена");
        }
        if (approve) {
            registrationApproval.approve(requestId, principal);
        } else {
            registrationApproval.reject(requestId, principal);
        }
    }

    private List<GroupConfirmationView> registrationConfirmations(UUID groupId) {
        return jdbc.query("""
                select r.id, r.full_name, r.email::text, g.name,
                       coalesce(r.email_verified_at, r.created_at) as submitted_at,
                       case r.status when 'EMAIL_VERIFIED' then 'PENDING' else r.status end as status
                from registration_requests r join study_groups g on g.id = r.group_id
                where r.group_id = ? and r.status in ('EMAIL_VERIFIED', 'APPROVED', 'REJECTED')
                order by submitted_at desc
                """, confirmationMapper(), groupId);
    }

    private List<GroupConfirmationView> confirmationsForGroup(UUID groupId) {
        return jdbc.query("""
                select c.id, u.full_name, u.email::text, g.name, c.submitted_at, c.status
                from campus_group_confirmations c join users u on u.id = c.student_id
                join study_groups g on g.id = c.group_id where c.group_id = ?
                order by c.submitted_at desc, c.id
                """, confirmationMapper(), groupId);
    }

    private void debit(UUID userId, BigDecimal amount, String type) {
        WalletRow wallet = lockWallet(userId, true);
        if (amount.signum() <= 0 || wallet.balance.compareTo(amount) < 0) {
            throw ApiException.badRequest("Недостаточно средств");
        }
        jdbc.update("update wallets set currency_balance = currency_balance - ?, updated_at = now() where id = ?",
                amount, wallet.id);
        jdbc.update("""
                insert into transactions(wallet_id, amount, type, idempotency_key)
                values (?, ?, ?, ?)
                """, wallet.id, amount, type, type.toLowerCase() + ":" + UUID.randomUUID());
    }

    private InventoryItemView insertInventory(UUID userId, String name, String kind, String cosmeticType,
                                              String rarity, String source) {
        Long caseId = jdbc.query("select id from cases where active = true and name = 'Кейс кампуса' order by id limit 1",
                (rs, row) -> rs.getLong(1)).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Каталог кейсов не настроен"));
        Long inventoryId = jdbc.queryForObject("""
                insert into inventory_items(case_id, owner_student_id, item_name, rarity, quantity, item_kind,
                    cosmetic_type, source)
                values (?, ?, ?, ?, 1, ?, ?, ?) returning id
                """, Long.class, caseId, userId, name, databaseRarity(rarity),
                kind, cosmeticType, source);
        return jdbc.query("""
                select id, item_name, item_kind, cosmetic_type, rarity, source, acquired_at
                from inventory_items where id = ?
                """, inventoryMapper(), inventoryId).getFirst();
    }

    private RewardCatalogRow chooseReward(List<RewardCatalogRow> rewards) {
        int totalWeight = rewards.stream().mapToInt(RewardCatalogRow::weight).sum();
        if (totalWeight <= 0) {
            throw ApiException.notFound("Для кейса не настроены награды");
        }
        int ticket = secureRandom.nextInt(totalWeight);
        List<Integer> weights = rewards.stream().map(RewardCatalogRow::weight).toList();
        return rewards.get(CampusRules.weightedIndex(weights, ticket));
    }

    private WalletRow lockWallet(UUID userId, boolean forUpdate) {
        String sql = "select id, currency_balance from wallets where user_id = ?" + (forUpdate ? " for update" : "");
        return jdbc.query(sql, (rs, row) -> new WalletRow(
                rs.getObject("id", UUID.class), rs.getBigDecimal("currency_balance")), userId).stream()
                .findFirst().orElseThrow(() -> ApiException.notFound("Кошелёк не найден"));
    }

    private void ensureWallet(UUID userId) {
        jdbc.update("""
                insert into wallets(user_id, currency_balance) values (?, 0)
                on conflict (user_id) do nothing
                """, userId);
    }

    private boolean premiumOwned(UUID userId) {
        jdbc.update("""
                insert into campus_pass_accounts(user_id, premium_pass_owned) values (?, false)
                on conflict (user_id) do nothing
                """, userId);
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select premium_pass_owned from campus_pass_accounts where user_id = ?", Boolean.class, userId));
    }

    private int academicRating(UUID userId) {
        return jdbc.query("select academic_rating from student_profiles where user_id = ?",
                (rs, row) -> rs.getInt(1), userId).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Профиль студента не найден"));
    }

    private void requireStudentProfile(UUID userId) {
        if (count("select count(*) from student_profiles where user_id = ?", userId) == 0) {
            throw ApiException.notFound("Профиль студента не найден");
        }
    }

    private void validateEvent(CreateEventRequest request) {
        if (request == null || blank(request.title()) || blank(request.location()) || blank(request.category())
                || blank(request.date()) || blank(request.endsAt())) {
            throw ApiException.badRequest("Заполните обязательные поля мероприятия");
        }
        if (request.title().trim().length() > 150 || request.description() != null
                && request.description().length() > 2000 || request.location().trim().length() > 255
                || request.category().trim().length() > 120) {
            throw ApiException.badRequest("Превышена допустимая длина поля мероприятия");
        }
        if (request.capacity() == null || request.capacity() < 1
                || request.maxPoints() == null || request.maxPoints() < 0 || request.maxPoints() > 100) {
            throw ApiException.badRequest("Проверьте вместимость и максимальные баллы");
        }
        if (!"individual".equals(request.format()) && !"group".equals(request.format())) {
            throw ApiException.badRequest("Формат должен быть individual или group");
        }
        if (!"БОА".equals(request.activityType()) && !"БУА".equals(request.activityType())) {
            throw ApiException.badRequest("Тип активности должен быть БОА или БУА");
        }
        if ("group".equals(request.format()) && (request.groupMinPercent() == null
                || request.groupMinPercent() < 0 || request.groupMinPercent() > 100)) {
            throw ApiException.badRequest("Порог участия группы должен быть от 0 до 100");
        }
        if ("individual".equals(request.format()) && request.groupMinPercent() != null
                && (request.groupMinPercent() < 0 || request.groupMinPercent() > 100)) {
            throw ApiException.badRequest("Порог участия должен быть от 0 до 100");
        }
        LocalDateTime start = parseDateTime(request.date(), "date");
        LocalDateTime end = parseDateTime(request.endsAt(), "endsAt");
        if (!end.isAfter(start)) {
            throw ApiException.badRequest("Дата окончания должна быть позже даты начала");
        }
    }

    private void validateReview(ReviewRequest request) {
        if (request == null || request.rating() == null || request.rating() < 1 || request.rating() > 5
                || blank(request.comment()) || request.comment().trim().length() < 20
                || request.comment().trim().length() > 1000) {
            throw ApiException.badRequest("Оценка должна быть от 1 до 5, комментарий — от 20 до 1000 символов");
        }
    }

    private void validateSchedule(ScheduleRequest request) {
        if (request == null || blank(request.weekday()) || blank(request.startTime()) || blank(request.endTime())
                || blank(request.groupId()) || blank(request.subject()) || blank(request.teacherId())
                || blank(request.room())) {
            throw ApiException.badRequest("Заполните все поля расписания");
        }
        if (request.weekday().length() > 24 || request.startTime().length() != 5
                || request.endTime().length() != 5 || request.subject().length() > 160
                || request.room().length() > 80) {
            throw ApiException.badRequest("Превышена допустимая длина поля расписания");
        }
        if (!request.startTime().matches("\\d{2}:\\d{2}") || !request.endTime().matches("\\d{2}:\\d{2}")) {
            throw ApiException.badRequest("Время должно быть в формате HH:mm");
        }
        try {
            LocalTime start = LocalTime.parse(request.startTime());
            LocalTime end = LocalTime.parse(request.endTime());
            if (!end.isAfter(start)) {
                throw ApiException.badRequest("Время окончания должно быть позже времени начала");
            }
        } catch (DateTimeParseException ex) {
            throw ApiException.badRequest("Время должно быть в формате HH:mm");
        }
        if (!List.of("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье",
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                .contains(request.weekday())) {
            throw ApiException.badRequest("Некорректный день недели");
        }
    }

    private boolean validTimeRange(String startValue, String endValue) {
        if (blank(startValue) || blank(endValue) || !startValue.matches("\\d{2}:\\d{2}")
                || !endValue.matches("\\d{2}:\\d{2}")) {
            return false;
        }
        try {
            return LocalTime.parse(endValue).isAfter(LocalTime.parse(startValue));
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private ScheduleView scheduleEntry(UUID id, boolean forUpdate) {
        return jdbc.query("""
                select s.id, s.weekday, s.start_time, s.end_time, s.group_id, g.name, s.subject,
                    s.teacher_id, coalesce(u.full_name, s.teacher) teacher_name, s.room
                from campus_schedule_entries s
                join study_groups g on g.id = s.group_id
                left join users u on u.id = s.teacher_id
                where s.id = ?
                """ + (forUpdate ? " for update of s" : ""), scheduleMapper(), id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Занятие расписания не найдено"));
    }

    private String activeTeacherName(String teacherIdValue) {
        UUID teacherId = parseUuid(teacherIdValue, "teacherId");
        return jdbc.query("""
                select full_name from users where id = ? and role = 'TEACHER' and is_active = true
                """, (rs, row) -> rs.getString(1), teacherId).stream().findFirst()
                .orElseThrow(() -> ApiException.badRequest("Выберите действующего преподавателя из списка"));
    }

    private void ensureTeacherHasNoScheduleOverlap(UUID excludedEntryId, ScheduleRequest request, UUID teacherId) {
        jdbc.query("select id from users where id = ? for update", (rs, row) -> rs.getObject(1, UUID.class), teacherId);
        String sql = """
                select count(*) from campus_schedule_entries
                where teacher_id = ? and weekday = ? and start_time < ? and end_time > ?
                """ + (excludedEntryId == null ? "" : " and id <> ?");
        Integer conflicts = excludedEntryId == null
                ? jdbc.queryForObject(sql, Integer.class, teacherId, request.weekday(), request.endTime(), request.startTime())
                : jdbc.queryForObject(sql, Integer.class, teacherId, request.weekday(), request.endTime(),
                        request.startTime(), excludedEntryId);
        if (conflicts != null && conflicts > 0) {
            throw ApiException.conflict("У преподавателя уже есть занятие в это время");
        }
    }

    private void recordScheduleChange(UUID groupId, UUID scheduleId, String type, String message) {
        jdbc.update("""
                insert into campus_schedule_changes(group_id, schedule_entry_id, change_type, message)
                values (?, ?, ?, ?)
                """, groupId, scheduleId, type, message);
    }

    private String describeSchedule(ScheduleView entry) {
        return "%s, %s %s–%s, %s, %s, ауд. %s".formatted(entry.group(), entry.weekday(),
                entry.startTime(), entry.endTime(), entry.subject(), entry.teacher(), entry.room());
    }

    private void requireRole(UserPrincipal principal, Role... allowed) {
        for (Role role : allowed) {
            if (principal.role() == role) {
                return;
            }
        }
        throw ApiException.forbidden("Недостаточно прав для выполнения операции");
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private String formatMode(String value) {
        return "group".equals(value) ? "COLLECTIVE" : "INDIVIDUAL";
    }

    private String databaseRarity(String rarity) {
        return switch (rarity) {
            case "Редкий" -> "RARE";
            case "Эпический" -> "EPIC";
            default -> "COMMON";
        };
    }

    private LocalDateTime parseDateTime(String value, String field) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toLocalDateTime();
            } catch (DateTimeParseException ex) {
                throw ApiException.badRequest("Некорректное значение " + field);
            }
        }
    }

    private UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("Некорректный идентификатор " + field);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private RowMapper<EventRow> eventMapper() {
        return (rs, row) -> new EventRow(rs.getLong("id"), rs.getObject("teacher_id", UUID.class),
                rs.getString("title"), rs.getTimestamp("starts_at").toLocalDateTime(),
                rs.getTimestamp("ends_at").toLocalDateTime(), rs.getInt("max_participants"),
                rs.getString("participation_mode").equals("COLLECTIVE") ? "GROUP" : "INDIVIDUAL",
                rs.getInt("required_activation_percent"), rs.getString("campus_activity_type"),
                rs.getInt("max_points"), rs.getBoolean("active"), rs.getString("full_name"),
                rs.getBigDecimal("reward_boa"), rs.getBigDecimal("reward_bua"),
                rs.getString("description"), rs.getString("location"), rs.getString("category"));
    }

    private RowMapper<GroupRow> groupMapper() {
        return (rs, row) -> new GroupRow(rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getInt("size"), rs.getObject("leader_id", UUID.class));
    }

    private RowMapper<GroupConfirmationView> confirmationMapper() {
        return (rs, row) -> new GroupConfirmationView(rs.getObject("id", UUID.class).toString(),
                rs.getString("full_name"), rs.getString("email"), rs.getString("name"),
                rs.getTimestamp("submitted_at").toInstant().toString(),
                rs.getString("status").toLowerCase(Locale.ROOT));
    }

    private RowMapper<ScheduleView> scheduleMapper() {
        return (rs, row) -> new ScheduleView(rs.getObject("id", UUID.class).toString(),
                rs.getString("weekday"), rs.getString("start_time"), rs.getString("end_time"),
                rs.getObject("group_id", UUID.class).toString(), rs.getString("name"), rs.getString("subject"),
                rs.getObject("teacher_id", UUID.class) == null ? null : rs.getObject("teacher_id", UUID.class).toString(),
                rs.getString("teacher_name"), rs.getString("room"));
    }

    private RowMapper<InventoryItemView> inventoryMapper() {
        return (rs, row) -> new InventoryItemView(Long.toString(rs.getLong("id")),
                rs.getString("item_name"), rs.getString("item_kind"), rs.getString("cosmetic_type"),
                localRarity(rs.getString("rarity")), rs.getString("source"),
                rs.getTimestamp("acquired_at").toInstant().toString());
    }

    private String localRarity(String value) {
        return switch (value) {
            case "RARE" -> "Редкий";
            case "EPIC" -> "Эпический";
            default -> COMMON_RARITY;
        };
    }

    private record EventRow(long id, UUID creatorId, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                            int capacity, String format, int groupMinPercent, String activityType, int maxPoints,
                            boolean active, String creatorName, BigDecimal rewardBoa, BigDecimal rewardBua,
                            String description, String location, String category) {
    }

    private record ParticipationRow(long id, UUID studentId, UUID groupRegistrationId,
                                    boolean participationConfirmed, boolean awarded, long activityId) {
    }

    private record ReviewEligibility(LocalDateTime endsAt, String participationMode,
                                     int requiredPercent, UUID groupRegistrationId,
                                     boolean participationConfirmed) {
    }

    private record GroupRow(UUID id, String name, int size, UUID leaderId) {
    }

    private record ConfirmationRow(UUID studentId, UUID groupId) {
    }

    private record CaseRow(long id, BigDecimal price) {
    }

    private record CaseInventoryRow(long id, long caseId, int quantity) {
    }

    private record RewardCatalogRow(String name, String cosmeticType, String rarity, int weight) {
    }

    private record WalletRow(UUID id, BigDecimal balance) {
    }

    private record PassRewardClaimRow(UUID id, String track, int level, String title, String kind,
                                      String cosmeticType, String rarity) {
    }
}
