package com.videoservice.manager.jpa.video;

import static com.videoservice.manager.jpa.video.QVideoJpaEntity.videoJpaEntity;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class VideoCustomRepositoryImpl implements VideoCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public VideoCustomRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<VideoJpaEntity> findByChannelId(String channelId) {
        return jpaQueryFactory.selectFrom(videoJpaEntity)
                .where(videoJpaEntity.channelId.eq(channelId))
                .fetch();
    }
}
