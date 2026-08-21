package com.maggu.maggu.map.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.config.TourismApiProperties;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiClient {

    private static final String LOCATION_BASED_LIST_PATH = "/locationBasedList2";
    private static final String DETAIL_COMMON_2 = "/detailCommon2";
    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "maggu";
    private static final String RESPONSE_TYPE = "json";
    private static final String ARRANGE_BY_DISTANCE = "E";
    private static final String SUCCESS_RESULT_CODE = "0000";

    private final RestClient tourApiRestClient;
    private final TourismApiProperties properties;
    private final ObjectMapper objectMapper;

    public Optional<ContentType> findContentType(String contentId) {

        String rawBody = requestRawBody(contentId);

        return parseContentType(rawBody);
    }

    /*
     * 좌표+반경 안의 관광지 후보 조회
     * contentTypeId가 null이면 타입 제한 없이 조회함
     */
    public List<TourSpot> findByLocation(
            double mapX, double mapY, int radiusMeters, Integer contentTypeId, int numOfRows) {

        String rawBody = requestRawBody(mapX, mapY, radiusMeters, contentTypeId, numOfRows);

        return parseSpots(rawBody);
    }

    private TourApiRawResponse validateRawResponse(String rawBody) {
        TourApiRawResponse response = readRawResponse(rawBody);

        if (response.response() == null) {
            log.warn("TourAPI 응답 형식이 예상과 다름: body={}", rawBody);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답 형식이 예상과 다름");
        }

        String resultCode = response.response().header().resultCode();
        if (!SUCCESS_RESULT_CODE.equals(resultCode)) {
            log.warn("TourAPI가 실패 응답 반환: resultCode={}, resultMsg={}",
                    resultCode, response.response().header().resultMsg());
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI가 실패 응답 반환");
        }

        return response;
    }

    private Optional<ContentType> parseContentType(String rawBody) {
        List<TourSpot> spots = parseSpots(rawBody);
        return spots.isEmpty() ? Optional.empty() : Optional.of(spots.get(0).contentType());
    }

    private String requestRawBody(String contentId) {

        try {
            return tourApiRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(DETAIL_COMMON_2)
                                .queryParam("contentId", contentId)
                                .queryParam("MobileOS", MOBILE_OS)
                                .queryParam("MobileApp", MOBILE_APP)
                                .queryParam("serviceKey", properties.serviceKey())
                                .queryParam("_type", RESPONSE_TYPE);

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("TourAPI 호출이 오류 상태코드 반환: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 호출이 오류 상태코드 반환");
        } catch (RestClientException e) {
            log.warn("TourAPI 호출 실패: ", e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 호출 실패");
        }
    }

    private String requestRawBody(double mapX, double mapY, int radiusMeters, Integer contentTypeId, int numOfRows) {

        try {
            return tourApiRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(LOCATION_BASED_LIST_PATH)
                                .queryParam("pageNo", 1)
                                .queryParam("numOfRows", numOfRows)
                                .queryParam("MobileOS", MOBILE_OS)
                                .queryParam("MobileApp", MOBILE_APP)
                                .queryParam("serviceKey", properties.serviceKey())
                                .queryParam("_type", RESPONSE_TYPE)
                                .queryParam("arrange", ARRANGE_BY_DISTANCE)
                                .queryParam("mapX", mapX)
                                .queryParam("mapY", mapY)
                                .queryParam("radius", radiusMeters);
                        if (contentTypeId != null) {
                            uriBuilder.queryParam("contentTypeId", contentTypeId);
                        }

                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("TourAPI 호출이 오류 상태코드 반환: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 호출이 오류 상태코드 반환");
        } catch (RestClientException e) {
            log.warn("TourAPI 호출 실패: ", e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 호출 실패");
        }
    }

    private List<TourSpot> parseSpots(String rawBody) {
        TourApiRawResponse response = validateRawResponse(rawBody);

        try {
            return response.response().body().items().stream()
                    .map(this::toSpot)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.warn("TourAPI 응답의 필드 값을 해석할 수 없음: body={}", rawBody, e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답의 필드 값을 해석할 수 없음");
        }
    }

    private TourApiRawResponse readRawResponse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TourApiRawResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("TourAPI 응답 파싱 실패: body={}", rawBody, e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답 파싱 실패");
        }
    }

    private TourSpot toSpot(LocationBasedItem item) {
        return new TourSpot(
                item.contentId(),
                ContentType.fromId(Integer.parseInt(item.contentTypeId())),
                item.title(),
                Double.parseDouble(item.mapX()),
                Double.parseDouble(item.mapY())
        );
    }
}
