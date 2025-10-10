package com.codelab.application;

import com.codelab.domain.User;
import com.codelab.domain.repository.UserRepository;
import com.codelab.infrastructure.security.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordUtils passwordUtils;

    @Transactional
    public void register(String username, String rawPassword, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        
        String salt = passwordUtils.generateSalt();
        String hashedPassword = passwordUtils.hashPassword(rawPassword, salt);
        
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(hashedPassword);
        u.setPasswordSalt(salt);
        u.setEmail(email);
        u.setRole("ROLE_USER");
        u.setEnabled(true);
        userRepository.save(u);
    }

    public User authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        
        User user = userOpt.get();
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("账户已被禁用");
        }
        
        if (!passwordUtils.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        
        return user;
    }
}


