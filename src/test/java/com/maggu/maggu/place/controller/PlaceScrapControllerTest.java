package com.maggu.maggu.place.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.entity.enums.Provider;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.i18n.RequestLocaleResolver;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.global.security.jwt.JwtAuthenticationFilter;
import com.maggu.maggu.place.dto.request.PlaceScrapCreateRequest;
import com.maggu.maggu.place.dto.response.PlaceFolderResponse;
import com.maggu.maggu.place.dto.response.PlaceFolderScrapsResponse;
import com.maggu.maggu.place.dto.response.PlaceScrapCreateResponse;
import com.maggu.maggu.place.dto.response.PlaceScrapResponse;
import com.maggu.maggu.place.service.PlaceScrapService;
import com.maggu.maggu.user.entity.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlaceScrapController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(RequestLocaleResolver.class)
class PlaceScrapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlaceScrapService placeScrapService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/my-places/scrap")
    class CreatePlaceScrap {

        @Test
        @DisplayName("정상 요청이면 생성된 장소 스크랩 정보를 반환한다")
        void createsPlaceScrap() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.createPlaceScrap(any(), any())).willReturn(
                    PlaceScrapCreateResponse.builder()
                            .placeScrapId(100L)
                            .tourismContentId("13579")
                            .stickerId(2L)
                            .placeFolderId(3L)
                            .build());

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", 2L, 3L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.placeScrapId").value(100))
                    .andExpect(jsonPath("$.data.tourismContentId").value("13579"))
                    .andExpect(jsonPath("$.data.stickerId").value(2))
                    .andExpect(jsonPath("$.data.placeFolderId").value(3));
        }

        @Test
        @DisplayName("tourismContentId가 비어 있으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenTourismContentIdBlank() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("", 2L, 3L))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(placeScrapService);
        }

        @Test
        @DisplayName("stickerId가 없으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenStickerIdMissing() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", null, 3L))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(placeScrapService);
        }

        @Test
        @DisplayName("placeFolderId가 없으면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenPlaceFolderIdMissing() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", 2L, null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(placeScrapService);
        }

        @Test
        @DisplayName("존재하지 않는 스티커면 404를 반환한다")
        void returnsNotFoundWhenStickerMissing() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.STICKER_NOT_FOUND))
                    .given(placeScrapService).createPlaceScrap(any(), any());

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", 2L, 3L))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("STICKER-001"));
        }

        @Test
        @DisplayName("다른 유저 소유 스티커면 403을 반환한다")
        void returnsForbiddenWhenStickerNotOwned() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.STICKER_ACCESS_DENIED))
                    .given(placeScrapService).createPlaceScrap(any(), any());

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", 2L, 3L))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("STICKER-002"));
        }

        @Test
        @DisplayName("존재하지 않는 폴더면 404를 반환한다")
        void returnsNotFoundWhenPlaceFolderMissing() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.PLACE_FOLDER_NOT_FOUND))
                    .given(placeScrapService).createPlaceScrap(any(), any());

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", 2L, 3L))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE-002"));
        }

        @Test
        @DisplayName("다른 유저 소유 폴더면 403을 반환한다")
        void returnsForbiddenWhenPlaceFolderNotOwned() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.PLACE_FOLDER_ACCESS_DENIED))
                    .given(placeScrapService).createPlaceScrap(any(), any());

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", 2L, 3L))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PLACE-003"));
        }

        @Test
        @DisplayName("같은 폴더에 이미 저장된 장소면 409를 반환한다")
        void returnsConflictWhenAlreadyScrapped() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.PLACE_SCRAP_DUPLICATE))
                    .given(placeScrapService).createPlaceScrap(any(), any());

            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", 2L, 3L))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PLACE-004"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/my-places/scrap")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new PlaceScrapCreateRequest("13579", 2L, 3L))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(placeScrapService);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/my-places/folders/{placeFolderId}/scraps")
    class GetPlaceScraps {

        private static final String URL = "/api/v1/my-places/folders/{placeFolderId}/scraps";

        @Test
        @DisplayName("폴더 정보, 전체 개수, 현재 페이지(content/nextCursor/hasNext)를 반환한다")
        void returnsPlaceFolderScrapsPage() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.getPlaceScraps(any(), eq(3L), eq(AppLocale.KO), isNull(), eq(20))).willReturn(
                    response(57, new CursorPageResponse<>(List.of(
                            PlaceScrapResponse.builder()
                                    .placeScrapId(101L).tourismContentId("24680").placeName("해동용궁사")
                                    .placeAddress("부산광역시 기장군 기장읍 용궁길 86").stickerId(2L).build(),
                            PlaceScrapResponse.builder()
                                    .placeScrapId(100L).tourismContentId("13579").stickerId(2L).build()),
                            "next-cursor", true)));

            mockMvc.perform(get(URL, 3L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalScrapCount").value(57))
                    .andExpect(jsonPath("$.data.placeFolder.placeFolderId").value(3))
                    .andExpect(jsonPath("$.data.placeFolder.name").value("가보고 싶은 곳"))
                    .andExpect(jsonPath("$.data.placeFolder.icon").value("✈️"))
                    .andExpect(jsonPath("$.data.placeScraps.content.length()").value(2))
                    .andExpect(jsonPath("$.data.placeScraps.content[0].placeScrapId").value(101))
                    .andExpect(jsonPath("$.data.placeScraps.content[0].placeName").value("해동용궁사"))
                    .andExpect(jsonPath("$.data.placeScraps.content[0].placeAddress").value("부산광역시 기장군 기장읍 용궁길 86"))
                    .andExpect(jsonPath("$.data.placeScraps.content[0].stickerId").value(2))
                    .andExpect(jsonPath("$.data.placeScraps.content[1].placeName").doesNotExist())
                    .andExpect(jsonPath("$.data.placeScraps.nextCursor").value("next-cursor"))
                    .andExpect(jsonPath("$.data.placeScraps.hasNext").value(true));
        }

        @Test
        @DisplayName("cursor와 size를 서비스에 그대로 전달한다")
        void passesCursorAndSize() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.getPlaceScraps(any(), eq(3L), any(), eq("abc"), eq(10))).willReturn(emptyResponse());

            mockMvc.perform(get(URL, 3L).param("cursor", "abc").param("size", "10"))
                    .andExpect(status().isOk());

            verify(placeScrapService).getPlaceScraps(any(), eq(3L), any(), eq("abc"), eq(10));
        }

        @Test
        @DisplayName("size가 100을 넘으면 100으로 보정한다")
        void clampsSizeToMax() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.getPlaceScraps(any(), eq(3L), any(), isNull(), eq(100))).willReturn(emptyResponse());

            mockMvc.perform(get(URL, 3L).param("size", "500"))
                    .andExpect(status().isOk());

            verify(placeScrapService).getPlaceScraps(any(), eq(3L), any(), isNull(), eq(100));
        }

        @Test
        @DisplayName("size가 1보다 작으면 1로 보정한다")
        void clampsSizeToMin() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.getPlaceScraps(any(), eq(3L), any(), isNull(), eq(1))).willReturn(emptyResponse());

            mockMvc.perform(get(URL, 3L).param("size", "0"))
                    .andExpect(status().isOk());

            verify(placeScrapService).getPlaceScraps(any(), eq(3L), any(), isNull(), eq(1));
        }

        @Test
        @DisplayName("lang=EN이면 서비스에 EN locale을 전달한다")
        void passesEnglishLocaleWhenLangIsEn() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.getPlaceScraps(any(), eq(3L), eq(AppLocale.EN), isNull(), eq(20))).willReturn(emptyResponse());

            mockMvc.perform(get(URL, 3L).param("lang", "EN"))
                    .andExpect(status().isOk());

            verify(placeScrapService).getPlaceScraps(any(), eq(3L), eq(AppLocale.EN), isNull(), eq(20));
        }

        @Test
        @DisplayName("lang이 없으면 로그인 유저의 locale을 서비스에 전달한다")
        void passesUserLocaleWhenLangMissing() throws Exception {
            AppUser user = appUser(1L, "나그네");
            user.changeLocale(AppLocale.EN);
            authenticateAs(user);
            given(placeScrapService.getPlaceScraps(any(), eq(3L), eq(AppLocale.EN), isNull(), eq(20))).willReturn(emptyResponse());

            mockMvc.perform(get(URL, 3L))
                    .andExpect(status().isOk());

            verify(placeScrapService).getPlaceScraps(any(), eq(3L), eq(AppLocale.EN), isNull(), eq(20));
        }

        @Test
        @DisplayName("지원하지 않는 lang이면 400을 반환하고 서비스는 호출하지 않는다")
        void returnsBadRequestWhenLangUnsupported() throws Exception {
            authenticateAs(appUser(1L, "나그네"));

            mockMvc.perform(get(URL, 3L).param("lang", "JP"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));

            verifyNoInteractions(placeScrapService);
        }

        @Test
        @DisplayName("잘못된 커서면 400을 반환한다")
        void returnsBadRequestWhenCursorInvalid() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE))
                    .given(placeScrapService).getPlaceScraps(any(), eq(3L), any(), eq("broken"), anyInt());

            mockMvc.perform(get(URL, 3L).param("cursor", "broken"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"));
        }

        @Test
        @DisplayName("저장된 장소가 없으면 빈 페이지와 개수 0을 반환한다")
        void returnsEmptyPageWhenNoScraps() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            given(placeScrapService.getPlaceScraps(any(), eq(3L), any(), isNull(), eq(20))).willReturn(emptyResponse());

            mockMvc.perform(get(URL, 3L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalScrapCount").value(0))
                    .andExpect(jsonPath("$.data.placeScraps.content").isEmpty())
                    .andExpect(jsonPath("$.data.placeScraps.hasNext").value(false))
                    .andExpect(jsonPath("$.data.placeScraps.nextCursor").doesNotExist());
        }

        @Test
        @DisplayName("존재하지 않는 폴더면 404를 반환한다")
        void returnsNotFoundWhenPlaceFolderMissing() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.PLACE_FOLDER_NOT_FOUND))
                    .given(placeScrapService).getPlaceScraps(any(), eq(3L), any(), any(), anyInt());

            mockMvc.perform(get(URL, 3L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLACE-002"));
        }

        @Test
        @DisplayName("다른 유저 소유 폴더면 403을 반환한다")
        void returnsForbiddenWhenPlaceFolderNotOwned() throws Exception {
            authenticateAs(appUser(1L, "나그네"));
            willThrow(new BusinessException(ErrorCode.PLACE_FOLDER_ACCESS_DENIED))
                    .given(placeScrapService).getPlaceScraps(any(), eq(3L), any(), any(), anyInt());

            mockMvc.perform(get(URL, 3L))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PLACE-003"));
        }

        @Test
        @DisplayName("인증 정보가 없으면 401을 반환하고 서비스는 호출하지 않는다")
        void returnsUnauthorizedWhenNotAuthenticated() throws Exception {
            mockMvc.perform(get(URL, 3L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));

            verifyNoInteractions(placeScrapService);
        }

        private PlaceFolderScrapsResponse emptyResponse() {
            return response(0, new CursorPageResponse<>(List.of(), null, false));
        }

        private PlaceFolderScrapsResponse response(int totalScrapCount, CursorPageResponse<PlaceScrapResponse> page) {
            return PlaceFolderScrapsResponse.builder()
                    .totalScrapCount(totalScrapCount)
                    .placeFolder(PlaceFolderResponse.builder()
                            .placeFolderId(3L).name("가보고 싶은 곳").icon("✈️").isDefault(false).build())
                    .placeScraps(page)
                    .build();
        }
    }

    private void authenticateAs(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of()));
    }

    private AppUser appUser(Long id, String nickname) {
        AppUser user = AppUser.builder()
                .provider(Provider.GOOGLE)
                .providerUserId("google-uid")
                .email("test@test.com")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
