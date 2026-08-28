package com.maggu.maggu.map.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.config.TourismApiProperties;
import com.maggu.maggu.map.dto.MapSpotDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TourApiClientTest {

    private static final String BASE_URL = "http://tourapi.test";
    private static final String CONTENT_ID = "126234";

    private MockRestServiceServer mockServer;
    private TourApiClient tourApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        // detailCommon2/detailImage2/detailIntro2가 CompletableFuture로 동시에 호출되므로 요청이 도착하는 순서를 보장할 수 없다 — 순서 무시 모드로 바인딩한다.
        mockServer = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RestClient restClient = builder.build();

        TourismApiProperties properties =
                new TourismApiProperties(BASE_URL, "test-service-key", Duration.ofSeconds(2), Duration.ofSeconds(2));
        tourApiClient = new TourApiClient(restClient, properties, new ObjectMapper());
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

    private void expectDetailCommon(String responseJson) {
        mockServer.expect(requestTo(containsString("/detailCommon2")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
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
