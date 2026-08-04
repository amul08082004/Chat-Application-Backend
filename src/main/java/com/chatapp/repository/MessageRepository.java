package com.chatapp.repository;

import com.chatapp.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
}
