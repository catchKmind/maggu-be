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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostQueryService postQueryService;

    public PostCreateResponse createPost(AppUser writer, PostCreateRequest request) {
        List<String> imageUrls = request.getImageUrls();
        boolean hasImages = imageUrls != null && !imageUrls.isEmpty();

        if (hasImages && imageUrls.size() > PostImage.MAX_IMAGE_COUNT) {
            throw new BusinessException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED);
        }
        if (hasImages && (request.getLatitude() == null || request.getLongitude() == null)) {
            throw new BusinessException(ErrorCode.POST_LOCATION_REQUIRED);
        }

        Point location = toPoint(request.getLatitude(), request.getLongitude());

        Post post = Post.builder()
                .user(writer)
                .slug(generateSlug())
                .content(request.getContent())
                .location(location)
                .tourismContentId(request.getTourismContentId())
                .placeName(request.getPlaceName())
                .category(request.getCategory())
                .build();
        postRepository.save(post);

        if (hasImages) {
            for (int i = 0; i < imageUrls.size(); i++) {
                postImageRepository.save(PostImage.builder()
                        .post(post)
                        .imageUrl(imageUrls.get(i))
                        .sortOrder(i)
                        .build());
            }
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
        if (latitude == null || longitude == null) {
            return null;
        }
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    // 짧고 URL-safe한 슬러그. 실제로는 유니크 제약 위반 시 재시도 로직 추가 권장
    private String generateSlug() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}