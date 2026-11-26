package com.maggu.maggu.sticker.service;

import com.maggu.maggu.community.repository.PostStickerReactionRepository;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.config.CloudFrontProperties;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.storage.CloudFrontUrlResolver;
import com.maggu.maggu.global.storage.PresignedUrlService;
import com.maggu.maggu.global.storage.UploadDomain;
import com.maggu.maggu.sticker.dto.request.GiphyStickerCreateRequest;
import com.maggu.maggu.place.repository.PlaceScrapRepository;
import com.maggu.maggu.sticker.dto.request.StickerCreateRequest;
import com.maggu.maggu.sticker.dto.request.StickersDeleteRequest;
import com.maggu.maggu.sticker.dto.response.StickerDeleteResponse;
import com.maggu.maggu.sticker.dto.response.StickerResponse;
import com.maggu.maggu.sticker.dto.response.StickersDeleteResponse;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.sticker.entity.StickerType;
import com.maggu.maggu.sticker.repository.StickerRepository;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StickerServiceTest {

    @Mock
    private StickerRepository stickerRepository;

    @Mock
    private PostStickerReactionRepository postStickerReactionRepository;

    @Mock
    private PresignedUrlService presignedUrlService;

    @Mock
    private PlaceScrapRepository placeScrapRepository;

    @Spy
    private CloudFrontUrlResolver cloudFrontUrlResolver =
            new CloudFrontUrlResolver(new CloudFrontProperties(""), "test-bucket");

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
            given(stickerRepository.findAllByUserAndDeletedFalse(user)).willReturn(List.of(sticker1, sticker2));

            List<StickerResponse> result = stickerService.getMyStickers(user);

            assertThat(result).extracting(StickerResponse::stickerId).containsExactly(1L, 2L);
            assertThat(result).extracting(StickerResponse::imageUrl)
                    .containsExactly("https://img/1.png", "https://img/2.png");
            assertThat(result).extracting(StickerResponse::type)
                    .containsExactly(StickerType.CUSTOM, StickerType.CUSTOM);
        }

        @Test
        @DisplayName("소유한 커스텀 스티커가 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoStickers() {
            AppUser user = appUser("나그네");
            given(stickerRepository.findAllByUserAndDeletedFalse(user)).willReturn(List.of());

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
            assertThat(result.type()).isEqualTo(StickerType.CUSTOM);
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
            assertThat(captor.getValue().getType()).isEqualTo(StickerType.CUSTOM);
        }

        @Test
        @DisplayName("imageUrl이 유효한 objectKey가 아니면 예외를 던지고 저장하지 않는다")
        void throwsAndDoesNotSaveWhenObjectKeyInvalid() {
            AppUser user = appUser("나그네");
            StickerCreateRequest request = new StickerCreateRequest("string");
            willThrow(new BusinessException(ErrorCode.UPLOAD_INVALID_OBJECT_KEY))
                    .given(presignedUrlService).validateObjectKey(user, UploadDomain.STICKER, "string");

            assertThatThrownBy(() -> stickerService.createMySticker(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_OBJECT_KEY));

            verify(stickerRepository, never()).save(any(Sticker.class));
        }
    }

    @Nested
    @DisplayName("createGiphySticker")
    class CreateGiphySticker {

        @Test
        @DisplayName("같은 giphyId로 저장된 스티커가 없으면 GIPHY 타입으로 새로 저장하고, 소유자 없이(user=null) 응답한다")
        void createsNewGiphyStickerWhenNotFound() {
            GiphyStickerCreateRequest request = new GiphyStickerCreateRequest("giphy-1", "https://media.giphy.com/1.gif");
            given(stickerRepository.findByGiphyId("giphy-1")).willReturn(Optional.empty());
            ArgumentCaptor<Sticker> captor = ArgumentCaptor.forClass(Sticker.class);
            given(stickerRepository.save(captor.capture())).willAnswer(invocation -> {
                Sticker toSave = invocation.getArgument(0);
                ReflectionTestUtils.setField(toSave, "id", 20L);
                return toSave;
            });

            StickerResponse result = stickerService.createGiphySticker(request);

            assertThat(result.stickerId()).isEqualTo(20L);
            assertThat(result.imageUrl()).isEqualTo("https://media.giphy.com/1.gif");
            assertThat(result.type()).isEqualTo(StickerType.GIPHY);
            assertThat(captor.getValue().getGiphyId()).isEqualTo("giphy-1");
            assertThat(captor.getValue().getUser()).isNull();
        }

        @Test
        @DisplayName("이미 같은 giphyId로 저장된 스티커가 있으면 새로 저장하지 않고 기존 스티커를 그대로 재사용한다")
        void reusesExistingStickerWithSameGiphyId() {
            Sticker existing = giphySticker(30L, "giphy-2", "https://media.giphy.com/2.gif");
            GiphyStickerCreateRequest request = new GiphyStickerCreateRequest("giphy-2", "https://media.giphy.com/2.gif");
            given(stickerRepository.findByGiphyId("giphy-2")).willReturn(Optional.of(existing));

            StickerResponse result = stickerService.createGiphySticker(request);

            assertThat(result.stickerId()).isEqualTo(30L);
            assertThat(result.type()).isEqualTo(StickerType.GIPHY);
            verify(stickerRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteMySticker")
    class DeleteMySticker {

        @Test
        @DisplayName("본인 소유이고 반응/장소 스크랩 어디에도 쓰이지 않은 스티커면 물리적으로 삭제한다")
        void deletesStickerOwnedByUser() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            given(stickerRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(sticker));
            given(postStickerReactionRepository.findStickerIdsInUse(List.of(sticker))).willReturn(Set.of());
            given(placeScrapRepository.findStickerIdsInUse(List.of(sticker))).willReturn(Set.of());

            StickerDeleteResponse result = stickerService.deleteMySticker(user, 10L);

            verify(stickerRepository).delete(sticker);
            assertThat(result.stickerId()).isEqualTo(10L);
            assertThat(result.deleted()).isTrue();
        }

        @Test
        @DisplayName("본인 소유이지만 게시글 반응에 쓰이고 있으면 물리 삭제 대신 소프트 삭제한다")
        void softDeletesStickerInUseByReaction() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            given(stickerRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(sticker));
            given(postStickerReactionRepository.findStickerIdsInUse(List.of(sticker))).willReturn(Set.of(10L));
            given(placeScrapRepository.findStickerIdsInUse(List.of(sticker))).willReturn(Set.of());

            StickerDeleteResponse result = stickerService.deleteMySticker(user, 10L);

            assertThat(sticker.isDeleted()).isTrue();
            verify(stickerRepository, never()).delete(any());
            assertThat(result.stickerId()).isEqualTo(10L);
            assertThat(result.deleted())
                    .as("소프트 삭제여도 호출자 입장에선 목록에서 사라진 것이니 true로 응답한다")
                    .isTrue();
        }

        @Test
        @DisplayName("본인 소유이지만 장소 스크랩에 쓰이고 있으면 물리 삭제 대신 소프트 삭제한다")
        void softDeletesStickerInUseByPlaceScrap() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            given(stickerRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(sticker));
            given(postStickerReactionRepository.findStickerIdsInUse(List.of(sticker))).willReturn(Set.of());
            given(placeScrapRepository.findStickerIdsInUse(List.of(sticker))).willReturn(Set.of(10L));

            StickerDeleteResponse result = stickerService.deleteMySticker(user, 10L);

            assertThat(sticker.isDeleted()).isTrue();
            verify(stickerRepository, never()).delete(any());
            assertThat(result.deleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 스티커면 예외를 던지고 삭제하지 않는다")
        void throwsWhenStickerNotFound() {
            AppUser user = appUserWithId(1L, "나그네");
            given(stickerRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> stickerService.deleteMySticker(user, 10L))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STICKER_NOT_FOUND));

            verify(stickerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("다른 유저 소유 스티커면 예외를 던지고 삭제하지 않는다")
        void throwsWhenNotOwner() {
            AppUser owner = appUserWithId(1L, "나그네");
            AppUser requester = appUserWithId(2L, "다른유저");
            Sticker sticker = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", owner);
            given(stickerRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(sticker));

            assertThatThrownBy(() -> stickerService.deleteMySticker(requester, 10L))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STICKER_ACCESS_DENIED));

            verify(stickerRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("deleteMyStickers")
    class DeleteMyStickers {

        @Test
        @DisplayName("본인 소유이고 어디에도 쓰이지 않은 스티커들은 물리적으로 삭제하고, 요청한 ID를 응답한다")
        void deletesUnusedStickersOwnedByUser() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker1 = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            Sticker sticker2 = sticker(11L, "나그네의 커스텀 스티커", "https://img/2.png", user);
            List<Sticker> stickers = List.of(sticker1, sticker2);
            given(stickerRepository.findAllByIdInAndDeletedFalse(List.of(10L, 11L))).willReturn(stickers);
            given(postStickerReactionRepository.findStickerIdsInUse(stickers)).willReturn(Set.of());
            given(placeScrapRepository.findStickerIdsInUse(stickers)).willReturn(Set.of());

            StickersDeleteResponse result = stickerService.deleteMyStickers(user, new StickersDeleteRequest(List.of(10L, 11L)));

            verify(stickerRepository).delete(sticker1);
            verify(stickerRepository).delete(sticker2);
            assertThat(result.stickerIds()).containsExactly(10L, 11L);
        }

        @Test
        @DisplayName("게시글 반응에 쓰이는 스티커는 소프트 삭제하고, 나머지는 물리 삭제한다")
        void softDeletesStickerInUseByReaction() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker inUse = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            Sticker unused = sticker(11L, "나그네의 커스텀 스티커", "https://img/2.png", user);
            List<Sticker> stickers = List.of(inUse, unused);
            given(stickerRepository.findAllByIdInAndDeletedFalse(List.of(10L, 11L))).willReturn(stickers);
            given(postStickerReactionRepository.findStickerIdsInUse(stickers)).willReturn(Set.of(10L));
            given(placeScrapRepository.findStickerIdsInUse(stickers)).willReturn(Set.of());

            stickerService.deleteMyStickers(user, new StickersDeleteRequest(List.of(10L, 11L)));

            assertThat(inUse.isDeleted()).isTrue();
            verify(stickerRepository, never()).delete(inUse);
            verify(stickerRepository).delete(unused);
        }

        @Test
        @DisplayName("장소 스크랩에 쓰이는 스티커도 물리 삭제 대신 소프트 삭제한다")
        void softDeletesStickerInUseByPlaceScrap() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker inUse = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            List<Sticker> stickers = List.of(inUse);
            given(stickerRepository.findAllByIdInAndDeletedFalse(List.of(10L))).willReturn(stickers);
            given(postStickerReactionRepository.findStickerIdsInUse(stickers)).willReturn(Set.of());
            given(placeScrapRepository.findStickerIdsInUse(stickers)).willReturn(Set.of(10L));

            stickerService.deleteMyStickers(user, new StickersDeleteRequest(List.of(10L)));

            assertThat(inUse.isDeleted()).isTrue();
            verify(stickerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("다른 유저 소유 스티커가 하나라도 섞여 있으면 예외를 던지고 아무것도 삭제하지 않는다")
        void throwsAndDeletesNothingWhenAnyStickerNotOwned() {
            AppUser user = appUserWithId(1L, "나그네");
            AppUser other = appUserWithId(2L, "다른유저");
            Sticker mine = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            Sticker others = sticker(11L, "다른유저의 커스텀 스티커", "https://img/2.png", other);
            given(stickerRepository.findAllByIdInAndDeletedFalse(List.of(10L, 11L))).willReturn(List.of(mine, others));

            assertThatThrownBy(() -> stickerService.deleteMyStickers(user, new StickersDeleteRequest(List.of(10L, 11L))))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STICKER_ACCESS_DENIED));

            verify(stickerRepository, never()).delete(any());
            assertThat(mine.isDeleted()).isFalse();
            verifyNoInteractions(postStickerReactionRepository, placeScrapRepository);
        }

        @Test
        @DisplayName("소유자가 없는 GIPHY 스티커가 섞여 있으면 예외를 던지고 아무것도 삭제하지 않는다")
        void throwsWhenGiphyStickerIncluded() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker giphy = giphySticker(30L, "giphy-1", "https://media.giphy.com/1.gif");
            given(stickerRepository.findAllByIdInAndDeletedFalse(List.of(30L))).willReturn(List.of(giphy));

            assertThatThrownBy(() -> stickerService.deleteMyStickers(user, new StickersDeleteRequest(List.of(30L))))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STICKER_ACCESS_DENIED));

            verify(stickerRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않거나 이미 삭제된 ID는 무시하고, 찾은 스티커만 삭제한 뒤 요청한 ID를 모두 응답한다")
        void ignoresMissingIds() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            List<Sticker> stickers = List.of(sticker);
            given(stickerRepository.findAllByIdInAndDeletedFalse(List.of(10L, 99L))).willReturn(stickers);
            given(postStickerReactionRepository.findStickerIdsInUse(stickers)).willReturn(Set.of());
            given(placeScrapRepository.findStickerIdsInUse(stickers)).willReturn(Set.of());

            StickersDeleteResponse result = stickerService.deleteMyStickers(user, new StickersDeleteRequest(List.of(10L, 99L)));

            verify(stickerRepository).delete(sticker);
            assertThat(result.stickerIds()).containsExactly(10L, 99L);
        }

        @Test
        @DisplayName("찾은 스티커가 하나도 없으면 사용 중 조회 없이 바로 성공으로 응답한다")
        void returnsImmediatelyWhenNothingFound() {
            AppUser user = appUserWithId(1L, "나그네");
            given(stickerRepository.findAllByIdInAndDeletedFalse(List.of(98L, 99L))).willReturn(List.of());

            StickersDeleteResponse result = stickerService.deleteMyStickers(user, new StickersDeleteRequest(List.of(98L, 99L)));

            assertThat(result.stickerIds()).containsExactly(98L, 99L);
            verify(stickerRepository, never()).delete(any());
            verifyNoInteractions(postStickerReactionRepository, placeScrapRepository);
        }

        @Test
        @DisplayName("중복된 ID는 한 번만 조회·응답한다")
        void deduplicatesIds() {
            AppUser user = appUserWithId(1L, "나그네");
            Sticker sticker1 = sticker(10L, "나그네의 커스텀 스티커", "https://img/1.png", user);
            Sticker sticker2 = sticker(11L, "나그네의 커스텀 스티커", "https://img/2.png", user);
            List<Sticker> stickers = List.of(sticker1, sticker2);
            given(stickerRepository.findAllByIdInAndDeletedFalse(List.of(10L, 11L))).willReturn(stickers);
            given(postStickerReactionRepository.findStickerIdsInUse(stickers)).willReturn(Set.of());
            given(placeScrapRepository.findStickerIdsInUse(stickers)).willReturn(Set.of());

            StickersDeleteResponse result = stickerService.deleteMyStickers(user, new StickersDeleteRequest(List.of(10L, 10L, 11L)));

            assertThat(result.stickerIds()).containsExactly(10L, 11L);
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

    private Sticker sticker(Long id, String name, String imageUrl, AppUser user) {
        Sticker sticker = Sticker.builder()
                .name(name)
                .imageUrl(imageUrl)
                .user(user)
                .type(StickerType.CUSTOM)
                .build();
        ReflectionTestUtils.setField(sticker, "id", id);
        return sticker;
    }

    private Sticker giphySticker(Long id, String giphyId, String imageUrl) {
        Sticker sticker = Sticker.builder()
                .name("GIPHY STICKER")
                .imageUrl(imageUrl)
                .type(StickerType.GIPHY)
                .giphyId(giphyId)
                .build();
        ReflectionTestUtils.setField(sticker, "id", id);
        return sticker;
    }
}
