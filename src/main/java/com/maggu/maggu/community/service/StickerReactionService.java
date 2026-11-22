package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.response.StickerReactionItem;
import com.maggu.maggu.community.dto.response.StickerReactionResponse;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.PostStickerReaction;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.community.repository.PostStickerReactionRepository;
import com.maggu.maggu.sticker.repository.StickerRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.storage.CloudFrontUrlResolver;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StickerReactionService {

    private final PostStickerReactionRepository reactionRepository;
    private final StickerRepository stickerRepository;
    private final PostQueryService postQueryService;
    private final CloudFrontUrlResolver cloudFrontUrlResolver;

    // 유저당 게시물당 스티커 1개. 같은 스티커 재클릭 시 취소, 다른 스티커면 교체
    public StickerReactionResponse react(AppUser user, Long postId, Long stickerId) {
        Post post = postQueryService.getActivePost(postId);
        Sticker sticker = stickerRepository.findById(stickerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STICKER_NOT_FOUND));

        Optional<PostStickerReaction> existing = reactionRepository.findByPostAndUser(post, user);
        Sticker current = sticker;
        if (existing.isPresent()) {
            PostStickerReaction reaction = existing.get();
            if (reaction.getSticker().getId().equals(stickerId)) {
                reactionRepository.delete(reaction);
                current = null;
            } else {
                reaction.changeSticker(sticker);
            }
        } else {
            reactionRepository.save(PostStickerReaction.builder().post(post).user(user).sticker(sticker).build());
        }

        List<StickerReactionItem> stickerReactions = postQueryService.getStickerReactions(post, user);
        return StickerReactionResponse.builder()
                .postId(postId)
                .myReactionSticker(current == null ? null : current.getName())
                .myReactionStickerId(current == null ? null : current.getId())
                .myReactionStickerImageUrl(current == null ? null : cloudFrontUrlResolver.toPublicUrl(current.getImageUrl()))
                .stickerReactions(stickerReactions)
                .build();
    }
}
