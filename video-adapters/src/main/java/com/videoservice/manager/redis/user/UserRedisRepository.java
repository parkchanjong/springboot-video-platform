package com.videoservice.manager.redis.user;

import org.springframework.data.repository.CrudRepository;

public interface UserRedisRepository extends CrudRepository<UserRedisHash, String> {
}
