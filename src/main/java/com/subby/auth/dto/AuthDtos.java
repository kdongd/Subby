package com.subby.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    /** 8~64자, 영문과 숫자를 각각 1자 이상 포함 */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_MESSAGE = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.";

    private AuthDtos() {
    }

    public record SignupRequest(
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @Size(max = 255, message = "이메일이 너무 깁니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE)
            String password,

            @NotBlank(message = "닉네임을 입력해 주세요.")
            @Size(max = 30, message = "닉네임은 30자 이하로 입력해 주세요.")
            String nickname
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Size(max = 255, message = "이메일이 너무 깁니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            @Size(max = 128, message = "비밀번호가 너무 깁니다.")
            String password
    ) {
    }

    public record PasswordResetRequest(
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @Size(max = 255, message = "이메일이 너무 깁니다.")
            String email
    ) {
    }

    public record PasswordResetConfirm(
            @NotBlank(message = "링크가 올바르지 않습니다.") String token,

            @NotBlank(message = "새 비밀번호를 입력해 주세요.")
            @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE)
            String newPassword
    ) {
    }
}
