package com.maggu.maggu.user.service;

import com.maggu.maggu.community.repository.CommentLikeRepository;
import com.maggu.maggu.community.repository.CommentRepository;
import com.maggu.maggu.community.repository.FolderRepository;
import com.maggu.maggu.community.repository.PostStickerReactionRepository;
import com.maggu.maggu.community.repository.ReportRepository;
import com.maggu.maggu.community.repository.ScrapRepository;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.sticker.repository.StickerRepository;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private PostStickerReactionRepository postStickerReactionRepository;
    @Mock
    private ScrapRepository scrapRepository;
    @Mock
    private FolderRepository folderRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private StickerRepository stickerRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserWithdrawalService userWithdrawalService;

    @Test
    @DisplayName("연관 데이터를 정리한 뒤 회원 행을 삭제한다")
    void deletesAccountAfterDetachingRelatedData() {
        AppUser user = AppUser.builder()
                .provider(Provider.APPLE)
                .providerUserId("apple-user-1")
                .email("user@test.com")
                .nickname("시진")
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        userWithdrawalService.deleteAccount(user);

        InOrder inOrder = inOrder(
                commentLikeRepository, postStickerReactionRepository, scrapRepository,
                folderRepository, reportRepository, commentRepository, stickerRepository,
                postRepository, userRepository
        );
        inOrder.verify(commentLikeRepository).deleteByUser(user);
        inOrder.verify(postStickerReactionRepository).deleteByUser(user);
        inOrder.verify(scrapRepository).deleteByUser(user);
        inOrder.verify(folderRepository).deleteByUser(user);
        inOrder.verify(reportRepository).deleteByReporter(user);
        inOrder.verify(commentRepository).detachUser(10L);
        inOrder.verify(stickerRepository).detachAndMarkDeletedByUserId(10L);
        inOrder.verify(postRepository).detachUser(10L);
        inOrder.verify(userRepository).deleteById(10L);
    }
}
