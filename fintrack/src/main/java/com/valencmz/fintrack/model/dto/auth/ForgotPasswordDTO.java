package com.valencmz.fintrack.model.dto.auth;

import lombok.Data;

@Data
public class ForgotPasswordDTO {
    private String email;

    public ForgotPasswordDTO() {
    }

    public ForgotPasswordDTO(String email) {
        this.email = email;
    }
}
