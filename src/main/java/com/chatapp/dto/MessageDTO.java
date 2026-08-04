package com.chatapp.dto;

import com.chatapp.model.Message;
import com.chatapp.model.ReceiptStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private Instant createdAt;
    // aggregate status across all recipients: SENT / DELIVERED / SEEN
    private ReceiptStatus status;

    public static MessageDTO from(Message m, ReceiptStatus aggregateStatus) {
        return new MessageDTO(
                m.getId(),
                m.getRoom().getId(),
                m.getSender().getId(),
                m.getSender().getUsername(),
                m.getContent(),
                m.getCreatedAt(),
                aggregateStatus
        );
    }
}
