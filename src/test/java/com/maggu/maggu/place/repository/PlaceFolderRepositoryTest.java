package com.maggu.maggu.place.repository;

import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.place.entity.PlaceFolder;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PlaceFolderRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PlaceFolderRepository placeFolderRepository;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = AppUser.builder()
                .provider(Provider.TEST)
                .providerUserId("provider-user-1")
                .email("tester@maggu.com")
                .nickname("tester")
                .build();
        em.persist(user);
    }

    private PlaceFolder persistFolder(AppUser owner, String name, String icon, boolean isDefault) {
        PlaceFolder folder = PlaceFolder.builder()
                .user(owner)
                .name(name)
                .icon(icon)
                .isDefault(isDefault)
                .build();
        em.persist(folder);
        return folder;
    }

    @Nested
    @DisplayName("findAllByUserOrderByIsDefaultDescCreatedAtAsc")
    class FindAllByUser {

        @Test
        @DisplayName("기본 폴더가 먼저, 그 다음은 생성 순으로 반환한다")
        void returnsDefaultFolderFirstThenByCreatedAtAsc() {
            PlaceFolder custom = persistFolder(user, "가보고 싶은 곳", "✈️", false);
            PlaceFolder defaultFolder = persistFolder(user, "내 장소", "❤️", true);
            em.flush();
            em.clear();

            List<PlaceFolder> result = placeFolderRepository.findAllByUserOrderByIsDefaultDescCreatedAtAsc(user);

            assertThat(result).extracting(PlaceFolder::getId)
                    .containsExactly(defaultFolder.getId(), custom.getId());
        }

        @Test
        @DisplayName("다른 유저의 폴더는 섞이지 않는다")
        void doesNotIncludeOtherUsersFolders() {
            AppUser other = AppUser.builder()
                    .provider(Provider.TEST)
                    .providerUserId("provider-user-2")
                    .email("other@maggu.com")
                    .nickname("other")
                    .build();
            em.persist(other);
            persistFolder(user, "내 장소", "❤️", true);
            persistFolder(other, "다른 유저 폴더", "📍", true);
            em.flush();
            em.clear();

            List<PlaceFolder> result = placeFolderRepository.findAllByUserOrderByIsDefaultDescCreatedAtAsc(user);

            assertThat(result).extracting(PlaceFolder::getName).containsExactly("내 장소");
        }
    }

    @Nested
    @DisplayName("existsByUserAndName")
    class ExistsByUserAndName {

        @Test
        @DisplayName("같은 유저가 같은 이름의 폴더를 이미 가지고 있으면 true를 반환한다")
        void returnsTrueWhenNameAlreadyExistsForUser() {
            persistFolder(user, "여행", "🐠", false);
            em.flush();

            boolean result = placeFolderRepository.existsByUserAndName(user, "여행");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("다른 유저가 같은 이름을 쓰고 있어도 false를 반환한다(유저 단위 유니크)")
        void returnsFalseWhenSameNameBelongsToAnotherUser() {
            AppUser other = AppUser.builder()
                    .provider(Provider.TEST)
                    .providerUserId("provider-user-2")
                    .email("other@maggu.com")
                    .nickname("other")
                    .build();
            em.persist(other);
            persistFolder(other, "여행", "🐠", false);
            em.flush();

            boolean result = placeFolderRepository.existsByUserAndName(user, "여행");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("이름이 다르면 false를 반환한다")
        void returnsFalseWhenNameDiffers() {
            persistFolder(user, "여행", "🐠", false);
            em.flush();

            boolean result = placeFolderRepository.existsByUserAndName(user, "출장");

            assertThat(result).isFalse();
        }
    }
}
