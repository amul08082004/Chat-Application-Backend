

package com.chatapp.controller;

import com.chatapp.dto.MessageDTO;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.security.UserPrincipal;
import com.chatapp.service.ChatRoomService;
import com.chatapp.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;

    /** Paginated message history, newest first. Also marks everything as SEEN (you opened the chat). */
    @GetMapping
    public ResponseEntity<Page<MessageDTO>> history(@AuthenticationPrincipal UserPrincipal principal,
                                                      @PathVariable Long roomId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "30") int size) {
        ChatRoom room = chatRoomService.getById(roomId);
        if (!chatRoomService.isMember(room, principal.getId())) {
            return ResponseEntity.status(403).build();
        }

        Page<Message> messages = messageService.getHistory(roomId, page, size);

        // convert each Message to a MessageDTO with a plain loop
        List<MessageDTO> dtoList = new ArrayList<>();
        for (Message m : messages.getContent()) {
            MessageDTO dto = MessageDTO.from(m, messageService.aggregateStatus(m));
            dtoList.add(dto);
        }

        Page<MessageDTO> dtoPage = new PageImpl<>(dtoList, messages.getPageable(), messages.getTotalElements());

        // opening the history for page 0 = the user has now seen everything up to this point
        if (page == 0) {
            messageService.markAllSeenInRoom(roomId, principal.getId());
        }

        return ResponseEntity.ok(dtoPage);
    }
}