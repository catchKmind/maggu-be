package com.maggu.maggu.place.service;

import com.maggu.maggu.global.entity.enums.Provider;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceScrapServiceTest {

    @Mock
    private PlaceFolderRepository placeFolderRepository;

    @Mock
    private PlaceScrapRepository placeScrapRepository;

    @Mock
    private StickerRepository stickerRepository;

    @InjectMocks
    private PlaceScrapService placeScrapService;

    @Nested
    @DisplayName("createPlaceScrap")
    class CreatePlaceScrap {

        @Test
        @DisplayName("정상 요청이면 저장 후 스크랩/장소/스티커/폴더 정보를 응답으로 반환한다")
        void createsPlaceScrapAndReturnsSavedInfo() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = sticker(2L, user);
            PlaceFolder folder = placeFolder(3L, user, "가보고 싶은 곳", "✈️", false);
            PlaceScrapCreateRequest request = new PlaceScrapCreateRequest("13579", 2L, 3L);
            given(stickerRepository.findById(2L)).willReturn(Optional.of(sticker));
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(placeScrapRepository.existsByPlaceFolderIdAndTourismContentId(3L, "13579")).willReturn(false);
            given(placeScrapRepository.saveAndFlush(any(PlaceScrap.class))).willAnswer(invocation -> {
                PlaceScrap toSave = invocation.getArgument(0);
                ReflectionTestUtils.setField(toSave, "id", 100L);
                return toSave;
            });

            PlaceScrapCreateResponse result = placeScrapService.createPlaceScrap(user, request);

            assertThat(result.placeScrapId()).isEqualTo(100L);
            assertThat(result.tourismContentId()).isEqualTo("13579");
            assertThat(result.stickerId()).isEqualTo(2L);
            assertThat(result.placeFolderId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("존재하지 않는 스티커면 예외를 던지고 저장하지 않는다")
        void throwsWhenStickerNotFound() {
            AppUser user = appUserWithId(1L, "나그네");
            PlaceScrapCreateRequest request = new PlaceScrapCreateRequest("13579", 2L, 3L);
            given(stickerRepository.findById(2L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> placeScrapService.createPlaceScrap(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STICKER_NOT_FOUND));

            verify(placeScrapRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("다른 유저 소유 스티커면 예외를 던지고 저장하지 않는다")
        void throwsWhenStickerNotOwned() {
            AppUser user = appUserWithId(1L, "나그네");
            AppUser otherUser = appUserWithId(2L, "다른유저");
            Sticker sticker = sticker(2L, otherUser);
            PlaceScrapCreateRequest request = new PlaceScrapCreateRequest("13579", 2L, 3L);
            given(stickerRepository.findById(2L)).willReturn(Optional.of(sticker));

            assertThatThrownBy(() -> placeScrapService.createPlaceScrap(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STICKER_ACCESS_DENIED));

            verify(placeFolderRepository, never()).findById(any());
            verify(placeScrapRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("존재하지 않는 폴더면 예외를 던지고 저장하지 않는다")
        void throwsWhenPlaceFolderNotFound() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = sticker(2L, user);
            PlaceScrapCreateRequest request = new PlaceScrapCreateRequest("13579", 2L, 3L);
            given(stickerRepository.findById(2L)).willReturn(Optional.of(sticker));
            given(placeFolderRepository.findById(3L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> placeScrapService.createPlaceScrap(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_FOLDER_NOT_FOUND));

            verify(placeScrapRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("다른 유저 소유 폴더면 예외를 던지고 저장하지 않는다")
        void throwsWhenPlaceFolderNotOwned() {
            AppUser user = appUserWithId(1L, "나그네");
            AppUser otherUser = appUserWithId(2L, "다른유저");
            Sticker sticker = sticker(2L, user);
            PlaceFolder folder = placeFolder(3L, otherUser, "남의 폴더", "📍", false);
            PlaceScrapCreateRequest request = new PlaceScrapCreateRequest("13579", 2L, 3L);
            given(stickerRepository.findById(2L)).willReturn(Optional.of(sticker));
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));

            assertThatThrownBy(() -> placeScrapService.createPlaceScrap(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_FOLDER_ACCESS_DENIED));

            verify(placeScrapRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("같은 폴더에 이미 저장된 장소면 예외를 던지고 저장하지 않는다")
        void throwsWhenAlreadyScrappedInSameFolder() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = sticker(2L, user);
            PlaceFolder folder = placeFolder(3L, user, "가보고 싶은 곳", "✈️", false);
            PlaceScrapCreateRequest request = new PlaceScrapCreateRequest("13579", 2L, 3L);
            given(stickerRepository.findById(2L)).willReturn(Optional.of(sticker));
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(placeScrapRepository.existsByPlaceFolderIdAndTourismContentId(3L, "13579")).willReturn(true);

            assertThatThrownBy(() -> placeScrapService.createPlaceScrap(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_SCRAP_DUPLICATE));

            verify(placeScrapRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("사전 체크를 통과했더라도 저장 시점에 유니크 제약 위반이 나면 중복 예외로 변환한다")
        void translatesConstraintViolationOnSaveToDuplicateException() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = sticker(2L, user);
            PlaceFolder folder = placeFolder(3L, user, "가보고 싶은 곳", "✈️", false);
            PlaceScrapCreateRequest request = new PlaceScrapCreateRequest("13579", 2L, 3L);
            given(stickerRepository.findById(2L)).willReturn(Optional.of(sticker));
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(placeScrapRepository.existsByPlaceFolderIdAndTourismContentId(3L, "13579")).willReturn(false);
            willThrow(new DataIntegrityViolationException("duplicate"))
                    .given(placeScrapRepository).saveAndFlush(any(PlaceScrap.class));

            assertThatThrownBy(() -> placeScrapService.createPlaceScrap(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_SCRAP_DUPLICATE));
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

    private AppUser appUserWithId(Long id, String nickname) {
        AppUser user = appUser(nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private PlaceFolder placeFolder(Long id, AppUser user, String name, String icon, boolean isDefault) {
        PlaceFolder folder = PlaceFolder.builder()
                .user(user)
                .name(name)
                .icon(icon)
                .isDefault(isDefault)
                .build();
        ReflectionTestUtils.setField(folder, "id", id);
        return folder;
    }

    private Sticker sticker(Long id, AppUser owner) {
        Sticker sticker = Sticker.builder()
                .name(owner.getNickname() + "의 커스텀 스티커")
                .imageUrl("https://img/" + id + ".png")
                .user(owner)
                .build();
        ReflectionTestUtils.setField(sticker, "id", id);
        return sticker;
    }
}
