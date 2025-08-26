package com.videoservice.manager.jpa.user;

import com.videoservice.manager.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserJpaEntity {
    @Id
    private String id;

    private String name;

    private String profileImageUrl;

    public static UserJpaEntity from(User user) {
        return new UserJpaEntity(user.getId(), user.getName(), user.getProfileImageUrl());
    }

    public User toDomain() {
        return User.builder()
                .id(this.getId())
                .name(this.getName())
                .profileImageUrl(this.getProfileImageUrl())
                .build();
    }
}
