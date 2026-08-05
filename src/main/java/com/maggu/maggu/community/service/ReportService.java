package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.request.ReportCreateRequest;
import com.maggu.maggu.community.dto.response.ReportResponse;
import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.Report;
import com.maggu.maggu.community.repository.CommentRepository;
import com.maggu.maggu.community.repository.ReportRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private static final int AUTO_HIDE_THRESHOLD = 5;

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public ReportResponse report(AppUser reporter, ReportCreateRequest request) {
        boolean isPostTarget = request.getPostId() != null;
        boolean isCommentTarget = request.getCommentId() != null;
        if (isPostTarget == isCommentTarget) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "postId, commentId 중 하나만 입력해야 합니다.");
        }

        return isPostTarget ? reportPost(reporter, request) : reportComment(reporter, request);
    }

    private ReportResponse reportPost(AppUser reporter, ReportCreateRequest request) {
        Post post = postRepository.findByIdAndDeletedFalse(request.getPostId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (reportRepository.existsByReporterAndPost(reporter, post)) {
            throw new BusinessException(ErrorCode.REPORT_DUPLICATE);
        }

        reportRepository.save(Report.builder().reporter(reporter).post(post).reason(request.getReason()).build());
        postRepository.incrementReportCount(post.getId());

        boolean autoHidden = post.getReportCount() + 1 >= AUTO_HIDE_THRESHOLD;
        if (autoHidden) {
            postRepository.markDeleted(post.getId());
        }

        return ReportResponse.builder()
                .postId(post.getId())
                .reported(true)
                .autoHidden(autoHidden)
                .build();
    }

    private ReportResponse reportComment(AppUser reporter, ReportCreateRequest request) {
        Comment comment = commentRepository.findById(request.getCommentId())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (reportRepository.existsByReporterAndComment(reporter, comment)) {
            throw new BusinessException(ErrorCode.REPORT_DUPLICATE);
        }

        reportRepository.save(Report.builder().reporter(reporter).comment(comment).reason(request.getReason()).build());
        commentRepository.incrementReportCount(comment.getId());

        boolean autoHidden = comment.getReportCount() + 1 >= AUTO_HIDE_THRESHOLD;
        if (autoHidden) {
            commentRepository.markDeleted(comment.getId());
        }

        return ReportResponse.builder()
                .commentId(comment.getId())
                .reported(true)
                .autoHidden(autoHidden)
                .build();
    }
}