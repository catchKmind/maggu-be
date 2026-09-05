package com.maggu.maggu.sticker.service;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.sticker.dto.StickerCreateRequest;
import com.maggu.maggu.sticker.dto.StickerResponse;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.sticker.repository.StickerRepository;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StickerServiceTest {

    @Mock
    private StickerRepository stickerRepository;

    @InjectMocks
    private StickerService stickerService;

    @Nested
    @DisplayName("getMyStickers")
    class GetMyStickers {

        @Test
        @DisplayName("유저가 소유한 커스텀 스티커 목록을 응답 DTO로 변환해 반환한다")
        void returnsStickersOwnedByUser() {
            AppUser user = appUser("나그네");
            Sticker sticker1 = sticker(1L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            Sticker sticker2 = sticker(2L, "나그네의 커스텀 스티커", "https://img/2.png", user);
            given(stickerRepository.findAllByUser(user)).willReturn(List.of(sticker1, sticker2));

            List<StickerResponse> result = stickerService.getMyStickers(user);

            assertThat(result).extracting(StickerResponse::stickerId).containsExactly(1L, 2L);
            assertThat(result).extracting(StickerResponse::imageUrl)
                    .containsExactly("https://img/1.png", "https://img/2.png");
        }

        @Test
        @DisplayName("소유한 커스텀 스티커가 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoStickers() {
            AppUser user = appUser("나그네");
            given(stickerRepository.findAllByUser(user)).willReturn(List.of());

            List<StickerResponse> result = stickerService.getMyStickers(user);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createMySticker")
    class CreateMySticker {

        @Test
        @DisplayName("전달받은 imageUrl로 커스텀 스티커를 저장하고, 저장된 id/imageUrl을 응답으로 반환한다")
        void createsStickerAndReturnsSavedInfo() {
            AppUser user = appUser("나그네");
            StickerCreateRequest request = new StickerCreateRequest("https://img/new.png");
            given(stickerRepository.save(any(Sticker.class))).willAnswer(invocation -> {
                Sticker toSave = invocation.getArgument(0);
                ReflectionTestUtils.setField(toSave, "id", 10L);
                return toSave;
            });

            StickerResponse result = stickerService.createMySticker(user, request);

            assertThat(result.stickerId()).isEqualTo(10L);
            assertThat(result.imageUrl()).isEqualTo("https://img/new.png");
        }

        @Test
        @DisplayName("스티커 이름은 FE 입력 없이 유저 닉네임을 기반으로 서버에서 조립하고, 소유자를 현재 유저로 설정한다")
        void assemblesNameFromNicknameAndOwnsSticker() {
            AppUser user = appUser("나그네");
            StickerCreateRequest request = new StickerCreateRequest("https://img/new.png");
            ArgumentCaptor<Sticker> captor = ArgumentCaptor.forClass(Sticker.class);
            given(stickerRepository.save(captor.capture())).willAnswer(invocation -> invocation.getArgument(0));

            stickerService.createMySticker(user, request);

            assertThat(captor.getValue().getName()).isEqualTo("나그네의 커스텀 스티커");
            assertThat(captor.getValue().getUser()).isEqualTo(user);
        }
    }

    private AppUser appUser(String nickname) {
        return AppUser.builder()
                .provider(Provider.GOOGLE)
                .providerUserId("google-uid")
                .email("test@test.com")
                .nickname(nickname)
                .build();
    }

    private Sticker sticker(Long id, String name, String imageUrl, AppUser user) {
        Sticker sticker = Sticker.builder()
                .name(name)
                .imageUrl(imageUrl)
                .user(user)
                .build();
        ReflectionTestUtils.setField(sticker, "id", id);
        return sticker;
    }
}
