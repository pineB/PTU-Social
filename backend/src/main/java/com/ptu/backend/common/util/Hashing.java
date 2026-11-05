package com.ptu.backend.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class Hashing {

	private static final SecureRandom RANDOM = new SecureRandom();

	private Hashing() {
	}

	public static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	public static boolean constantTimeEquals(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
	}

	public static String randomToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String randomSalt() {
		byte[] bytes = new byte[16];
		RANDOM.nextBytes(bytes);
		return Base64.getEncoder().encodeToString(bytes);
	}
}
