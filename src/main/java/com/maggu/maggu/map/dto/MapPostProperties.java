package com.maggu.maggu.map.dto;

import com.maggu.maggu.post.repository.MapPostProjection;
import io.swagger.v3.oas.annotations.media.Schema;

/*
 * GeoJSON Feature의 properties
 * 지도 핀 렌더링에 필요한 게시글 정보(대표이미지/스크랩수/장소명 등)
 */
public record MapPostProperties(
        @Schema(description = "게시글 ID", example = "1")
        Long postId,

        @Schema(description = "게시글 slug", example = "ab12cd")
        String slug,

        @Schema(description = "대표 이미지 URL (post_image 중 sort_order 최소값 1장)")
        String representativeImageUrl,

        @Schema(description = "스크랩 수", example = "12")
        int scrapCount,

        @Schema(description = "관광공사 콘텐츠 ID. 연결 안 된 글이면 null")
        String tourismContentId,

        @Schema(description = "장소명 스냅샷")
        String placeName,

        @Schema(description = "같은 관광지(tourismContentId)에 연결된 게시글 전체 개수. tourismContentId가 없으면 null")
        Integer placePostCount
) {
    public static MapPostProperties from(MapPostProjection projection, Integer placePostCount) {
        return new MapPostProperties(
                projection.getPostId(),
                projection.getSlug(),
                projection.getRepresentativeImageUrl(),
                projection.getScrapCount(),
                projection.getTourismContentId(),
                projection.getPlaceName(),
                placePostCount
        );
    }
}
