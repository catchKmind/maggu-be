package com.maggu.maggu.place.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.place.dto.request.PlaceScrapCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceScrapCreateResponse;
import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.entity.PlaceScrap;
import com.maggu.maggu.place.repository.PlaceFolderRepository;
import com.maggu.maggu.place.repository.PlaceScrapRepository;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.sticker.repository.StickerRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceScrapService {

    private final PlaceFolderRepository placeFolderRepository;
    private final PlaceScrapRepository placeScrapRepository;
    private final StickerRepository stickerRepository;

    @Transactional
    public PlaceScrapCreateResponse createPlaceScrap(AppUser user, PlaceScrapCreateRequest request) {
        Sticker sticker = resolveSticker(user, request.stickerId());

        PlaceFolder placeFolder = resolvePlaceFolder(user, request.placeFolderId());

        if (placeScrapRepository.existsByPlaceFolderIdAndTourismContentId(request.placeFolderId(), request.tourismContentId())) {
            throw new BusinessException(ErrorCode.PLACE_SCRAP_DUPLICATE);
        }

        PlaceScrap scrap;
        try {
            scrap = placeScrapRepository.saveAndFlush(PlaceScrap.builder()
                    .user(user)
                    .tourismContentId(request.tourismContentId())
                    .sticker(sticker)
                    .placeFolder(placeFolder)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PLACE_SCRAP_DUPLICATE);
        }

        return toResponse(scrap, sticker);
    }

    private static PlaceScrapCreateResponse toResponse(PlaceScrap scrap, Sticker sticker) {
        return PlaceScrapCreateResponse.builder()
                .placeScrapId(scrap.getId())
                .tourismContentId(scrap.getTourismContentId())
                .stickerId(sticker.getId())
                .placeFolderId(scrap.getPlaceFolder().getId())
                .build();
    }

    private PlaceFolder resolvePlaceFolder(AppUser user, Long placeFolderId) {
        PlaceFolder placeFolder = placeFolderRepository.findById(placeFolderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_FOLDER_NOT_FOUND));
        if (!placeFolder.isOwnedBy(user)) {
            throw new BusinessException(ErrorCode.PLACE_FOLDER_ACCESS_DENIED);
        }

        return placeFolder;
    }

    private Sticker resolveSticker(AppUser user, Long stickerId) {
        Sticker sticker = stickerRepository.findById(stickerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STICKER_NOT_FOUND));
        if (sticker.isCustom() && !sticker.isOwnedBy(user)) {
            throw new BusinessException(ErrorCode.STICKER_ACCESS_DENIED);
        }

        return sticker;
    }

}
