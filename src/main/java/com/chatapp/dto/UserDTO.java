package com.chatapp.dto;

import com.chatapp.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private boolean online;
    private Instant lastSeen;

    public static UserDTO from(User u) {
        return new UserDTO(u.getId(), u.getUsername(), u.getFullName(), u.getAvatarUrl(), u.isOnline(), u.getLastSeen());
    }
}
