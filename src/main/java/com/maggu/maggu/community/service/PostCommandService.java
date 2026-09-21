package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.request.PostCreateRequest;
import com.maggu.maggu.community.dto.response.PostCreateResponse;
import com.maggu.maggu.community.dto.response.PostDeleteResponse;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.PostImage;
import com.maggu.maggu.community.repository.PostImageRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService {

    private static final int TITLE_MAX_LENGTH = 100;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostQueryService postQueryService;

    public PostCreateResponse createPost(AppUser writer, PostCreateRequest request) {
        List<String> imageUrls = normalizeImageUrls(request.imageUrls());

        if (imageUrls.size() > PostImage.MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }
        if (!imageUrls.isEmpty() && !hasValidCoordinates(request.latitude(), request.longitude())) {
            throw new BusinessException(ErrorCode.POST_LOCATION_REQUIRED);
        }

        Point location = toPoint(request.latitude(), request.longitude());

        Post post = Post.builder()
                .user(writer)
                .slug(generateSlug())
                .title(toTitle(request.content()))
                .content(request.content())
                .location(location)
                .tourismContentId(blankToNull(request.tourismContentId()))
                .placeName(blankToNull(request.placeName()))
                .category(request.category())
                .build();
        postRepository.saveAndFlush(post);

        for (int i = 0; i < imageUrls.size(); i++) {
            postImageRepository.save(PostImage.builder()
                    .post(post)
                    .imageUrl(imageUrls.get(i))
                    .sortOrder(i)
                    .build());
        }

        return PostCreateResponse.builder()
                .postId(post.getId())
                .slug(post.getSlug())
                .build();
    }

    public PostDeleteResponse deletePost(AppUser requester, Long postId) {
        Post post = postQueryService.getActivePost(postId);
        if (!post.isWrittenBy(requester)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }
        post.markDeleted();

        return PostDeleteResponse.builder()
                .postId(postId)
                .deleted(true)
                .build();
    }

    private Point toPoint(Double latitude, Double longitude) {
        if (!hasValidCoordinates(latitude, longitude)) {
            return null;
        }
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private static boolean hasValidCoordinates(Double latitude, Double longitude) {
        // 스웨거 숫자 기본값 (0, 0)은 한국 서비스에서 유효 좌표가 아니므로 미입력으로 본다
        return latitude != null && longitude != null && !(latitude == 0.0 && longitude == 0.0);
    }

    private static List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    private static String toTitle(String content) {
        return content.length() <= TITLE_MAX_LENGTH ? content : content.substring(0, TITLE_MAX_LENGTH);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateSlug() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
