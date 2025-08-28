package com.videoservice.manager;

import com.videoservice.manager.command.CommentCommand;
import com.videoservice.manager.comment.Comment;
import com.videoservice.manager.comment.CommentResponse;
import com.videoservice.manager.user.User;
import java.util.List;

public interface CommentUseCase  {
    Comment createComment(User user, CommentCommand commentCommand);

    Comment updateComment(String commentId, User user, CommentCommand commentCommand);

    void deleteComment(String commentId, User user);

    CommentResponse getComment(String commentId);

    List<CommentResponse> listComments(String videoId, String order, String offset, Integer maxSize);

    List<CommentResponse> listComments(User user, String videoId, String order, String offset, Integer maxSize);

    List<CommentResponse> listReplies(String parentId, String offset, Integer maxSize);
}
