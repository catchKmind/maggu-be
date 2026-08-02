package com.maggu.maggu.post.repository;

// PostRepository#findInBbox 네이티브 쿼리 결과를 매핑하는 인터페이스 프로젝션
public interface MapPostProjection {

    Long getPostId();

    String getSlug();

    Double getLng();

    Double getLat();

    int getScrapCount();

    String getTourismContentId();

    String getPlaceName();

    String getRepresentativeImageUrl();
}
