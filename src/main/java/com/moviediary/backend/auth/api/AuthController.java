package com.moviediary.backend.auth.api;

import com.moviediary.backend.security.JwtTokenProvider;
import com.moviediary.backend.user.domain.User;
import com.moviediary.backend.user.dao.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Register/Login API", description = "JWT 기반 인증")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        // 사용자 인증 (Username + Password)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // JWT 토큰 생성
        String token = jwtTokenProvider.generateToken((UserDetails) authentication.getPrincipal());

        return ResponseEntity.ok(token);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username, @RequestParam String email, @RequestParam String password) {
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body("이미 존재하는 사용자입니다.");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));

        userRepository.save(newUser);

        return ResponseEntity.ok("회원가입 성공!");
    }
}
