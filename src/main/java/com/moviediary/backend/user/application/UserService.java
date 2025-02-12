package com.moviediary.backend.user.application;

import com.moviediary.backend.user.dao.UserRepository;
import com.moviediary.backend.user.domain.Role;
import com.moviediary.backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 🔹 회원가입 (비밀번호 암호화 후 저장)
     */
    @Transactional
    public void registerUser(String username, String email, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("이미 존재하는 사용자명입니다.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }

        String encryptedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(encryptedPassword)
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }

    /**
     * 🔹 로그인 (아이디 & 비밀번호 검증)
     */
    public Optional<User> login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return user;
        }
        return Optional.empty();
    }
}
