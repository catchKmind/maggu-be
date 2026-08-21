package com.maggu.maggu.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourismApiPropertiesTest {

    @Nested
    @DisplayName("connect-timeout / read-timeout 바인딩")
    class DurationBinding {

        @Test
        @DisplayName("초 단위 접미사(s)가 붙은 값이 Duration으로 바인딩된다")
        void bindsDurationPropertiesWithSecondsSuffix() {
            TourismApiProperties properties = bind(Map.of(
                    "tourism-api.base-url", "http://apis.data.go.kr/B551011/KorService2",
                    "tourism-api.service-key", "test-key",
                    "tourism-api.connect-timeout", "2s",
                    "tourism-api.read-timeout", "10s"
            ));

            assertThat(properties.baseUrl()).isEqualTo("http://apis.data.go.kr/B551011/KorService2");
            assertThat(properties.serviceKey()).isEqualTo("test-key");
            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(10));
        }

        @Test
        @DisplayName("밀리초 단위 접미사(ms)가 붙은 값도 Duration으로 바인딩된다")
        void bindsDurationPropertiesWithMillisSuffix() {
            TourismApiProperties properties = bind(Map.of(
                    "tourism-api.base-url", "http://apis.data.go.kr/B551011/KorService2",
                    "tourism-api.service-key", "test-key",
                    "tourism-api.connect-timeout", "500ms",
                    "tourism-api.read-timeout", "1500ms"
            ));

            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofMillis(500));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofMillis(1500));
        }

        @Test
        @DisplayName("단위 접미사 없이 숫자만 있으면 밀리초로 해석된다 — 그래서 yml에는 항상 접미사를 명시해야 한다")
        void bareNumberWithoutSuffixIsInterpretedAsMillis() {
            TourismApiProperties properties = bind(Map.of(
                    "tourism-api.base-url", "http://apis.data.go.kr/B551011/KorService2",
                    "tourism-api.service-key", "test-key",
                    "tourism-api.connect-timeout", "2000",
                    "tourism-api.read-timeout", "10000"
            ));

            assertThat(properties.connectTimeout()).isEqualTo(Duration.ofMillis(2000));
            assertThat(properties.readTimeout()).isEqualTo(Duration.ofMillis(10000));
        }

        @Test
        @DisplayName("Duration으로 해석할 수 없는 값이면 바인딩에 실패한다")
        void throwsWhenValueIsNotAValidDuration() {
            MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                    "tourism-api.base-url", "http://apis.data.go.kr/B551011/KorService2",
                    "tourism-api.service-key", "test-key",
                    "tourism-api.connect-timeout", "not-a-duration",
                    "tourism-api.read-timeout", "10s"
            ));
            Binder binder = new Binder(source);

            assertThatThrownBy(() -> binder.bind("tourism-api", TourismApiProperties.class).get())
                    .isInstanceOf(BindException.class);
        }

        private TourismApiProperties bind(Map<String, String> source) {
            Binder binder = new Binder(new MapConfigurationPropertySource(source));
            return binder.bind("tourism-api", TourismApiProperties.class).get();
        }
    }
}
