package com.videoservice.manager;

import com.videoservice.manager.domain.user.UserFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommentBlockServiceTest {
    private CommentBlockService sut;

    private final CommentBlockPort commentBlockPort = mock(CommentBlockPort.class);

    @BeforeEach
    void setUp() {
        sut = new CommentBlockService(commentBlockPort);
    }

    @Test
    void testBlockComment() {
        var user = UserFixtures.stub();
        var commentId = "commentId";

        sut.blockComment(user, commentId);

        verify(commentBlockPort).saveUserCommentBlock(user.getId(), commentId);
    }
}