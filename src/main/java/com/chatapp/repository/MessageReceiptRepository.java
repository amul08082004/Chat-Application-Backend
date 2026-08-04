package com.chatapp.repository;

import com.chatapp.model.MessageReceipt;
import com.chatapp.model.ReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageReceiptRepository extends JpaRepository<MessageReceipt, Long> {

    Optional<MessageReceipt> findByMessageIdAndUserId(Long messageId, Long userId);

    List<MessageReceipt> findByMessageId(Long messageId);

    // all undelivered receipts for a user who just came online, across all their rooms
    @Query(value = """
           SELECT * FROM message_receipts
           WHERE user_id = :userId AND status = 'SENT'
           """, nativeQuery = true)
    List<MessageReceipt> findPendingDeliveryForUser(@Param("userId") Long userId);

  @Query(value = """
           SELECT * FROM message_receipts
           WHERE user_id = :userId
             AND status != 'SEEN'
             AND message_id IN (SELECT id FROM messages WHERE room_id = :roomId)
           """, nativeQuery = true)
    List<MessageReceipt> findUnseenInRoomForUser(@Param("roomId") Long roomId, @Param("userId") Long userId);

    long countByMessageIdAndStatus(Long messageId, ReceiptStatus status);
}