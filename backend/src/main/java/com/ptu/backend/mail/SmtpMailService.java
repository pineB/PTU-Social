package com.ptu.backend.mail;

import java.time.Duration;

import com.ptu.backend.common.exception.ApiException;
import com.ptu.backend.config.AppProperties;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SmtpMailService implements MailService {

	private final JavaMailSender mailSender;
	private final String from;

	public SmtpMailService(JavaMailSender mailSender, AppProperties properties) {
		this.mailSender = mailSender;
		this.from = properties.mailFrom();
	}

	@Override
	public void sendVerificationCode(String email, String fullName, String code, Duration ttl) {
		String text = """
				Здравствуйте, %s!

				Код подтверждения регистрации: %s
				Код действует %d минут и может быть использован только один раз.
				""".formatted(fullName, code, ttl.toMinutes());
		send(email, "Подтверждение email — ПТУ-Сошиал", text);
	}

	@Override
	public void sendTeacherInvite(String email, String fullName, String inviteLink, Duration ttl) {
		String text = """
				Здравствуйте, %s!

				Учебная часть приглашает вас в систему «ПТУ-Сошиал» как преподавателя.
				Перейдите по ссылке, чтобы установить пароль: %s
				Ссылка действует %d дней.
				""".formatted(fullName, inviteLink, ttl.toDays());
		send(email, "Приглашение преподавателя — ПТУ-Сошиал", text);
	}

	@Override
	public void sendPasswordResetCode(String email, String fullName, String code, Duration ttl) {
		String text = """
				Здравствуйте, %s!

				Код для сброса пароля: %s
				Код действует %d минут и может быть использован только один раз.
				Если вы не запрашивали сброс пароля, просто проигнорируйте письмо.
				""".formatted(fullName, code, ttl.toMinutes());
		send(email, "Сброс пароля — ПТУ-Сошиал", text);
	}

	private void send(String email, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(email);
		message.setSubject(subject);
		message.setText(text);
		try {
			mailSender.send(message);
		}
		catch (MailException ex) {
			log.error("Failed to send email to {}", email, ex);
			throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
					"Не удалось отправить письмо, попробуйте позже");
		}
	}
}
