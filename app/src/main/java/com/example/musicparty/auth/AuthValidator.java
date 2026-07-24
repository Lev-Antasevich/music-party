package com.example.musicparty.auth;

import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Client-side registration / login validation.
 * Password minimum is 6 because Firebase Auth rejects shorter passwords.
 */
public final class AuthValidator {

    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 40;

    private static final Pattern HAS_LETTER = Pattern.compile(".*\\p{L}.*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern EMAIL_STRICT = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private AuthValidator() {
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValidEmail(String email) {
        String normalized = normalizeEmail(email);
        if (TextUtils.isEmpty(normalized) || normalized.length() > 254) {
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) {
            return false;
        }
        return EMAIL_STRICT.matcher(normalized).matches();
    }

    public static boolean isValidDisplayName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.length() >= MIN_NAME_LENGTH && trimmed.length() <= MAX_NAME_LENGTH;
    }

    public static boolean isValidPasswordForLogin(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    /**
     * Modern registration password: length + at least one letter and one digit.
     */
    public static boolean isValidPasswordForRegistration(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        if (password.length() > 128) {
            return false;
        }
        return HAS_LETTER.matcher(password).matches() && HAS_DIGIT.matcher(password).matches();
    }

    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }

    public enum Field {
        NAME,
        EMAIL,
        PASSWORD,
        CONFIRM_PASSWORD
    }

    public static final class ValidationResult {
        public final boolean ok;
        @Nullable
        public final Field field;
        @Nullable
        public final String errorResKey;

        private ValidationResult(boolean ok, @Nullable Field field, @Nullable String errorResKey) {
            this.ok = ok;
            this.field = field;
            this.errorResKey = errorResKey;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult error(Field field, String errorResKey) {
            return new ValidationResult(false, field, errorResKey);
        }
    }

    public static ValidationResult validateLogin(String email, String password) {
        if (!isValidEmail(email)) {
            return ValidationResult.error(Field.EMAIL, "invalid_email");
        }
        if (!isValidPasswordForLogin(password)) {
            return ValidationResult.error(Field.PASSWORD, "invalid_password");
        }
        return ValidationResult.success();
    }

    public static ValidationResult validateRegistration(
            String email,
            String password,
            String confirmPassword
    ) {
        if (!isValidEmail(email)) {
            return ValidationResult.error(Field.EMAIL, "invalid_email");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return ValidationResult.error(Field.PASSWORD, "invalid_password");
        }
        if (!isValidPasswordForRegistration(password)) {
            return ValidationResult.error(Field.PASSWORD, "weak_password");
        }
        if (!passwordsMatch(password, confirmPassword)) {
            return ValidationResult.error(Field.CONFIRM_PASSWORD, "passwords_mismatch");
        }
        return ValidationResult.success();
    }

    public static ValidationResult validateGuestName(String name) {
        if (!isValidDisplayName(name)) {
            return ValidationResult.error(Field.NAME, "invalid_name");
        }
        return ValidationResult.success();
    }

    public static boolean isValidRoomName(String roomName) {
        String trimmed = roomName == null ? "" : roomName.trim();
        return trimmed.length() >= MIN_NAME_LENGTH && trimmed.length() <= MAX_NAME_LENGTH;
    }
}
