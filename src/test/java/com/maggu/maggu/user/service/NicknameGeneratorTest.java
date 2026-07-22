package com.maggu.maggu.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import static org.assertj.core.api.Assertions.assertThat;

class NicknameGeneratorTest {

    private final NicknameGenerator nicknameGenerator = new NicknameGenerator();

    @RepeatedTest(50)
    @DisplayName("형용사+명사+숫자 형식이며 30자를 넘지 않는 닉네임을 생성한다")
    void generate() {
        String nickname = nicknameGenerator.generate();

        assertThat(nickname)
                .matches("^[가-힣]+\\d{1,4}$")
                .hasSizeLessThanOrEqualTo(30);
    }
}
