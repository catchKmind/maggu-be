package com.maggu.maggu.user.service;

import com.maggu.maggu.community.repository.CommentLikeRepository;
import com.maggu.maggu.community.repository.CommentRepository;
import com.maggu.maggu.community.repository.FolderRepository;
import com.maggu.maggu.community.repository.PostStickerReactionRepository;
import com.maggu.maggu.community.repository.ReportRepository;
import com.maggu.maggu.community.repository.ScrapRepository;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.sticker.repository.StickerRepository;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final CommentLikeRepository commentLikeRepository;
    private final PostStickerReactionRepository postStickerReactionRepository;
    private final ScrapRepository scrapRepository;
    private final FolderRepository folderRepository;
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final StickerRepository stickerRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void deleteAccount(AppUser user) {
        Long userId = user.getId();

        commentLikeRepository.deleteByUser(user);
        postStickerReactionRepository.deleteByUser(user);
        scrapRepository.deleteByUser(user);
        folderRepository.deleteByUser(user);
        reportRepository.deleteByReporter(user);

        commentRepository.detachUser(userId);
        stickerRepository.detachAndMarkDeletedByUserId(userId);
        postRepository.detachUser(userId);

        userRepository.deleteById(userId);
    }
}
