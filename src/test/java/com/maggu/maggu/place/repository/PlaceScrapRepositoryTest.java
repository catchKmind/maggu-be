package com.maggu.maggu.place.repository;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.place.entity.PlaceScrap;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.sticker.entity.StickerType;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PlaceScrapRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PlaceScrapRepository placeScrapRepository;

    private AppUser user;
    private Sticker sticker;
    private PlaceFolder folder;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .provider(Provider.TEST)
                .providerUserId("provider-user-1")
                .email("tester@maggu.com")
                .nickname("tester")
                .build();
        em.persist(user);

        sticker = Sticker.builder()
                .name("tester의 커스텀 스티커")
                .imageUrl("STICKER/1/sticker.png")
                .user(user)
                .type(StickerType.CUSTOM)
                .build();
        em.persist(sticker);

        folder = persistFolder("가보고 싶은 곳", "✈️");
    }

    private PlaceFolder persistFolder(String name, String icon) {
        PlaceFolder placeFolder = PlaceFolder.builder()
                .user(user)
                .name(name)
                .icon(icon)
                .isDefault(false)
                .build();
        em.persist(placeFolder);
        return placeFolder;
    }

    // createdAt은 감사(auditing) 필드라 세터가 없어, 정렬 검증용 테스트 데이터에서만 ReflectionTestUtils로 값을 심는다
    private PlaceScrap persistScrap(PlaceFolder placeFolder, String tourismContentId, Instant createdAt) {
        PlaceScrap scrap = PlaceScrap.builder()
                .user(user)
                .tourismContentId(tourismContentId)
                .sticker(sticker)
                .placeFolder(placeFolder)
                .build();
        em.persist(scrap);
        ReflectionTestUtils.setField(scrap, "createdAt", createdAt);
        em.flush();
        return scrap;
    }

    @Nested
    @DisplayName("findPageByPlaceFolderId")
    class FindPageByPlaceFolderId {

        @Test
        @DisplayName("커서가 없으면 저장일 최신순으로 size개까지 반환한다")
        void returnsFirstPageOrderedByCreatedAtDesc() {
            PlaceScrap oldest = persistScrap(folder, "11111", Instant.parse("2026-10-01T00:00:00Z"));
            PlaceScrap newest = persistScrap(folder, "33333", Instant.parse("2026-10-03T00:00:00Z"));
            PlaceScrap middle = persistScrap(folder, "22222", Instant.parse("2026-10-02T00:00:00Z"));
            em.clear();

            List<PlaceScrap> result = placeScrapRepository.findPageByPlaceFolderId(folder.getId(), null, null, 2);

            assertThat(result).extracting(PlaceScrap::getId).containsExactly(newest.getId(), middle.getId());
        }

        @Test
        @DisplayName("커서가 있으면 커서보다 오래된 스크랩만 이어서 반환한다")
        void returnsScrapsOlderThanCursor() {
            PlaceScrap oldest = persistScrap(folder, "11111", Instant.parse("2026-10-01T00:00:00Z"));
            PlaceScrap middle = persistScrap(folder, "22222", Instant.parse("2026-10-02T00:00:00Z"));
            persistScrap(folder, "33333", Instant.parse("2026-10-03T00:00:00Z"));
            em.clear();

            List<PlaceScrap> result = placeScrapRepository.findPageByPlaceFolderId(
                    folder.getId(), middle.getCreatedAt(), middle.getId(), 10);

            assertThat(result).extracting(PlaceScrap::getId).containsExactly(oldest.getId());
        }

        @Test
        @DisplayName("저장일이 같으면 id 내림차순으로 정렬하고, 커서도 id로 구분해 중복/누락이 없다")
        void breaksTiesById() {
            Instant sameTime = Instant.parse("2026-10-02T00:00:00Z");
            PlaceScrap first = persistScrap(folder, "11111", sameTime);
            PlaceScrap second = persistScrap(folder, "22222", sameTime);
            PlaceScrap third = persistScrap(folder, "33333", sameTime);
            em.clear();

            List<PlaceScrap> firstPage = placeScrapRepository.findPageByPlaceFolderId(folder.getId(), null, null, 2);
            PlaceScrap lastOfFirstPage = firstPage.get(firstPage.size() - 1);
            List<PlaceScrap> secondPage = placeScrapRepository.findPageByPlaceFolderId(
                    folder.getId(), lastOfFirstPage.getCreatedAt(), lastOfFirstPage.getId(), 2);

            assertThat(firstPage).extracting(PlaceScrap::getId).containsExactly(third.getId(), second.getId());
            assertThat(secondPage).extracting(PlaceScrap::getId).containsExactly(first.getId());
        }

        @Test
        @DisplayName("같은 유저의 다른 폴더에 저장된 장소는 섞이지 않는다")
        void doesNotIncludeScrapsInOtherFolders() {
            PlaceFolder otherFolder = persistFolder("가본 곳", "📍");
            persistScrap(folder, "11111", Instant.parse("2026-10-01T00:00:00Z"));
            persistScrap(otherFolder, "22222", Instant.parse("2026-10-02T00:00:00Z"));
            em.clear();

            List<PlaceScrap> result = placeScrapRepository.findPageByPlaceFolderId(folder.getId(), null, null, 10);

            assertThat(result).extracting(PlaceScrap::getTourismContentId).containsExactly("11111");
        }

        @Test
        @DisplayName("저장된 장소가 없는 폴더면 빈 리스트를 반환한다")
        void returnsEmptyListWhenFolderHasNoScraps() {
            em.flush();
            em.clear();

            List<PlaceScrap> result = placeScrapRepository.findPageByPlaceFolderId(folder.getId(), null, null, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("countByPlaceFolder")
    class CountByPlaceFolder {

        @Test
        @DisplayName("해당 폴더에 담긴 스크랩 수만 센다")
        void countsOnlyScrapsInFolder() {
            PlaceFolder otherFolder = persistFolder("가본 곳", "📍");
            persistScrap(folder, "11111", Instant.parse("2026-10-01T00:00:00Z"));
            persistScrap(folder, "22222", Instant.parse("2026-10-02T00:00:00Z"));
            persistScrap(otherFolder, "33333", Instant.parse("2026-10-03T00:00:00Z"));
            em.clear();

            long result = placeScrapRepository.countByPlaceFolder(folder);

            assertThat(result).isEqualTo(2);
        }
    }
}
