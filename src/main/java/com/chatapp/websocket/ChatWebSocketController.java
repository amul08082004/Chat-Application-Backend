package com.chatapp.websocket;

import com.chatapp.dto.*;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.MessageReceipt;
import com.chatapp.model.ReceiptStatus;
import com.chatapp.model.User;
import com.chatapp.service.ChatRoomService;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void send(@Payload SendMessageRequest request, Principal principal) {
        if (principal == null) return;
        User sender = userService.getByUsername(principal.getName());
        ChatRoom room = chatRoomService.getById(request.getRoomId());

        if (!chatRoomService.isMember(room, sender.getId())) {
            messagingTemplate.convertAndSendToUser(sender.getUsername(), "/queue/errors",
                    "You are not a member of room " + room.getId());
            return;
        }

        Message saved = messageService.sendMessage(room, sender, request.getContent());
        MessageDTO dto = MessageDTO.from(saved, messageService.aggregateStatus(saved));

        messagingTemplate.convertAndSend("/topic/room." + room.getId(), dto);
    }

    @MessageMapping("/chat.delivered")
    public void delivered(@Payload ReceiptUpdateRequest request, Principal principal) {
        if (principal == null) return;
        User user = userService.getByUsername(principal.getName());
        MessageReceipt receipt = messageService.markDelivered(request.getMessageId(), user.getId());  
        broadcastReceipt(receipt, user);
    }

    @MessageMapping("/chat.seen")
    public void seen(@Payload ReceiptUpdateRequest request, Principal principal) {
        if (principal == null) return;
        User user = userService.getByUsername(principal.getName());

        if (request.getRoomId() != null) {
            for (MessageReceipt r : messageService.markAllSeenInRoom(request.getRoomId(), user.getId())) {
                broadcastReceipt(r, user);
            }
        } else if (request.getMessageId() != null) {
            MessageReceipt r = messageService.markSeen(request.getMessageId(), user.getId());
            broadcastReceipt(r, user);
        }
    }

    private void broadcastReceipt(MessageReceipt receipt, User byUser) {
        // recompute the message's overall status NOW, after this receipt just changed
        ReceiptStatus aggregate = messageService.aggregateStatus(receipt.getMessage());

        ReceiptEvent event = new ReceiptEvent(
                receipt.getMessage().getId(),
                receipt.getMessage().getRoom().getId(),
                byUser.getId(),
                receipt.getStatus(),
                aggregate
        );

        String originalSenderUsername = receipt.getMessage().getSender().getUsername();
        messagingTemplate.convertAndSendToUser(originalSenderUsername, "/queue/receipts", event);
    }
}