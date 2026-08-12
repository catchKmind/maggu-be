package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.response.*;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.PostCategory;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.entity.PostStickerReaction;
import com.maggu.maggu.community.repository.CommentRepository;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.community.repository.PostStickerReactionRepository;
import com.maggu.maggu.community.repository.ScrapRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private static final String SORT_POPULAR = "popular";

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final ScrapRepository scrapRepository;
    private final PostStickerReactionRepository reactionRepository;

    public PageResponse<PostSummaryResponse> getFeed(PostCategory category, String sort, AppUser viewer, int page, int size) {
        boolean popular = SORT_POPULAR.equalsIgnoreCase(sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> posts = category == null
                ? (popular
                ? postRepository.findByDeletedFalseOrderByScrapCountDescCreatedAtDesc(pageable)
                : postRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable))
                : (popular
                ? postRepository.findByCategoryAndDeletedFalseOrderByScrapCountDescCreatedAtDesc(category, pageable)
                : postRepository.findByCategoryAndDeletedFalseOrderByCreatedAtDesc(category, pageable));

        return PageResponse.from(toSummaryPage(posts, viewer));
    }

    public PageResponse<PostSummaryResponse> search(String keyword, String sort, AppUser viewer, int page, int size) {
        boolean popular = SORT_POPULAR.equalsIgnoreCase(sort);
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> posts = popular
                ? postRepository.searchByKeywordOrderByScrapCountDescCreatedAtDesc(keyword, pageable)
                : postRepository.searchByKeywordOrderByCreatedAtDesc(keyword, pageable);

        return PageResponse.from(toSummaryPage(posts, viewer));
    }

    public PostDetailResponse getDetail(Long postId, AppUser viewer) {
        Post post = getActivePost(postId);

        List<String> imageUrls = postImageRepository.findByPostOrderBySortOrderAsc(post).stream()
                .map(PostImage::getImageUrl)
                .toList();

        boolean scrappedByMe = scrapRepository.existsByUserAndPost(viewer, post);

        Map<String, Long> reactionCounts = new LinkedHashMap<>();
        for (PostStickerReactionRepository.StickerCount count : reactionRepository.countByPostGroupBySticker(post)) {
            reactionCounts.put(count.getSticker().getName(), count.getCount());
        }

        Optional<PostStickerReaction> myReaction = reactionRepository.findByPostAndUser(post, viewer);
        String myReactionSticker = myReaction.map(r -> r.getSticker().getName()).orElse(null);

        return toDetailResponse(post, imageUrls, scrappedByMe, reactionCounts, myReactionSticker);
    }

    public PostShareResponse getShareLink(Long postId) {
        Post post = getActivePost(postId);
        return PostShareResponse.builder()
                .postId(post.getId())
                .url("https://maggu.app/p/" + post.getSlug())
                .build();
    }

    public Post getActivePost(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private Page<PostSummaryResponse> toSummaryPage(Page<Post> posts, AppUser viewer) {
        List<Post> content = posts.getContent();
        if (content.isEmpty()) {
            return posts.map(post -> null);
        }

        Map<Long, List<String>> imagesByPostId = postImageRepository.findByPostInOrderBySortOrderAsc(content).stream()
                .collect(Collectors.groupingBy(
                        image -> image.getPost().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(PostImage::getImageUrl, Collectors.toList())
                ));

        Set<Long> scrappedPostIds = scrapRepository.findByUserAndPostIn(viewer, content).stream()
                .map(scrap -> scrap.getPost().getId())
                .collect(Collectors.toSet());

        // 1. 게시글별 스티커 반응 수 Batch Map
        Map<Long, Long> reactionCountMap = reactionRepository.countByPostInGrouped(content).stream()
                .collect(Collectors.toMap(
                        PostStickerReactionRepository.PostReactionCount::getPostId,
                        PostStickerReactionRepository.PostReactionCount::getCount
                ));

        // 2. 내가 반응한 게시글 ID Batch Set
        Set<Long> reactedPostIds = reactionRepository.findByUserAndPostIn(viewer, content).stream()
                .map(reaction -> reaction.getPost().getId())
                .collect(Collectors.toSet());

        return posts.map(post -> toSummaryResponse(
                post,
                imagesByPostId.getOrDefault(post.getId(), List.of()),
                commentRepository.countByPostAndDeletedFalse(post),
                reactionCountMap.getOrDefault(post.getId(), 0L),
                reactedPostIds.contains(post.getId()),
                scrappedPostIds.contains(post.getId())
        ));
    }

    private PostSummaryResponse toSummaryResponse(Post post, List<String> imageUrls, long commentCount,
                                                  long reactionCount, boolean reactedByMe, boolean scrappedByMe) {
        return PostSummaryResponse.builder()
                .postId(post.getId())
                .slug(post.getSlug())
                .writerNickname(post.getUser().getNickname())
                .content(post.getContent())
                .imageUrls(imageUrls)
                .placeName(post.getPlaceName())
                .category(post.getCategory())
                .reactionCount(reactionCount)
                .reactedByMe(reactedByMe)
                .scrapCount(post.getScrapCount())
                .commentCount(commentCount)
                .scrappedByMe(scrappedByMe)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private PostDetailResponse toDetailResponse(Post post, List<String> imageUrls, boolean scrappedByMe,
                                                Map<String, Long> reactionCounts, String myReactionSticker) {
        Double latitude = post.getLocation() != null ? post.getLocation().getY() : null;
        Double longitude = post.getLocation() != null ? post.getLocation().getX() : null;

        return PostDetailResponse.builder()
                .postId(post.getId())
                .slug(post.getSlug())
                .writerNickname(post.getUser().getNickname())
                .content(post.getContent())
                .imageUrls(imageUrls)
                .placeName(post.getPlaceName())
                .latitude(latitude)
                .longitude(longitude)
                .tourismContentId(post.getTourismContentId())
                .category(post.getCategory())
                .scrapCount(post.getScrapCount())
                .scrappedByMe(scrappedByMe)
                .stickerReactionCounts(reactionCounts)
                .myReactionSticker(myReactionSticker)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public SearchAutocompleteResponse getAutocomplete(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return SearchAutocompleteResponse.builder()
                    .keywords(List.of())
                    .build();
        }

        // 디자인 요구사항: 관련 연관 검색어 최대 6개 노출
        Pageable limitSix = PageRequest.of(0, 6);
        List<String> keywords = postRepository.findDistinctPlaceNamesByKeyword(keyword.trim(), limitSix);

        return SearchAutocompleteResponse.builder()
                .keywords(keywords)
                .build();
    }

    // 큐레이션 탭 조회 (주제별 상위 5개 게시글 슬라이드)
    public List<CurationResponse> getCuration(AppUser viewer) {
        // 예시 키워드임
        List<String> curationKeywords = List.of("야장", "단풍 명소", "불꽃축제", "지역축제");

        Pageable limitFive = PageRequest.of(0, 5);

        return curationKeywords.stream()
                .map(keyword -> {
                    Page<Post> postPage = postRepository.searchByKeywordOrderByScrapCountDescCreatedAtDesc(keyword, limitFive);
                    List<PostSummaryResponse> summaryPosts = toSummaryPage(postPage, viewer).getContent();

                    return CurationResponse.builder()
                            .title(keyword + " 추천")
                            .keyword(keyword)
                            .posts(summaryPosts)
                            .build();
                })
                .filter(curation -> !curation.getPosts().isEmpty()) // 게시글이 있는 큐레이션만 노출
                .toList();
    }


}