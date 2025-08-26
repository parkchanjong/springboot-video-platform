package com.videoservice.manager.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class User {
    private String id;
    private String name;
    private String profileImageUrl;

    public static User defaultUser(String id) {
        return User.builder()
                .id(id)
                .name("")
                .profileImageUrl("")
                .build();
    }
}
