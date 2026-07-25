package com.maggu.maggu.global.response;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

@Getter
public class CursorPageResponse<T> {

    private final List<T> content;
    private final Long nextCursor;
    private final boolean hasNext;

    public CursorPageResponse(List<T> content, Long nextCursor, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static <T> CursorPageResponse<T> of(Slice<T> slice, Function<T, Long> cursorExtractor) {
        List<T> content = slice.getContent();
        Long nextCursor = slice.hasNext() && !content.isEmpty()
                ? cursorExtractor.apply(content.get(content.size() - 1))
                : null;
        return new CursorPageResponse<>(content, nextCursor, slice.hasNext());
    }
}
