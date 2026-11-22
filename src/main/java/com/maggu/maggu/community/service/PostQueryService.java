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
import com.maggu.maggu.global.storage.CloudFrontUrlResolver;
import com.maggu.maggu.post.dto.enums.FeedSort;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final ScrapRepository scrapRepository;
    private final PostStickerReactionRepository reactionRepository;
    private final CloudFrontUrlResolver cloudFrontUrlResolver;

    public PageResponse<PostSummaryResponse> getFeed(PostCategory category, FeedSort sort, AppUser viewer, int page, int size) {
        boolean mostScrapped = sort == FeedSort.MOST_SCRAPPED;
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> posts = category == null
                ? (mostScrapped
                ? postRepository.findByDeletedFalseOrderByScrapCountDescCreatedAtDesc(pageable)
                : postRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable))
                : (mostScrapped
                ? postRepository.findByCategoryAndDeletedFalseOrderByScrapCountDescCreatedAtDesc(category, pageable)
                : postRepository.findByCategoryAndDeletedFalseOrderByCreatedAtDesc(category, pageable));

        return PageResponse.from(toSummaryPage(posts, viewer));
    }

    public PageResponse<PostSummaryResponse> search(String keyword, FeedSort sort, AppUser viewer, int page, int size) {
        boolean mostScrapped = sort == FeedSort.MOST_SCRAPPED;
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> posts = mostScrapped
                ? postRepository.searchByKeywordOrderByScrapCountDescCreatedAtDesc(keyword, pageable)
                : postRepository.searchByKeywordOrderByCreatedAtDesc(keyword, pageable);

        return PageResponse.from(toSummaryPage(posts, viewer));
    }

    public PostDetailResponse getDetail(Long postId, AppUser viewer) {
        Post post = getActivePost(postId);

        List<String> imageUrls = postImageRepository.findByPostOrderBySortOrderAsc(post).stream()
                .map(image -> cloudFrontUrlResolver.toPublicUrl(image.getImageUrl()))
                .toList();

        boolean scrappedByMe = scrapRepository.existsByUserAndPost(viewer, post);

        Optional<PostStickerReaction> myReaction = reactionRepository.findByPostAndUser(post, viewer);
        Long myStickerId = myReaction.map(r -> r.getSticker().getId()).orElse(null);
        List<StickerReactionItem> stickerReactions = toReactionItemsFromStickerCounts(
                reactionRepository.countByPostGroupBySticker(post), myStickerId);

        return toDetailResponse(post, imageUrls, scrappedByMe, stickerReactions, myReaction.orElse(null));
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

    public PageResponse<PostSummaryResponse> toSummaryPageResponse(Page<Post> posts, AppUser viewer) {
        return PageResponse.from(toSummaryPage(posts, viewer));
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
                        Collectors.mapping(image -> cloudFrontUrlResolver.toPublicUrl(image.getImageUrl()), Collectors.toList())
                ));

        Set<Long> scrappedPostIds = scrapRepository.findByUserAndPostIn(viewer, content).stream()
                .map(scrap -> scrap.getPost().getId())
                .collect(Collectors.toSet());

        Map<Long, List<PostStickerReactionRepository.PostStickerCount>> reactionsByPostId =
                reactionRepository.countByPostInGroupBySticker(content).stream()
                        .collect(Collectors.groupingBy(PostStickerReactionRepository.PostStickerCount::getPostId));

        Map<Long, Long> myStickerIdByPostId = reactionRepository.findByUserAndPostIn(viewer, content).stream()
                .collect(Collectors.toMap(
                        reaction -> reaction.getPost().getId(),
                        reaction -> reaction.getSticker().getId()
                ));

        return posts.map(post -> {
            List<StickerReactionItem> stickerReactions = toReactionItemsFromPostCounts(
                    reactionsByPostId.getOrDefault(post.getId(), List.of()),
                    myStickerIdByPostId.get(post.getId()));
            long reactionCount = stickerReactions.stream().mapToLong(StickerReactionItem::count).sum();
            boolean reactedByMe = myStickerIdByPostId.containsKey(post.getId());
            return toSummaryResponse(
                    post,
                    imagesByPostId.getOrDefault(post.getId(), List.of()),
                    commentRepository.countByPostAndDeletedFalse(post),
                    reactionCount,
                    reactedByMe,
                    scrappedPostIds.contains(post.getId()),
                    stickerReactions
            );
        });
    }

    public List<StickerReactionItem> getStickerReactions(Post post, AppUser viewer) {
        Long myStickerId = reactionRepository.findByPostAndUser(post, viewer)
                .map(r -> r.getSticker().getId())
                .orElse(null);
        return toReactionItemsFromStickerCounts(reactionRepository.countByPostGroupBySticker(post), myStickerId);
    }

    private PostSummaryResponse toSummaryResponse(Post post, List<String> imageUrls, long commentCount,
                                                  long reactionCount, boolean reactedByMe, boolean scrappedByMe,
                                                  List<StickerReactionItem> stickerReactions) {
        return PostSummaryResponse.builder()
                .postId(post.getId())
                .slug(post.getSlug())
                .writerNickname(post.getUser() != null ? post.getUser().getNickname() : "탈퇴한 회원")
                .content(post.getContent())
                .imageUrls(imageUrls)
                .placeName(post.getPlaceName())
                .category(post.getCategory())
                .reactionCount(reactionCount)
                .reactedByMe(reactedByMe)
                .stickerReactions(stickerReactions)
                .scrapCount(post.getScrapCount())
                .commentCount(commentCount)
                .scrappedByMe(scrappedByMe)
                .createdAt(post.getCreatedAt())
                .build();
    }

    private PostDetailResponse toDetailResponse(Post post, List<String> imageUrls, boolean scrappedByMe,
                                                List<StickerReactionItem> stickerReactions,
                                                PostStickerReaction myReaction) {
        Double latitude = post.getLocation() != null ? post.getLocation().getY() : null;
        Double longitude = post.getLocation() != null ? post.getLocation().getX() : null;

        Map<String, Long> reactionCounts = new LinkedHashMap<>();
        for (StickerReactionItem item : stickerReactions) {
            reactionCounts.put(String.valueOf(item.stickerId()), item.count());
        }

        return PostDetailResponse.builder()
                .postId(post.getId())
                .slug(post.getSlug())
                .writerNickname(post.getUser() != null ? post.getUser().getNickname() : "탈퇴한 회원")
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
                .stickerReactions(stickerReactions)
                .myReactionSticker(myReaction == null ? null : myReaction.getSticker().getName())
                .myReactionStickerId(myReaction == null ? null : myReaction.getSticker().getId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private List<StickerReactionItem> toReactionItemsFromStickerCounts(
            List<PostStickerReactionRepository.StickerCount> counts, Long myStickerId) {
        return counts.stream()
                .map(count -> toReactionItem(count.getSticker(), count.getCount(), myStickerId))
                .toList();
    }

    private List<StickerReactionItem> toReactionItemsFromPostCounts(
            List<PostStickerReactionRepository.PostStickerCount> counts, Long myStickerId) {
        return counts.stream()
                .map(count -> toReactionItem(count.getSticker(), count.getCount(), myStickerId))
                .toList();
    }

    private StickerReactionItem toReactionItem(Sticker sticker, long count, Long myStickerId) {
        return StickerReactionItem.builder()
                .stickerId(sticker.getId())
                .imageUrl(cloudFrontUrlResolver.toPublicUrl(sticker.getImageUrl()))
                .count(count)
                .reactedByMe(myStickerId != null && myStickerId.equals(sticker.getId()))
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