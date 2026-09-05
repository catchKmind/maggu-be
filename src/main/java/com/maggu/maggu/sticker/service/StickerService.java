package com.maggu.maggu.sticker.service;

import com.maggu.maggu.community.repository.PostStickerReactionRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.sticker.dto.StickerCreateRequest;
import com.maggu.maggu.sticker.dto.StickerDeleteResponse;
import com.maggu.maggu.sticker.dto.StickerResponse;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.sticker.repository.StickerRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StickerService {

    private final StickerRepository stickerRepository;
    private final PostStickerReactionRepository postStickerReactionRepository;

    public List<StickerResponse> getMyStickers(AppUser user) {
        List<Sticker> stickerList = stickerRepository.findAllByUserAndDeletedFalse(user);

        return stickerList.stream()
                .map(s -> StickerResponse.builder()
                        .stickerId(s.getId())
                        .imageUrl(s.getImageUrl())
                        .build())
                .toList();
    }

    @Transactional
    public StickerResponse createMySticker(AppUser user, StickerCreateRequest request) {
        Sticker savedSticker = stickerRepository.save(Sticker.builder()
                .name(user.getNickname() + "의 커스텀 스티커")
                .imageUrl(request.imageUrl())
                .user(user)
                .build());

        return StickerResponse.builder()
                .stickerId(savedSticker.getId())
                .imageUrl(savedSticker.getImageUrl())
                .build();
    }

    @Transactional
    public StickerDeleteResponse deleteMySticker(AppUser user, Long stickerId) {
        Sticker sticker = stickerRepository.findByIdAndDeletedFalse(stickerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STICKER_NOT_FOUND));

        if (!sticker.isOwnedBy(user)) {
            throw new BusinessException(ErrorCode.STICKER_ACCESS_DENIED);
        }

        if (postStickerReactionRepository.existsBySticker(sticker)) {
            sticker.markDeleted();
        } else {
            stickerRepository.delete(sticker);
        }

        return StickerDeleteResponse.builder()
                .stickerId(stickerId)
                .deleted(true)
                .build();

    }
}
