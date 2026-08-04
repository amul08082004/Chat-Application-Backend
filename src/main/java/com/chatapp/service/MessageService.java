package com.chatapp.service;

import com.chatapp.model.*;
import com.chatapp.repository.MessageReceiptRepository;
import com.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageReceiptRepository receiptRepository;

    /**
     * Persists a message and creates one receipt per recipient (every room member
     * except the sender). Recipients who are currently online are marked DELIVERED
     * immediately (their client will get it pushed live); offline recipients start at SENT
     * and get reconciled to DELIVERED when they reconnect.
     */
    @Transactional
    public Message sendMessage(ChatRoom room, User sender, String content) {
        Message message = Message.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .createdAt(Instant.now())
                .build();
        message = messageRepository.save(message);

        for (User member : room.getMembers()) {
            if (member.getId().equals(sender.getId())) continue;

            ReceiptStatus initialStatus = member.isOnline() ? ReceiptStatus.DELIVERED : ReceiptStatus.SENT;
            MessageReceipt receipt = MessageReceipt.builder()
                    .message(message)
                    .user(member)
                    .status(initialStatus)
                    .deliveredAt(member.isOnline() ? Instant.now() : null)
                    .build();
            receiptRepository.save(receipt);
        }

        return message;
    }

    @Transactional
    public MessageReceipt markDelivered(Long messageId, Long userId) {
        MessageReceipt receipt = receiptRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        if (receipt.getStatus() == ReceiptStatus.SENT) {
            receipt.setStatus(ReceiptStatus.DELIVERED);
            receipt.setDeliveredAt(Instant.now());
            receiptRepository.save(receipt);
        }
        return receipt;
    }

    @Transactional
    public MessageReceipt markSeen(Long messageId, Long userId) {
        MessageReceipt receipt = receiptRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        receipt.setStatus(ReceiptStatus.SEEN);
        receipt.setSeenAt(Instant.now());
        if (receipt.getDeliveredAt() == null) receipt.setDeliveredAt(Instant.now());
        receiptRepository.save(receipt);
        return receipt;
    }

    /** Marks every not-yet-seen message in a room as SEEN for this user (opening a chat). */
    @Transactional
    public List<MessageReceipt> markAllSeenInRoom(Long roomId, Long userId) {
        List<MessageReceipt> pending = receiptRepository.findUnseenInRoomForUser(roomId, userId);
        Instant now = Instant.now();
        for (MessageReceipt r : pending) {
            r.setStatus(ReceiptStatus.SEEN);
            r.setSeenAt(now);
            if (r.getDeliveredAt() == null) r.setDeliveredAt(now);
        }
        receiptRepository.saveAll(pending);
        return pending;
    }

    /** Called when a user reconnects: flips their pending SENT receipts to DELIVERED. */
    @Transactional
    public List<MessageReceipt> reconcileDeliveryOnReconnect(Long userId) {
        List<MessageReceipt> pending = receiptRepository.findPendingDeliveryForUser(userId);
        Instant now = Instant.now();
        for (MessageReceipt r : pending) {
            r.setStatus(ReceiptStatus.DELIVERED);
            r.setDeliveredAt(now);
        }
        receiptRepository.saveAll(pending);
        return pending;
    }

    public Page<Message> getHistory(Long roomId, int page, int size) {
        return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(page, size));
    }

    /** Aggregate status shown to the sender: SEEN if everyone saw it, DELIVERED if everyone got it, else SENT. */
    public ReceiptStatus aggregateStatus(Message message) {
        List<MessageReceipt> receipts = receiptRepository.findByMessageId(message.getId());

        if (receipts.isEmpty()) {
            return ReceiptStatus.SENT;
        }

        int seenCount = 0;
        int deliveredOrBetterCount = 0;

        for (MessageReceipt r : receipts) {
            if (r.getStatus() == ReceiptStatus.SEEN) {
                seenCount++;
                deliveredOrBetterCount++;
            } else if (r.getStatus() == ReceiptStatus.DELIVERED) {
                deliveredOrBetterCount++;
            }
        }

        if (seenCount == receipts.size()) {
            return ReceiptStatus.SEEN;
        }
        if (deliveredOrBetterCount == receipts.size()) {
            return ReceiptStatus.DELIVERED;
        }
        return ReceiptStatus.SENT;
    }
}