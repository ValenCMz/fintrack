package com.valencmz.fintrack.model.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterResponseDTO {
    private String msg;

    public RegisterResponseDTO(String msg) {
        this.msg = msg;
    }
}
