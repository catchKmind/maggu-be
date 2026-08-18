package com.maggu.maggu.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-002", "지원하지 않는 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-003", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-999", "서버 내부 오류가 발생했습니다."),

    // User
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "USER-001", "이미 사용 중인 닉네임입니다."),

    // Auth
    AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-001", "인증이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-002", "유효하지 않은 토큰입니다."),
    AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "만료된 토큰입니다."),
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-004", "접근 권한이 없습니다."),

    // Map
    EXTERNAL_TOURISM_API_ERROR(HttpStatus.BAD_GATEWAY, "MAP-001", "관광 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST-001", "게시글을 찾을 수 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "POST-002", "본인이 작성한 게시글만 삭제할 수 있습니다."),
    POST_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "POST-003", "사진은 최대 4장까지 첨부할 수 있습니다."),
    POST_LOCATION_REQUIRED(HttpStatus.BAD_REQUEST, "POST-004", "사진이 포함된 게시글은 위치 정보가 필요합니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT-001", "댓글을 찾을 수 없습니다."),
    COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "COMMENT-002", "대댓글에는 답글을 달 수 없습니다."),

    // Folder / Scrap
    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLDER-001", "폴더를 찾을 수 없습니다."),
    FOLDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "FOLDER-002", "본인 소유의 폴더만 사용할 수 있습니다."),
    FOLDER_NAME_DUPLICATE(HttpStatus.CONFLICT, "FOLDER-003", "이미 사용 중인 폴더명입니다."),
    SCRAP_DUPLICATE(HttpStatus.CONFLICT, "SCRAP-001", "이미 스크랩한 게시글입니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "SCRAP-002", "스크랩 내역을 찾을 수 없습니다."),

    // Report
    REPORT_DUPLICATE(HttpStatus.CONFLICT, "REPORT-001", "이미 신고한 게시글/댓글입니다."),

    // Sticker
    STICKER_NOT_FOUND(HttpStatus.NOT_FOUND, "STICKER-001", "스티커를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}