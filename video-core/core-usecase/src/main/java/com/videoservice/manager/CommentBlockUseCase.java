package com.videoservice.manager;

import com.videoservice.manager.user.User;

public interface CommentBlockUseCase {
    void blockComment(User user, String commentId);
}
