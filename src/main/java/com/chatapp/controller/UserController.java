package com.chatapp.controller;

import com.chatapp.dto.UserDTO;
import com.chatapp.model.User;
import com.chatapp.security.UserPrincipal;
import com.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** All other users, with online/offline status - useful for a "start new chat" screen. */
    @GetMapping
    public List<UserDTO> listUsers(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.listOtherUsers(principal.getId());
    }

    @GetMapping("/me")
    public UserDTO me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userService.getById(principal.getId());
        return UserDTO.from(user);
    }
}
