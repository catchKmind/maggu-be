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
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiClient {

    private static final String LOCATION_BASED_LIST_PATH = "/locationBasedList2";
    private static final String DETAIL_COMMON_PATH = "/detailCommon2";
    private static final String AREA_BASED_LIST_PATH = "/areaBasedList2";

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "maggu";
    private static final String RESPONSE_TYPE = "json";
    private static final String ARRANGE_BY_DISTANCE = "E";
    private static final String SUCCESS_RESULT_CODE = "0000";
    private static final String AREA_BATCH_NUM_OF_ROWS = "4000";

    private final RestClient tourApiRestClient;
    private final TourismApiProperties properties;
    private final ObjectMapper objectMapper;

    public List<TourSpot> findAllByArea(TourServiceArea area) {

        String rawBody = requestRawBody(area);

        return parseAreaSpots(rawBody);
    }

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

    private TourApiAreaRawResponse validateAreaRawResponse(String rawBody) {
        TourApiAreaRawResponse response = readAreaRawResponse(rawBody);

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

    private TourApiLocationRawResponse validateLocationRawResponse(String rawBody) {
        TourApiLocationRawResponse response = readLocationRawResponse(rawBody);

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

    private String requestRawBody(TourServiceArea area) {
        try {
            return tourApiRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(AREA_BASED_LIST_PATH)
                                .queryParam("lDongRegnCd", area.getLDongRegnCd())
                                .queryParam("numOfRows", AREA_BATCH_NUM_OF_ROWS)
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

    private String requestRawBody(String contentId) {
        try {
            return tourApiRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(DETAIL_COMMON_PATH)
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

    private List<TourSpot> parseAreaSpots(String rawBody) {
        TourApiAreaRawResponse response = validateAreaRawResponse(rawBody);

        return response.response().body().items().stream()
                .map(this::toAreaSpotOrNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<TourSpot> parseSpots(String rawBody) {
        TourApiLocationRawResponse response = validateLocationRawResponse(rawBody);

        try {
            return response.response().body().items().stream()
                    .map(this::toSpot)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.warn("TourAPI 응답의 필드 값을 해석할 수 없음: body={}", rawBody, e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답의 필드 값을 해석할 수 없음");
        }
    }

    private TourApiAreaRawResponse readAreaRawResponse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TourApiAreaRawResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("TourAPI 응답 파싱 실패: body={}", rawBody, e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답 파싱 실패");
        }
    }

    private TourApiLocationRawResponse readLocationRawResponse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TourApiLocationRawResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("TourAPI 응답 파싱 실패: body={}", rawBody, e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답 파싱 실패");
        }
    }

    // 대량 배치 중 항목 하나가 깨져도 전체를 실패시키지 않음
    private TourSpot toAreaSpotOrNull(AreaBasedItem item) {
        try {
            return new TourSpot(
                    item.contentId(),
                    ContentType.fromId(Integer.parseInt(item.contentTypeId())),
                    item.title(),
                    Double.parseDouble(item.mapX()),
                    Double.parseDouble(item.mapY())
            );
        } catch (IllegalArgumentException e) {
            log.warn("area 배치 항목 파싱 실패, 건너뜀: contentId={}", item.contentId(), e);
            return null;
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
