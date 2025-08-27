package com.videoservice.manager.jpa.subscribe;

import com.videoservice.manager.jpa.channel.ChannelJpaEntity;
import com.videoservice.manager.jpa.user.UserJpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscribe")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SubscribeJpaEntity {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "channel_id")
    private ChannelJpaEntity channel;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;
}
