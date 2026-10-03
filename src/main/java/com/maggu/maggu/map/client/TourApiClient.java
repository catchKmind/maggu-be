package com.maggu.maggu.map.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maggu.maggu.global.config.TourismApiProperties;
import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.map.dto.MapSpotDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

@Slf4j
@Component
public class TourApiClient {

    private static final String LOCATION_BASED_LIST_PATH = "/locationBasedList2";
    private static final String DETAIL_COMMON_PATH = "/detailCommon2";
    private static final String AREA_BASED_LIST_PATH = "/areaBasedList2";
    private static final String DETAIL_IMAGE_PATH = "/detailImage2";
    private static final String DETAIL_INTRO_PATH = "/detailIntro2";
    private static final String SEARCH_FESTIVAL_PATH = "/searchFestival2";

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "maggu";
    private static final String RESPONSE_TYPE = "json";
    private static final String ARRANGE_BY_DISTANCE = "E";
    private static final String SUCCESS_RESULT_CODE = "0000";
    private static final String AREA_BATCH_NUM_OF_ROWS = "4000";
    private static final String DETAIL_IMAGE_NUM_OF_ROWS = "3";
    private static final String FESTIVAL_NUM_OF_ROWS = "1000";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient korTourApiRestClient;
    private final RestClient engTourApiRestClient;
    private final Executor tourApiExecutor;
    private final TourismApiProperties properties;
    private final ObjectMapper objectMapper;

    public TourApiClient(
            @Qualifier("korTourApiRestClient") RestClient korTourApiRestClient,
            @Qualifier("engTourApiRestClient") RestClient engTourApiRestClient,
            @Qualifier("tourApiExecutor") Executor tourApiExecutor,
            TourismApiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.korTourApiRestClient = korTourApiRestClient;
        this.engTourApiRestClient = engTourApiRestClient;
        this.tourApiExecutor = tourApiExecutor;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<FestivalSpot> searchFestival(TourServiceArea area) {
        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DATE_FORMATTER);

        String rawBody = requestSearchFestival(area, today);

        return parseFestivalSpots(rawBody);
    }

    public MapSpotDetail findSpotDetail(String contentId) {
        return findSpotDetail(contentId, AppLocale.KO);
    }

    public MapSpotDetail findSpotDetail(String contentId, AppLocale locale) {
        AppLocale resolved = resolveLocale(locale);
        CompletableFuture<String> detailCommonFuture;
        try {
            detailCommonFuture = CompletableFuture.supplyAsync(
                    () -> requestDetailCommonRawBody(contentId, resolved), tourApiExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("[MAP] 스레드 풀 포화로 상세조회 거절, {} 응답: contentId={}, locale={}",
                    ErrorCode.TOURISM_API_OVERLOADED.getCode(), contentId, resolved);
            throw new BusinessException(ErrorCode.TOURISM_API_OVERLOADED);
        }

        CompletableFuture<String> imageFuture = supplyOptionalAsync(
                () -> requestDetailImageRawBody(contentId, resolved), "이미지", contentId);

        CompletableFuture<String> introFuture = detailCommonFuture.thenCompose(detailCommonRawBody -> {
            String contentTypeId = extractContentTypeId(detailCommonRawBody);
            ContentType contentType = ContentType.fromId(Integer.parseInt(contentTypeId));

            if (contentType.supportsDetailIntro()) {
                return supplyOptionalAsync(
                        () -> requestDetailIntroRawBody(contentId, contentTypeId, resolved), "영업 관련 정보", contentId);
            } else {
                log.warn("[MAP] 서비스에서 지원하는 콘텐츠 타입 아님, 지원하는 콘텐츠 타입: 관광지/축제/음식점: contentId={}, contentTypeId={}", contentId, contentTypeId);
                return CompletableFuture.completedFuture(null);
            }
        });

        try {
            String detailCommonRawBody = detailCommonFuture.join();
            String imageRawBody = imageFuture.join();
            String introRawBody = introFuture.join();

            return parseSpotDetail(detailCommonRawBody, imageRawBody, introRawBody);
        } catch (CompletionException e) {
            if (e.getCause() instanceof BusinessException businessException) {
                throw businessException;
            } else {
                log.warn("예상치 못한 예외 발생", e.getCause());
                throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR);
            }
        }
    }

