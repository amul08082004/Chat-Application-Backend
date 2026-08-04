package com.chatapp.dto;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.RoomType;
import com.chatapp.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class ChatRoomDTO {
    private Long id;
    private String name;
    private RoomType type;
    private List<UserDTO> members;
    private Instant createdAt;

    public static ChatRoomDTO from(ChatRoom r) {
        List<UserDTO> memberDtos = new ArrayList<>();
        for (User user : r.getMembers()) {
            memberDtos.add(UserDTO.from(user));
        }

        return new ChatRoomDTO(
                r.getId(),
                r.getName(),
                r.getType(),
                memberDtos,
                r.getCreatedAt()
        );
    }
}