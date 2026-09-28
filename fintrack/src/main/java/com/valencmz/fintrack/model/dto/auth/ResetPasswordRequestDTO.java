package com.valencmz.fintrack.model.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    private String newPassword;
    private String resetToken;

    public ResetPasswordRequestDTO() {
    }

    public ResetPasswordRequestDTO(String newPassword, String resetToken) {
        this.newPassword = newPassword;
        this.resetToken = resetToken;
    }
}