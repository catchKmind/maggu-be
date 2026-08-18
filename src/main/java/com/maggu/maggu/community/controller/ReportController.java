package com.maggu.maggu.community.controller;

import com.maggu.maggu.community.dto.request.ReportCreateRequest;
import com.maggu.maggu.community.dto.response.ReportResponse;
import com.maggu.maggu.community.service.ReportService;
import com.maggu.maggu.global.auth.CurrentUser;
import com.maggu.maggu.user.entity.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "게시글/댓글 신고", description = "혐오/성적/괴롭힘/부적절 사유 중 선택, 5회 누적 시 자동 비공개")
    public ReportResponse report(
            @CurrentUser AppUser user,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        return reportService.report(user, request);
    }
}