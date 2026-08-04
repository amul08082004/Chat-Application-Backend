package com.chatapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

// Payload for /app/chat.delivered and /app/chat.seen
@Data
public class ReceiptUpdateRequest {
    @NotNull
    private Long messageId;

    // used for the "seen" bulk case: mark all messages in a room as seen
    private Long roomId;
}
