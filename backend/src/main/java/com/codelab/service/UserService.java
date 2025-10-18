package com.codelab.service;

import com.codelab.domain.User;
import com.codelab.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getUsername().equals(username)) // 精确匹配大小写
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public void updateUser(User currentUser) {
        userRepository.save(currentUser);
    }
}
