package com.videoservice.manager;

import com.videoservice.manager.user.User;
import org.springframework.stereotype.Service;

@Service
public class CommentBlockService implements CommentBlockUseCase {
    private final CommentBlockPort commentBlockPort;

    public CommentBlockService(CommentBlockPort commentBlockPort) {
        this.commentBlockPort = commentBlockPort;
    }

    @Override
    public void blockComment(User user, String commentId) {
        commentBlockPort.saveUserCommentBlock(user.getId(), commentId);
    }
}
