package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CurationResponse {
    private String title;                     // 예: "야장 명소", "불꽃축제"
    private String keyword;                   // 검색 키워드
    private List<PostSummaryResponse> posts;  // 해당 주제의 상위 5개 게시글
}