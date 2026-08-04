package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Payload the client sends over STOMP to /app/chat.send
@Data
public class SendMessageRequest {
    @NotNull
    private Long roomId;

    @NotBlank
    private String content;
}
