package com.maggu.maggu.sticker.repository;

import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StickerRepository extends JpaRepository<Sticker, Long> {
    List<Sticker> findAllByUser(AppUser user);
}