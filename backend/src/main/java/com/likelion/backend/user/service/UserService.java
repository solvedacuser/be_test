package com.likelion.backend.user.service;

import com.likelion.backend.global.error.BusinessException;
import com.likelion.backend.global.error.ErrorCode;
import com.likelion.backend.user.domain.Role;
import com.likelion.backend.user.domain.User;
import com.likelion.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            log.debug("Signup failed due to duplicated email: {}", email);
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        log.debug("Signup succeeded for userId: {}", savedUser.getUserId());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));
    }
}
