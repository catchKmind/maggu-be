package com.maggu.maggu.community.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;       // 0부터 시작
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    // Spring Data Page -> PageResponse 변환. 도메인 매핑이 아니라 페이징 메타데이터 변환용이라 여기 둔다.
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}