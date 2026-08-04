package com.chatapp.controller;

import com.chatapp.dto.ChatRoomDTO;
import com.chatapp.dto.CreateGroupRequest;
import com.chatapp.model.ChatRoom;
import com.chatapp.security.UserPrincipal;
import com.chatapp.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /** All rooms (1-1 and group) the current user belongs to. */
    @GetMapping
    public List<ChatRoomDTO> myRooms(@AuthenticationPrincipal UserPrincipal principal) {
        List<ChatRoom> rooms = chatRoomService.getRoomsForUser(principal.getId());

        List<ChatRoomDTO> result = new ArrayList<>();
        for (ChatRoom room : rooms) {
            result.add(ChatRoomDTO.from(room));
        }
        return result;
    }

    /** Get-or-create a 1-1 room with another user. Idempotent - safe to call every time you open a DM. */
    @PostMapping("/one-to-one/{otherUserId}")
    public ChatRoomDTO oneToOne(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long otherUserId) {
        ChatRoom room = chatRoomService.getOrCreateOneToOneRoom(principal.getId(), otherUserId);
        return ChatRoomDTO.from(room);
    }

    @PostMapping("/group")
    public ChatRoomDTO createGroup(@AuthenticationPrincipal UserPrincipal principal,
                                    @Valid @RequestBody CreateGroupRequest request) {
        ChatRoom room = chatRoomService.createGroup(principal.getId(), request);
        return ChatRoomDTO.from(room);
    }

    @PostMapping("/{roomId}/members/{userId}")
    public ChatRoomDTO addMember(@AuthenticationPrincipal UserPrincipal principal,
                                  @PathVariable Long roomId, @PathVariable Long userId) {
        ChatRoom room = chatRoomService.getById(roomId);
        if (!chatRoomService.isMember(room, principal.getId())) {
            throw new IllegalStateException("Not a member of this room");
        }
        return ChatRoomDTO.from(chatRoomService.addMember(roomId, userId));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomDTO> getRoom(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable Long roomId) {
        ChatRoom room = chatRoomService.getById(roomId);
        if (!chatRoomService.isMember(room, principal.getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(ChatRoomDTO.from(room));
    }
}