package com.maggu.maggu.place.service;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.TourSpot;
import com.maggu.maggu.place.dto.request.PlaceScrapCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceFolderResponse;
import com.maggu.maggu.place.dto.response.PlaceScrapCreateResponse;
import com.maggu.maggu.place.dto.response.PlaceFolderScrapsResponse;
import com.maggu.maggu.place.dto.response.PlaceScrapResponse;
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

import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceScrapService {

    private final PlaceFolderRepository placeFolderRepository;
    private final PlaceScrapRepository placeScrapRepository;
    private final StickerRepository stickerRepository;
    private final TourSpotCache tourSpotCache;

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

    @Transactional(readOnly = true)
    public PlaceFolderScrapsResponse getPlaceScraps(
            AppUser user, Long placeFolderId, AppLocale locale, String cursor, int size
    ) {
        PlaceScrapCursor decodedCursor = (cursor == null) ? null : PlaceScrapCursor.decode(cursor);
        Instant createdAt = (decodedCursor == null) ? null : decodedCursor.createdAt();
        Long cursorId = (decodedCursor == null) ? null : decodedCursor.id();

        PlaceFolder placeFolder = resolvePlaceFolder(user, placeFolderId);

        List<PlaceScrap> fetched = placeScrapRepository.findPageByPlaceFolderId(placeFolder.getId(), createdAt, cursorId, size + 1);

        CursorPageResponse<PlaceScrapResponse> page = CursorPageResponse.of(fetched, size, scrap -> PlaceScrapCursor.from(scrap).encode())
                .map(scrap -> {
                    TourSpot spot = tourSpotCache.findByContentId(locale, scrap.getTourismContentId())
                            .orElse(null); // 캐시에 없으면 null
                    return toResponse(scrap, spot);
                });

        return PlaceFolderScrapsResponse.builder()
                .totalScrapCount(placeScrapRepository.countByPlaceFolder(placeFolder))
                .placeFolder(PlaceFolderResponse.from(placeFolder))
                .placeScraps(page)
                .build();
    }

    private static PlaceScrapCreateResponse toResponse(PlaceScrap scrap, Sticker sticker) {
        return PlaceScrapCreateResponse.builder()
                .placeScrapId(scrap.getId())
                .tourismContentId(scrap.getTourismContentId())
                .stickerId(sticker.getId())
                .placeFolderId(scrap.getPlaceFolder().getId())
                .build();
    }

    private static PlaceScrapResponse toResponse(PlaceScrap placeScrap, TourSpot spot) {
        return PlaceScrapResponse.builder()
                .placeScrapId(placeScrap.getId())
                .tourismContentId(placeScrap.getTourismContentId())
                .placeName(spot == null ? null : spot.title())
                .placeAddress(spot == null ? null : spot.address())
                .stickerId(placeScrap.getSticker().getId())
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
