package com.chatapp.dto;

import com.chatapp.model.ReceiptStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

// Broadcast back to the sender when a receipt changes
@Data
@AllArgsConstructor
public class ReceiptEvent {
    private Long messageId;
    private Long roomId;
    private Long userId;                 // which single recipient just delivered/saw it
    private ReceiptStatus status;        // that ONE recipient's status - do not use this to drive the UI tick in group chats
    private ReceiptStatus aggregateStatus; // the message's overall status across ALL recipients - use this for the UI tick
}