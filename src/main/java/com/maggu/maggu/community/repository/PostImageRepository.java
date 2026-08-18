package com.maggu.maggu.community.repository;

import com.maggu.maggu.post.entity.Post;
import com.maggu.maggu.community.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostOrderBySortOrderAsc(Post post);

    // 여러 게시글을 한 번에 조회할 때(피드 목록) N+1 방지용
    List<PostImage> findByPostInOrderBySortOrderAsc(List<Post> posts);
}