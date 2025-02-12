package com.moviediary.backend.user.dao;

import com.moviediary.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // ✅ 존재 여부를 확인하는 메서드 추가
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
