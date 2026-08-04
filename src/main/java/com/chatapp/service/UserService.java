package com.chatapp.service;

import com.chatapp.dto.UserDTO;
import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserDTO> listOtherUsers(Long currentUserId) {
        return userRepository.findByIdNot(currentUserId).stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional
    public void setOnline(String username, boolean online) {
        User user = getByUsername(username);
        user.setOnline(online);
        if (!online) {
            user.setLastSeen(Instant.now());
        }
        userRepository.save(user);
    }
}