    // 실패해도 상세조회 전체를 실패시키지 않는 부가 정보(이미지/영업시간) 호출용
    // 실행 중 실패(.exceptionally)와 풀 포화로 인한 제출 거절(RejectedExecutionException) 모두 null로 대체
    // 거절은 supplyAsync 호출 시점에 동기적으로 던져져 .exceptionally로는 잡히지 않으므로 따로 catch
    private CompletableFuture<String> supplyOptionalAsync(Supplier<String> request, String target, String contentId) {
        try {
            return CompletableFuture.supplyAsync(request, tourApiExecutor)
                    .exceptionally(throwable -> {
                        log.warn("[MAP] {} 조회 실패, null로 대체: contentId={}", target, contentId, throwable);
                        return null;
                    });
        } catch (RejectedExecutionException e) {
            log.warn("[MAP] 스레드 풀 포화로 {} 조회 생략, null로 대체: contentId={}", target, contentId);
            return CompletableFuture.completedFuture(null);
        }
    }

    public List<TourSpot> findAllByArea(TourServiceArea area) {
        return findAllByArea(area, AppLocale.KO);
    }

    public List<TourSpot> findAllByArea(TourServiceArea area, AppLocale locale) {
        String rawBody = requestAreaBasedListRawBody(area, resolveLocale(locale));

        return parseAreaSpots(rawBody);
    }

    public Optional<ContentType> findContentType(String contentId) {
        String rawBody = requestDetailCommonRawBody(contentId, AppLocale.KO);

        return parseContentType(rawBody);
    }

    /*
     * 좌표+반경 안의 관광지 후보 조회
     * contentTypeId가 null이면 타입 제한 없이 조회함
     */
    public List<TourSpot> findByLocation(
            double mapX, double mapY, int radiusMeters, Integer contentTypeId, int numOfRows) {

        String rawBody = requestLocationBasedListRawBody(
                mapX, mapY, radiusMeters, contentTypeId, numOfRows, AppLocale.KO);

        return parseSpots(rawBody);
    }

