package com.maggu.maggu.sticker.service;

import com.maggu.maggu.community.repository.PostStickerReactionRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.storage.CloudFrontUrlResolver;
import com.maggu.maggu.global.storage.PresignedUrlService;
import com.maggu.maggu.global.storage.UploadDomain;
import com.maggu.maggu.place.repository.PlaceScrapRepository;
import com.maggu.maggu.sticker.dto.request.GiphyStickerCreateRequest;
import com.maggu.maggu.sticker.dto.request.StickerCreateRequest;
import com.maggu.maggu.sticker.dto.request.StickersDeleteRequest;
import com.maggu.maggu.sticker.dto.response.StickerDeleteResponse;
import com.maggu.maggu.sticker.dto.response.StickerResponse;
import com.maggu.maggu.sticker.dto.response.StickersDeleteResponse;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.sticker.entity.StickerType;
import com.maggu.maggu.sticker.repository.StickerRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StickerService {

    private final StickerRepository stickerRepository;
    private final PostStickerReactionRepository postStickerReactionRepository;
    private final CloudFrontUrlResolver cloudFrontUrlResolver;
    private final PresignedUrlService presignedUrlService;
    private final PlaceScrapRepository placeScrapRepository;

    @Transactional
    public StickerResponse createGiphySticker(GiphyStickerCreateRequest request) {
        Sticker sticker = stickerRepository.findByGiphyId(request.giphyId())
                .orElseGet(() -> stickerRepository.save(Sticker.builder()
                        .name("GIPHY STICKER")
                        .imageUrl(request.imageUrl())
                        .user(null)
                        .type(StickerType.GIPHY)
                        .giphyId(request.giphyId())
                        .build()));

        return getStickerResponse(sticker);
    }

    @Transactional(readOnly = true)
    public List<StickerResponse> getMyStickers(AppUser user) {
        List<Sticker> stickerList = stickerRepository.findAllByUserAndDeletedFalse(user);

        return stickerList.stream()
                .map(s -> StickerResponse.builder()
                        .stickerId(s.getId())
                        .imageUrl(cloudFrontUrlResolver.toPublicUrl(s.getImageUrl()))
                        .type(s.getType())
                        .build())
                .toList();
    }

    @Transactional
    public StickerResponse createMySticker(AppUser user, StickerCreateRequest request) {
        presignedUrlService.validateObjectKey(user, UploadDomain.STICKER, request.imageUrl());

        Sticker savedSticker = stickerRepository.save(Sticker.builder()
                .name(user.getNickname() + "의 커스텀 스티커")
                .imageUrl(request.imageUrl())
                .user(user)
                .type(StickerType.CUSTOM)
                .build());

        return getStickerResponse(savedSticker);
    }

    @Transactional
    public StickerDeleteResponse deleteMySticker(AppUser user, Long stickerId) {
        Sticker sticker = stickerRepository.findByIdAndDeletedFalse(stickerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STICKER_NOT_FOUND));

        if (!sticker.isOwnedBy(user)) {
            throw new BusinessException(ErrorCode.STICKER_ACCESS_DENIED);
        }

        deleteOrSoftDelete(List.of(sticker));

        return StickerDeleteResponse.builder()
                .stickerId(stickerId)
                .deleted(true)
                .build();
    }

    @Transactional
    public StickersDeleteResponse deleteMyStickers(AppUser user, StickersDeleteRequest request) {
        // id 중복 제거
        List<Long> uniqueIds = request.stickerIds().stream().distinct().toList();
        List<Sticker> stickers = stickerRepository.findAllByIdInAndDeletedFalse(uniqueIds);
        if (stickers.isEmpty()) {
            return StickersDeleteResponse.from(uniqueIds);
        }

        // 소유권 검증
        stickers.forEach(sticker -> {
            if (!sticker.isOwnedBy(user)) {
                throw new BusinessException(ErrorCode.STICKER_ACCESS_DENIED);
            }
        });

        deleteOrSoftDelete(stickers);

        return StickersDeleteResponse.from(uniqueIds);
    }

    // ---------- private methods ----------
    private StickerResponse getStickerResponse(Sticker savedSticker) {
        return StickerResponse.builder()
                .stickerId(savedSticker.getId())
                .imageUrl(cloudFrontUrlResolver.toPublicUrl(savedSticker.getImageUrl()))
                .type(savedSticker.getType())
                .build();
    }

    private void deleteOrSoftDelete(List<Sticker> stickers) {
        Set<Long> inUse = new HashSet<>(postStickerReactionRepository.findStickerIdsInUse(stickers));
        inUse.addAll(placeScrapRepository.findStickerIdsInUse(stickers));

        stickers.forEach(sticker -> {
            if (inUse.contains(sticker.getId())) {
                sticker.markDeleted();
            } else {
                stickerRepository.delete(sticker);
            }
        });
    }
}
