package com.maggu.maggu.post.dto.enums;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedSortTest {

    @Test
    @DisplayName("latest와 most_scrapped만 허용한다")
    void parsesCanonicalValues() {
        assertThat(FeedSort.from("latest")).isEqualTo(FeedSort.LATEST);
        assertThat(FeedSort.from("LATEST")).isEqualTo(FeedSort.LATEST);
        assertThat(FeedSort.from("most_scrapped")).isEqualTo(FeedSort.MOST_SCRAPPED);
        assertThat(FeedSort.from("MOST_SCRAPPED")).isEqualTo(FeedSort.MOST_SCRAPPED);
        assertThat(FeedSort.from("most-scrapped")).isEqualTo(FeedSort.MOST_SCRAPPED);
    }

    @Test
    @DisplayName("값이 없으면 최신순으로 둔다")
    void defaultsBlankToLatest() {
        assertThat(FeedSort.fromOrDefault(null)).isEqualTo(FeedSort.LATEST);
        assertThat(FeedSort.fromOrDefault("")).isEqualTo(FeedSort.LATEST);
        assertThat(FeedSort.fromOrDefault("  ")).isEqualTo(FeedSort.LATEST);
    }

    @Test
    @DisplayName("popular, Most scrapped 같은 별칭은 거부한다")
    void rejectsAliases() {
        assertThatThrownBy(() -> FeedSort.from("popular"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        assertThatThrownBy(() -> FeedSort.from("Most scrapped"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> FeedSort.from("POPULAR"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> FeedSort.fromOrDefault("popular"))
                .isInstanceOf(BusinessException.class);
    }
}
