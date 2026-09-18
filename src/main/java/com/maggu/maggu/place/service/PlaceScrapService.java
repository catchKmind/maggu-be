package com.maggu.maggu.place.service;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.place.dto.request.PlaceFolderCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceFolderCreateResponse;
import com.maggu.maggu.place.dto.response.PlaceFolderResponse;
import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.repository.PlaceFolderRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceScrapService {

    private final PlaceFolderRepository placeFolderRepository;

    @Transactional(readOnly = true)
    public List<PlaceFolderResponse> getPlaceFolders(AppUser user) {
        List<PlaceFolder> folders = placeFolderRepository.findAllByUserOrderByIsDefaultDescCreatedAtAsc(user);

        return folders.stream()
                .map(f -> PlaceFolderResponse.builder()
                        .placeFolderId(f.getId())
                        .name(f.getName())
                        .icon(f.getIcon())
                        .isDefault(f.isDefault())
                        .build())
                .toList();
    }

    @Transactional
    public PlaceFolderCreateResponse createPlaceFolder(AppUser user, PlaceFolderCreateRequest request) {
        if (placeFolderRepository.existsByUserAndName(user, request.name())) {
            throw new BusinessException(ErrorCode.PLACE_FOLDER_NAME_DUPLICATE);
        }

        PlaceFolder folder;
        try {
            folder = placeFolderRepository.saveAndFlush(PlaceFolder.builder()
                    .name(request.name())
                    .icon(request.icon())
                    .isDefault(false)
                    .user(user)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PLACE_FOLDER_NAME_DUPLICATE);
        }

        return PlaceFolderCreateResponse.builder()
                .placeFolderId(folder.getId())
                .name(folder.getName())
                .icon(folder.getIcon())
                .build();
    }

    @Transactional
    public void createDefaultPlaceFolder(AppUser user) {
        placeFolderRepository.save(PlaceFolder.builder()
                .name("내 장소")
                .icon("❤️")
                .isDefault(true)
                .user(user)
                .build());
    }
}
