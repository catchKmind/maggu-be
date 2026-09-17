package com.maggu.maggu.global.storage;

import com.maggu.maggu.global.config.CloudFrontProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudFrontUrlResolverTest {

    private static final String BUCKET = "maggu-bucket-381492214724-ap-northeast-2-an";
    private static final String CDN = "d111111abcdef8.cloudfront.net";

    private final CloudFrontUrlResolver resolver =
            new CloudFrontUrlResolver(new CloudFrontProperties(CDN), BUCKET);

    @Test
    @DisplayName("CloudFront domain이 없으면 원본 URL을 그대로 반환한다")
    void returnsOriginalWhenDomainIsBlank() {
        CloudFrontUrlResolver passthrough = new CloudFrontUrlResolver(new CloudFrontProperties(""), BUCKET);

        assertThat(passthrough.toPublicUrl("posts/1.jpg")).isEqualTo("posts/1.jpg");
    }

    @Test
    @DisplayName("객체 키는 CloudFront URL로 변환한다")
    void convertsObjectKey() {
        assertThat(resolver.toPublicUrl("posts/1.jpg"))
                .isEqualTo("https://d111111abcdef8.cloudfront.net/posts/1.jpg");
    }

    @Test
    @DisplayName("우리 버킷의 S3 virtual-hosted URL을 CloudFront URL로 변환한다")
    void convertsVirtualHostedS3Url() {
        String stored = "https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/posts/1.jpg";

        assertThat(resolver.toPublicUrl(stored))
                .isEqualTo("https://d111111abcdef8.cloudfront.net/posts/1.jpg");
    }

    @Test
    @DisplayName("우리 버킷의 S3 path-style URL을 CloudFront URL로 변환한다")
    void convertsPathStyleS3Url() {
        String stored = "https://s3.ap-northeast-2.amazonaws.com/" + BUCKET + "/posts/1.jpg";

        assertThat(resolver.toPublicUrl(stored))
                .isEqualTo("https://d111111abcdef8.cloudfront.net/posts/1.jpg");
    }

    @Test
    @DisplayName("s3:// 스킴 URL을 CloudFront URL로 변환한다")
    void convertsS3SchemeUrl() {
        assertThat(resolver.toPublicUrl("s3://" + BUCKET + "/posts/1.jpg"))
                .isEqualTo("https://d111111abcdef8.cloudfront.net/posts/1.jpg");
    }

    @Test
    @DisplayName("이미 CloudFront URL이면 그대로 반환한다")
    void keepsExistingCloudFrontUrl() {
        String stored = "https://d111111abcdef8.cloudfront.net/posts/1.jpg";

        assertThat(resolver.toPublicUrl(stored)).isEqualTo(stored);
    }

    @Test
    @DisplayName("외부 URL은 변환하지 않는다")
    void leavesExternalUrlUnchanged() {
        assertThat(resolver.toPublicUrl("https://img/external.jpg"))
                .isEqualTo("https://img/external.jpg");
    }
}
