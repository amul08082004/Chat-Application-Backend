package com.chatapp.repository;

import com.chatapp.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query(value = """
           SELECT r.*
           FROM chat_rooms r
           JOIN chat_room_members crm ON crm.room_id = r.id
           WHERE crm.user_id = :userId
           """, nativeQuery = true)
    List<ChatRoom> findAllByMemberId(@Param("userId") Long userId);

    @Query(value = """
           SELECT r.*
           FROM chat_rooms r
           WHERE r.type = 'ONE_TO_ONE'
             AND EXISTS (SELECT 1 FROM chat_room_members WHERE room_id = r.id AND user_id = :userA)
             AND EXISTS (SELECT 1 FROM chat_room_members WHERE room_id = r.id AND user_id = :userB)
             AND (SELECT COUNT(*) FROM chat_room_members WHERE room_id = r.id) = 2
           """, nativeQuery = true)
    Optional<ChatRoom> findOneToOneRoom(@Param("userA") Long userA, @Param("userB") Long userB);
}