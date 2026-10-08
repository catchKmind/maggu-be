package com.maggu.maggu.map.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.config.TourismApiProperties;
import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.dto.MapSpotDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiClientTest {

    private static final String BASE_URL = "http://tourapi.test";
    private static final String CONTENT_ID = "126234";

    private MockRestServiceServer mockServer;
    private RestClient restClient;
    private TourismApiProperties properties;
    private TourApiClient tourApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        // detailCommon2/detailImage2/detailIntro2가 CompletableFuture로 동시에 호출되므로 요청이 도착하는 순서를 보장할 수 없다 — 순서 무시 모드로 바인딩한다.
        mockServer = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        restClient = builder.build();
        properties = new TourismApiProperties(BASE_URL, BASE_URL, "test-service-key", Duration.ofSeconds(2), Duration.ofSeconds(2));
        // 호출 스레드에서 바로 실행 — 병렬성 없이 결과를 결정적으로 검증한다.
        tourApiClient = createClient(Runnable::run);
    }

    private TourApiClient createClient(Executor executor) {
        return new TourApiClient(restClient, restClient, executor, properties, new ObjectMapper());
    }

    // 앞의 allowedSubmissions건까지만 호출 스레드에서 실행하고, 그 이후 제출은 풀 포화처럼 거절한다.
    // Runnable::run 기반이라 제출 순서가 고정된다: detailCommon2(1) → detailImage2(2) → detailIntro2(3).
    private Executor rejectingAfter(int allowedSubmissions) {
        AtomicInteger submitted = new AtomicInteger();
        return task -> {
            if (submitted.incrementAndGet() > allowedSubmissions) {
                throw new RejectedExecutionException("thread pool saturated");
            }
            task.run();
        };
    }

    @Nested
    @DisplayName("findSpotDetail")
    class FindSpotDetail {

        @Test
        @DisplayName("관광지(12) 타입은 usetime/restdate가 businessHours/closedDays로 매핑된다")
        void mapsAttractionIntroFields() {
            expectDetailCommon(detailCommonJson("12"));
            expectDetailImage(emptyItemsJson());
            expectDetailIntro(introAttractionJson("매주 월요일", "09:00~18:00"));

            MapSpotDetail detail = tourApiClient.findSpotDetail(CONTENT_ID);

            assertThat(detail.businessHours()).isEqualTo("09:00~18:00");
            assertThat(detail.closedDays()).isEqualTo("매주 월요일");
            assertThat(detail.eventPeriod()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("지원하지 않는 콘텐츠 타입(14)이면 detailIntro2를 호출하지 않는다")
        void skipsIntroCallForUnsupportedType() {
            expectDetailCommon(detailCommonJson("14"));
            expectDetailImage(emptyItemsJson());
            // detailIntro2에 대한 expectation을 등록하지 않는다 — 실제로 호출되면 MockRestServiceServer가 "예상치 못한 요청"으로 테스트를 실패시킨다.

            MapSpotDetail detail = tourApiClient.findSpotDetail(CONTENT_ID);

            assertThat(detail.businessHours()).isNull();
            assertThat(detail.closedDays()).isNull();
            assertThat(detail.eventPeriod()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("detailIntro2가 실패 응답을 줘도 나머지 상세 정보는 정상 반환된다")
        void degradesGracefullyWhenIntroFails() {
            expectDetailCommon(detailCommonJson("39"));
            expectDetailImage(emptyItemsJson());
            expectDetailIntro(failureResponseJson());

            MapSpotDetail detail = tourApiClient.findSpotDetail(CONTENT_ID);

            assertThat(detail.title()).isEqualTo("테스트장소");
            assertThat(detail.tel()).isEqualTo("02-1234-5678");
            assertThat(detail.businessHours()).isNull();
            assertThat(detail.closedDays()).isNull();
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("findSpotDetail - 스레드 풀 포화로 제출이 거절될 때")
    class FindSpotDetailWhenRejected {

        @Test
        @DisplayName("핵심 정보(detailCommon2) 제출이 거절되면 TOURISM_API_OVERLOADED를 던지고 TourAPI를 호출하지 않는다")
        void throwsOverloadedWhenDetailCommonRejected() {
            TourApiClient client = createClient(rejectingAfter(0));
            // expectation을 등록하지 않는다 — 어떤 TourAPI 요청이든 나가면 테스트가 실패한다.

            assertThatThrownBy(() -> client.findSpotDetail(CONTENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOURISM_API_OVERLOADED));
            mockServer.verify();
        }

        @Test
        @DisplayName("이미지/영업시간 제출이 거절되면 해당 필드만 비우고 나머지 상세 정보는 정상 반환한다")
        void degradesWhenImageAndIntroRejected() {
            TourApiClient client = createClient(rejectingAfter(1));
            expectDetailCommon(detailCommonJson("12"));
            // detailImage2/detailIntro2는 거절되어 호출되지 않아야 하므로 expectation을 등록하지 않는다.

            MapSpotDetail detail = client.findSpotDetail(CONTENT_ID);

            assertThat(detail.title()).isEqualTo("테스트장소");
            assertThat(detail.images()).isEmpty();
            assertThat(detail.businessHours()).isNull();
            assertThat(detail.closedDays()).isNull();
            mockServer.verify();
        }

        @Test
        @DisplayName("thenCompose 안에서 제출되는 영업시간 조회가 거절돼도 502로 번지지 않고 null로 대체된다")
        void degradesWhenOnlyIntroRejected() {
            TourApiClient client = createClient(rejectingAfter(2));
            expectDetailCommon(detailCommonJson("12"));
            expectDetailImage(emptyItemsJson());
            // detailIntro2는 거절되어 호출되지 않아야 하므로 expectation을 등록하지 않는다.

            MapSpotDetail detail = client.findSpotDetail(CONTENT_ID);

            assertThat(detail.title()).isEqualTo("테스트장소");
            assertThat(detail.businessHours()).isNull();
            assertThat(detail.closedDays()).isNull();
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("TourAPI 오류 상태코드 응답 처리")
    class ErrorStatusResponse {

        @Test
        @DisplayName("게이트웨이가 한도 초과(22)를 반환하면 TOURISM_API_QUOTA_EXCEEDED를 던진다")
        void throwsQuotaExceededWhenGatewayReturns22() {
            expectDetailCommonError(HttpStatus.FORBIDDEN,
                    gatewayErrorJson("22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"));
            // detailCommon2가 실패해도 detailImage2는 이미 제출되어 요청이 나간다(Runnable::run). detailIntro2는 thenCompose가 실행되지 않아 호출되지 않는다.
            expectDetailImage(emptyItemsJson());

            assertThatThrownBy(() -> tourApiClient.findSpotDetail(CONTENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOURISM_API_QUOTA_EXCEEDED));
            mockServer.verify();
        }

        @Test
        @DisplayName("게이트웨이가 설정 오류(30, 미등록 서비스키)를 반환하면 쿼터 초과가 아닌 EXTERNAL_TOURISM_API_ERROR를 던진다")
        void throwsExternalErrorWhenGatewayReturnsActionRequiredCode() {
            expectDetailCommonError(HttpStatus.FORBIDDEN,
                    gatewayErrorJson("30", "SERVICE_KEY_IS_NOT_REGISTERED_ERROR"));
            expectDetailImage(emptyItemsJson());

            assertThatThrownBy(() -> tourApiClient.findSpotDetail(CONTENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_TOURISM_API_ERROR));
            mockServer.verify();
        }

        @Test
        @DisplayName("게이트웨이 오류 형식이 아닌 본문(JSON 아님)이면 파싱 실패로 터지지 않고 EXTERNAL_TOURISM_API_ERROR를 던진다")
        void throwsExternalErrorWhenBodyIsNotGatewayFormat() {
            mockServer.expect(requestTo(containsString("/detailCommon2")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body("Internal Server Error"));
            expectDetailImage(emptyItemsJson());

            assertThatThrownBy(() -> tourApiClient.findSpotDetail(CONTENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_TOURISM_API_ERROR));
            mockServer.verify();
        }

        @Test
        @DisplayName("부가 정보(detailImage2)만 한도 초과여도 상세조회는 정상 반환되고 이미지만 비어 있다")
        void degradesWhenOnlyImageQuotaExceeded() {
            // 14(문화시설)는 detailIntro2를 호출하지 않는 타입이라 expectation을 줄일 수 있다.
            expectDetailCommon(detailCommonJson("14"));
            mockServer.expect(requestTo(containsString("/detailImage2")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.FORBIDDEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(gatewayErrorJson("22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR")));

            MapSpotDetail detail = tourApiClient.findSpotDetail(CONTENT_ID);

            assertThat(detail.title()).isEqualTo("테스트장소");
            assertThat(detail.images()).isEmpty();
            mockServer.verify();
        }

        @Test
        @DisplayName("배치 경로(searchFestival)도 한도 초과를 TOURISM_API_QUOTA_EXCEEDED로 던진다")
        void throwsQuotaExceededOnBatchPathToo() {
            mockServer.expect(requestTo(containsString("/searchFestival2")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.FORBIDDEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(gatewayErrorJson("22", "LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR")));

            assertThatThrownBy(() -> tourApiClient.searchFestival(TourServiceArea.GB))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TOURISM_API_QUOTA_EXCEEDED));
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("searchFestival")
    class SearchFestival {

        @Test
        @DisplayName("정상 응답이면 축제 목록을 파싱해 반환한다")
        void returnsParsedFestivalSpots() {
            expectSearchFestival(searchFestivalJson(CONTENT_ID, "20260918", "20260920"));

            List<FestivalSpot> result = tourApiClient.searchFestival(TourServiceArea.GB);

            assertThat(result).hasSize(1);
            FestivalSpot spot = result.get(0);
            assertThat(spot.contentId()).isEqualTo(CONTENT_ID);
            assertThat(spot.contentType()).isEqualTo(ContentType.FESTIVAL);
            assertThat(spot.eventStartDate()).isEqualTo(LocalDate.of(2026, 9, 18));
            assertThat(spot.eventEndDate()).isEqualTo(LocalDate.of(2026, 9, 20));
            mockServer.verify();
        }

        @Test
        @DisplayName("날짜 형식이 잘못된 항목은 건너뛰고 나머지는 정상 반환된다(전체 실패시키지 않음)")
        void skipsItemWithMalformedDateInsteadOfFailingWholeBatch() {
            expectSearchFestival(searchFestivalJson(CONTENT_ID, "invalid-date", "20260920"));

            List<FestivalSpot> result = tourApiClient.searchFestival(TourServiceArea.GB);

            assertThat(result).isEmpty();
            mockServer.verify();
        }
    }

    @Nested
    @DisplayName("findAllByArea")
    class FindAllByArea {

        @Test
        @DisplayName("정상 응답이면 지역 스팟 목록을 파싱해 반환한다")
        void returnsParsedAreaSpots() {
            expectAreaBasedList(areaBasedListJson(List.of(areaItemJson("1", "12"), areaItemJson("2", "39"))));

            List<TourSpot> result = tourApiClient.findAllByArea(TourServiceArea.GB, AppLocale.KO);

            assertThat(result).extracting(TourSpot::contentId).containsExactly("1", "2");
            assertThat(result).extracting(TourSpot::contentType)
                    .containsExactly(ContentType.TOURIST_ATTRACTION, ContentType.RESTAURANT);
            mockServer.verify();
        }

        @Test
        @DisplayName("파싱에 실패한 항목은 건너뛰고 나머지는 정상 반환된다(전체 실패시키지 않음)")
        void skipsMalformedItemInsteadOfFailingWholeBatch() {
            expectAreaBasedList(areaBasedListJson(List.of(areaItemJson("1", "12"), areaItemJson("2", "invalid"))));

            List<TourSpot> result = tourApiClient.findAllByArea(TourServiceArea.GB, AppLocale.KO);

            assertThat(result).extracting(TourSpot::contentId).containsExactly("1");
            mockServer.verify();
        }
    }

    private void expectAreaBasedList(String responseJson) {
        mockServer.expect(requestTo(containsString("/areaBasedList2")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private String areaBasedListJson(List<String> items) {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[%s]}}}}
                """.formatted(String.join(",", items));
    }

    private String areaItemJson(String contentId, String contentTypeId) {
        return """
                {"contentid":"%s","contenttypeid":"%s","title":"장소%s","mapx":"128.6","mapy":"36.0"}
                """.formatted(contentId, contentTypeId, contentId);
    }

    private void expectSearchFestival(String responseJson) {
        mockServer.expect(requestTo(containsString("/searchFestival2")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private String searchFestivalJson(String contentId, String eventStartDate, String eventEndDate) {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":{
                  "contentid":"%s","contenttypeid":"15","title":"테스트축제",
                  "eventstartdate":"%s","eventenddate":"%s","mapx":"127.05","mapy":"37.55"
                }}}}}
                """.formatted(contentId, eventStartDate, eventEndDate);
    }

    private void expectDetailCommon(String responseJson) {
        mockServer.expect(requestTo(containsString("/detailCommon2")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private void expectDetailCommonError(HttpStatus status, String body) {
        mockServer.expect(requestTo(containsString("/detailCommon2")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body));
    }

    // 공공데이터포털 게이트웨이 오류 응답(_type=json). 실제로 잘못된 서비스키로 호출했을 때 받은 403 응답과 같은 구조.
    private String gatewayErrorJson(String reasonCode, String errMsg) {
        return """
                {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{
                  "errMsg":"%s","returnAuthMsg":"테스트","returnReasonCode":"%s"
                }}}
                """.formatted(errMsg, reasonCode);
    }

    private void expectDetailImage(String responseJson) {
        mockServer.expect(requestTo(containsString("/detailImage2")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private void expectDetailIntro(String responseJson) {
        mockServer.expect(requestTo(containsString("/detailIntro2")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private String detailCommonJson(String contentTypeId) {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":{
                  "contentid":"%s","contenttypeid":"%s","tel":"02-1234-5678","title":"테스트장소",
                  "addr1":"서울 용산구","addr2":"남산공원길 105","mapx":"127.05","mapy":"37.55"
                }}}}}
                """.formatted(CONTENT_ID, contentTypeId);
    }

    private String introAttractionJson(String restDate, String useTime) {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":{
                  "contentid":"%s","contenttypeid":"12","restdate":"%s","usetime":"%s","infocenter":"02-9999"
                }}}}}
                """.formatted(CONTENT_ID, restDate, useTime);
    }

    private String emptyItemsJson() {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":""}}}
                """;
    }

    private String failureResponseJson() {
        return """
                {"response":{"header":{"resultCode":"99","resultMsg":"ERROR"},"body":{"items":""}}}
                """;
    }
}
