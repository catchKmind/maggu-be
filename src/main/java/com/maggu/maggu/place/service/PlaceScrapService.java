package com.maggu.maggu.place.service;

import com.maggu.maggu.place.dto.response.PlaceFolderResponse;
import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.repository.PlaceFolderRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceScrapService {

    private final PlaceFolderRepository placeFolderRepository;

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
}
