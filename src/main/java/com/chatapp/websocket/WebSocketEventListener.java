package com.chatapp.websocket;

import com.chatapp.dto.MessageDTO;
import com.chatapp.dto.PresenceEvent;
import com.chatapp.dto.ReceiptEvent;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.MessageReceipt;
import com.chatapp.model.ReceiptStatus;
import com.chatapp.model.User;
import com.chatapp.service.ChatRoomService;
import com.chatapp.service.MessageService;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

/**
 * Drives the online/offline presence feature. A user is considered "online" for
 * as long as they have at least one live STOMP session; we mark them offline the
 * moment their (last) session disconnects and stamp lastSeen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserService userService;
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;

        String username = principal.getName();
        userService.setOnline(username, true);
        User user = userService.getByUsername(username);

        // flip any pending SENT receipts for this user to DELIVERED, and notify each original sender
        List<MessageReceipt> reconciled = messageService.reconcileDeliveryOnReconnect(user.getId());
        for (MessageReceipt r : reconciled) {
            ReceiptStatus aggregate = messageService.aggregateStatus(r.getMessage());
            ReceiptEvent re = new ReceiptEvent(
                    r.getMessage().getId(),
                    r.getMessage().getRoom().getId(),
                    user.getId(),
                    r.getStatus(),
                    aggregate
            );
            String originalSenderUsername = r.getMessage().getSender().getUsername();
            messagingTemplate.convertAndSendToUser(originalSenderUsername, "/queue/receipts", re);
        }

        broadcastPresence(user, true);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) return;

        String username = principal.getName();
        userService.setOnline(username, false);
        User user = userService.getByUsername(username);

        broadcastPresence(user, false);
    }

    /** Notify every room this user belongs to that their presence changed. */
    private void broadcastPresence(User user, boolean online) {
        PresenceEvent event = new PresenceEvent(user.getId(), user.getUsername(), online, user.getLastSeen());
        for (ChatRoom room : chatRoomService.getRoomsForUser(user.getId())) {
            messagingTemplate.convertAndSend("/topic/room." + room.getId() + ".presence", event);
        }
        // also a global presence topic, handy for a contacts/user list screen
        messagingTemplate.convertAndSend("/topic/presence", event);
    }
}