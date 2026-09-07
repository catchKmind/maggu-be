package com.maggu.maggu.auth.apple;

import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AppleAuthClient {

    static final String APPLE_BASE_URL = "https://appleid.apple.com";
    static final String TOKEN_PATH = "/auth/token";
    static final String REVOKE_PATH = "/auth/revoke";

    private final RestClient restClient;
    private final AppleProperties appleProperties;
    private final AppleClientSecretGenerator clientSecretGenerator;

    public AppleAuthClient(
            RestClient.Builder restClientBuilder,
            AppleProperties appleProperties,
            AppleClientSecretGenerator clientSecretGenerator
    ) {
        this.restClient = restClientBuilder.baseUrl(APPLE_BASE_URL).build();
        this.appleProperties = appleProperties;
        this.clientSecretGenerator = clientSecretGenerator;
    }

    public String exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> form = commonForm();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);

        try {
            AppleTokenResponse response = restClient.post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);
            if (response == null || response.refreshToken() == null || response.refreshToken().isBlank()) {
                throw new BusinessException(ErrorCode.AUTH_APPLE_TOKEN_EXCHANGE_FAILED);
            }
            return response.refreshToken();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Apple token 교환 실패 status={}", e.getStatusCode().value());
            throw new BusinessException(ErrorCode.AUTH_APPLE_TOKEN_EXCHANGE_FAILED);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.AUTH_APPLE_TOKEN_EXCHANGE_FAILED);
        }
    }

    public void revokeRefreshToken(String refreshToken) {
        MultiValueMap<String, String> form = commonForm();
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        try {
            restClient.post()
                    .uri(REVOKE_PATH)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 400) {
                log.warn("Apple revoke가 400을 반환했습니다. 이미 해제된 토큰으로 보고 계속합니다.");
                return;
            }
            log.warn("Apple revoke 실패 status={}", e.getStatusCode().value());
            throw new BusinessException(ErrorCode.AUTH_APPLE_REVOKE_FAILED);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.AUTH_APPLE_REVOKE_FAILED);
        }
    }

    private MultiValueMap<String, String> commonForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", appleProperties.bundleId());
        form.add("client_secret", clientSecretGenerator.create());
        return form;
    }
}
