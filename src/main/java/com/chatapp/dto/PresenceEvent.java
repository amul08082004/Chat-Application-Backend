package com.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class PresenceEvent {
    private Long userId;
    private String username;
    private boolean online;
    private Instant lastSeen;
}