    private <T> TourApiRawResponse<T> validateRawResponse(Class<T> itemType, String rawBody) {
        TourApiRawResponse<T> response = readRawResponse(itemType, rawBody);

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

    private <T> TourApiRawResponse<T> readRawResponse(Class<T> itemType, String rawBody) {
        try {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(TourApiRawResponse.class, itemType);
            return objectMapper.readValue(rawBody, type);
        } catch (JsonProcessingException e) {
            log.warn("TourAPI 응답 파싱 실패: body={}", rawBody, e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답 파싱 실패");
        }
    }

    private RestClient restClient(AppLocale locale) {
        return locale == AppLocale.EN ? engTourApiRestClient : korTourApiRestClient;
    }

    private AppLocale resolveLocale(AppLocale locale) {
        return locale == null ? AppLocale.KO : locale;
    }

    // 지역기반 관광정보 조회 API 호출
    private String requestAreaBasedListRawBody(TourServiceArea area, AppLocale locale) {
        return executeRequest(() -> restClient(locale).get()
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
                .body(String.class));
    }

    // 공통 정보 조회 API 호출
    private String requestDetailCommonRawBody(String contentId, AppLocale locale) {
        return executeRequest(() -> restClient(locale).get()
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
                .body(String.class));
    }

    // 위치기반 관광정보 조회 API 호출
    private String requestLocationBasedListRawBody(double mapX, double mapY, int radiusMeters, Integer contentTypeId,
                                                   int numOfRows, AppLocale locale) {
        return executeRequest(() -> restClient(locale).get()
                .uri(uriBuilder -> {
                    uriBuilder.path(LOCATION_BASED_LIST_PATH)
                            .queryParam("arrange", ARRANGE_BY_DISTANCE)
                            .queryParam("mapX", mapX)
                            .queryParam("mapY", mapY)
                            .queryParam("radius", radiusMeters)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("MobileOS", MOBILE_OS)
                            .queryParam("MobileApp", MOBILE_APP)
                            .queryParam("serviceKey", properties.serviceKey())
                            .queryParam("_type", RESPONSE_TYPE);
                    if (contentTypeId != null) {
                        uriBuilder.queryParam("contentTypeId", contentTypeId);
                    }

                    return uriBuilder.build();
                })
                .retrieve()
                .body(String.class));
    }

    // 이미지 정보 조회 API 호출
    private String requestDetailImageRawBody(String contentId, AppLocale locale) {
        return executeRequest(() -> restClient(locale).get()
                .uri(uriBuilder -> {
                    uriBuilder.path(DETAIL_IMAGE_PATH)
                            .queryParam("contentId", contentId)
                            .queryParam("numOfRows", DETAIL_IMAGE_NUM_OF_ROWS)
                            .queryParam("MobileOS", MOBILE_OS)
                            .queryParam("MobileApp", MOBILE_APP)
                            .queryParam("serviceKey", properties.serviceKey())
                            .queryParam("_type", RESPONSE_TYPE);
                    return uriBuilder.build();
                }).retrieve()
                .body(String.class));
    }

    // 영업 시간 조회 API 호출
    private String requestDetailIntroRawBody(String contentId, String contentTypeId, AppLocale locale) {
        return executeRequest(() -> restClient(locale).get()
                .uri(uriBuilder -> {
                    uriBuilder.path(DETAIL_INTRO_PATH)
                            .queryParam("contentId", contentId)
                            .queryParam("contentTypeId", contentTypeId)
                            .queryParam("MobileOS", MOBILE_OS)
                            .queryParam("MobileApp", MOBILE_APP)
                            .queryParam("serviceKey", properties.serviceKey())
                            .queryParam("_type", RESPONSE_TYPE);
                    return uriBuilder.build();
                }).retrieve()
                .body(String.class));
    }

    // 행사 정보 조회 API 호출
    private String requestSearchFestival(TourServiceArea area, String today) {
        return executeRequest(() -> restClient(AppLocale.KO).get()
                .uri(uriBuilder -> {
                    uriBuilder.path(SEARCH_FESTIVAL_PATH)
                            .queryParam("eventStartDate", today)
                            .queryParam("eventEndDate", today)
                            .queryParam("lDongRegnCd", area.getLDongRegnCd())
                            .queryParam("numOfRows", FESTIVAL_NUM_OF_ROWS)
                            .queryParam("MobileOS", MOBILE_OS)
                            .queryParam("MobileApp", MOBILE_APP)
                            .queryParam("serviceKey", properties.serviceKey())
                            .queryParam("_type", RESPONSE_TYPE);
                    return uriBuilder.build();
                }).retrieve()
                .body(String.class));
    }

    private String executeRequest(Supplier<String> requestSupplier) {
        try {
            return requestSupplier.get();
        } catch (RestClientResponseException e) {
            log.warn("TourAPI 호출이 오류 상태코드 반환: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 호출이 오류 상태코드 반환");
        } catch (RestClientException e) {
            log.warn("TourAPI 호출 실패: ", e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 호출 실패");
        }
    }

    private String extractContentTypeId(String detailCommonRawBody) {
        TourApiRawResponse<DetailCommonItem> response = validateRawResponse(DetailCommonItem.class, detailCommonRawBody);

        return response.response().body().items().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MAP_CONTENT_NOT_FOUND))
                .contentTypeId();
    }

    private List<FestivalSpot> parseFestivalSpots(String rawBody) {
        TourApiRawResponse<SearchFestivalItem> response = validateRawResponse(SearchFestivalItem.class, rawBody);

        return response.response().body().items().stream()
                .map(this::toFestivalSpotOrNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private Optional<ContentType> parseContentType(String rawBody) {
        List<TourSpot> spots = parseSpots(rawBody);
        return spots.isEmpty() ? Optional.empty() : Optional.of(spots.get(0).contentType());
    }

    private List<TourSpot> parseAreaSpots(String rawBody) {
        TourApiRawResponse<AreaBasedItem> response = validateRawResponse(AreaBasedItem.class, rawBody);

        return response.response().body().items().stream()
                .map(this::toAreaSpotOrNull)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<TourSpot> parseSpots(String rawBody) {
        TourApiRawResponse<LocationBasedItem> response = validateRawResponse(LocationBasedItem.class, rawBody);

        try {
            return response.response().body().items().stream()
                    .map(this::toSpot)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.warn("TourAPI 응답의 필드 값을 해석할 수 없음: body={}", rawBody, e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답의 필드 값을 해석할 수 없음");
        }
    }

    private MapSpotDetail parseSpotDetail(String detailCommonRawBody, String imageRawBody, String introRawBody) {
        TourApiRawResponse<DetailCommonItem> response = validateRawResponse(DetailCommonItem.class, detailCommonRawBody);

        List<String> images = List.of();
        if (imageRawBody != null) {
            try {
                TourApiRawResponse<DetailImageItem> imageResponse = validateRawResponse(DetailImageItem.class, imageRawBody);
                images = imageResponse.response().body().items().stream()
                        .map(DetailImageItem::originImgUrl)
                        .toList();
            } catch (BusinessException e) {
                log.warn("이미지 응답 검증 실패, 빈 이미지로 대체: {}", e.getMessage());
            }
        }

        try {
            DetailCommonItem detailCommonItem = response.response().body().items().stream()
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.MAP_CONTENT_NOT_FOUND));

            String businessHours = null;
            String closedDays = null;
            String eventPeriod = null;
            String introTel = null;
            if (introRawBody != null) {
                try {
                    switch (ContentType.fromId(Integer.parseInt(detailCommonItem.contentTypeId()))) {
                        case TOURIST_ATTRACTION -> {
                            TourApiRawResponse<DetailIntroAttractionItem> introResponse = validateRawResponse(DetailIntroAttractionItem.class, introRawBody);
                            DetailIntroAttractionItem item = introResponse.response().body().items().stream()
                                    .findFirst()
                                    .orElse(null);
                            if (item != null) {
                                businessHours = item.useTime();
                                closedDays = item.restDate();
                                introTel = item.infoCenter();
                            }
                        }
                        case FESTIVAL -> {
                            TourApiRawResponse<DetailIntroEventItem> introResponse = validateRawResponse(DetailIntroEventItem.class, introRawBody);
                            DetailIntroEventItem item = introResponse.response().body().items().stream()
                                    .findFirst()
                                    .orElse(null);
                            if (item != null) {
                                businessHours = item.playTime();
                                eventPeriod = item.eventStartDate() + " - " + item.eventEndDate();
                                introTel = item.sponsorTel();
                            }
                        }
                        case RESTAURANT -> {
                            TourApiRawResponse<DetailIntroRestaurantItem> introResponse = validateRawResponse(DetailIntroRestaurantItem.class, introRawBody);
                            DetailIntroRestaurantItem item = introResponse.response().body().items().stream()
                                    .findFirst()
                                    .orElse(null);
                            if (item != null) {
                                businessHours = item.openTime();
                                closedDays = item.restDate();
                                introTel = item.infoCenter();
                            }
                        }
                        default ->
                                log.warn("영업시간 정보가 없는 타입: contentId={}, contentTypeId={}", detailCommonItem.contentId(), detailCommonItem.contentTypeId());
                    }
                } catch (BusinessException e) {
                    log.warn("영업시간 관련 응답 검증 실패, null로 대체: {}", e.getMessage());
                }
            }

            String tel = (detailCommonItem.tel() == null || detailCommonItem.tel().isBlank())
                    ? introTel
                    : detailCommonItem.tel();

            return toSpotDetail(detailCommonItem, images, businessHours, closedDays, eventPeriod, tel);
        } catch (IllegalArgumentException e) {
            log.warn("TourAPI 응답의 필드 값을 해석할 수 없음: body={}", detailCommonRawBody, e);
            throw new BusinessException(ErrorCode.EXTERNAL_TOURISM_API_ERROR, "TourAPI 응답의 필드 값을 해석할 수 없음");
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

    private FestivalSpot toFestivalSpotOrNull(SearchFestivalItem item) {
        try {
            return FestivalSpot.builder()
                    .contentId(item.contentId())
                    .contentType(ContentType.fromId(Integer.parseInt(item.contentTypeId())))
                    .title(item.title())
                    .eventStartDate(LocalDate.parse(item.eventStartDate(), DATE_FORMATTER))
                    .eventEndDate(LocalDate.parse(item.eventEndDate(), DATE_FORMATTER))
                    .mapX(Double.parseDouble(item.mapX()))
                    .mapY(Double.parseDouble(item.mapY()))
                    .build();
        } catch (IllegalArgumentException | DateTimeParseException e) {
            log.warn("festival 배치 항목 파싱 실패, 건너뜀: contentId={}", item.contentId(), e);
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

    private MapSpotDetail toSpotDetail(DetailCommonItem item,
                                       List<String> images,
                                       String businessHours, String closedDays, String eventPeriod,
                                       String tel) {
        return new MapSpotDetail(
                item.contentId(),
                ContentType.fromId(Integer.parseInt(item.contentTypeId())),
                tel,
                item.title(),
                item.addr2() == null || item.addr2().isBlank()
                        ? item.addr1()
                        : item.addr1() + " " + item.addr2(),
                images,
                businessHours,
                closedDays,
                eventPeriod,
                Double.parseDouble(item.mapX()),
                Double.parseDouble(item.mapY())
        );
    }
}
