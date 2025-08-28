package com.videoservice.manager.mongo.comment;

import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.query.spi.Limit;
import org.springframework.data.repository.CrudRepository;

public interface CommentMongoRepository extends CrudRepository<CommentDocument, String> {
    List<CommentDocument> findAllByVideoIdAndParentIdAndPublishedAtLessThanEqualOrderByPublishedAtDesc(String videoId, String parentId, LocalDateTime offset, Limit limit);

    List<CommentDocument> findAllByParentIdAndPublishedAtLessThanEqualOrderByPublishedAtDesc(String parentId, LocalDateTime offset, Limit limit);
}
