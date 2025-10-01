package com.videoservice.manager.jpa.video;

import java.util.List;

public interface VideoCustomRepository {

    List<VideoJpaEntity> findByChannelId(String channelId);
}
