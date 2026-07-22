package com.maggu.maggu.user.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of(
            "노래하는", "춤추는", "달리는", "웃는", "졸린",
            "배고픈", "행복한", "용감한", "게으른", "부지런한",
            "수줍은", "당당한", "느긋한", "성실한", "장난스런",
            "차분한", "씩씩한", "귀여운", "엉뚱한", "다정한"
    );

    private static final List<String> NOUNS = List.of(
            "사자", "호랑이", "코알라", "수달", "하마",
            "기린", "코끼리", "두더지", "오리", "햄스터",
            "판다", "너구리", "고양이", "강아지", "여우",
            "토끼", "다람쥐", "부엉이", "고래", "펭귄"
    );

    private static final int NUMERIC_SUFFIX_BOUND = 10000;

    public String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        int suffix = random.nextInt(NUMERIC_SUFFIX_BOUND);
        return adjective + noun + suffix;
    }
}
