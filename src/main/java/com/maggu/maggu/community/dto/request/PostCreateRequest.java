package com.maggu.maggu.community.dto.request;

import com.maggu.maggu.community.entity.PostCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class PostCreateRequest {

    @NotBlank
    @Size(max = 500)
    private String content;

    @NotNull
    private PostCategory category;

    // 0~4장. 순서(sortOrder)는 리스트 순서를 그대로 사용
    @Size(max = 4, message = "사진은 최대 4장까지 첨부할 수 있습니다.")
    private List<String> imageUrls;

    private String placeName;

    // 지도 핀 연동용 관광공사 콘텐츠 ID (선택)
    private String tourismContentId;

    // 위치 출처: EXIF 자동 추출(AUTO) 또는 지도 검색으로 수동 입력(MANUAL). 사진이 있으면 latitude/longitude 필수
    private LocationSource locationSource;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    public enum LocationSource {
        AUTO, MANUAL
    }
}