package com.maggu.maggu.map.service;

import com.maggu.maggu.map.dto.MapSpotDetail;
import com.maggu.maggu.post.dto.enums.FeedSort;
import com.maggu.maggu.post.dto.response.PostFeedItemResponse;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.post.service.FeedCursor;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.entity.enums.AppLocale;
import com.maggu.maggu.global.response.CursorPageResponse;
import com.maggu.maggu.map.cache.TourSpotCache;
import com.maggu.maggu.map.client.TourSpot;
import com.maggu.maggu.map.dto.AutocompleteCandidateResponse;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapSearchService {

    private static final int AUTOCOMPLETE_MAX_RESULTS = 6;
    private static final int SPOTS_MAX_RESULTS = 10;
    private static final Set<String> POPULAR_KEYWORDS = Set.of("HOT PLACES", "POPULAR", "인기");

    private final TourSpotCache tourSpotCache;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final MapService mapService;

    public List<AutocompleteCandidateResponse> getAutocompleteCandidates(String keyword) {
        return getAutocompleteCandidates(keyword, AppLocale.KO);
    }

    public List<AutocompleteCandidateResponse> getAutocompleteCandidates(String keyword, AppLocale locale) {
        String normalizeKeyword = normalizeKeyword(keyword);

        List<TourSpot> spots = tourSpotCache.findByKeyword(
                resolveLocale(locale), normalizeKeyword, AUTOCOMPLETE_MAX_RESULTS);

        return spots.stream()
                .map(spot ->
                        AutocompleteCandidateResponse.builder()
                                .contentId(spot.contentId())
                                .contentType(spot.contentType())
                                .title(spot.title())
                                .build())
                .toList();
    }

    public CursorPageResponse<PostFeedItemResponse> searchPosts(String keyword, FeedSort sort, String cursor, int size) {
        String normalizeKeyword = normalizeKeyword(keyword);

        FeedCursor decodedCursor = (cursor == null)
                ? null
                : FeedCursor.decode(cursor);
        Integer scrapCount = (decodedCursor == null)
                ? null
                : decodedCursor.scrapCount();
        Instant createdAt = (decodedCursor == null)
                ? null
                : decodedCursor.createdAt();
        Long cursorId = (decodedCursor == null)
                ? null
                : decodedCursor.id();

        boolean isPopularKeyword = isPopularKeyword(normalizeKeyword);

        List<Post> posts;
        if (sort == FeedSort.POPULAR) {
            posts = isPopularKeyword
                    ? postRepository.findAllPopular(scrapCount, createdAt, cursorId, size + 1)
                    : postRepository.findByKeywordPopular(normalizeKeyword, scrapCount, createdAt, cursorId, size + 1);
        } else if (sort == FeedSort.LATEST) {
            posts = isPopularKeyword
                    ? postRepository.findAllLatest(createdAt, cursorId, size + 1)
                    : postRepository.findByKeywordLatest(normalizeKeyword, createdAt, cursorId, size + 1);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        CursorPageResponse<Post> postCursorPageResponse = CursorPageResponse.of(posts, size, post -> FeedCursor.from(post).encode());

        Map<Long, String> thumbnailByPostId = postImageRepository.findByPostInOrderBySortOrderAsc(posts).stream()
                .collect(Collectors.toMap(
                        image -> image.getPost().getId(),
                        PostImage::getImageUrl,
                        (first, second) -> first // 첫 번째 값만 유효
                ));

        return postCursorPageResponse.map(post -> PostFeedItemResponse.builder()
                .postId(post.getId())
                .imageUrl(thumbnailByPostId.get(post.getId()))
                .build());
    }

    public List<MapSpotDetail> searchSpots(String keyword) {
        return searchSpots(keyword, AppLocale.KO);
    }

    public List<MapSpotDetail> searchSpots(String keyword, AppLocale locale) {
        String normalizeKeyword = normalizeKeyword(keyword);
        AppLocale resolved = resolveLocale(locale);

        List<String> contentIds = isPopularKeyword(normalizeKeyword)
                ? postRepository.findTopTourismContentIdsByScrapCount(SPOTS_MAX_RESULTS)
                : tourSpotCache.findByKeyword(resolved, normalizeKeyword, SPOTS_MAX_RESULTS).stream()
                .map(TourSpot::contentId)
                .toList();

        return contentIds.stream()
                .map(contentId -> {
                    try {
                        return mapService.getMapSpotDetail(contentId, resolved);
                    } catch (BusinessException e) {
                        if (e.getErrorCode() == ErrorCode.MAP_CONTENT_NOT_FOUND) {
                            return null;
                        }
                        throw e;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private AppLocale resolveLocale(AppLocale locale) {
        return locale == null ? AppLocale.KO : locale;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return keyword.trim();
    }

    private boolean isPopularKeyword(String normalizeKeyword) {
        return POPULAR_KEYWORDS.contains(normalizeKeyword.toUpperCase());
    }
}
