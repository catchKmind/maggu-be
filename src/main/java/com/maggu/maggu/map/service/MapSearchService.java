package com.maggu.maggu.map.service;

import com.maggu.maggu.post.dto.enums.FeedSort;
import com.maggu.maggu.post.dto.response.PostFeedItemResponse;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.post.service.FeedCursor;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapSearchService {

    private static final int AUTOCOMPLETE_MAX_RESULTS = 6;

    private final TourSpotCache tourSpotCache;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;

    public List<AutocompleteCandidateResponse> getAutocompleteCandidates(String keyword) {
        List<TourSpot> spots = tourSpotCache.findByKeyword(keyword, AUTOCOMPLETE_MAX_RESULTS);

        return spots.stream()
                .map(spot ->
                        AutocompleteCandidateResponse.builder()
                                .contentId(spot.contentId())
                                .contentType(spot.contentType())
                                .title(spot.title())
                                .build())
                .toList();
    }

    public CursorPageResponse<PostFeedItemResponse> getPosts(String keyword, FeedSort sort, String cursor, int size) {
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

        List<Post> posts = null;
        if (sort == FeedSort.POPULAR) {
            posts = postRepository.findByKeywordPopular(keyword, scrapCount, createdAt, cursorId, size + 1);
        } else if (sort == FeedSort.LATEST) {
            posts = postRepository.findByKeywordLatest(keyword, createdAt, cursorId, size + 1);
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

}
