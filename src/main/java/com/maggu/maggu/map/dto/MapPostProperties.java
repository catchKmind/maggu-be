package com.maggu.maggu.map.dto;

import com.maggu.maggu.post.repository.MapPostProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * GeoJSON Feature의 properties
 * 지도 핀 렌더링에 필요한 게시글 정보(대표이미지/스크랩수/장소명 등)
 * */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapPostProperties {

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "게시글 slug", example = "ab12cd")
    private String slug;

    @Schema(description = "대표 이미지 URL (post_image 중 sort_order 최소값 1장)")
    private String representativeImageUrl;

    @Schema(description = "스크랩 수", example = "12")
    private int scrapCount;

    @Schema(description = "관광공사 콘텐츠 ID. 연결 안 된 글이면 null")
    private String tourismContentId;

    @Schema(description = "장소명 스냅샷")
    private String placeName;

    private MapPostProperties(Long postId, String slug, String representativeImageUrl,
                              int scrapCount, String tourismContentId, String placeName) {
        this.postId = postId;
        this.slug = slug;
        this.representativeImageUrl = representativeImageUrl;
        this.scrapCount = scrapCount;
        this.tourismContentId = tourismContentId;
        this.placeName = placeName;
    }

    public static MapPostProperties from(MapPostProjection projection) {
        return new MapPostProperties(
                projection.getPostId(),
                projection.getSlug(),
                projection.getRepresentativeImageUrl(),
                projection.getScrapCount(),
                projection.getTourismContentId(),
                projection.getPlaceName()
        );
    }
}
