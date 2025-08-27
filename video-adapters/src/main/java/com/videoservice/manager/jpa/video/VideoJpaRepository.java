package com.videoservice.manager.jpa.video;

import org.springframework.data.repository.CrudRepository;

public interface VideoJpaRepository extends CrudRepository<VideoJpaEntity, String> {

}
