package com.maggu.maggu.sticker.service;

import com.maggu.maggu.sticker.dto.StickerCreateRequest;
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

    public List<StickerResponse> getMyStickers(AppUser user) {
        List<Sticker> stickerList = stickerRepository.findAllByUser(user);

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
}
