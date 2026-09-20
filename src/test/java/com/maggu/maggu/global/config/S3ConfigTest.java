package com.maggu.maggu.global.config;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3ConfigTest {

    private final S3Config config = new S3Config();

    @Test
    @DisplayName("설정값으로 지정한 리전의 AmazonS3 클라이언트를 생성한다")
    void createsAmazonS3ClientWithConfiguredRegion() {
        AmazonS3 amazonS3 = config.amazonS3("test-access-key", "test-secret-key", "ap-northeast-2");

        assertThat(amazonS3).isNotNull();
        assertThat(amazonS3.getRegionName()).isEqualTo("ap-northeast-2");
    }
}
