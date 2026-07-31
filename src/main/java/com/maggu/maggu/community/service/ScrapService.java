package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.request.FolderCreateRequest;
import com.maggu.maggu.community.dto.request.ScrapCreateRequest;
import com.maggu.maggu.community.dto.response.FolderCreateResponse;
import com.maggu.maggu.community.dto.response.FolderResponse;
import com.maggu.maggu.community.dto.response.PostSummaryResponse;
import com.maggu.maggu.community.dto.response.ScrapResponse;
import com.maggu.maggu.community.entity.Folder;
import com.maggu.maggu.community.entity.Post;
import com.maggu.maggu.community.entity.Scrap;
import com.maggu.maggu.community.repository.FolderRepository;
import com.maggu.maggu.community.repository.PostRepository;
import com.maggu.maggu.community.repository.ScrapRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScrapService {

    private static final String DEFAULT_FOLDER_NAME = "기본 폴더";

    private final FolderRepository folderRepository;
    private final ScrapRepository scrapRepository;
    private final PostRepository postRepository;

    // 회원가입 시 호출하여 기본 폴더 1개를 만들어 둔다(AuthController/가입 로직에서 호출 필요)
    @Transactional
    public Folder createDefaultFolder(AppUser user) {
        return folderRepository.save(Folder.builder()
                .user(user)
                .name(DEFAULT_FOLDER_NAME)
                .isDefault(true)
                .build());
    }

    @Transactional
    public FolderCreateResponse createFolder(AppUser user, FolderCreateRequest request) {
        if (folderRepository.existsByUserAndName(user, request.getName())) {
            throw new BusinessException(ErrorCode.FOLDER_NAME_DUPLICATE);
        }
        Folder folder = folderRepository.save(Folder.builder()
                .user(user)
                .name(request.getName())
                .isDefault(false)
                .build());

        return FolderCreateResponse.builder()
                .folderId(folder.getId())
                .name(folder.getName())
                .build();
    }

    public List<FolderResponse> getFolders(AppUser user) {
        return folderRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(user).stream()
                .map(this::toFolderResponse)
                .toList();
    }

    @Transactional
    public ScrapResponse scrap(AppUser user, ScrapCreateRequest request) {
        Post post = postRepository.findByIdAndDeletedFalse(request.getPostId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (scrapRepository.existsByUserAndPost(user, post)) {
            throw new BusinessException(ErrorCode.SCRAP_DUPLICATE);
        }

        Folder folder = resolveFolder(user, request.getFolderId());

        scrapRepository.save(Scrap.builder().user(user).post(post).folder(folder).build());
        postRepository.incrementScrapCount(post.getId());

        return ScrapResponse.builder()
                .postId(post.getId())
                .folderId(folder.getId())
                .scrapped(true)
                .build();
    }

    @Transactional
    public ScrapResponse unscrap(AppUser user, Long postId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Scrap scrap = scrapRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_NOT_FOUND));

        scrapRepository.delete(scrap);
        postRepository.decrementScrapCount(post.getId());

        return ScrapResponse.builder()
                .postId(postId)
                .scrapped(false)
                .build();
    }

    @Transactional
    public ScrapResponse moveFolder(AppUser user, Long postId, Long targetFolderId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Scrap scrap = scrapRepository.findByUserAndPost(user, post)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCRAP_NOT_FOUND));

        Folder target = resolveFolder(user, targetFolderId);
        scrap.changeFolder(target);

        return ScrapResponse.builder()
                .postId(postId)
                .folderId(target.getId())
                .scrapped(true)
                .build();
    }

    public Page<PostSummaryResponse> getScrapsInFolder(AppUser user, Long folderId, Pageable pageable) {
        Folder folder = folderRepository.findById(folderId)
                .filter(f -> f.isOwnedBy(user))
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

        Page<Scrap> scraps = scrapRepository.findByUserAndFolderOrderByCreatedAtDesc(user, folder, pageable);
        return scraps.map(scrap -> toSummaryResponse(scrap.getPost()));
    }

    private Folder resolveFolder(AppUser user, Long folderId) {
        if (folderId == null) {
            return folderRepository.findByUserAndIsDefaultTrue(user)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
        }
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
        if (!folder.isOwnedBy(user)) {
            throw new BusinessException(ErrorCode.FOLDER_ACCESS_DENIED);
        }
        return folder;
    }

    private FolderResponse toFolderResponse(Folder folder) {
        return FolderResponse.builder()
                .folderId(folder.getId())
                .name(folder.getName())
                .isDefault(folder.isDefault())
                .build();
    }

    // 스크랩 폴더 목록에서는 댓글 수/스크랩 여부 등 부가 정보 없이 게시글 요약만 필요해 최소 필드만 채운다
    private PostSummaryResponse toSummaryResponse(Post post) {
        return PostSummaryResponse.builder()
                .postId(post.getId())
                .slug(post.getSlug())
                .writerNickname(post.getUser().getNickname())
                .content(post.getContent())
                .placeName(post.getPlaceName())
                .category(post.getCategory())
                .scrapCount(post.getScrapCount())
                .scrappedByMe(true)
                .createdAt(post.getCreatedAt())
                .build();
    }
}