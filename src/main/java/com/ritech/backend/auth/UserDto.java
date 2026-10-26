package com.ritech.backend.auth;

import com.ritech.backend.user.Role;
import com.ritech.backend.user.User;

public record UserDto(Integer id, String name, String email, Role role) {

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
