package com.maggu.maggu.global.security.jwt;

import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.global.security.CustomUserDetails;
import com.maggu.maggu.user.entity.AppUser;
import com.maggu.maggu.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 인증 실패 원인(ErrorCode)을 담아두는 request attribute 키. CustomAuthenticationEntryPoint가 읽습니다. */
    public static final String AUTH_ERROR_ATTRIBUTE = "authErrorCode";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository appUserRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);

                if (jwtTokenProvider.isRefreshToken(claims)) {
                    // Access Token 자리에 Refresh Token이 들어온 경우 인증을 거부합니다.
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.AUTH_INVALID_TOKEN);
                } else {
                    authenticate(request, claims);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.AUTH_EXPIRED_TOKEN);
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("JWT 파싱 실패: {}", e.getMessage());
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.AUTH_INVALID_TOKEN);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Claims claims) {
        Long userId = jwtTokenProvider.getUserId(claims);
        Optional<AppUser> appUserOpt = appUserRepository.findById(userId);

        if (appUserOpt.isEmpty()) {
            // 토큰은 유효하지만 대상 유저가 DB에 없는 경우 (탈퇴 등)
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, ErrorCode.AUTH_INVALID_TOKEN);
            return;
        }

        CustomUserDetails userDetails = new CustomUserDetails(appUserOpt.get());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}