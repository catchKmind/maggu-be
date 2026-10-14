package com.maggu.maggu.place.service;

import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.ContentType;
import com.maggu.maggu.map.client.TourSpot;
import com.maggu.maggu.place.dto.request.PlaceScrapCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceFolderScrapsResponse;
import com.maggu.maggu.place.dto.response.PlaceScrapCreateResponse;
import com.maggu.maggu.place.dto.response.PlaceScrapResponse;
import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.entity.PlaceScrap;
import com.maggu.maggu.place.repository.PlaceFolderRepository;
import com.maggu.maggu.place.repository.PlaceScrapRepository;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.sticker.entity.StickerType;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlaceScrapServiceTest {

    @Mock
    private PlaceFolderRepository placeFolderRepository;

    @Mock
    private PlaceScrapRepository placeScrapRepository;

    @Mock
    private StickerRepository stickerRepository;

    @Mock
    private TourSpotCache tourSpotCache;

    @InjectMocks
    private PlaceScrapService placeScrapService;

    @Nested
    @DisplayName("getPlaceScraps")
    class GetPlaceScraps {

        @Test
        @DisplayName("본인 폴더면 폴더 정보, 장소 수, 리포지토리 순서(최신 저장순) 그대로의 스크랩 목록을 반환한다")
        void returnsFolderInfoCountAndScrapsInRepositoryOrder() {
            AppUser user = appUserWithId(1L, "나그네");
            PlaceFolder folder = placeFolder(3L, user, "가보고 싶은 곳", "✈️", false);
            Sticker sticker = sticker(2L, user);
            PlaceScrap newer = placeScrap(101L, user, "24680", sticker, folder);
            PlaceScrap older = placeScrap(100L, user, "13579", sticker, folder);
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(placeScrapRepository.findAllByPlaceFolderOrderByCreatedAtDesc(folder)).willReturn(List.of(newer, older));
            given(tourSpotCache.findByContentId(AppLocale.KO, "24680")).willReturn(Optional.of(tourSpot("24680", "해동용궁사")));
            given(tourSpotCache.findByContentId(AppLocale.KO, "13579")).willReturn(Optional.of(tourSpot("13579", "해운대해수욕장")));

            PlaceFolderScrapsResponse result = placeScrapService.getPlaceScraps(user, 3L, AppLocale.KO);

            assertThat(result.totalScrapCount()).isEqualTo(2);
            assertThat(result.placeFolder().placeFolderId()).isEqualTo(3L);
            assertThat(result.placeFolder().name()).isEqualTo("가보고 싶은 곳");
            assertThat(result.placeFolder().icon()).isEqualTo("✈️");
            assertThat(result.placeFolder().isDefault()).isFalse();
            assertThat(result.placeScraps())
                    .extracting(PlaceScrapResponse::placeScrapId, PlaceScrapResponse::tourismContentId,
                            PlaceScrapResponse::placeName, PlaceScrapResponse::stickerId)
                    .containsExactly(
                            tuple(101L, "24680", "해동용궁사", 2L),
                            tuple(100L, "13579", "해운대해수욕장", 2L));
        }

        @Test
        @DisplayName("요청한 언어의 캐시로 장소를 조회한다")
        void looksUpCacheWithRequestedLocale() {
            AppUser user = appUserWithId(1L, "나그네");
            PlaceFolder folder = placeFolder(3L, user, "가보고 싶은 곳", "✈️", false);
            PlaceScrap scrap = placeScrap(100L, user, "264337", sticker(2L, user), folder);
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(placeScrapRepository.findAllByPlaceFolderOrderByCreatedAtDesc(folder)).willReturn(List.of(scrap));
            given(tourSpotCache.findByContentId(AppLocale.EN, "264337"))
                    .willReturn(Optional.of(tourSpot("264337", "Gyeongbokgung Palace")));

            PlaceFolderScrapsResponse result = placeScrapService.getPlaceScraps(user, 3L, AppLocale.EN);

            assertThat(result.placeScraps()).extracting(PlaceScrapResponse::placeName)
                    .containsExactly("Gyeongbokgung Palace");
        }

        @Test
        @DisplayName("캐시에 없는 장소는 목록에서 빠지지 않고 장소명/주소만 null로 반환한다")
        void keepsScrapWithNullPlaceNameWhenCacheMisses() {
            AppUser user = appUserWithId(1L, "나그네");
            PlaceFolder folder = placeFolder(3L, user, "가보고 싶은 곳", "✈️", false);
            Sticker sticker = sticker(2L, user);
            PlaceScrap cached = placeScrap(101L, user, "24680", sticker, folder);
            PlaceScrap notCached = placeScrap(100L, user, "13579", sticker, folder);
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(placeScrapRepository.findAllByPlaceFolderOrderByCreatedAtDesc(folder)).willReturn(List.of(cached, notCached));
            given(tourSpotCache.findByContentId(AppLocale.KO, "24680")).willReturn(Optional.of(tourSpot("24680", "해동용궁사")));
            given(tourSpotCache.findByContentId(AppLocale.KO, "13579")).willReturn(Optional.empty());

            PlaceFolderScrapsResponse result = placeScrapService.getPlaceScraps(user, 3L, AppLocale.KO);

            assertThat(result.totalScrapCount()).isEqualTo(2);
            assertThat(result.placeScraps())
                    .extracting(PlaceScrapResponse::placeScrapId, PlaceScrapResponse::placeName, PlaceScrapResponse::placeAddress)
                    .containsExactly(tuple(101L, "해동용궁사", null), tuple(100L, null, null));
        }

        @Test
        @DisplayName("캐시에 있는 장소는 캐시의 주소로 placeAddress를 채운다")
        void fillsPlaceAddressFromCache() {
            AppUser user = appUserWithId(1L, "나그네");
            PlaceFolder folder = placeFolder(3L, user, "가보고 싶은 곳", "✈️", false);
            PlaceScrap scrap = placeScrap(100L, user, "13579", sticker(2L, user), folder);
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(placeScrapRepository.findAllByPlaceFolderOrderByCreatedAtDesc(folder)).willReturn(List.of(scrap));
            given(tourSpotCache.findByContentId(AppLocale.KO, "13579"))
                    .willReturn(Optional.of(tourSpot("13579", "해운대해수욕장", "부산광역시 해운대구 우동")));

            PlaceFolderScrapsResponse result = placeScrapService.getPlaceScraps(user, 3L, AppLocale.KO);

            assertThat(result.placeScraps()).extracting(PlaceScrapResponse::placeAddress)
                    .containsExactly("부산광역시 해운대구 우동");
        }

        @Test
        @DisplayName("폴더에 저장된 장소가 없으면 개수 0과 빈 목록을 반환한다")
        void returnsZeroCountAndEmptyListWhenFolderHasNoScraps() {
            AppUser user = appUserWithId(1L, "나그네");
            PlaceFolder folder = placeFolder(3L, user, "가보고 싶은 곳", "✈️", false);
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));
            given(placeScrapRepository.findAllByPlaceFolderOrderByCreatedAtDesc(folder)).willReturn(List.of());

            PlaceFolderScrapsResponse result = placeScrapService.getPlaceScraps(user, 3L, AppLocale.KO);

            assertThat(result.totalScrapCount()).isZero();
            assertThat(result.placeScraps()).isEmpty();
            assertThat(result.placeFolder().placeFolderId()).isEqualTo(3L);
            verifyNoInteractions(tourSpotCache);
        }

        @Test
        @DisplayName("존재하지 않는 폴더면 예외를 던지고 스크랩/캐시를 조회하지 않는다")
        void throwsWhenPlaceFolderNotFound() {
            AppUser user = appUserWithId(1L, "나그네");
            given(placeFolderRepository.findById(3L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> placeScrapService.getPlaceScraps(user, 3L, AppLocale.KO))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_FOLDER_NOT_FOUND));

            verify(placeScrapRepository, never()).findAllByPlaceFolderOrderByCreatedAtDesc(any());
            verifyNoInteractions(tourSpotCache);
        }

        @Test
        @DisplayName("다른 유저 소유 폴더면 예외를 던지고 스크랩/캐시를 조회하지 않는다")
        void throwsWhenPlaceFolderNotOwned() {
            AppUser user = appUserWithId(1L, "나그네");
            AppUser otherUser = appUserWithId(2L, "다른유저");
            PlaceFolder folder = placeFolder(3L, otherUser, "남의 폴더", "📍", false);
            given(placeFolderRepository.findById(3L)).willReturn(Optional.of(folder));

            assertThatThrownBy(() -> placeScrapService.getPlaceScraps(user, 3L, AppLocale.KO))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_FOLDER_ACCESS_DENIED));

            verify(placeScrapRepository, never()).findAllByPlaceFolderOrderByCreatedAtDesc(any());
            verifyNoInteractions(tourSpotCache);
        }
    }

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
        @DisplayName("GIPHY 스티커는 소유자가 없어도(공유 스티커) 누구나 사용할 수 있다")
        void allowsAnyUserToUseGiphySticker() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = giphySticker(2L);
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

            assertThat(result.stickerId()).isEqualTo(2L);
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

    private PlaceScrap placeScrap(Long id, AppUser user, String tourismContentId, Sticker sticker, PlaceFolder folder) {
        PlaceScrap scrap = PlaceScrap.builder()
                .user(user)
                .tourismContentId(tourismContentId)
                .sticker(sticker)
                .placeFolder(folder)
                .build();
        ReflectionTestUtils.setField(scrap, "id", id);
        return scrap;
    }

    private Sticker sticker(Long id, AppUser owner) {
        Sticker sticker = Sticker.builder()
                .name(owner.getNickname() + "의 커스텀 스티커")
                .imageUrl("https://img/" + id + ".png")
                .user(owner)
                .type(StickerType.CUSTOM)
                .build();
        ReflectionTestUtils.setField(sticker, "id", id);
        return sticker;
    }

    private TourSpot tourSpot(String contentId, String title) {
        return tourSpot(contentId, title, null);
    }

    private TourSpot tourSpot(String contentId, String title, String address) {
        return new TourSpot(contentId, ContentType.TOURIST_ATTRACTION, title, address, 129.0, 35.0);
    }

    private Sticker giphySticker(Long id) {
        Sticker sticker = Sticker.builder()
                .name("GIPHY STICKER")
                .imageUrl("https://media.giphy.com/" + id + ".gif")
                .type(StickerType.GIPHY)
                .giphyId("giphy-" + id)
                .build();
        ReflectionTestUtils.setField(sticker, "id", id);
        return sticker;
    }
}
