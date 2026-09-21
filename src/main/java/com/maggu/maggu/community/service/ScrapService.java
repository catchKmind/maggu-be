package com.maggu.maggu.community.service;

import com.maggu.maggu.community.dto.request.FolderCreateRequest;
import com.maggu.maggu.community.dto.request.ScrapCreateRequest;
import com.maggu.maggu.community.dto.response.FolderCreateResponse;
import com.maggu.maggu.community.dto.response.FolderResponse;
import com.maggu.maggu.community.dto.response.PageResponse;
import com.maggu.maggu.community.dto.response.PostSummaryResponse;
import com.maggu.maggu.community.dto.response.ScrapResponse;
import com.maggu.maggu.community.entity.Folder;
import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.Scrap;
import com.maggu.maggu.community.repository.FolderRepository;
import com.maggu.maggu.community.repository.ScrapRepository;
import com.maggu.maggu.global.exception.BusinessException;
import com.maggu.maggu.global.exception.ErrorCode;
import com.maggu.maggu.post.repository.PostRepository;
import com.maggu.maggu.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final PostQueryService postQueryService;

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

    @Transactional
    public List<FolderResponse> getFolders(AppUser user) {
        ensureDefaultFolder(user);
        return folderRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(user).stream()
                .map(this::toFolderResponse)
                .toList();
    }

    @Transactional
    public ScrapResponse scrap(AppUser user, ScrapCreateRequest request) {
        Post post = postRepository.findByIdAndDeletedFalse(request.postId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (scrapRepository.existsByUserAndPost(user, post)) {
            throw new BusinessException(ErrorCode.SCRAP_DUPLICATE);
        }

        Folder folder = resolveFolder(user, request.folderId());

        try {
            scrapRepository.saveAndFlush(Scrap.builder().user(user).post(post).folder(folder).build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.SCRAP_DUPLICATE);
        }
        postRepository.incrementScrapCount(post.getId());

        return ScrapResponse.builder()
                .postId(post.getId())
                .folderId(folder.getId())
                .scrapped(true)
                .build();
    }

    @Transactional
    public ScrapResponse unscrap(AppUser user, Long postId) {
        Post post = postRepository.findById(postId)
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
        Post post = postRepository.findById(postId)
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

    public PageResponse<PostSummaryResponse> getScrapsInFolder(AppUser user, Long folderId, int page, int size) {
        Folder folder = folderRepository.findById(folderId)
                .filter(f -> f.isOwnedBy(user))
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<Scrap> scraps = scrapRepository.findByUserAndFolderAndPostDeletedFalseOrderByCreatedAtDesc(user, folder, pageable);
        return postQueryService.toSummaryPageResponse(scraps.map(Scrap::getPost), user);
    }

    private Folder resolveFolder(AppUser user, Long folderId) {
        if (folderId == null || folderId <= 0) {
            return ensureDefaultFolder(user);
        }
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLDER_NOT_FOUND));
        if (!folder.isOwnedBy(user)) {
            throw new BusinessException(ErrorCode.FOLDER_ACCESS_DENIED);
        }
        return folder;
    }

    private Folder ensureDefaultFolder(AppUser user) {
        return folderRepository.findByUserAndIsDefaultTrue(user)
                .orElseGet(() -> createDefaultFolder(user));
    }

    private FolderResponse toFolderResponse(Folder folder) {
        return FolderResponse.builder()
                .folderId(folder.getId())
                .name(folder.getName())
                .isDefault(folder.isDefault())
                .build();
    }
}
