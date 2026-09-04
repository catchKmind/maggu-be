package com.maggu.maggu.global.response;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

@Getter
@RequiredArgsConstructor
@Builder
public class CursorPageResponse<T> {

    private final List<T> content;
    private final String nextCursor;
    private final boolean hasNext;

    /*
     * fetched:리포지토리가 size+1개 가져온 원본 리스트
     * cursorExtractor: "T를 보고 다음 커서 문자열을 어떻게 만들지 알려주는 함수
     */
    public static <T> CursorPageResponse<T> of(List<T> fetched, int size, Function<T, String> cursorExtractor) {
        boolean hasNext = fetched.size() > size; // size+1개 다 왔으면 더 있다는 뜻
        List<T> content = hasNext ? fetched.subList(0, size) : fetched; // size로 잘라내기
        String nextCursor = hasNext
                ? cursorExtractor.apply(content.get(content.size() - 1)) // 잘라낸 마지막 항목에서 다음 커서 추출
                : null;
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }

    public <R> CursorPageResponse<R> map(Function<T, R> mapper) {
        return new CursorPageResponse<>(content.stream().map(mapper).toList(), nextCursor, hasNext);
    }
}
