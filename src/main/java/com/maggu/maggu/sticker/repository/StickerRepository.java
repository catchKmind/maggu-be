package com.maggu.maggu.sticker.repository;

import com.maggu.maggu.sticker.entity.Sticker;
import com.maggu.maggu.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StickerRepository extends JpaRepository<Sticker, Long> {

    List<Sticker> findAllByUserAndDeletedFalse(AppUser user);

    Optional<Sticker> findByIdAndDeletedFalse(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Sticker s SET s.deleted = true, s.user = null WHERE s.user.id = :userId")
    void detachAndMarkDeletedByUserId(@Param("userId") Long userId);
}
