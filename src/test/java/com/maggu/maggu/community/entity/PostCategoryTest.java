package com.maggu.maggu.community.entity;

import com.maggu.maggu.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostCategoryTest {

    @Test
    @DisplayName("추천/질문 카테고리와 별칭을 매핑한다")
    void mapsRecommendAndQuestion() {
        assertThat(PostCategory.from("RECOMMEND")).isEqualTo(PostCategory.RECOMMEND);
        assertThat(PostCategory.from("추천")).isEqualTo(PostCategory.RECOMMEND);
        assertThat(PostCategory.from("QUESTION")).isEqualTo(PostCategory.QUESTION);
        assertThat(PostCategory.from("QNA")).isEqualTo(PostCategory.QUESTION);
        assertThat(PostCategory.from("질문")).isEqualTo(PostCategory.QUESTION);
    }

    @Test
    @DisplayName("알 수 없는 카테고리는 거부한다")
    void rejectsUnknown() {
        assertThatThrownBy(() -> PostCategory.from("UNKNOWN"))
                .isInstanceOf(BusinessException.class);
    }
}
