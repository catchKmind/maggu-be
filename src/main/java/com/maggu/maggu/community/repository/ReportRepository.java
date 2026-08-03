package com.maggu.maggu.community.repository;

import com.maggu.maggu.community.entity.Comment;
import com.maggu.maggu.community.entity.Post;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<com.maggu.maggu.community.entity.Report, Long> {

    boolean existsByReporterAndPost(AppUser reporter, Post post);

    boolean existsByReporterAndComment(AppUser reporter, Comment comment);
}