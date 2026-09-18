package com.maggu.maggu.place.service;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.place.dto.request.PlaceFolderCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceFolderCreateResponse;
import com.maggu.maggu.place.dto.response.PlaceFolderResponse;
import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.repository.PlaceFolderRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceScrapServiceTest {

    @Mock
    private PlaceFolderRepository placeFolderRepository;

    @InjectMocks
    private PlaceScrapService placeScrapService;

    @Nested
    @DisplayName("getPlaceFolders")
    class GetPlaceFolders {

        @Test
        @DisplayName("유저의 장소 스크랩 폴더 목록을 기본 폴더가 먼저 오도록 응답 DTO로 변환해 반환한다")
        void returnsPlaceFoldersOwnedByUser() {
            AppUser user = appUser("나그네");
            PlaceFolder defaultFolder = placeFolder(1L, user, "내 장소", "❤️", true);
            PlaceFolder customFolder = placeFolder(2L, user, "가보고 싶은 곳", "✈️", false);
            given(placeFolderRepository.findAllByUserOrderByIsDefaultDescCreatedAtAsc(user))
                    .willReturn(List.of(defaultFolder, customFolder));

            List<PlaceFolderResponse> result = placeScrapService.getPlaceFolders(user);

            assertThat(result).extracting(PlaceFolderResponse::placeFolderId).containsExactly(1L, 2L);
            assertThat(result).extracting(PlaceFolderResponse::name).containsExactly("내 장소", "가보고 싶은 곳");
            assertThat(result).extracting(PlaceFolderResponse::icon).containsExactly("❤️", "✈️");
            assertThat(result).extracting(PlaceFolderResponse::isDefault).containsExactly(true, false);
        }

        @Test
        @DisplayName("폴더가 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoFolders() {
            AppUser user = appUser("나그네");
            given(placeFolderRepository.findAllByUserOrderByIsDefaultDescCreatedAtAsc(user))
                    .willReturn(List.of());

            List<PlaceFolderResponse> result = placeScrapService.getPlaceFolders(user);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createPlaceFolder")
    class CreatePlaceFolder {

        @Test
        @DisplayName("중복된 이름이 없으면 기본 폴더가 아닌 상태로 저장하고, 저장된 id/name/icon을 응답으로 반환한다")
        void createsFolderAndReturnsSavedInfo() {
            AppUser user = appUser("나그네");
            PlaceFolderCreateRequest request = new PlaceFolderCreateRequest("여행", "🐠");
            given(placeFolderRepository.existsByUserAndName(user, "여행")).willReturn(false);
            given(placeFolderRepository.save(any(PlaceFolder.class))).willAnswer(invocation -> {
                PlaceFolder toSave = invocation.getArgument(0);
                ReflectionTestUtils.setField(toSave, "id", 10L);
                return toSave;
            });

            PlaceFolderCreateResponse result = placeScrapService.createPlaceFolder(user, request);

            assertThat(result.placeFolderId()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("여행");
            assertThat(result.icon()).isEqualTo("🐠");
        }

        @Test
        @DisplayName("생성되는 폴더는 항상 기본 폴더가 아닌 상태(isDefault=false)로 저장한다")
        void alwaysSavesAsNonDefaultFolder() {
            AppUser user = appUser("나그네");
            PlaceFolderCreateRequest request = new PlaceFolderCreateRequest("여행", "🐠");
            given(placeFolderRepository.existsByUserAndName(user, "여행")).willReturn(false);
            ArgumentCaptor<PlaceFolder> captor = ArgumentCaptor.forClass(PlaceFolder.class);
            given(placeFolderRepository.save(captor.capture())).willAnswer(invocation -> invocation.getArgument(0));

            placeScrapService.createPlaceFolder(user, request);

            assertThat(captor.getValue().isDefault()).isFalse();
            assertThat(captor.getValue().getUser()).isEqualTo(user);
        }

        @Test
        @DisplayName("같은 유저가 이미 같은 이름의 폴더를 가지고 있으면 예외를 던지고 저장하지 않는다")
        void throwsWhenNameAlreadyExistsForUser() {
            AppUser user = appUser("나그네");
            PlaceFolderCreateRequest request = new PlaceFolderCreateRequest("여행", "🐠");
            given(placeFolderRepository.existsByUserAndName(user, "여행")).willReturn(true);

            assertThatThrownBy(() -> placeScrapService.createPlaceFolder(user, request))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_FOLDER_NAME_DUPLICATE));

            verify(placeFolderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createDefaultPlaceFolder")
    class CreateDefaultPlaceFolder {

        @Test
        @DisplayName("유저 가입 시 호출되면 기본 폴더를 고정된 이름/아이콘으로 생성한다")
        void createsDefaultFolderWithFixedNameAndIcon() {
            AppUser user = appUser("나그네");
            ArgumentCaptor<PlaceFolder> captor = ArgumentCaptor.forClass(PlaceFolder.class);
            given(placeFolderRepository.save(captor.capture())).willAnswer(invocation -> invocation.getArgument(0));

            placeScrapService.createDefaultPlaceFolder(user);

            assertThat(captor.getValue().isDefault()).isTrue();
            assertThat(captor.getValue().getUser()).isEqualTo(user);
            assertThat(captor.getValue().getName()).isNotBlank();
            assertThat(captor.getValue().getIcon()).isNotBlank();
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
}
