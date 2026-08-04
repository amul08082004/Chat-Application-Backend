package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {
    @NotBlank
    private String name;

    @NotEmpty
    private List<Long> memberIds; // other members besides the creator
}
