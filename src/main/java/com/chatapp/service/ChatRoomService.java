package com.chatapp.service;

import com.chatapp.dto.CreateGroupRequest;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.RoomType;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserService userService;

    public List<ChatRoom> getRoomsForUser(Long userId) {
        return chatRoomRepository.findAllByMemberId(userId);
    }

    public ChatRoom getById(Long roomId) {
        Optional<ChatRoom> room = chatRoomRepository.findById(roomId);
        if (room.isPresent()) {
            return room.get();
        }
        throw new IllegalArgumentException("Room not found: " + roomId);
    }

    public boolean isMember(ChatRoom room, Long userId) {
        for (User member : room.getMembers()) {
            if (member.getId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public ChatRoom getOrCreateOneToOneRoom(Long userAId, Long userBId) {
        Optional<ChatRoom> existingRoom = chatRoomRepository.findOneToOneRoom(userAId, userBId);

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        User a = userService.getById(userAId);
        User b = userService.getById(userBId);

        Set<User> members = new HashSet<>();
        members.add(a);
        members.add(b);

        ChatRoom room = ChatRoom.builder()
                .type(RoomType.ONE_TO_ONE)
                .createdBy(a)
                .members(members)
                .build();

        return chatRoomRepository.save(room);
    }

    @Transactional
    public ChatRoom createGroup(Long creatorId, CreateGroupRequest request) {
        User creator = userService.getById(creatorId);

        Set<User> members = new HashSet<>();
        for (Long memberId : request.getMemberIds()) {
            members.add(userService.getById(memberId));
        }
        members.add(creator);

        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .type(RoomType.GROUP)
                .createdBy(creator)
                .members(members)
                .build();

        return chatRoomRepository.save(room);
    }

    @Transactional
    public ChatRoom addMember(Long roomId, Long newMemberId) {
        ChatRoom room = getById(roomId);
        if (room.getType() != RoomType.GROUP) {
            throw new IllegalStateException("Cannot add members to a 1-1 chat");
        }
        room.getMembers().add(userService.getById(newMemberId));
        return chatRoomRepository.save(room);
    }
}