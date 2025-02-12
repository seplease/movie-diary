package com.moviediary.backend.auth.dto;

import lombok.Getter;
import lombok.Setter;

public class AuthRequest {

    @Getter @Setter
    public static class Register {
        private String username;
        private String email;
        private String password;
    }

    @Getter @Setter
    public static class Login {
        private String username;
        private String password;
    }
}
