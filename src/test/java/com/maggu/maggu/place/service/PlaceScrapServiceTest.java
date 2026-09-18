package com.maggu.maggu.place.service;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.place.dto.response.PlaceFolderResponse;
import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.repository.PlaceFolderRepository;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
